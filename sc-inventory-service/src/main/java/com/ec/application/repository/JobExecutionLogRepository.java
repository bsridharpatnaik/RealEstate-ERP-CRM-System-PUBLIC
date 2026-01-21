package com.ec.application.repository;

import com.ec.application.model.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Optional;

public interface JobExecutionLogRepository
        extends JpaRepository<JobExecutionLog, Long> {

    Optional<JobExecutionLog> findByJobName(String jobName);

    @Query("SELECT j.lastEndTime FROM JobExecutionLog j WHERE j.jobName = :jobName AND j.lastStatus = 'SUCCESS'")
    Date findLastSuccessfulRunTime(@Param("jobName") String jobName);

}
