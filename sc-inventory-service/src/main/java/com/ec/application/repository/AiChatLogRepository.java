package com.ec.application.repository;

import com.ec.application.model.AiChatLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {

    long countByUsernameAndCreatedAtGreaterThanEqual(String username, Date since);

    /** [username, requests, costUsd] per user since the given time. */
    @Query("SELECT a.username, COUNT(a), COALESCE(SUM(a.costUsd), 0) FROM AiChatLog a WHERE a.createdAt >= :since GROUP BY a.username")
    List<Object[]> usageByUserSince(@Param("since") Date since);

    @Query("SELECT a FROM AiChatLog a WHERE a.createdAt >= :since AND (:username IS NULL OR a.username = :username) ORDER BY a.createdAt DESC")
    List<AiChatLog> history(@Param("username") String username, @Param("since") Date since, Pageable page);

    boolean existsByUsernameAndSessionId(String username, String sessionId);
}
