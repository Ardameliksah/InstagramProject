import pandas as pd
from collections import defaultdict

# =========================================================
# CONFIG
# =========================================================
excel_path = "01_Get_Variables_Info/univariate_selected_features.xlsx"
final_parquet = "outputs/final_test.parquet"
output_parquet = "outputs/final_test_transaction.parquet"

middle_table = "analytics_risk_sb.SME_MNG_RATING_UNQ_FNL_V5"

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
middle_date_col = "DATA_DATE_SP"

skip_existing_final_cols = True


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


def map_features_to_tables(features, table_lower_map, existing_final_cols=None):
    feature_to_table = {}
    missing_features = []

    existing_final_cols_lower = set()
    if existing_final_cols is not None:
        existing_final_cols_lower = {c.lower() for c in existing_final_cols}

    for feat in features:
        feat_lower = feat.lower()

        if feat_lower in existing_final_cols_lower:
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


def force_month_end_from_corrupted_datetime(series):
    s = pd.to_datetime(series, errors="coerce")
    s = s + pd.offsets.MonthEnd(0)
    return s.dt.normalize()


def deduplicate_columns_preserve_order(cols):
    seen = set()
    out = []
    for c in cols:
        if c not in seen:
            seen.add(c)
            out.append(c)
    return out


# =========================================================
# 1) LOAD LOCAL FINAL PARQUET
# =========================================================
final_df = pd.read_parquet(final_parquet)
print("Initial final_df shape:", final_df.shape)

if proposal_id_col not in final_df.columns:
    raise ValueError(f"{proposal_id_col} not found in local final parquet")

final_df[proposal_id_col] = final_df[proposal_id_col].astype(str)


# =========================================================
# 2) READ MIDDLE TABLE AND FIX DATA_DATE_SP -> PRT_DATE
# =========================================================
middle_sdf = spark.sql(f"""
    SELECT
        PROPOSAL_ID,
        PARTY_ID,
        DATA_DATE_SP
    FROM {middle_table}
""")

middle_pdf = middle_sdf.toPandas()

middle_pdf[proposal_id_col] = middle_pdf[proposal_id_col].astype(str)
middle_pdf[party_id_col] = middle_pdf[party_id_col].astype(str)

middle_pdf["DATA_DATE_FIXED"] = force_month_end_from_corrupted_datetime(middle_pdf[middle_date_col])
middle_pdf[prt_date_col] = middle_pdf["DATA_DATE_FIXED"].dt.strftime("%Y%m%d")

middle_pdf = middle_pdf[[proposal_id_col, party_id_col, prt_date_col]].drop_duplicates()

print("Middle mapping shape:", middle_pdf.shape)
print(middle_pdf.head())


# =========================================================
# 3) JOIN LOCAL FINAL_DF WITH MIDDLE TABLE
# =========================================================
final_df = final_df.merge(
    middle_pdf,
    on=proposal_id_col,
    how="left"
)

print("After middle join shape:", final_df.shape)
print("Missing PARTY_ID:", final_df[party_id_col].isna().sum())
print("Missing PRT_DATE:", final_df[prt_date_col].isna().sum())


# =========================================================
# 4) READ ALL REQUESTED FEATURES FROM ALL EXCEL SHEETS
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

print("Total unique requested features from all sheets:", len(all_requested_features_unique))


# =========================================================
# 5) BUILD SPARK TABLE INVENTORY
# =========================================================
required_id_cols = [party_id_col, prt_date_col]
table_cols, table_lower_map = build_table_column_inventory(candidate_tables, required_id_cols)

if not table_cols:
    raise ValueError("No valid Spark tables found with required id cols.")


# =========================================================
# 6) MAP FEATURES TO TABLES
# =========================================================
existing_final_cols = set(final_df.columns) if skip_existing_final_cols else None

feature_to_table, missing_features = map_features_to_tables(
    all_requested_features_unique,
    table_lower_map,
    existing_final_cols=existing_final_cols
)

print("Found features:", len(feature_to_table))
print("Missing features:", len(missing_features))

if missing_features:
    print("Some missing features:")
    print(missing_features[:50])


# =========================================================
# 7) GROUP FEATURES BY TABLE
# =========================================================
table_to_features = defaultdict(list)

for feat, (table, real_col_name) in feature_to_table.items():
    table_to_features[table].append(real_col_name)

for table, cols in table_to_features.items():
    print(f"{table}: {len(cols)} selected features")


# =========================================================
# 8) CREATE UNIQUE JOIN KEYS FROM FINAL_DF
# =========================================================
keys_pdf = final_df[[party_id_col, prt_date_col]].dropna().drop_duplicates().copy()
keys_pdf[party_id_col] = keys_pdf[party_id_col].astype(str)
keys_pdf[prt_date_col] = keys_pdf[prt_date_col].astype(str)

print("Unique key count:", keys_pdf.shape)

keys_sdf = spark.createDataFrame(keys_pdf)


# =========================================================
# 9) READ NEEDED FEATURES TABLE BY TABLE AND JOIN BACK
# =========================================================
for table, feature_cols in table_to_features.items():
    lower_map = table_lower_map[table]

    real_party_col = lower_map[party_id_col.lower()]
    real_prt_col = lower_map[prt_date_col.lower()]

    select_cols = [real_party_col, real_prt_col] + feature_cols
    select_cols = deduplicate_columns_preserve_order(select_cols)

    print("\n=========================================================")
    print(f"Reading from table: {table}")
    print(f"Feature count: {len(feature_cols)}")

    src_sdf = spark.table(table).select(*select_cols)

    joined_sdf = (
        src_sdf.alias("src")
        .join(
            keys_sdf.alias("k"),
            (src_sdf[real_party_col].cast("string") == keys_sdf[party_id_col].cast("string")) &
            (src_sdf[real_prt_col].cast("string") == keys_sdf[prt_date_col].cast("string")),
            how="inner"
        )
        .select(
            src_sdf[real_party_col].cast("string").alias(party_id_col),
            src_sdf[real_prt_col].cast("string").alias(prt_date_col),
            *[src_sdf[c] for c in feature_cols]
        )
    )

    feat_pdf = joined_sdf.toPandas()

    if feat_pdf.empty:
        print(f"[WARN] No matched rows found for {table}")
        continue

    # if same PARTY_ID + PRT_DATE appears multiple times, keep first
    feat_pdf = feat_pdf.drop_duplicates(subset=[party_id_col, prt_date_col])

    print(f"Matched rows from {table}: {feat_pdf.shape}")

    # prevent duplicate column collisions except join keys
    incoming_non_keys = [c for c in feat_pdf.columns if c not in [party_id_col, prt_date_col]]
    duplicate_non_keys = [c for c in incoming_non_keys if c in final_df.columns]

    if duplicate_non_keys:
        print(f"[WARN] These columns already exist in final_df and will be skipped: {duplicate_non_keys[:20]}")
        keep_cols = [party_id_col, prt_date_col] + [c for c in incoming_non_keys if c not in final_df.columns]
        feat_pdf = feat_pdf[keep_cols]

    if feat_pdf.shape[1] <= 2:
        print(f"[WARN] Nothing new to merge from {table}")
        continue

    final_df = final_df.merge(
        feat_pdf,
        on=[party_id_col, prt_date_col],
        how="left"
    )

    print("Current final_df shape:", final_df.shape)


# =========================================================
# 10) SAVE
# =========================================================
final_df.to_parquet(output_parquet, index=False)
print(f"\nSaved final parquet to: {output_parquet}")
print("Final shape:", final_df.shape)
