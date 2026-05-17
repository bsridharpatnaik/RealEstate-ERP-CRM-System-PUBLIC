package com.ec.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private final Cache<String, Integer> processedKeys = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(50_000)
            .build();

    public boolean isDuplicate(String key) {
        return processedKeys.getIfPresent(key) != null;
    }

    public void markProcessed(String key) {
        processedKeys.put(key, 1);
    }
}
