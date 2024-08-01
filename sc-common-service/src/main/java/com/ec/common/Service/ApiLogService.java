package com.ec.common.Service;

import com.ec.common.Model.ApiLog;
import com.ec.common.Repository.ApiLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApiLogService {

    @Autowired
    ApiLogRepository apiLogRepository;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Async
    public void logToDatabase(String url, String method, String payload, String username) {
        ApiLog log = new ApiLog();
        log.setUrl(url);
        log.setMethod(method);
        log.setPayload(payload);
        log.setUsername(username);
        log.setTimestamp(LocalDateTime.now());
        apiLogRepository.save(log);
    }
}
