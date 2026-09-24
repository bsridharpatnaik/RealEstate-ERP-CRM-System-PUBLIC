package com.ec.application.repository;

import com.ec.application.model.AiUsageLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageLimitRepository extends JpaRepository<AiUsageLimit, Long> {

    AiUsageLimit findByUsername(String username);
}
