-- Run this script in EACH tenant schema where activity logging is required.
-- Table is purged automatically after 30 days by AuditCleanupScheduler.

CREATE TABLE IF NOT EXISTS `activity_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `activity_time` DATETIME    NOT NULL,
  `action`        VARCHAR(30) NOT NULL,
  `entity_type`   VARCHAR(50) NOT NULL,
  `entity_id`     VARCHAR(50) NULL,
  `description`   TEXT        NULL,
  `performed_by`  VARCHAR(100) NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_al_time`   (`activity_time`),
  INDEX `idx_al_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
