import os
import glob
import subprocess
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm

# =========================
# 🔧 CONFIGURATION
# =========================

CREATE_VIEWS_SQL_FILE = "/Users/bsridharpatnaik/GitHub/RealEstate-ERP-CRM-System/sc-inventory-service/src/main/resources/SQLs/CreateViews.sql"

CREATE_VIEWS_SCHEMAS = [
    "drgtrdcntr",
    "bhaavbhumi",
    "citycenter",
    "mnglmcity",
    "mhvrtrdcntr",
    "iseries",
    "smartcity",
    "dextension",
    "anantamsamosharan"
]
MYSQL_BIN = "/opt/homebrew/opt/mysql@8.0/bin/mysql"
MC_BIN = "/opt/homebrew/bin/mc"
RCLONE_BIN = "/opt/homebrew/bin/rclone"
DOWNLOAD_DIR = os.path.expanduser("~/Downloads")
FILE_PATTERN = "all-databases-*.sql"

MYSQL_HOST = "localhost"
MYSQL_USER = "root"
MYSQL_PASSWORD = "REDACTED"
MYSQL_PORT = "3306"

MAX_WORKERS = 4

SCHEMAS = [
    "drgtrdcntr",
    "bhaavbhumi",
    "citycenter",
    "mnglmcity",
    "mhvrtrdcntr",
    "iseries",
    "smartcity",
    "masterschema",
    "anantamsamosharan"
]

# MinIO sync config
MINIO_GDRIVE_SOURCE  = "gdrive:Suncity/DBBackup/files-backup"
MINIO_LOCAL_REMOTE   = "minio-local:"
MINIO_LOCAL_MC_ALIAS = "minio-local"
MINIO_LOCAL_URL      = "http://localhost:9000"
MINIO_LOCAL_USER     = "minioadmin"
MINIO_LOCAL_PASS     = "minioadmin"
MINIO_SYNC_ENABLED   = True

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
POST_SQL_FILE = os.path.join(SCRIPT_DIR, "post_script.sql")

# =========================
# 🧠 HELPERS
# =========================

def log(msg):
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")


def get_latest_sql_file():
    files = glob.glob(os.path.join(DOWNLOAD_DIR, FILE_PATTERN))
    if not files:
        raise Exception("No matching SQL files found")
    return max(files, key=os.path.getmtime)


def run_mysql(file=None, sql=None, database=None):
    cmd = [MYSQL_BIN, f"-h{MYSQL_HOST}", f"-P{MYSQL_PORT}", f"-u{MYSQL_USER}"]

    if database:
        cmd.append(database)

    env = os.environ.copy()
    env["MYSQL_PWD"] = MYSQL_PASSWORD

    try:
        if file:
            with open(file, "rb") as f:
                subprocess.run(cmd, stdin=f, check=True, env=env)
        elif sql:
            subprocess.run(cmd, input=sql.encode(), check=True, env=env)

    except subprocess.CalledProcessError as e:
        raise Exception(f"MySQL error: {e}")

def run_cmd(cmd, desc=""):
    log(f"Running: {desc or ' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Command failed: {result.stderr.strip()}")
    return result.stdout.strip()


# =========================
# ⚙️ CORE STEPS
# =========================

def reset_schemas():
    log("Dropping & creating schemas...")
    for schema in SCHEMAS:
        run_mysql(sql=f"DROP DATABASE IF EXISTS {schema};")
        run_mysql(sql=f"CREATE DATABASE {schema};")
    log("Schemas ready")


def import_dump(dump_file):
    log("Importing dump (single run)...")
    run_mysql(file=dump_file)
    log("Dump import completed")


def post_process_schema(schema):
    run_mysql(database=schema, file=POST_SQL_FILE)
    return schema


def ensure_minio_buckets():
    log("Ensuring local MinIO buckets exist...")
    run_cmd([
        MC_BIN, "alias", "set", MINIO_LOCAL_MC_ALIAS,
        MINIO_LOCAL_URL, MINIO_LOCAL_USER, MINIO_LOCAL_PASS
    ], "mc alias set")

    for schema in SCHEMAS:
        result = subprocess.run(
            [MC_BIN, "ls", f"{MINIO_LOCAL_MC_ALIAS}/{schema}"],
            capture_output=True
        )
        if result.returncode != 0:
            run_cmd([MC_BIN, "mb", f"{MINIO_LOCAL_MC_ALIAS}/{schema}"], f"create bucket {schema}")
            log(f"  Created bucket: {schema}")
        else:
            log(f"  Bucket exists: {schema}")


def sync_minio_files():
    log("Syncing MinIO files from Google Drive → local MinIO...")
    log(f"  Source : {MINIO_GDRIVE_SOURCE}")
    log(f"  Dest   : {MINIO_LOCAL_REMOTE}")

    cmd = [
        RCLONE_BIN, "copy",
        MINIO_GDRIVE_SOURCE,
        MINIO_LOCAL_REMOTE,
        "--transfers", "4",
        "--checkers", "8",
        "--progress",
        "--log-level", "INFO"
    ]

    result = subprocess.run(cmd)
    if result.returncode != 0:
        raise Exception("rclone copy failed — check rclone config and Google Drive access")

    log("MinIO sync completed")


# =========================
# 🚀 MAIN
# =========================

def main():
    log("==== START ====")

    dump_file = get_latest_sql_file()
    log(f"Using dump: {dump_file}")

    if not os.path.exists(POST_SQL_FILE):
        raise Exception(f"post_script.sql not found at {POST_SQL_FILE}")

    # Step 1: Reset schemas
    reset_schemas()

    # Step 2: Import dump
    import_dump(dump_file)

    # Step 3: Post SQL in parallel
    log("Running post SQL in parallel...")
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {executor.submit(post_process_schema, s): s for s in SCHEMAS}
        for future in tqdm(as_completed(futures), total=len(SCHEMAS), desc="Post Processing"):
            schema = futures[future]
            try:
                future.result()
                log(f"✅ Done: {schema}")
            except Exception as e:
                log(f"❌ Failed: {schema} → {e}")

    # Step 4: Sync MinIO files
    if MINIO_SYNC_ENABLED:
        ensure_minio_buckets()
        sync_minio_files()
    else:
        log("MinIO sync skipped (MINIO_SYNC_ENABLED=False)")

    # Step 5: Execute CreateViews.sql for all schemas
    execute_multi_schema_sql(
        CREATE_VIEWS_SQL_FILE,
        CREATE_VIEWS_SCHEMAS
    )
    log("==== COMPLETED SUCCESSFULLY ====")

def execute_multi_schema_sql(sql_file, schemas):
    log(f"Executing SQL file across {len(schemas)} schemas...")
    log(f"SQL File: {sql_file}")

    if not os.path.exists(sql_file):
        raise Exception(f"SQL file not found: {sql_file}")

    for schema in schemas:
        log(f"▶ Running SQL for schema: {schema}")

        try:
            # mysql <schema> < file.sql
            run_mysql(database=schema, file=sql_file)

            log(f"✅ Completed: {schema}")

        except Exception as e:
            log(f"❌ Failed for {schema}: {e}")
            raise

    log("Multi-schema SQL execution completed")

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"❌ SCRIPT FAILED: {e}")