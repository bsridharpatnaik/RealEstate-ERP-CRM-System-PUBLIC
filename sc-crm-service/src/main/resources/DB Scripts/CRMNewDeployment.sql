USE egcity;
SET SQL_SAFE_UPDATES=0;

-- Update prospect lead for existing leads
UPDATE customer_lead SET is_prospect_lead=false WHERE is_prospect_lead IS NULL;
UPDATE customer_lead SET status='Visit_Scheduled' WHERE status='Property_Visit';


-- Create view for activities
CREATE OR REPLACE VIEW activities_for_dashboard AS
SELECT UUID() as id,'today' as type, su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)=DATE(sysdate())
GROUP BY su.user_name
UNION ALL
-- Tomorrow's Activity
SELECT UUID() as id,'tomorrow' as type, su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)=DATE(DATE_ADD(sysdate(),INTERVAL 1 DAY))
GROUP BY su.user_name
UNION ALL
-- Pending Activity
SELECT UUID() as id,'pending', su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)<DATE(sysdate())
    AND la.isOpen=true
GROUP BY su.user_name
UNION ALL
-- Upcoming
SELECT UUID() as id,'upcoming', su.user_name,COUNT(leadactivity_id) as count FROM LeadActivity la
INNER JOIN customer_lead l on l.lead_id=la.lead_id
INNER JOIN security_user su on su.user_id=l.user_id
WHERE la.is_deleted=0
	AND DATE(la.activity_date_time)>DATE(sysdate()) AND DATE(la.activity_date_time)<=LAST_DAY(DATE(sysdate())) AND la.isOpen=true
GROUP BY su.user_name
UNION ALL
-- Live Leads
SELECT UUID() as id,'live', su.user_name,COUNT(l.lead_id) as count FROM customer_lead l
INNER JOIN security_user su on su.user_id=l.user_id
WHERE l.is_deleted=0
	AND l.status NOT IN ('Deal_Closed','Deal_Lost')
GROUP BY su.user_name
UNION ALL
SELECT UUID() as id,'prospect', su.user_name,COUNT(l.lead_id) as count FROM customer_lead l
INNER JOIN security_user su on su.user_id=l.user_id
WHERE l.is_deleted=0
	AND l.status NOT IN ('Deal_Closed','Deal_Lost') AND l.is_prospect_lead=true
GROUP BY su.user_name;


CREATE OR REPLACE VIEW lead_stage_agent_mapping AS
SELECT
	user_name,
    SUM(CASE WHEN l.status='New_Lead' THEN 1 ELSE 0 END) as new_lead ,
    SUM(CASE WHEN l.status='Visit_Scheduled' THEN 1 ELSE 0 END) as visit_scheduled,
    SUM(CASE WHEN l.status='Visit_Completed' THEN 1 ELSE 0 END) as visit_completed,
    SUM(CASE WHEN l.status='Negotiation' THEN 1 ELSE 0 END) as negotiation,
    SUM(CASE WHEN l.status='Deal_Lost' THEN 1 ELSE 0 END) as deal_lost,
    SUM(CASE WHEN l.status='Deal_Closed' THEN 1 ELSE 0 END) as deal_closed
FROM customer_lead l
INNER JOIN security_user su on su.user_id=l.user_id
WHERE l.is_deleted=0
GROUP BY su.user_name;