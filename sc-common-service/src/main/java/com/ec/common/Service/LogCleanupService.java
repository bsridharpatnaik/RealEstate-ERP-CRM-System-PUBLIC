package com.ec.common.Service;

import com.ec.common.Repository.ApiLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogCleanupService {

    @Autowired
    private ApiLogRepository apiLogRepository;

    private final Logger logger = LoggerFactory.getLogger(LogCleanupService.class);
    // This method will run every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(20);
        apiLogRepository.deleteByTimestampBefore(cutoffDate);
        logger.info("Cleanup completed. API Logs older than " + cutoffDate + " have been deleted.");
    }
}
