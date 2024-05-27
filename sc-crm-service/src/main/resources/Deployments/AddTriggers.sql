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
