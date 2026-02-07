package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "job_execution_log")
@Data
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, unique = true)
    private String jobName;

    private Date lastStartTime;
    private Date lastEndTime;

    @Column(length = 20)
    private String lastStatus;   // RUNNING, SUCCESS, FAILED

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private String triggeredBy;  // API / CRON

    private Date updatedAt;
}
