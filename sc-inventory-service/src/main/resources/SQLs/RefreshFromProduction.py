#!/usr/bin/env python3
"""
Refresh Mahavir QA or local MySQL from the latest production backup.

Just run (no arguments needed):
    python3 RefreshFromProduction.py

On macOS you can also double-click the file in Finder — it will open a
Terminal window automatically if one is not already attached.

MinIO credentials are never stored here. The script SSHes into each server
and reads /etc/default/minio at runtime. The local `mc` CLI must be installed
(brew install minio/stable/mc) for MinIO sync to work.
"""

from __future__ import annotations

import getpass
import gzip
import os
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime
from pathlib import Path


# ─── Configuration — edit these as needed ────────────────────────────────────

PROD_SSH            = "root@198.38.86.44"
PROD_SSH_PORT       = 22
PROD_BACKUP_DIR     = "/opt/dbbk"
PROD_BACKUP_PATTERN = "all-databases-*.sql.gz"
PROD_MINIO_URL      = "http://198.38.86.44:9000"

QA_SSH              = "root@163.128.113.28"
QA_SSH_PORT         = 2244
QA_BACKUP_DIR       = "/opt/dbbk/production-refresh"
QA_MINIO_URL        = "http://163.128.113.28:9000"

# Local MinIO (matches application-sc-local-v2.properties).
LOCAL_MINIO_URL         = "http://localhost:9000"
LOCAL_MINIO_ACCESS_KEY  = "minioadmin"
LOCAL_MINIO_SECRET_KEY  = "minioadmin"

# Local MySQL connection (used only when target = local)
# Password must be set via the MYSQL_PWD environment variable before running.
# Example:  MYSQL_PWD='mypassword' python3 RefreshFromProduction.py
LOCAL_MYSQL_HOST    = "localhost"
LOCAL_MYSQL_PORT    = 3306
LOCAL_MYSQL_USER    = "root"

# Local backup cache directory
CACHE_DIR = Path.home() / "Downloads" / "mahavir-production-refresh"

# Schemas to skip in addition to MySQL system schemas.
# Add QA-only or irrelevant schemas here so they are not dropped.
EXTRA_EXCLUDED_SCHEMAS: list[str] = []

# ─── Post-restore configuration ──────────────────────────────────────────────

# CreateViews.sql is in the same folder as this script and is the canonical
# source of views/procedures/indexes (also used to set up tenant schemas
# elsewhere). Run it directly instead of the old post_script.sql copy, which
# had drifted out of sync with CreateViews.sql and was missing newer views
# (Excess-Found / Write-Off types, etc.).
POST_SCRIPT_PATH = Path(__file__).parent / "CreateViews.sql"

# CreateViews.sql starts with a hardcoded "use <schema>;" line for manual
# running. Strip any leading USE statement — this script issues its own
# "USE `<schema>`;" per tenant when running the script in a loop.
LEADING_USE_STATEMENT = re.compile(r"(?im)^\s*use\s+[`\w]+\s*;\s*$")

# Real ERP/CRM tenant schemas (matches common.tenant WHERE is_inventory = 1).
# CreateViews.sql is only run against these — NOT every schema in the prod
# dump. Production also contains unrelated, non-ERP databases (businesspark,
# dhabba, kalpavrish, kanboard_db, riddhisiddhi, school, suncitynx) that have
# none of the expected tables (Product, Warehouse, etc). Running CreateViews.sql
# against one of those throws and used to abort the whole post-processing step
# — including the password reset that runs after it. Keep this list in sync
# with common.tenant.
TENANT_SCHEMAS: set[str] = {
    "anantamsamosharan", "bextension", "bhaavbhumi", "citycenter",
    "dextension", "drgtrdcntr", "iseries", "mhvrtrdcntr", "mnglmcity",
    "smartcity",
}

# After restore all prod user passwords are wiped and replaced with this hash
# so QA logins use a known password instead of leaked prod credentials.
# This bcrypt hash corresponds to the standard QA password.
# Generate a new one with: python3 -c "import bcrypt; print(bcrypt.hashpw(b'yourpassword', bcrypt.gensalt(12)).decode())"
QA_PASSWORD_HASH = "$2a$12$Wz0MZ4k6pkL6w5gviou82OYRPuPJv4/xRAFsTNAs9SIOQYPu.aCv."

# Schema that holds the security_user table (user accounts)
USER_SCHEMA = "common"

# ─── Internal constants ───────────────────────────────────────────────────────

DATABASE_MARKER  = re.compile(r"^-- Current Database: `([^`]+)`")
SAFE_SCHEMA_NAME = re.compile(r"^[A-Za-z0-9_]+$")
SYSTEM_SCHEMAS   = {"information_schema", "mysql", "performance_schema", "sys"}


# ─── Utilities ────────────────────────────────────────────────────────────────

def log(message: str) -> None:
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {message}", flush=True)


def command_text(command: list[str]) -> str:
    return " ".join(shlex.quote(part) for part in command)


def run(
    command: list[str],
    *,
    input_data: str | bytes | None = None,
    env: dict[str, str] | None = None,
    capture_output: bool = False,
    check: bool = True,
    display_command: str | None = None,
) -> subprocess.CompletedProcess:
    log(f"$ {display_command or command_text(command)}")
    text_mode = not isinstance(input_data, bytes)
    return subprocess.run(
        command,
        input=input_data,
        env=env,
        text=text_mode,
        capture_output=capture_output,
        check=check,
    )


def ssh_command(host: str, port: int, remote_command: str) -> list[str]:
    return [
        "ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=15",
        "-p", str(port), host, remote_command,
    ]


def ssh(
    host: str,
    port: int,
    remote_command: str,
    *,
    input_data: str | bytes | None = None,
    capture_output: bool = False,
) -> subprocess.CompletedProcess:
    return run(
        ssh_command(host, port, remote_command),
        input_data=input_data,
        capture_output=capture_output,
    )


def require_commands(commands: list[str]) -> None:
    missing = [c for c in commands if shutil.which(c) is None]
    if missing:
        raise RuntimeError(f"Required command(s) not found: {', '.join(missing)}")


# ─── Interactive prompts ──────────────────────────────────────────────────────

def reopen_in_terminal_if_needed() -> None:
    """
    On macOS, if the script was launched without a TTY (e.g. double-clicked in
    Finder), reopen it inside a new Terminal.app window so interactive prompts
    work correctly.
    """
    if sys.stdin.isatty():
        return
    if sys.platform != "darwin":
        sys.exit("This script requires an interactive terminal. Run it from a shell.")
    script = Path(__file__).resolve()
    apple_script = (
        'tell application "Terminal"\n'
        '    activate\n'
        f'    do script "python3 {shlex.quote(str(script))}; exit"\n'
        'end tell\n'
    )
    subprocess.run(["osascript", "-e", apple_script], check=True)
    sys.exit(0)


def ask(prompt: str, valid: tuple[str, ...]) -> str:
    while True:
        answer = input(prompt).strip().lower()
        if answer in valid:
            return answer
        print(f"  Please enter one of: {', '.join(valid)}")


def prompt_options() -> tuple[str, bool, bool]:
    """
    Ask the user interactively for all options.
    Returns (target, execute, sync_minio).
    """
    print()
    print("╔══════════════════════════════════════════╗")
    print("║   Production → QA / Local Refresh Tool   ║")
    print("╚══════════════════════════════════════════╝")
    print()

    # Target
    print("Where do you want to restore?")
    print("  1) QA server  ({})".format(QA_SSH))
    print("  2) Local MySQL ({})".format(LOCAL_MYSQL_HOST))
    target_choice = ask("\nYour choice [1/2]: ", ("1", "2"))
    target = "qa" if target_choice == "1" else "local"
    print()

    # MinIO sync — available for both QA and local
    print(f"Sync MinIO? (copies objects missing on {target.upper()} from production,")
    print(f"never deletes {target.upper()}-only objects. Requires local `mc` CLI.)")
    ans = ask("Sync MinIO as well? [y/n]: ", ("y", "n", "yes", "no"))
    sync_minio = ans in ("y", "yes")
    print()

    # Preview vs execute
    print("Run mode:")
    print("  1) Preview only — inspect what would happen, no changes made")
    print("  2) Execute      — DROP and restore schemas for real")
    mode_choice = ask("\nYour choice [1/2]: ", ("1", "2"))
    execute = mode_choice == "2"
    print()

    return target, execute, sync_minio


# ─── Backup discovery and download ───────────────────────────────────────────

def discover_latest_backup() -> str:
    command = (
        f"find {shlex.quote(PROD_BACKUP_DIR)} -maxdepth 1 -type f "
        f"-name {shlex.quote(PROD_BACKUP_PATTERN)} "
        r"-printf '%T@ %p\n' | sort -nr | head -1 | cut -d' ' -f2-"
    )
    result = ssh(PROD_SSH, PROD_SSH_PORT, command, capture_output=True)
    backup = result.stdout.strip()
    if not backup:
        raise RuntimeError("No production backup file was found")
    return backup


def download_backup(remote_path: str, cache_dir: Path) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)
    local_path = cache_dir / Path(remote_path).name

    remote_size = ssh(
        PROD_SSH, PROD_SSH_PORT,
        f"stat -c %s {shlex.quote(remote_path)}",
        capture_output=True,
    ).stdout.strip()

    if local_path.exists() and str(local_path.stat().st_size) == remote_size:
        log(f"Using cached backup: {local_path}")
    else:
        temporary_path = local_path.with_suffix(local_path.suffix + ".part")
        temporary_path.unlink(missing_ok=True)
        run(["scp", "-P", str(PROD_SSH_PORT), f"{PROD_SSH}:{remote_path}", str(temporary_path)])
        temporary_path.replace(local_path)

    log("Verifying backup gzip integrity…")
    with gzip.open(local_path, "rb") as f:
        while f.read(1024 * 1024):
            pass
    log("Backup gzip integrity check passed")
    return local_path


# ─── Dump analysis and filtering ─────────────────────────────────────────────

def schemas_in_dump(dump_path: Path) -> list[str]:
    schemas: list[str] = []
    with gzip.open(dump_path, "rt", encoding="utf-8", errors="replace") as f:
        for line in f:
            match = DATABASE_MARKER.match(line)
            if match:
                schema = match.group(1)
                if schema not in schemas:
                    schemas.append(schema)
    return schemas


def validate_schema_names(schemas: list[str]) -> None:
    invalid = [s for s in schemas if not SAFE_SCHEMA_NAME.fullmatch(s)]
    if invalid:
        raise RuntimeError(f"Unsafe schema name(s) in dump: {', '.join(invalid)}")


def filter_dump(source_path: Path, schemas: set[str]) -> Path:
    """
    Write a new gzip dump containing only the requested schemas.
    The filtered file is written atomically (via a .part temp file).
    Schemas are tracked during the write pass — no second scan needed.
    """
    output_path = source_path.with_name(
        source_path.name.removesuffix(".sql.gz") + "-filtered.sql.gz"
    )
    temporary_path = output_path.with_suffix(output_path.suffix + ".part")

    log(f"Creating filtered restore dump: {output_path}")
    included_schemas: set[str] = set()
    include_section = False
    seen_first_marker = False
    current_schema: str | None = None

    with (
        gzip.open(source_path, "rt", encoding="utf-8", errors="replace") as source,
        gzip.open(temporary_path, "wt", encoding="utf-8", compresslevel=6) as target,
    ):
        for line in source:
            match = DATABASE_MARKER.match(line)
            if match:
                seen_first_marker = True
                current_schema = match.group(1)
                include_section = current_schema in schemas
                if include_section:
                    included_schemas.add(current_schema)
            # Global preamble (session SET statements before the first
            # "-- Current Database" marker) must always be kept — later
            # restore statements (e.g. SET TIME_ZONE=@OLD_TIME_ZONE) pair
            # with it and fail with a NULL value if it's dropped.
            if include_section or not seen_first_marker:
                target.write(line)

    temporary_path.replace(output_path)

    missing = schemas - included_schemas
    if missing:
        raise RuntimeError(
            f"Filtered dump is missing expected schemas: {', '.join(sorted(missing))}"
        )
    return output_path


# ─── SQL helpers ─────────────────────────────────────────────────────────────

def reset_sql(schemas: list[str]) -> str:
    statements = ["SET FOREIGN_KEY_CHECKS=0;"]
    statements.extend(f"DROP DATABASE IF EXISTS `{s}`;" for s in schemas)
    statements.append("SET FOREIGN_KEY_CHECKS=1;")
    return "\n".join(statements) + "\n"


# ─── QA restore ──────────────────────────────────────────────────────────────

def fetch_qa_schemas() -> set[str]:
    """Return all non-system schemas currently on the QA server."""
    query = (
        "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA "
        "WHERE SCHEMA_NAME NOT IN "
        "('information_schema','mysql','performance_schema','sys') "
        "ORDER BY SCHEMA_NAME;"
    )
    result = ssh(QA_SSH, QA_SSH_PORT, f"mysql -NBe {shlex.quote(query)}", capture_output=True)
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def copy_backup_to_qa(local_path: Path) -> str:
    ssh(QA_SSH, QA_SSH_PORT, f"mkdir -p {shlex.quote(QA_BACKUP_DIR)}")
    remote_path = f"{QA_BACKUP_DIR}/{local_path.name}"
    run(["scp", "-P", str(QA_SSH_PORT), str(local_path), f"{QA_SSH}:{remote_path}"])
    return remote_path


def load_post_script() -> str:
    if not POST_SCRIPT_PATH.exists():
        raise RuntimeError(f"{POST_SCRIPT_PATH.name} not found at {POST_SCRIPT_PATH}")
    text = POST_SCRIPT_PATH.read_text(encoding="utf-8")
    return LEADING_USE_STATEMENT.sub("", text)


def password_reset_sql() -> str:
    return (
        f"USE `{USER_SCHEMA}`;\n"
        "SET sql_safe_updates=0;\n"
        f"UPDATE security_user SET password='{QA_PASSWORD_HASH}';\n"
        "SET sql_safe_updates=1;\n"
    )


def run_post_scripts_qa(schemas: list[str]) -> None:
    """
    Run CreateViews.sql in each real tenant schema on QA, then reset all user
    passwords. Only schemas in TENANT_SCHEMAS are touched — the prod dump also
    contains unrelated non-ERP databases without the expected tables.
    """
    post_script = load_post_script()
    tenant_schemas = [s for s in schemas if s in TENANT_SCHEMAS]

    log(f"Running CreateViews.sql in {len(tenant_schemas)} tenant schema(s) on QA")
    for schema in tenant_schemas:
        log(f"  CreateViews.sql → {schema}")
        sql = f"USE `{schema}`;\n{post_script}"
        try:
            ssh(QA_SSH, QA_SSH_PORT, "mysql", input_data=sql)
        except Exception as error:
            log(f"  ⚠ CreateViews.sql failed for {schema}: {error} — continuing")

    log("Resetting all user passwords to QA-safe hash")
    ssh(QA_SSH, QA_SSH_PORT, "mysql", input_data=password_reset_sql())
    log("Password reset complete")


def run_post_scripts_local(schemas: list[str], mysql_command: list[str], env: dict[str, str]) -> None:
    """
    Run CreateViews.sql in each real tenant schema on local MySQL, then reset
    passwords. Only schemas in TENANT_SCHEMAS are touched — see run_post_scripts_qa.
    """
    post_script = load_post_script()
    tenant_schemas = [s for s in schemas if s in TENANT_SCHEMAS]

    log(f"Running CreateViews.sql in {len(tenant_schemas)} tenant schema(s) locally")
    for schema in tenant_schemas:
        log(f"  CreateViews.sql → {schema}")
        sql = f"USE `{schema}`;\n{post_script}"
        try:
            run(mysql_command, input_data=sql, env=env)
        except Exception as error:
            log(f"  ⚠ CreateViews.sql failed for {schema}: {error} — continuing")

    log("Resetting all user passwords to QA-safe hash")
    run(mysql_command, input_data=password_reset_sql(), env=env)
    log("Password reset complete")


def verify_qa_restore(expected_schemas: list[str]) -> None:
    query = (
        "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA "
        "WHERE SCHEMA_NAME NOT IN "
        "('information_schema','mysql','performance_schema','sys') "
        "ORDER BY SCHEMA_NAME;"
    )
    result = ssh(QA_SSH, QA_SSH_PORT, f"mysql -NBe {shlex.quote(query)}", capture_output=True)
    restored = {line.strip() for line in result.stdout.splitlines() if line.strip()}
    missing = sorted(set(expected_schemas) - restored)
    if missing:
        raise RuntimeError(f"QA restore verification failed; missing schemas: {', '.join(missing)}")
    log(f"QA restore verified: {len(expected_schemas)} schemas present")


def refresh_qa(source_dump: Path, filtered_dump: Path, prod_schemas: list[str]) -> None:
    log("Copying untouched production backup to QA for traceability")
    source_remote_path = copy_backup_to_qa(source_dump)
    log(f"Production backup copied to QA: {source_remote_path}")

    log("Copying filtered restore artifact to QA")
    filtered_remote_path = copy_backup_to_qa(filtered_dump)

    # Collect schemas to drop: production schemas + any QA-only schemas that
    # are no longer in production (prevents stale schemas accumulating on QA).
    log("Fetching current QA schema list…")
    qa_existing = fetch_qa_schemas()
    qa_only = sorted(qa_existing - set(prod_schemas))
    if qa_only:
        log(f"QA-only schemas that will also be dropped: {', '.join(qa_only)}")
    all_to_drop = prod_schemas + qa_only

    ssh(QA_SSH, QA_SSH_PORT, "systemctl stop tomcat")
    try:
        log(f"Dropping {len(all_to_drop)} QA schemas (prod + QA-only stale)")
        ssh(QA_SSH, QA_SSH_PORT, "mysql", input_data=reset_sql(all_to_drop))

        log("Restoring filtered production data on QA")
        restore_command = (
            "set -o pipefail; "
            f"gzip -dc {shlex.quote(filtered_remote_path)} | "
            "mysql --init-command='SET FOREIGN_KEY_CHECKS=0;'"
        )
        ssh(QA_SSH, QA_SSH_PORT, f"bash -c {shlex.quote(restore_command)}")
    except Exception:
        log("Restore failed — restarting Tomcat before propagating error")
        ssh(QA_SSH, QA_SSH_PORT, "systemctl start tomcat")
        raise

    ssh(QA_SSH, QA_SSH_PORT, "systemctl start tomcat")
    verify_qa_restore(prod_schemas)
    run_post_scripts_qa(prod_schemas)


# ─── Local restore ────────────────────────────────────────────────────────────

def local_mysql_command() -> tuple[list[str], dict[str, str]]:
    mysql_binary = os.environ.get("MYSQL_BIN", "/opt/homebrew/opt/mysql@8.0/bin/mysql")
    if not Path(mysql_binary).exists():
        resolved = shutil.which("mysql")
        if not resolved:
            raise RuntimeError("Local mysql client not found. Set MYSQL_BIN env var.")
        mysql_binary = resolved

    env = os.environ.copy()
    if not env.get("MYSQL_PWD"):
        env["MYSQL_PWD"] = getpass.getpass("Local MySQL password: ")

    command = [
        mysql_binary,
        "-h", os.environ.get("MYSQL_HOST", LOCAL_MYSQL_HOST),
        "-P", os.environ.get("MYSQL_PORT", str(LOCAL_MYSQL_PORT)),
        "-u", os.environ.get("MYSQL_USER", LOCAL_MYSQL_USER),
    ]
    return command, env


def fetch_local_schemas(mysql_command: list[str], env: dict[str, str]) -> set[str]:
    query = (
        "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA "
        "WHERE SCHEMA_NAME NOT IN "
        "('information_schema','mysql','performance_schema','sys');"
    )
    result = subprocess.run(
        mysql_command + ["-NBe", query],
        env=env, capture_output=True, text=True, check=True,
    )
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def verify_local_restore(
    mysql_command: list[str], env: dict[str, str], expected_schemas: list[str]
) -> None:
    restored = fetch_local_schemas(mysql_command, env)
    missing = sorted(set(expected_schemas) - restored)
    if missing:
        raise RuntimeError(f"Local restore verification failed; missing: {', '.join(missing)}")
    log(f"Local restore verified: {len(expected_schemas)} schemas present")


def refresh_local(filtered_dump: Path, prod_schemas: list[str]) -> None:
    mysql_command, env = local_mysql_command()

    log("Fetching current local schema list…")
    local_existing = fetch_local_schemas(mysql_command, env)
    local_only = sorted(local_existing - set(prod_schemas))
    if local_only:
        log(f"Local-only schemas that will also be dropped: {', '.join(local_only)}")
    all_to_drop = prod_schemas + local_only

    log(f"Dropping {len(all_to_drop)} local schemas")
    run(mysql_command, input_data=reset_sql(all_to_drop), env=env)

    log("Restoring filtered production data locally")
    gzip_proc = subprocess.Popen(["gzip", "-dc", str(filtered_dump)], stdout=subprocess.PIPE)
    try:
        mysql_proc = subprocess.run(
            mysql_command + ["--init-command=SET FOREIGN_KEY_CHECKS=0;"],
            stdin=gzip_proc.stdout, env=env, check=False,
        )
    finally:
        if gzip_proc.stdout:
            gzip_proc.stdout.close()

    gzip_status = gzip_proc.wait()
    if gzip_status != 0 or mysql_proc.returncode != 0:
        raise RuntimeError(
            f"Local restore failed (gzip={gzip_status}, mysql={mysql_proc.returncode})"
        )

    verify_local_restore(mysql_command, env, prod_schemas)
    run_post_scripts_local(prod_schemas, mysql_command, env)


# ─── MinIO sync ───────────────────────────────────────────────────────────────

def read_minio_credentials(host: str, port: int) -> tuple[str, str]:
    """
    Read MinIO root credentials from /etc/default/minio on the remote server.
    Credentials are never stored in this script.
    """
    command = (
        "sed -n "
        r"'/^MINIO_ROOT_USER=/p; /^MINIO_ROOT_PASSWORD=/p' "
        "/etc/default/minio"
    )
    result = ssh(host, port, command, capture_output=True)
    values: dict[str, str] = {}
    for line in result.stdout.splitlines():
        key, separator, value = line.partition("=")
        if separator:
            values[key] = value.strip().strip("\"'")
    try:
        return values["MINIO_ROOT_USER"], values["MINIO_ROOT_PASSWORD"]
    except KeyError as error:
        raise RuntimeError(f"MinIO credentials missing on {host}: /etc/default/minio") from error


def sync_minio(execute: bool, target: str) -> None:
    require_commands(["mc"])
    log("Reading MinIO credentials from production server…")
    prod_user, prod_password = read_minio_credentials(PROD_SSH, PROD_SSH_PORT)

    if target == "qa":
        log("Reading MinIO credentials from QA server…")
        dest_user, dest_password = read_minio_credentials(QA_SSH, QA_SSH_PORT)
        dest_url = QA_MINIO_URL
    else:
        dest_user, dest_password = LOCAL_MINIO_ACCESS_KEY, LOCAL_MINIO_SECRET_KEY
        dest_url = LOCAL_MINIO_URL

    dest_alias = f"mahavir-{target}"
    with tempfile.TemporaryDirectory(prefix="mahavir-mc-") as config_dir:
        env = os.environ.copy()
        env["MC_CONFIG_DIR"] = config_dir
        run(
            ["mc", "alias", "set", "mahavir-prod", PROD_MINIO_URL, prod_user, prod_password],
            env=env,
            display_command=f"mc alias set mahavir-prod {PROD_MINIO_URL} <credentials redacted>",
        )
        run(
            ["mc", "alias", "set", dest_alias, dest_url, dest_user, dest_password],
            env=env,
            display_command=f"mc alias set {dest_alias} {dest_url} <credentials redacted>",
        )
        # mc mirror copies objects missing on the destination from prod.
        # Without --overwrite it skips existing destination objects.
        # Without --remove it never deletes destination-only objects.
        mirror_command = ["mc", "mirror", "--quiet", "--summary", "mahavir-prod", dest_alias]
        if not execute:
            mirror_command.insert(2, "--dry-run")
        run(mirror_command, env=env)


# ─── Entry point ─────────────────────────────────────────────────────────────

def main() -> int:
    reopen_in_terminal_if_needed()
    require_commands(["ssh", "scp", "gzip"])

    target, execute, do_sync_minio = prompt_options()

    log("Connecting to production server to find latest backup…")
    latest_remote_backup = discover_latest_backup()
    log(f"Latest production backup: {latest_remote_backup}")

    source_dump = download_backup(latest_remote_backup, CACHE_DIR)

    all_schemas = schemas_in_dump(source_dump)
    excluded = SYSTEM_SCHEMAS | set(EXTRA_EXCLUDED_SCHEMAS)
    restore_schemas = [s for s in all_schemas if s not in excluded]
    validate_schema_names(restore_schemas)

    if not restore_schemas:
        raise RuntimeError("No non-system schemas found in the production dump")

    log(f"Production schemas to restore ({len(restore_schemas)}):")
    for s in restore_schemas:
        log(f"  - {s}")
    log(f"Excluded: {', '.join(sorted(set(all_schemas) - set(restore_schemas)))}")

    filtered_dump = filter_dump(source_dump, set(restore_schemas))
    log(f"Filtered restore artifact: {filtered_dump}")

    if not execute:
        log("Preview complete. No databases were changed.")
        log("Run the script again and choose 'Execute' to perform the refresh.")
        if do_sync_minio:
            log("Running MinIO mirror dry-run…")
            sync_minio(execute=False, target=target)
        return 0

    # Final confirmation before destructive operation
    confirmation = f"REFRESH-{target.upper()}-{len(restore_schemas)}-SCHEMAS"
    print(f"\nThis will DROP {len(restore_schemas)} schemas on {target.upper()} and restore from production.")
    entered = input(f"Type  {confirmation}  to confirm: ").strip()
    if entered != confirmation:
        log("Confirmation did not match — refresh cancelled")
        return 1

    if target == "qa":
        refresh_qa(source_dump, filtered_dump, restore_schemas)
    else:
        refresh_local(filtered_dump, restore_schemas)

    if do_sync_minio:
        log(f"Copying production MinIO objects missing from {target.upper()}…")
        sync_minio(execute=True, target=target)

    log("Refresh completed successfully")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        log("Cancelled by user")
        raise SystemExit(130)
    except Exception as error:
        log(f"FAILED: {error}")
        raise SystemExit(1)
