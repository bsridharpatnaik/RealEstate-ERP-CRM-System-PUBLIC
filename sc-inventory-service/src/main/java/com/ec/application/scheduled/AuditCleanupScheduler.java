package com.ec.application.scheduled;

import com.ec.application.config.SchemaConfig;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AuditCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditCleanupScheduler.class);
    private static final int RETAIN_DAYS = 30;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaConfig schemaConfig;

    // Runs daily at 3:30 AM IST
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Kolkata")
    public void purgeOldAuditRecords() {
        long cutoffEpochMs = Instant.now().minus(RETAIN_DAYS, ChronoUnit.DAYS).toEpochMilli();
        log.info("Starting daily audit purge. Removing records older than {} days.", RETAIN_DAYS);

        for (String schema : schemaConfig.getSchemaList()) {
            ThreadLocalStorage.setTenantName(schema);
            try {
                purgeForSchema(cutoffEpochMs);
            } catch (Exception e) {
                log.error("Audit purge failed for schema: {}", schema, e);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        }

        log.info("Daily audit purge complete.");
    }

    private void purgeForSchema(long cutoffEpochMs) {
        List<String> audTables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE '%\\_AUD'",
            String.class
        );

        if (audTables.isEmpty()) return;

        String currentSchema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        log.info("Purging {} AUD tables in schema: {}", audTables.size(), currentSchema);

        int totalAudRows = 0;
        for (String table : audTables) {
            try {
                int deleted = jdbcTemplate.update(
                    "DELETE FROM `" + table + "` WHERE REV IN " +
                    "(SELECT REV FROM REVINFO WHERE REVTSTMP < ?)",
                    cutoffEpochMs
                );
                if (deleted > 0) {
                    log.info("Deleted {} rows from {}", deleted, table);
                    totalAudRows += deleted;
                }
            } catch (Exception e) {
                log.error("Failed purging table: {}", table, e);
            }
        }

        int revDeleted = jdbcTemplate.update(
            "DELETE FROM REVINFO WHERE REVTSTMP < ?", cutoffEpochMs
        );
        log.info("Schema purge done. AUD rows deleted: {}, REVINFO rows deleted: {}", totalAudRows, revDeleted);
    }
}
