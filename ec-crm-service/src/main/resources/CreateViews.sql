-- Fetch lead with latest activity by date -- 
SELECT cl.*,la.* from customer_lead cl
INNER  JOIN lead_activity la on la.lead_id=cl.lead_id
LEFT OUTER JOIN lead_activity la2 on 
	(cl.lead_id=la2.lead_id AND
		(la.created_at < la2.created_at OR 
        (la.created_at = la2.created_at AND la.leadactivity_id<la2.leadactivity_id)))
WHERE la2.leadactivity_id IS NULL
AND cl.is_deleted=false;