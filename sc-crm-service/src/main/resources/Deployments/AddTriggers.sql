use egcity;

set sql_safe_updates=0;
ALTER TABLE LeadActivity
ADD COLUMN isLatest TINYINT(1) DEFAULT 0;

SET SQL_SAFE_UPDATES=0;
-- Step 1: Create a temporary table to store the leadactivity_id of the latest activity for each lead_id
CREATE TEMPORARY TABLE LatestActivity (
    leadactivity_id BIGINT PRIMARY KEY
);

-- Step 2: Insert the latest activity for each lead_id into the temporary table
-- Using ROW_NUMBER to ensure only one record per lead_id in case of tie
INSERT INTO LatestActivity (leadactivity_id)
SELECT leadactivity_id
FROM (
    SELECT leadactivity_id,
           ROW_NUMBER() OVER (PARTITION BY lead_id ORDER BY activity_date_time DESC, leadactivity_id DESC) as rn
    FROM LeadActivity
    WHERE is_deleted = 0
) subquery
WHERE subquery.rn = 1;

-- Step 3: Update all records to set isLatest to FALSE where is_deleted = 0
UPDATE LeadActivity
SET isLatest = FALSE
WHERE is_deleted = 0;

-- Step 4: Update records to set isLatest to TRUE for the latest activity in each lead_id
UPDATE LeadActivity la
JOIN LatestActivity l ON la.leadactivity_id = l.leadactivity_id
SET la.isLatest = TRUE;

-- Step 5: Drop the temporary table
DROP TEMPORARY TABLE LatestActivity;

-- add new columns
ALTER TABLE Lead
ADD COLUMN notes TEXT,
ADD COLUMN lastActivityModifiedDate TIMESTAMP,
ADD COLUMN stagnantDaysCount BIGINT,
ADD COLUMN loanStatus VARCHAR(255),
ADD COLUMN customerStatus VARCHAR(255),
ADD COLUMN nextPaymentDate DATE,
ADD COLUMN totalPending DECIMAL(10, 2);


CREATE TABLE IF NOT EXISTS execution_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    last_execution TIMESTAMP NOT NULL
);


INSERT INTO execution_history (last_execution) VALUES ('2000-01-01 00:00:00');


DELIMITER //
DROP PROCEDURE IF EXISTS UpdateLeadDerivedFields;
CREATE PROCEDURE UpdateLeadDerivedFields()
BEGIN
    DECLARE last_exec TIMESTAMP;

    -- Get the last execution time
    SELECT last_execution INTO last_exec
    FROM execution_history
    ORDER BY id DESC
    LIMIT 1;

    -- Create a temporary table for leads to update
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;
    CREATE TEMPORARY TABLE LeadsToUpdate AS
    SELECT DISTINCT l.lead_id
    FROM customer_lead l
    LEFT JOIN note n ON n.lead_id = l.lead_id AND n.is_deleted = 0
    LEFT JOIN LeadActivity la ON la.lead_id = l.lead_id AND la.is_deleted = 0
    LEFT JOIN customer_deal_structure cds ON cds.lead_id = l.lead_id AND cds.is_deleted = 0
    LEFT JOIN customer_payment_schedule cps ON cps.deal_id = cds.deal_id AND cps.is_deleted = 0 AND cps.isReceived = false
    WHERE n.updated_at > last_exec
       OR la.updated_at > last_exec
       OR cds.updated_at > last_exec
       OR cps.updated_at > last_exec;

    -- Update notes
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.notes = '';

    -- Update lastActivityModifiedDate
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.lastActivityModifiedDate = (
        SELECT MAX(la.updated_at)
        FROM LeadActivity la
        WHERE la.lead_id = l.lead_id
          AND la.is_deleted = 0
    );

    -- Update stagnantDaysCount
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.stagnantDaysCount = (
        SELECT CASE
                   WHEN l.status IN ('Deal_closed', 'Deal_Lost') THEN 0
                   ELSE DATEDIFF(NOW(), MAX(la.updated_at))
               END
        FROM LeadActivity la
        INNER JOIN customer_lead cl ON cl.lead_id = la.lead_id
        WHERE la.lead_id = l.lead_id
          AND la.is_deleted = 0
    );

    -- Update loanStatus
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.loanStatus = (
        SELECT cds.loanStatus
        FROM customer_deal_structure cds
        INNER JOIN customer_lead cl ON cl.lead_id = cds.lead_id
        WHERE cds.is_deleted = 0
          AND cl.is_deleted = 0
          AND cl.lead_id = l.lead_id
          AND cds.loanStatus IS NOT NULL
        LIMIT 1
    );

    -- Update customerStatus
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.customerStatus = (
        SELECT cds.customerStatus
        FROM customer_deal_structure cds
        INNER JOIN customer_lead cl ON cl.lead_id = cds.lead_id
        WHERE cds.is_deleted = 0
          AND cl.is_deleted = 0
          AND cl.lead_id = l.lead_id
          AND cds.customerStatus IS NOT NULL
        LIMIT 1
    );

    -- Update nextPaymentDate
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.nextPaymentDate = (
        SELECT MIN(cps.payment_date)
        FROM customer_payment_schedule cps
        INNER JOIN customer_deal_structure cds ON cps.deal_id = cds.deal_id
        WHERE cps.is_deleted = 0
          AND cds.is_deleted = 0
          AND cps.isReceived = false
          AND cds.lead_id = l.lead_id
    );

    -- Update totalPending
    UPDATE customer_lead l
    JOIN LeadsToUpdate lu ON lu.lead_id = l.lead_id
    SET l.totalPending = (
        SELECT SUM(cps.amount)
        FROM customer_payment_schedule cps
        INNER JOIN customer_deal_structure cds ON cps.deal_id = cds.deal_id
        WHERE cps.is_deleted = 0
          AND cds.is_deleted = 0
          AND cps.isReceived = false
          AND cds.lead_id = l.lead_id
    );

    -- Update the last execution time
    INSERT INTO execution_history (last_execution) VALUES (NOW());

    -- Drop the temporary table
    DROP TEMPORARY TABLE LeadsToUpdate;
END //

DELIMITER ;

-- Schedule the stored procedure:
-- Drop the existing event if it exists
DROP EVENT IF EXISTS UpdateLeadDerivedFieldsEvent;

-- Create the event
CREATE EVENT UpdateLeadDerivedFieldsEvent
ON SCHEDULE EVERY 30 MINUTE
DO
    CALL UpdateLeadDerivedFields();

-- Enable the event scheduler:
SET GLOBAL event_scheduler = ON;


