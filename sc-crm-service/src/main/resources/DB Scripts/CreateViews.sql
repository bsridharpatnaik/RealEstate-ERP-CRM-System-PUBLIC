use newbhaavbhumi; -- newsuncitynx,newkalpavrish,newsmartcity,newdrgtrdcntr,newbhaavbhumi
set @dbname='common';

INSERT IGNORE INTO `source`
(
`source_id`,
`source_name`)
VALUES
(111,'Broker');

-- Fetch lead with latest activity by date --

SELECT cl.*,la.* from customer_lead cl
INNER  JOIN LeadActivity la on la.lead_id=cl.lead_id
LEFT OUTER JOIN LeadActivity la2 on
	(cl.lead_id=la2.lead_id AND
		(la.created_at < la2.created_at OR
        (la.created_at = la2.created_at AND la.leadactivity_id<la2.leadactivity_id)))
WHERE la2.leadactivity_id IS NULL
AND cl.is_deleted=false;

-- User Information
set @q=concat('CREATE OR REPLACE view userdetails AS SELECT t.user_id, t.user_name,t.email, t.roles, u.tenants FROM
(
SELECT su.user_id,su.user_name,su.email, group_concat(ur.role_name SEPARATOR \',\') as roles FROM ',@dbname,'.security_user su
INNER JOIN ',@dbname,'.user_role ur on ur.user_id = su.user_id
INNER JOIN ',@dbname,'.role r on r.name = ur.role_name
WHERE su.status=true
GROUP BY su.user_id,su.user_name, su.email
) as t
LEFT JOIN
(
SELECT su.user_id,su.user_name,su.email,group_concat(tam.tenant_name SEPARATOR \',\') as tenants FROM ',@dbname,'.security_user su
INNER JOIN ',@dbname,'.user_role ur on ur.user_id = su.user_id
LEFT JOIN ',@dbname,'.user_tenant_mapping utm on su.user_id =utm.user_id
INNER JOIN ',@dbname,'.tenant_authorization_mapping tam ON tam.mapping_id=utm.mapping_id
WHERE su.status=true
GROUP BY su.user_id,su.user_name, su.email
) as u ON t.user_id=u.user_id;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

--       FETCH Stagnant Leads --
set @q=concat('CREATE OR REPLACE VIEW stangnantdetails AS
SELECT
assigneename,
       Sum(CASE
             WHEN stagnantdays = \'<10 Days\' THEN c
             ELSE 0
           END) AS \'lessThan10Days\',
       Sum(CASE
             WHEN stagnantdays = \'10-20 Days\' THEN c
             ELSE 0
           END) AS \'tenTo20Days\',
       Sum(CASE
             WHEN stagnantdays = \'20-30 Days\' THEN c
             ELSE 0
           END) AS \'twentyTo30Days\',
       Sum(CASE
             WHEN stagnantdays = \'>30 Days\' THEN c
             ELSE 0
           END) AS \'greaterThan30Days\'
FROM   (SELECT u.user_name       AS assigneeName,
               Count(cl.lead_id) AS c, CASE WHEN Datediff(Now(), la1.updated_at)
       >=10
       AND Datediff(Now(), la1.updated_at)<20 THEN \'10-20 Days\' WHEN Datediff(
       Now(),
       la1.updated_at) >=20 AND Datediff(Now(), la1.updated_at)<30 THEN
       \'20-30 Days\'
       WHEN Datediff(Now(), la1.updated_at) >30 THEN \'>30 Days\' WHEN Datediff(
       Now(),
       la1.updated_at) <10 THEN \'<10 Days\' END AS \'StagnantDays\'
        FROM   customer_lead cl
               INNER JOIN LeadActivity la1
                       ON cl.lead_id = la1.lead_id
               LEFT OUTER JOIN LeadActivity la2
                            ON ( cl.lead_id = la2.lead_id
                                 AND ( la1.updated_at < la2.updated_at
                                        OR ( la1.updated_at = la2.updated_at
                                             AND
                                       la1.leadactivity_id < la2.leadactivity_id
                                           ) )
                               )
               INNER JOIN ',@dbname,'.security_user u
                       ON cl.user_id = u.user_id
        WHERE  cl.is_deleted=0 AND cl.status NOT IN (\'Deal_Lost\',\'Deal_Closed\') AND la2.leadactivity_id IS NULL GROUP BY
       assigneename,
       stagnantdays) AS tx
GROUP  BY assigneename;');

PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----- Conversion Ratio  -------


set @q=concat('CREATE OR REPLACE view convertion_ratio AS
SELECT y.user_id AS \'user_id\', su.user_name AS \'asigneeName\',
       totalcount,
       CASE
		WHEN convertedcount IS NULL THEN 0
         ELSE convertedcount END AS convertedcount,
       CASE
         WHEN convertedcount / totalcount * 100 IS NULL THEN 0
         ELSE TRUNCATE(convertedcount / totalcount * 100,2)
       END          AS ratio
FROM   (SELECT cl.user_id,
               Count(DISTINCT cl.lead_id) AS totalCount
        FROM   LeadActivity la
               INNER JOIN customer_lead cl
                       ON cl.lead_id = la.lead_id  AND cl.created_at>=DATE_SUB(LAST_DAY(NOW()),INTERVAL DAY(LAST_DAY(NOW()))-
1 DAY) AND cl.is_deleted != 1 AND la.is_deleted !=1 and la.created_at>=DATE_SUB(LAST_DAY(NOW()),INTERVAL DAY(LAST_DAY(NOW()))-
1 DAY)
        GROUP  BY cl.user_id) AS y
       LEFT JOIN (SELECT cl.user_id,
                         Count(DISTINCT cl.lead_id) AS convertedCount
                  FROM   LeadActivity la
                         INNER JOIN customer_lead cl
                                 ON cl.lead_id = la.lead_id AND cl.is_deleted != 1  AND la.is_deleted !=1
                  WHERE  la.activityType = \'Deal_Close\' AND cl.status=\'Deal_Closed\' AND la.created_at>=DATE_SUB(LAST_DAY(NOW()),INTERVAL DAY(LAST_DAY(NOW()))-
1 DAY)
                  GROUP  BY cl.user_id) AS tx
              ON tx.user_id = y.user_id
       INNER JOIN ',@dbname, '.security_user su
               ON su.user_id = y.user_id;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


--       FETCH Stagnant Leads BY Property Type--
CREATE OR REPLACE VIEW stangnant_details_proptype AS
SELECT CASE WHEN propertyType IS NULL THEN 'OTHERS' ELSE propertyType END as 'property_type',
       Sum(CASE
             WHEN stagnantdays = '<10 Days' THEN c
             ELSE 0
           END) AS 'lessThan10Days',
       Sum(CASE
             WHEN stagnantdays = '10-20 Days' THEN c
             ELSE 0
           END) AS 'tenTo20Days',
       Sum(CASE
             WHEN stagnantdays = '20-30 Days' THEN c
             ELSE 0
           END) AS 'twentyTo30Days',
       Sum(CASE
             WHEN stagnantdays = '>30 Days' THEN c
             ELSE 0
           END) AS 'greaterThan30Days'
FROM   (SELECT cl.propertytype   AS propertyType,
               Count(cl.lead_id) AS c,
               CASE WHEN Datediff(Now(), la1.updated_at)>=10
       AND Datediff(Now(), la1.updated_at)<20 THEN '10-20 Days' WHEN Datediff(
       Now(),
       la1.updated_at) >=20 AND Datediff(Now(), la1.updated_at)<30 THEN
       '20-30 Days'
       WHEN Datediff(Now(), la1.updated_at) >30 THEN '>30 Days' WHEN Datediff(
       Now(),
       la1.updated_at) <10 THEN '<10 Days' END AS 'StagnantDays'
        FROM   customer_lead cl
               INNER JOIN LeadActivity la1
                       ON cl.lead_id = la1.lead_id
               LEFT OUTER JOIN LeadActivity la2
                            ON ( cl.lead_id = la2.lead_id
                                 AND ( la1.updated_at < la2.updated_at
                                        OR ( la1.updated_at = la2.updated_at
                                             AND la1.leadactivity_id <
                                                 la2.leadactivity_id ) )
                               )
        WHERE  cl.is_deleted=0 AND cl.status NOT IN ('Deal_Lost', 'Deal_Closed')
       AND
       la2.leadactivity_id IS NULL GROUP BY propertyType, stagnantdays) AS tx
GROUP  BY propertyType;

--    Fetch Conversion Ratio Property Type

CREATE OR REPLACE view convertion_ratio_prop_type AS
SELECT
		y.propertyType AS 'propertyType',
       totalcount,
       CASE
		WHEN convertedcount IS NULL THEN 0
         ELSE convertedcount END AS convertedcount,
       CASE
         WHEN convertedcount / totalcount * 100 IS NULL THEN 0
         ELSE TRUNCATE(convertedcount / totalcount * 100,2)
       END          AS ratio
FROM   (SELECT CASE WHEN cl.propertytype IS NULL THEN 'OTHERS' ELSE  cl.propertytype END           AS propertyType,
       Count(DISTINCT cl.lead_id) AS totalCount
		FROM   LeadActivity la
			INNER JOIN customer_lead cl
               ON cl.lead_id = la.lead_id
                  AND cl.created_at >= Date_sub(Last_day(Now()),
                                       INTERVAL Day(Last_day(Now()))- 1
                                       day)
                  AND cl.is_deleted != 1
                  AND la.is_deleted != 1
                  AND la.created_at >= Date_sub(Last_day(Now()),
                                       INTERVAL Day(Last_day(Now()))- 1 day)
		GROUP  BY cl.propertytype
        ) AS y
       LEFT JOIN (SELECT cl.propertyType as propertyType,
                         Count(DISTINCT cl.lead_id) AS convertedCount
                  FROM   LeadActivity la
                         INNER JOIN customer_lead cl
                                 ON cl.lead_id = la.lead_id AND cl.is_deleted != 1  AND la.is_deleted !=1
                  WHERE  la.activityType = 'Deal_Close' AND cl.status='Deal_Closed' AND la.created_at>=DATE_SUB(LAST_DAY(NOW()),INTERVAL DAY(LAST_DAY(NOW()))-
1 DAY)
                  GROUP  BY cl.propertyType) AS tx
              ON tx.propertyType = y.propertyType;

-- Payments Page
set @q=concat('CREATE OR REPLACE VIEW payments_page AS
SELECT
	row_number() over (
    ORDER BY cps.payment_date,cl.lead_id,
    cl.name,
    cds.deal_id,
    cps.amount,
    su.user_name,
    pt.propertyType,
    pn.name) as id,
	cl.lead_id,
    cl.name as customerName,
    cds.deal_id as dealStructureId,
    CASE WHEN cps.payment_date IS NULL THEN \'\' ELSE cps.payment_date END as paymentDate,
    cps.amount,
    cps.isReceived,
    cps.isCustomerPayment,
    su.user_name,
    su.user_id,
    pt.propertyType,
    pn.name as propertyName
FROM customer_deal_structure cds
INNER JOIN customer_lead cl ON cds.lead_id=cl.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=cl.user_id
INNER JOIN customer_payment_schedule cps ON cps.deal_id=cds.deal_id AND cps.is_deleted=0
INNER JOIN property_type pt ON pt.property_type_id=cds.property_type_id
INNER JOIN property_name pn ON pn.property_name_id=cds.property_name_id
WHERE cds.is_Deleted=0 ;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- Activities for dashboard
set @q=concat('CREATE OR REPLACE VIEW activities_for_dashboard AS
SELECT UUID() as id,\'today\' as type, su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)=DATE(sysdate())
GROUP BY su.user_name
UNION ALL
SELECT UUID() as id,\'tomorrow\' as type, su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)=DATE(DATE_ADD(sysdate(),INTERVAL 1 DAY))
GROUP BY su.user_name
UNION ALL
-- Pending Activity
SELECT UUID() as id,\'pending\', su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)<DATE(sysdate())
    AND la.isOpen=true
GROUP BY su.user_name
UNION ALL
-- Upcoming
SELECT UUID() as id,\'upcoming\', su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)>DATE(sysdate())
GROUP BY su.user_name
UNION ALL
-- Live Leads
SELECT UUID() as id,\'live\', su.user_name,COUNT(l.lead_id) as count FROM customer_lead l
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE l.is_deleted=0
	AND l.status NOT IN (\'Deal_Closed\',\'Deal_Lost\')
GROUP BY su.user_name
UNION ALL
SELECT UUID() as id,\'prospect\', su.user_name,COUNT(l.lead_id) as count FROM customer_lead l
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE l.is_deleted=0
	AND l.status NOT IN (\'Deal_Closed\',\'Deal_Lost\') AND l.is_prospect_lead=true
GROUP BY su.user_name
UNION ALL
SELECT UUID() as id, \'stale\', su.user_name, COUNT(t.lead_id) FROM
(
SELECT
  cl.lead_id,
  SUM(CASE WHEN la.isOpen = 1 THEN 1 ELSE 0 END) AS openCount
FROM
  LeadActivity la
	INNER JOIN customer_lead cl ON cl.lead_id=la.lead_id AND cl.status NOT IN (\'Deal_Closed\',\'Deal_Lost\')
WHERE la.is_deleted=0 AND cl.is_deleted=0 AND la.is_deleted=0
GROUP BY lead_id
HAVING SUM(CASE WHEN la.isOpen = 1 THEN 1 ELSE 0 END)=0
) t INNER JOIN customer_lead cl ON cl.lead_id=t.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=cl.user_id
GROUP BY su.user_name;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Lead stage mapping

set @q=concat('CREATE OR REPLACE VIEW lead_stage_agent_mapping AS
SELECT
	user_name,
    SUM(CASE WHEN l.status=\'New_Lead\' THEN 1 ELSE 0 END) as new_lead ,
    SUM(CASE WHEN l.status=\'Visit_Scheduled\' THEN 1 ELSE 0 END) as visit_scheduled,
    SUM(CASE WHEN l.status=\'Visit_Completed\' THEN 1 ELSE 0 END) as visit_completed,
    SUM(CASE WHEN l.status=\'Negotiation\' THEN 1 ELSE 0 END) as negotiation,
    SUM(CASE WHEN l.status=\'Deal_Lost\' THEN 1 ELSE 0 END) as deal_lost,
    SUM(CASE WHEN l.status=\'Deal_Closed\' THEN 1 ELSE 0 END) as deal_closed
FROM customer_lead l
INNER JOIN ',@dbname,'.security_user su on su.user_id=l.user_id
WHERE l.is_deleted=0
GROUP BY su.user_name;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- Recent Lead Activity For Pipeline
CREATE OR REPLACE view lead_activity_for_pipeline AS
SELECT la.lead_id,la.isOpen as recentIsOpen,la.activity_date_time as recentActivityDateTime FROM
(
SELECT
	lead_id,
    CASE
		WHEN pastOpenId IS NOT NULL THEN pastOpenId
		WHEN todayOpenId IS NOT NULL THEN todayOpenId
		WHEN todayOpenId IS NULL AND upcominOpen IS NULL THEN
        (CASE
			WHEN upcominClosed IS NOT NULL THEN upcominClosed
			WHEN todayClosed IS NOT NULL THEN todayClosed
            ELSE pastClosed
		END)
        WHEN todayOpenId IS NULL AND upcominOpen IS NOT NULL THEN upcominOpen
    END as activityId
FROM
(
SELECT
	cl.lead_id,
    poa.leadactivity_id as pastOpenId,
    toa.leadactivity_id as todayOpenId,
    uoa.leadactivity_id as upcominOpen,
        pca.leadactivity_id as pastClosed,
    tca.leadactivity_id as todayClosed,
    uca.leadactivity_id as upcominClosed
FROM customer_lead cl
LEFT JOIN
(
	SELECT la.lead_id, max(la.leadactivity_id) as leadactivity_id  FROM LeadActivity la
    WHERE CURDATE()>DATE_FORMAT(la.activity_date_time, "%Y-%m-%d") AND la.isOpen=true AND la.is_deleted=false
    GROUP BY la.lead_id
    ) poa ON cl.lead_id = poa.lead_id
LEFT JOIN
(
	SELECT la.lead_id, max(la.leadactivity_id) as leadactivity_id  FROM LeadActivity la
    WHERE CURDATE()=DATE_FORMAT(la.activity_date_time, "%Y-%m-%d") AND la.isOpen=true AND la.is_deleted=false
    GROUP BY la.lead_id
    ) toa ON cl.lead_id = toa.lead_id
  LEFT JOIN
(
	SELECT la.lead_id, max(la.leadactivity_id) as leadactivity_id  FROM LeadActivity la
    WHERE CURDATE()<DATE_FORMAT(la.activity_date_time, "%Y-%m-%d") AND la.isOpen=true AND la.is_deleted=false
    GROUP BY la.lead_id
    ) uoa ON cl.lead_id = uoa.lead_id
LEFT JOIN
(
	SELECT la.lead_id, max(la.leadactivity_id) as leadactivity_id  FROM LeadActivity la
    WHERE CURDATE()<DATE_FORMAT(la.activity_date_time, "%Y-%m-%d") AND la.isOpen=false AND la.is_deleted=false
    GROUP BY la.lead_id
    ) uca ON cl.lead_id = uca.lead_id
LEFT JOIN
(
	SELECT la.lead_id, max(la.leadactivity_id) as leadactivity_id  FROM LeadActivity la
    WHERE CURDATE()=DATE_FORMAT(la.activity_date_time, "%Y-%m-%d") AND la.isOpen=false AND la.is_deleted=false
    GROUP BY la.lead_id
    ) tca ON cl.lead_id = tca.lead_id
LEFT JOIN
(
	SELECT la.lead_id, max(la.leadactivity_id) as leadactivity_id  FROM LeadActivity la
    WHERE CURDATE()>DATE_FORMAT(la.activity_date_time, "%Y-%m-%d") AND la.isOpen=false AND la.is_deleted=false
    GROUP BY la.lead_id
    ) pca ON cl.lead_id = pca.lead_id
WHERE cl.is_deleted=false AND cl.status != 'Deal_Lost'
) ids
) a INNER JOIN LeadActivity la on la.leadactivity_id=a.activityId;

-- Execution history table
CREATE TABLE IF NOT EXISTS  execution_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    procedure_name VARCHAR(50),
    last_execution TIMESTAMP NOT NULL
);



INSERT IGNORE INTO execution_history (procedure_name, last_execution) VALUES ('UpdateLeadNotesAndStagnantDays','2000-01-01 00:00:00');
INSERT IGNORE INTO execution_history (procedure_name, last_execution) VALUES ('UpdateLeadDerivedFields','2000-01-01 00:00:00');
INSERT IGNORE INTO execution_history (procedure_name, last_execution) VALUES ('UpdatePipelineActivityForLead','2000-01-01 00:00:00');

DELIMITER //

DROP PROCEDURE IF EXISTS UpdateLeadNotesAndStagnantDays;
CREATE PROCEDURE UpdateLeadNotesAndStagnantDays()
BEGIN
    DECLARE last_exec TIMESTAMP;

    -- Get the last execution time for this procedure
    SELECT last_execution INTO last_exec
    FROM execution_history
    WHERE procedure_name = 'UpdateLeadNotesAndStagnantDays'
    ORDER BY id DESC
    LIMIT 1;

    -- Create a temporary table for leads to update
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;
    CREATE TEMPORARY TABLE LeadsToUpdate AS
    SELECT DISTINCT l.lead_id
    FROM customer_lead l
    LEFT JOIN note n ON n.lead_id = l.lead_id AND n.is_deleted = 0
    LEFT JOIN LeadActivity la ON la.lead_id = l.lead_id AND la.is_deleted = 0
    WHERE n.updated_at >= last_exec
       OR la.updated_at >= last_exec;

    -- Create temporary table to hold hashtags
    DROP TEMPORARY TABLE IF EXISTS TempNotes;
    CREATE TEMPORARY TABLE TempNotes AS
    SELECT lead_id,
           GROUP_CONCAT(DISTINCT SUBSTRING_INDEX(SUBSTRING(n.content, LOCATE('#', n.content)), ' ', 1) SEPARATOR ', ') AS notes
    FROM note n
    WHERE n.is_deleted = 0 AND n.content REGEXP '#[a-zA-Z0-9_]+'
    GROUP BY n.lead_id;

    -- Update notes
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    LEFT JOIN TempNotes tn ON tn.lead_id = l.lead_id
    SET l.notes = tn.notes;

    -- Drop TempNotes temporary table
    DROP TEMPORARY TABLE IF EXISTS TempNotes;

    -- Create temporary table for stagnant days count
    DROP TEMPORARY TABLE IF EXISTS TempStagnantDays;
    CREATE TEMPORARY TABLE TempStagnantDays AS
    SELECT la.lead_id,
           CASE
               WHEN cl.status IN ('Deal_closed', 'Deal_Lost') THEN 0
               ELSE DATEDIFF(NOW(), MAX(la.updated_at))
           END AS stagnantDaysCount
    FROM LeadActivity la
    INNER JOIN customer_lead cl ON cl.lead_id = la.lead_id
    WHERE la.is_deleted = 0
    GROUP BY la.lead_id, cl.status;

    -- Update stagnantDaysCount
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    LEFT JOIN TempStagnantDays tsd ON tsd.lead_id = l.lead_id
    SET l.stagnantDaysCount = tsd.stagnantDaysCount;

    -- Drop TempStagnantDays temporary table
    DROP TEMPORARY TABLE IF EXISTS TempStagnantDays;

    -- Update the last execution time
    INSERT INTO execution_history (last_execution, procedure_name) VALUES (NOW(), 'UpdateLeadNotesAndStagnantDays');

    -- Drop LeadsToUpdate temporary table
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;
END //

DELIMITER ;


-- Procedure to derive other fields once every 15 minutes
DELIMITER //

DROP PROCEDURE IF EXISTS UpdateLeadDerivedFields;
CREATE PROCEDURE UpdateLeadDerivedFields()
BEGIN
    DECLARE last_exec TIMESTAMP;
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END; -- Continue on error

    -- Get the last execution time for this procedure
    SELECT last_execution INTO last_exec
    FROM execution_history
    WHERE procedure_name = 'UpdateLeadDerivedFields'
    ORDER BY id DESC
    LIMIT 1;

    -- Create a temporary table for leads to update
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;
    CREATE TEMPORARY TABLE LeadsToUpdate AS
    SELECT DISTINCT l.lead_id
    FROM customer_lead l
    LEFT JOIN customer_deal_structure cds ON cds.lead_id = l.lead_id AND cds.is_deleted = 0
    LEFT JOIN customer_payment_schedule cps ON cps.deal_id = cds.deal_id AND cps.is_deleted = 0 AND cps.isReceived = false
    WHERE cds.updated_at >= last_exec
       OR cps.updated_at >= last_exec;

    -- Create temporary table for loan status
    DROP TEMPORARY TABLE IF EXISTS TempLoanStatus;
    CREATE TEMPORARY TABLE TempLoanStatus AS
    SELECT cds.lead_id, cds.loanStatus
    FROM customer_deal_structure cds
    INNER JOIN customer_lead cl ON cl.lead_id = cds.lead_id
    WHERE cds.is_deleted = 0 AND cl.is_deleted = 0 AND cds.loanStatus IS NOT NULL;

    -- Update loanStatus
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    LEFT JOIN TempLoanStatus tls ON tls.lead_id = l.lead_id
    SET l.loanStatus = tls.loanStatus;

    -- Drop TempLoanStatus temporary table
    DROP TEMPORARY TABLE IF EXISTS TempLoanStatus;

    -- Create temporary table for customer status
    DROP TEMPORARY TABLE IF EXISTS TempCustomerStatus;
    CREATE TEMPORARY TABLE TempCustomerStatus AS
    SELECT cds.lead_id, cds.customerStatus
    FROM customer_deal_structure cds
    INNER JOIN customer_lead cl ON cl.lead_id = cds.lead_id
    WHERE cds.is_deleted = 0 AND cl.is_deleted = 0 AND cds.customerStatus IS NOT NULL;

    -- Update customerStatus
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    LEFT JOIN TempCustomerStatus tcs ON tcs.lead_id = l.lead_id
    SET l.customerStatus = tcs.customerStatus;

    -- Drop TempCustomerStatus temporary table
    DROP TEMPORARY TABLE IF EXISTS TempCustomerStatus;

    -- Create temporary table for next payment date
    DROP TEMPORARY TABLE IF EXISTS TempNextPayment;
    CREATE TEMPORARY TABLE TempNextPayment AS
    SELECT cds.lead_id, MIN(cps.payment_date) AS nextPaymentDate
    FROM customer_payment_schedule cps
    INNER JOIN customer_deal_structure cds ON cps.deal_id = cds.deal_id
    WHERE cps.is_deleted = 0 AND cds.is_deleted = 0 AND cps.isReceived = false
    GROUP BY cds.lead_id;

    -- Update nextPaymentDate
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    LEFT JOIN TempNextPayment tnp ON tnp.lead_id = l.lead_id
    SET l.nextPaymentDate = tnp.nextPaymentDate;

    -- Drop TempNextPayment temporary table
    DROP TEMPORARY TABLE IF EXISTS TempNextPayment;

    -- Create temporary table for total pending
    DROP TEMPORARY TABLE IF EXISTS TempTotalPending;
    CREATE TEMPORARY TABLE TempTotalPending AS
    SELECT cds.lead_id, SUM(cps.amount) AS totalPending
    FROM customer_payment_schedule cps
    INNER JOIN customer_deal_structure cds ON cps.deal_id = cds.deal_id
    WHERE cps.is_deleted = 0 AND cds.is_deleted = 0 AND cps.isReceived = false
    GROUP BY cds.lead_id;

    -- Update totalPending
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    LEFT JOIN TempTotalPending ttp ON ttp.lead_id = l.lead_id
    SET l.totalPending = ttp.totalPending;

    -- Drop TempTotalPending temporary table
    DROP TEMPORARY TABLE IF EXISTS TempTotalPending;

    -- Update the last execution time
    INSERT INTO execution_history (last_execution, procedure_name) VALUES (NOW(), 'UpdateLeadDerivedFields');

    -- Drop LeadsToUpdate temporary table
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;
END //

DELIMITER ;



-- Procedure to get actvity for pipeline for the lead
DELIMITER //

DROP PROCEDURE IF EXISTS UpdatePipelineActivityForLead;
CREATE PROCEDURE UpdatePipelineActivityForLead()
BEGIN
    DECLARE last_exec TIMESTAMP DEFAULT '1970-01-01 00:00:00';  -- Default value if no record is found
    DECLARE proc_name VARCHAR(255) DEFAULT 'UpdatePipelineActivityForLead';

    -- Error handler
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        -- Rollback the transaction if there is an error
        ROLLBACK;
    END;

    -- Start transaction
    START TRANSACTION;

    -- Get the last execution time for this procedure
    SELECT last_execution INTO last_exec
    FROM execution_history
    WHERE procedure_name = proc_name
    ORDER BY id DESC
    LIMIT 1;

    -- Create a temporary table for leads to update based on LeadActivity updates
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;
    CREATE TEMPORARY TABLE LeadsToUpdate AS
    SELECT DISTINCT l.lead_id
    FROM customer_lead l
    LEFT JOIN LeadActivity la ON la.lead_id = l.lead_id AND la.is_deleted = 0
    WHERE la.updated_at >= last_exec;

    -- Create a temporary table to hold the lead activity details for the pipeline
    DROP TEMPORARY TABLE IF EXISTS TempPipelineActivity;
    CREATE TEMPORARY TABLE TempPipelineActivity AS
    SELECT
        cl.lead_id,
        la.isOpen AS recentIsOpen,
        la.activity_date_time AS recentActivityDateTime
    FROM
        customer_lead cl
        INNER JOIN LeadActivity la ON cl.lead_id = la.lead_id
    WHERE la.leadactivity_id IN (
        SELECT
            CASE
                WHEN poa.leadactivity_id IS NOT NULL THEN poa.leadactivity_id
                WHEN toa.leadactivity_id IS NOT NULL THEN toa.leadactivity_id
                WHEN toa.leadactivity_id IS NULL AND uoa.leadactivity_id IS NULL THEN
                    CASE
                        WHEN uca.leadactivity_id IS NOT NULL THEN uca.leadactivity_id
                        WHEN tca.leadactivity_id IS NOT NULL THEN tca.leadactivity_id
                        ELSE pca.leadactivity_id
                    END
                WHEN toa.leadactivity_id IS NULL AND uoa.leadactivity_id IS NOT NULL THEN uoa.leadactivity_id
            END
        FROM
            customer_lead cl
            LEFT JOIN (
                SELECT lead_id, MAX(leadactivity_id) as leadactivity_id
                FROM LeadActivity
                WHERE CURDATE() > DATE_FORMAT(activity_date_time, '%Y-%m-%d')
                  AND isOpen = true
                  AND is_deleted = false
                GROUP BY lead_id
            ) poa ON cl.lead_id = poa.lead_id
            LEFT JOIN (
                SELECT lead_id, MAX(leadactivity_id) as leadactivity_id
                FROM LeadActivity
                WHERE CURDATE() = DATE_FORMAT(activity_date_time, '%Y-%m-%d')
                  AND isOpen = true
                  AND is_deleted = false
                GROUP BY lead_id
            ) toa ON cl.lead_id = toa.lead_id
            LEFT JOIN (
                SELECT lead_id, MAX(leadactivity_id) as leadactivity_id
                FROM LeadActivity
                WHERE CURDATE() < DATE_FORMAT(activity_date_time, '%Y-%m-%d')
                  AND isOpen = true
                  AND is_deleted = false
                GROUP BY lead_id
            ) uoa ON cl.lead_id = uoa.lead_id
            LEFT JOIN (
                SELECT lead_id, MAX(leadactivity_id) as leadactivity_id
                FROM LeadActivity
                WHERE CURDATE() < DATE_FORMAT(activity_date_time, '%Y-%m-%d')
                  AND isOpen = false
                  AND is_deleted = false
                GROUP BY lead_id
            ) uca ON cl.lead_id = uca.lead_id
            LEFT JOIN (
                SELECT lead_id, MAX(leadactivity_id) as leadactivity_id
                FROM LeadActivity
                WHERE CURDATE() = DATE_FORMAT(activity_date_time, '%Y-%m-%d')
                  AND isOpen = false
                  AND is_deleted = false
                GROUP BY lead_id
            ) tca ON cl.lead_id = tca.lead_id
            LEFT JOIN (
                SELECT lead_id, MAX(leadactivity_id) as leadactivity_id
                FROM LeadActivity
                WHERE CURDATE() > DATE_FORMAT(activity_date_time, '%Y-%m-%d')
                  AND isOpen = false
                  AND is_deleted = false
                GROUP BY lead_id
            ) pca ON cl.lead_id = pca.lead_id
        WHERE cl.is_deleted = false
          AND cl.status != 'Deal_Lost'
    );

    -- Update the customer_lead table with the selected lead activity details
    UPDATE customer_lead cl
    JOIN TempPipelineActivity tpa ON cl.lead_id = tpa.lead_id
    SET cl.recentIsOpen = tpa.recentIsOpen,
        cl.recentActivityDateTime = tpa.recentActivityDateTime
    WHERE cl.lead_id IN (SELECT lead_id FROM LeadsToUpdate);

    -- Drop the temporary tables
    DROP TEMPORARY TABLE IF EXISTS TempPipelineActivity;
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;

    -- Update the last execution time in the execution_history table
    INSERT INTO execution_history (last_execution, procedure_name) VALUES (NOW(), proc_name);

    -- Commit the transaction
    COMMIT;
END //

DELIMITER ;

