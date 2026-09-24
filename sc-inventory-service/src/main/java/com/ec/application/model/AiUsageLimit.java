package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;

/** Daily AI request limit per user; the row with username "*" is the default for everyone else. Master schema. */
@Entity
@Table(name = "ai_usage_limit")
@Data
public class AiUsageLimit {

    public static final String DEFAULT_USER = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private Integer dailyRequests;   // 0 = blocked
}
