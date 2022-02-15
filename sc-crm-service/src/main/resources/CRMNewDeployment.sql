USE egcity;
SET SQL_SAFE_UPDATES=0;

-- Update prospect lead for existing leads
UPDATE customer_lead SET is_prospect_lead=false WHERE is_prospect_lead IS NULL;