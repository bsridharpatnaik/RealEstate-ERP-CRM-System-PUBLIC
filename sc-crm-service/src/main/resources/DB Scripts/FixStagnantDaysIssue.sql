-- Procedure to update lead notes (runs every 30 minutes)
DELIMITER //
CREATE PROCEDURE UpdateLeadNotes()
BEGIN
    DECLARE last_exec TIMESTAMP;

    -- Get the last execution time for this procedure
    SELECT last_execution INTO last_exec
    FROM execution_history
    WHERE procedure_name = 'UpdateLeadNotes'
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

    -- Clean up temporary tables
    DROP TEMPORARY TABLE IF EXISTS TempNotes;
    DROP TEMPORARY TABLE IF EXISTS LeadsToUpdate;

    -- Update the last execution time
    INSERT INTO execution_history (last_execution, procedure_name)
    VALUES (NOW(), 'UpdateLeadNotes');
END //
DELIMITER ;

-- Procedure to update stagnant days (runs daily)
DELIMITER //
CREATE PROCEDURE UpdateStagnantDays()
BEGIN
    -- Create temporary table for stagnant days count for ALL leads
    DROP TEMPORARY TABLE IF EXISTS TempStagnantDays;
    CREATE TEMPORARY TABLE TempStagnantDays AS
    SELECT
        cl.lead_id,
        CASE
            WHEN cl.status IN ('Deal_closed', 'Deal_Lost') THEN 0
            ELSE COALESCE(
                DATEDIFF(NOW(), MAX(la.updated_at)),
                DATEDIFF(NOW(), cl.created_at)  -- Fallback to created_at if no activities
            )
        END AS stagnantDaysCount
    FROM customer_lead cl
    LEFT JOIN LeadActivity la ON la.lead_id = cl.lead_id AND la.is_deleted = 0
    GROUP BY cl.lead_id, cl.status;

    -- Update stagnantDaysCount for ALL leads
    UPDATE customer_lead l
    LEFT JOIN TempStagnantDays tsd ON tsd.lead_id = l.lead_id
    SET l.stagnantDaysCount = tsd.stagnantDaysCount;

    -- Clean up
    DROP TEMPORARY TABLE IF EXISTS TempStagnantDays;

    -- Update the last execution time
    INSERT INTO execution_history (last_execution, procedure_name)
    VALUES (NOW(), 'UpdateStagnantDays');
END //
DELIMITER ;