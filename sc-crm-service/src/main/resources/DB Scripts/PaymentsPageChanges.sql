use egcity;
set @dbname='egcity';
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
    DECLARE last_exec TIMESTAMP;
    DECLARE proc_name VARCHAR(255) DEFAULT 'UpdatePipelineActivityForLead';

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
END //

DELIMITER ;