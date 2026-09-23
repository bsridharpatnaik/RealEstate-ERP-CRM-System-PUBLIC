# MinIO Setup Guide

File storage migration from MySQL BLOBs to MinIO object storage.  
Covers: fresh server setup, Spring Boot config, data migration, nightly backup, and local dev sync.

---

## Architecture Overview

- MinIO runs on the **same VPS as Tomcat** (port 9000 internal, never exposed publicly)
- Each tenant schema gets its own bucket: `bhaavbhumi`, `drgtrdcntr`, etc.
- PO and Indent attachments go to the `masterschema` bucket (they are global entities)
- All other attachments go to their respective tenant bucket
- `files` table stores metadata (`id`, `fileName`, `fileType`, `bucketName`) — MySQL dump backs up metadata; MinIO backup backs up actual file bytes

---

## Schemas Reference

| Environment | Schemas |
|-------------|---------|
| QA          | drgtrdcntr, bhaavbhumi, citycenter, mnglmcity, mhvrtrdcntr, iseries, masterschema |
| Prod        | drgtrdcntr, bhaavbhumi, citycenter, mnglmcity, mhvrtrdcntr, iseries, smartcity, masterschema |

---

## 1. Server Setup (Ubuntu 20.04)

### 1.1 Download MinIO binary

```bash
sudo wget https://dl.min.io/server/minio/release/linux-amd64/minio -O /usr/local/bin/minio
sudo chmod +x /usr/local/bin/minio
minio --version
```

### 1.2 Create system user and data directory

```bash
sudo useradd -r -s /sbin/nologin minio-user
sudo mkdir -p /data/minio
sudo chown minio-user:minio-user /data/minio
```

### 1.3 Create environment config

```bash
sudo nano /etc/default/minio
```

```bash
MINIO_VOLUMES="/data/minio"
MINIO_OPTS="--address :9000 --console-address :9001"
MINIO_ROOT_USER=your_admin_username
MINIO_ROOT_PASSWORD=your_strong_password
```

```bash
sudo chmod 600 /etc/default/minio
```

> Use different credentials per environment (QA vs Prod). Never use default `minioadmin/minioadmin` on any server.

### 1.4 Create systemd service

```bash
sudo nano /etc/systemd/system/minio.service
```

```ini
[Unit]
Description=MinIO Object Storage
After=network.target

[Service]
User=minio-user
Group=minio-user
EnvironmentFile=/etc/default/minio
ExecStart=/usr/local/bin/minio server $MINIO_VOLUMES $MINIO_OPTS
Restart=always
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

### 1.5 Start and enable

```bash
sudo systemctl daemon-reload
sudo systemctl enable minio
sudo systemctl start minio
sudo systemctl status minio
```

Verify it survives reboots:

```bash
sudo systemctl is-enabled minio   # expected: enabled
sudo systemctl is-active minio    # expected: active
```

### 1.6 Install mc CLI and create buckets

```bash
sudo wget https://dl.min.io/client/mc/release/linux-amd64/mc -O /usr/local/bin/mc
sudo chmod +x /usr/local/bin/mc

mc alias set myserver http://localhost:9000 your_admin_username your_strong_password

# QA buckets
mc mb myserver/masterschema
mc mb myserver/drgtrdcntr
mc mb myserver/bhaavbhumi
mc mb myserver/citycenter
mc mb myserver/mnglmcity
mc mb myserver/mhvrtrdcntr
mc mb myserver/iseries

# Prod only — add this too
mc mb myserver/smartcity
```

### 1.7 Block external access

MinIO must not be accessible from the internet. Only Tomcat (running on same server) needs access.

```bash
sudo ufw deny 9000
sudo ufw deny 9001
sudo ufw status   # confirm 9000/9001 are denied
```

---

## 2. Spring Boot Configuration

Add MinIO properties to the environment-specific properties file.

**QA** — `application-sc-qa-v2.properties`:
```properties
minio.url=http://localhost:9000
minio.accessKey=your_qa_username
minio.secretKey=your_qa_password
```

**Prod** — `application-sc-prod-v2.properties`:
```properties
minio.url=http://localhost:9000
minio.accessKey=your_prod_username
minio.secretKey=your_prod_password
```

**Local (Mac)** — `application-sc-local-v2.properties`:
```properties
minio.url=http://localhost:9000
minio.accessKey=minioadmin
minio.secretKey=minioadmin
```

---

## 3. Deploy WAR

Build and deploy the WAR to Tomcat as usual. Confirm Tomcat starts without errors before proceeding to migration.

---

## 4. Run Data Migration

Migration moves existing DB blob files into MinIO. It is safe to re-run — already-migrated files are skipped automatically (`WHERE data IS NOT NULL`).

### 4.1 Trigger migration

```bash
curl -X POST http://localhost:8080/sc-inventory-service-v2/master-file/admin/migrate-files/start
```

### 4.2 Monitor progress

```bash
watch -n 30 'curl -s http://localhost:8080/sc-inventory-service-v2/master-file/admin/migrate-files/status | python3 -m json.tool'
```

Expected final output:

```json
{
  "running": false,
  "completed": true,
  "migrated": 5865,
  "skipped": 0,
  "errors": 0
}
```

### 4.3 If errors occur

Check Tomcat logs, fix the issue, then re-trigger. Migration resumes from where it left off.

### 4.4 Verify DB blobs are cleared

After `errors=0`, confirm blobs are gone (optional — migration already sets `data = NULL` per file):

```sql
SELECT COUNT(*) FROM `masterschema`.files WHERE data IS NOT NULL;
SELECT COUNT(*) FROM `bhaavbhumi`.files WHERE data IS NOT NULL;
-- repeat per schema; all should return 0
```

---

## 5. Nightly Backup to Google Drive

Uses rclone (already installed for MySQL dump backups).

### 5.1 Add MinIO as rclone remote

Append to `/root/.config/rclone/rclone.conf`:

```ini
[minio-prod]
type = s3
provider = Minio
access_key_id = your_prod_username
secret_access_key = your_strong_password
endpoint = http://localhost:9000
```

### 5.2 Create backup script

```bash
sudo nano /usr/local/bin/minio-backup.sh
```

```bash
#!/bin/bash

LOG=/var/log/minio-backup.log
GDRIVE_DEST="gdrive:backups/minio-prod"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$DATE] MinIO backup started" >> $LOG

rclone copy minio-prod: "$GDRIVE_DEST" \
    --transfers 4 \
    --checkers 8 \
    --log-file "$LOG" \
    --log-level INFO

EXIT=$?
if [ $EXIT -eq 0 ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup completed successfully" >> $LOG
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup FAILED with exit code $EXIT" >> $LOG
fi
```

```bash
sudo chmod +x /usr/local/bin/minio-backup.sh
```

### 5.3 Schedule via cron

```bash
sudo crontab -e
```

```cron
# MySQL dump (existing)
0 2 * * * /usr/local/bin/mysql-backup.sh

# MinIO backup — runs daily at 3am
0 3 * * * /usr/local/bin/minio-backup.sh
```

`rclone copy` is incremental — only new files uploaded each night. Existing files are skipped.

---

## 6. Restore from Google Drive

Full restore if server is rebuilt from scratch:

```bash
# 1. Install MinIO fresh (steps 1.1–1.7 above)

# 2. Restore MySQL dump (existing process)

# 3. Restore MinIO files from Google Drive
rclone copy gdrive:backups/minio-prod minio-prod:

# 4. Start Tomcat — app works immediately
```

Both backups are required together:

| Backup | Contains | Without it |
|--------|----------|------------|
| MySQL dump | `files` table — file metadata + `bucketName` | app can't locate files |
| MinIO rclone | Actual file bytes | metadata exists but downloads fail |

---

## 7. Access MinIO Console (Browser)

Port 9001 is internal only. Use SSH tunnel to access from your laptop:

```bash
ssh -L 9001:localhost:9001 your-user@your-server-ip
```

Then open `http://localhost:9001` in browser.

---

## 8. Local Dev Setup (Mac)

### 8.1 Install MinIO via Homebrew

```bash
brew install minio/stable/minio
minio server ~/data/minio --console-address :9001
```

### 8.2 Create local buckets

```bash
brew install minio/stable/mc
mc alias set local http://localhost:9000 minioadmin minioadmin

mc mb local/masterschema
mc mb local/drgtrdcntr
mc mb local/bhaavbhumi
mc mb local/citycenter
mc mb local/mnglmcity
mc mb local/mhvrtrdcntr
mc mb local/iseries
mc mb local/smartcity
```

### 8.3 Sync prod files to local

Uses the Python script at the root of this repo. Configure `MINIO_GDRIVE_SOURCE` to point to the correct Google Drive backup path, then run:

```bash
python3 setup-local.py
```

The script:
1. Resets all local schemas from latest SQL dump
2. Runs post-processing SQL
3. Creates missing MinIO buckets
4. Copies only non-existing files from Google Drive → local MinIO

---

## Deployment Checklist

### New Server Setup

- [ ] MinIO binary downloaded and executable
- [ ] `minio-user` system user created
- [ ] `/data/minio` directory created with correct ownership
- [ ] `/etc/default/minio` created with custom credentials (chmod 600)
- [ ] systemd service created and enabled
- [ ] MinIO status: `active (running)`
- [ ] All buckets created via `mc mb`
- [ ] Ports 9000/9001 blocked from external access
- [ ] Properties file updated with MinIO config
- [ ] WAR deployed and Tomcat started cleanly
- [ ] Migration triggered and completed with `errors=0`
- [ ] DB blobs verified cleared (`COUNT(*) = 0`)
- [ ] Nightly backup cron configured
- [ ] Test: upload attachment in UI → visible in MinIO console
- [ ] Test: download from PO details tab works
- [ ] Test: download from Indent details tab works

---

## Troubleshooting

**MinIO not starting**
```bash
sudo journalctl -u minio -n 50
```

**Migration errors**
- Check Tomcat logs: `catalina.out`
- Common cause: MinIO not running when migration was triggered
- Fix: start MinIO, re-trigger migration (safe to re-run)

**Download failing — "File not found"**
- PO/Indent files are in `masterschema` bucket
- Other files are in their tenant bucket
- Check `bucketName` column in `files` table matches the bucket that exists in MinIO

**Cannot determine target DataSource**
- Background migration thread lost tenant context
- Fixed in `FileMigrationService` — `ThreadLocalStorage.setTenantName(schema)` is set per schema loop

**rclone backup failing**
```bash
rclone listremotes          # verify remote names
rclone ls minio-prod:       # verify MinIO connection
rclone ls gdrive:backups/   # verify Google Drive access
cat /var/log/minio-backup.log
```
