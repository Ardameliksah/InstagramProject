import pandas as pd
from collections import defaultdict

# =========================================================
# CONFIG
# =========================================================

excel_path = r"C:\path\to\selected_features.xlsx"
final_parquet_path = r"C:\path\to\final_data.parquet"
output_parquet_path = r"C:\path\to\final_data_joined.parquet"

# join key
id_cols = ["proposal_id"]   # örn ["proposal_id"] veya ["proposal_id", "ref_date"]

# Spark'ta taranacak tablolar
candidate_tables = [
    "schema.table1",
    "schema.table2",
    "schema.table3",
    # ...
]

# Aynı feature birden fazla tabloda varsa ilk bulunanı al
take_first_match_only = True

# final parquet'te zaten varsa tekrar ekleme
skip_existing_final_cols = True


# =========================================================
# HELPERS
# =========================================================

def read_selected_features_from_sheet(excel_file, sheet_name):
    """
    Sheetin ilk kolonunda 'selected features' başlığı altında feature listesi olduğunu varsayar.
    """
    df_sheet = pd.read_excel(excel_file, sheet_name=sheet_name, header=None)

    if df_sheet.empty:
        return []

    col0 = df_sheet.iloc[:, 0].astype(str).str.strip()

    features = [
        x for x in col0.tolist()
        if x
        and x.lower() != "nan"
        and x.lower() != "selected features"
    ]

    # unique, order preserved
    seen = set()
    unique_features = []
    for f in features:
        if f not in seen:
            seen.add(f)
            unique_features.append(f)

    return unique_features


def build_table_column_inventory(candidate_tables, id_cols):
    """
    Spark tablolarının kolon envanterini çıkarır.
    """
    table_cols = {}
    lower_map = {}

    for table in candidate_tables:
        try:
            cols = spark.table(table).columns
            table_cols[table] = cols
            lower_map[table] = {c.lower(): c for c in cols}
            print(f"[OK] {table}: {len(cols)} columns")
        except Exception as e:
            print(f"[ERROR] Could not read {table}: {e}")

    # id kolonları hangi tablolarda var
    valid_tables = []
    for table, cols in table_cols.items():
        if all(c in cols for c in id_cols):
            valid_tables.append(table)
        else:
            print(f"[WARN] Skipping {table} because not all id_cols exist")

    return {t: table_cols[t] for t in valid_tables}, {t: lower_map[t] for t in valid_tables}


def map_features_to_tables(features, table_lower_map, final_existing_cols=None, take_first_match_only=True):
    """
    Her feature'ın hangi tabloda bulunduğunu map eder.
    """
    feature_to_tables = defaultdict(list)

    for feat in features:
        if final_existing_cols is not None and feat in final_existing_cols:
            continue

        feat_lower = feat.lower()

        for table, lower_map in table_lower_map.items():
            if feat_lower in lower_map:
                real_col_name = lower_map[feat_lower]
                feature_to_tables[feat].append((table, real_col_name))

                if take_first_match_only:
                    break

    return feature_to_tables


# =========================================================
# LOAD LOCAL FINAL PARQUET
# =========================================================

final_df = pd.read_parquet(final_parquet_path)
print("Initial final_df shape:", final_df.shape)

missing_ids = [c for c in id_cols if c not in final_df.columns]
if missing_ids:
    raise ValueError(f"id_cols missing in final parquet: {missing_ids}")


# =========================================================
# BUILD TABLE INVENTORY ONCE
# =========================================================

table_cols, table_lower_map = build_table_column_inventory(candidate_tables, id_cols)

if not table_cols:
    raise ValueError("No valid Spark tables found with required id_cols.")


# =========================================================
# READ EXCEL AND COLLECT ALL FEATURES FROM ALL SHEETS
# =========================================================

xls = pd.ExcelFile(excel_path)
sheet_names = xls.sheet_names

all_requested_features = []
for sheet in sheet_names:
    feats = read_selected_features_from_sheet(excel_path, sheet)
    print(f"{sheet}: {len(feats)} features")
    all_requested_features.extend(feats)

# unique preserve order
seen = set()
all_requested_features_unique = []
for f in all_requested_features:
    if f not in seen:
        seen.add(f)
        all_requested_features_unique.append(f)

print("Total unique requested features from all sheets:", len(all_requested_features_unique))


# =========================================================
# MAP FEATURES TO TABLES
# =========================================================

final_existing_cols = set(final_df.columns) if skip_existing_final_cols else None

feature_to_tables = map_features_to_tables(
    all_requested_features_unique,
    table_lower_map,
    final_existing_cols=final_existing_cols,
    take_first_match_only=take_first_match_only
)

found_features = set(feature_to_tables.keys())
missing_features = [f for f in all_requested_features_unique if f not in found_features]

print("Found features:", len(found_features))
print("Missing features:", len(missing_features))

if missing_features:
    print("Some missing features:")
    print(missing_features[:50])


# =========================================================
# REVERSE MAP: TABLE -> COLS TO TAKE
# =========================================================

table_to_cols = defaultdict(list)

for feat, matches in feature_to_tables.items():
    for table, real_col_name in matches:
        table_to_cols[table].append(real_col_name)

# unique each table col list
for table in table_to_cols:
    seen = set()
    uniq = []
    for c in table_to_cols[table]:
        if c not in seen:
            seen.add(c)
            uniq.append(c)
    table_to_cols[table] = uniq


# =========================================================
# EXTRACT FROM SPARK AND MERGE INTO LOCAL FINAL DF
# =========================================================

for table, cols in table_to_cols.items():
    print("\n" + "=" * 80)
    print(f"Processing table: {table}")
    print(f"Selected feature count from this table: {len(cols)}")

    if len(cols) == 0:
        continue

    cols_to_take = id_cols + cols

    sdf = spark.table(table).select(*cols_to_take).dropDuplicates(id_cols)
    local_part = sdf.toPandas()

    print("local_part shape:", local_part.shape)

    before_shape = final_df.shape
    final_df = final_df.merge(local_part, on=id_cols, how="left")
    after_shape = final_df.shape

    print(f"Merged {table}. Shape before: {before_shape}, after: {after_shape}")


# =========================================================
# SAVE OUTPUT
# =========================================================

final_df.to_parquet(output_parquet_path, index=False)

print("\nDone.")
print("Final shape:", final_df.shape)
print("Saved to:", output_parquet_path)


# =========================================================
# OPTIONAL: MISSING FEATURE REPORT
# =========================================================

missing_report_path = output_parquet_path.replace(".parquet", "_missing_features.xlsx")
pd.DataFrame({"missing_feature": missing_features}).to_excel(missing_report_path, index=False)
print("Missing feature report saved to:", missing_report_path)