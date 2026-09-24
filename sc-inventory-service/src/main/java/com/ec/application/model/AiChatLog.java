package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/** One admin question to the AI assistant — audit trail and source of the per-user daily spend cap. Master schema. */
@Entity
@Table(name = "ai_chat_log", indexes = @Index(name = "idx_ai_chat_user_time", columnList = "username,createdAt"))
@Data
public class AiChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 36)
    private String sessionId;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String answer;

    @Column(length = 20)
    private String status;   // OK, ERROR, TIMEOUT

    private Double costUsd;
    private Integer numTurns;
    private Long durationMs;

    @Column(nullable = false)
    private Date createdAt;
}
