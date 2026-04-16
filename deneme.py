import pandas as pd
from collections import defaultdict
from pyspark.sql import functions as F

# =========================================================
# CONFIG
# =========================================================
excel_path = "../../../00_Tüm_Kitle_Trend_Analizi/01_Get_Variables_Info/univariate_selected_features.xlsx"
output_parquet = "outputs/final_from_v6_with_univariate_features.parquet"

middle_table = "analytics_risk_sb.SME_MNG_RATING_UNQ_FNL_V6"

candidate_tables = [
    "cr_mva_trx.sme_temporal_trx_government_payments_monthly",
    "cr_mva_trx.sme_temporal_trx_cash_flow_monthly",
    "cr_mva_trx.sme_temporal_trx_credit_monthly",
    "cr_mva_trx.sme_temporal_trx_goods_service_monthly",
    "cr_mva_trx.sme_temporal_trx_income_compensation_monthly",
    "cr_mva_trx.sme_temporal_trx_investment_monthly",
    "cr_mva_trx.sme_temporal_trx_other_monthly",
    "cr_mva_trx.sme_temporal_trx_utilities_monthly",
    "cr_mva_trx.sme_temporal_trx_all_monthly",
]

proposal_id_col = "PROPOSAL_ID"
party_id_col = "PARTY_ID"
prt_date_col = "PRT_DATE"
middle_date_col = "DATA_DATE_TRUE"

# skip variables that already exist in V6
skip_existing_middle_cols = True


# =========================================================
# HELPERS
# =========================================================
def read_selected_features_from_sheet(excel_file, sheet_name):
    df_sheet = pd.read_excel(excel_file, sheet_name=sheet_name, header=None)

    if df_sheet.empty:
        return []

    col0 = df_sheet.iloc[:, 0]

    features = []
    for x in col0.tolist():
        if pd.isna(x):
            continue
        x = str(x).strip()
        if x.lower() in ["", "nan", "selected_features"]:
            continue
        features.append(x)

    seen = set()
    unique_features = []
    for f in features:
        if f not in seen:
            seen.add(f)
            unique_features.append(f)

    return unique_features


def build_table_column_inventory(candidate_tables, required_id_cols):
    table_cols = {}
    table_lower_map = {}

    required_id_cols_lower = [c.lower() for c in required_id_cols]

    for table in candidate_tables:
        try:
            cols = spark.table(table).columns
            cols_lower = [c.lower() for c in cols]

            if all(req in cols_lower for req in required_id_cols_lower):
                table_cols[table] = cols
                table_lower_map[table] = {c.lower(): c for c in cols}
                print(f"[OK] {table}: {len(cols)} columns")
            else:
                print(f"[WARN] Skipping {table} because required id cols do not exist")
        except Exception as e:
            print(f"[ERROR] Could not read {table}: {e}")

    return table_cols, table_lower_map


def map_features_to_tables(features, table_lower_map, existing_cols=None):
    feature_to_table = {}
    missing_features = []

    existing_cols_lower = set()
    if existing_cols is not None:
        existing_cols_lower = {c.lower() for c in existing_cols}

    for feat in features:
        feat_lower = feat.lower()

        if feat_lower in existing_cols_lower:
            continue

        found = False
        for table, lower_map in table_lower_map.items():
            if feat_lower in lower_map:
                feature_to_table[feat] = (table, lower_map[feat_lower])
                found = True
                break

        if not found:
            missing_features.append(feat)

    return feature_to_table, missing_features


def deduplicate_columns_preserve_order(cols):
    seen = set()
    out = []
    for c in cols:
        if c not in seen:
            seen.add(c)
            out.append(c)
    return out


# =========================================================
# 1) READ ALL REQUESTED FEATURES FROM EXCEL
# =========================================================
xls = pd.ExcelFile(excel_path)
sheet_names = xls.sheet_names

all_requested_features = []
for sheet in sheet_names:
    feats = read_selected_features_from_sheet(excel_path, sheet)
    print(f"{sheet}: {len(feats)} features")
    all_requested_features.extend(feats)

seen = set()
all_requested_features_unique = []
for f in all_requested_features:
    if f not in seen:
        seen.add(f)
        all_requested_features_unique.append(f)

print("Total unique requested features:", len(all_requested_features_unique))


# =========================================================
# 2) READ FULL V6 TABLE AS BASE
# =========================================================
base_sdf = spark.table(middle_table)

# Cast ids
if proposal_id_col in base_sdf.columns:
    base_sdf = base_sdf.withColumn(proposal_id_col, F.col(proposal_id_col).cast("string"))

if party_id_col in base_sdf.columns:
    base_sdf = base_sdf.withColumn(party_id_col, F.col(party_id_col).cast("string"))
else:
    raise ValueError(f"{party_id_col} not found in {middle_table}")

if middle_date_col not in base_sdf.columns:
    raise ValueError(f"{middle_date_col} not found in {middle_table}")

# recreate/fix PRT_DATE from DATA_DATE_TRUE
base_sdf = base_sdf.withColumn(
    prt_date_col,
    F.date_format(F.last_day(F.to_date(F.col(middle_date_col))), "yyyyMMdd")
)

# optional: if one proposal should appear once
# base_sdf = base_sdf.dropDuplicates([proposal_id_col])

print(f"Base V6 column count: {len(base_sdf.columns)}")


# =========================================================
# 3) BUILD SOURCE TABLE INVENTORY
# =========================================================
required_id_cols = [party_id_col, prt_date_col]
table_cols, table_lower_map = build_table_column_inventory(candidate_tables, required_id_cols)

if not table_cols:
    raise ValueError("No valid Spark tables found with required id cols.")


# =========================================================
# 4) MAP FEATURES TO TABLES
# =========================================================
existing_base_cols = base_sdf.columns if skip_existing_middle_cols else None

feature_to_table, missing_features = map_features_to_tables(
    all_requested_features_unique,
    table_lower_map,
    existing_cols=existing_base_cols
)

print("Found features:", len(feature_to_table))
print("Missing features:", len(missing_features))

if missing_features:
    print("Some missing features:")
    print(missing_features[:50])


# =========================================================
# 5) GROUP FEATURES BY TABLE
# =========================================================
table_to_features = defaultdict(list)

for feat, (table, real_col_name) in feature_to_table.items():
    table_to_features[table].append(real_col_name)

for table, cols in table_to_features.items():
    print(f"{table}: {len(cols)} selected features")


# =========================================================
# 6) LEFT JOIN SELECTED FEATURES ONTO FULL V6
# =========================================================
final_sdf = base_sdf

for table, feature_cols in table_to_features.items():
    lower_map = table_lower_map[table]

    real_party_col = lower_map[party_id_col.lower()]
    real_prt_col = lower_map[prt_date_col.lower()]

    select_cols = [real_party_col, real_prt_col] + feature_cols
    select_cols = deduplicate_columns_preserve_order(select_cols)

    print("\n=========================================================")
    print(f"Joining from table: {table}")
    print(f"Feature count: {len(feature_cols)}")

    src_sdf = (
        spark.table(table)
        .select(*select_cols)
        .withColumn(party_id_col, F.col(real_party_col).cast("string"))
        .withColumn(prt_date_col, F.col(real_prt_col).cast("string"))
        .select(
            party_id_col,
            prt_date_col,
            *[F.col(c) for c in feature_cols]
        )
        .dropDuplicates([party_id_col, prt_date_col])
    )

    existing_cols = set(final_sdf.columns)
    incoming_feature_cols = [c for c in feature_cols if c not in existing_cols]

    if not incoming_feature_cols:
        print(f"[WARN] Nothing new to join from {table}")
        continue

    src_sdf = src_sdf.select(
        party_id_col,
        prt_date_col,
        *incoming_feature_cols
    )

    final_sdf = final_sdf.join(
        src_sdf,
        on=[party_id_col, prt_date_col],
        how="left"
    )

    print(f"Current column count: {len(final_sdf.columns)}")


# =========================================================
# 7) SAVE ONE FINAL PARQUET
# =========================================================
final_sdf.write.mode("overwrite").parquet(output_parquet)

print(f"\nSaved final parquet to: {output_parquet}")
print(f"Final column count: {len(final_sdf.columns)}")
