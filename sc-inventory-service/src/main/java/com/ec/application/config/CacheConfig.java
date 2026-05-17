package com.ec.application.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    private CaffeineCache makeCache(String name, long ttlMinutes, long maxSize) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                        .maximumSize(maxSize)
                        .build());
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
                makeCache("currentUser",      1,  10_000),
                makeCache("refCategories",   15,     500),
                makeCache("refProducts",     15,   5_000),
                makeCache("refWorkAreas",    15,     500),
                makeCache("refBuildingTypes",15,     500),
                makeCache("boqOutwardQty",    2,  10_000),
                makeCache("boqStatusRows",    2,       1)
        ));
        return manager;
    }
}
