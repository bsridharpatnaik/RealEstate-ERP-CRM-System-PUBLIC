-- Fetch lead with latest activity by date -- 
SELECT cl.*,la.* from customer_lead cl
INNER  JOIN lead_activity la on la.lead_id=cl.lead_id
LEFT OUTER JOIN lead_activity la2 on 
	(cl.lead_id=la2.lead_id AND
		(la.created_at < la2.created_at OR 
        (la.created_at = la2.created_at AND la.leadactivity_id<la2.leadactivity_id)))
WHERE la2.leadactivity_id IS NULL
AND cl.is_deleted=false;


--       FETCH Stagnant Leads --
CREATE OR REPLACE VIEW stangnantdetails AS
SELECT 
assigneename, 
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
FROM   (SELECT u.user_name       AS assigneeName, 
               Count(cl.lead_id) AS c, CASE WHEN Datediff(Now(), la1.updated_at) 
       >=10 
       AND Datediff(Now(), la1.updated_at)<20 THEN '10-20 Days' WHEN Datediff( 
       Now(), 
       la1.updated_at) >=20 AND Datediff(Now(), la1.updated_at)<30 THEN 
       '20-30 Days' 
       WHEN Datediff(Now(), la1.updated_at) >30 THEN '>30 Days' WHEN Datediff( 
       Now(), 
       la1.updated_at) <10 THEN '<10 Days' END AS 'StagnantDays' 
        FROM   customer_lead cl 
               INNER JOIN lead_activity la1 
                       ON cl.lead_id = la1.lead_id 
               LEFT OUTER JOIN lead_activity la2 
                            ON ( cl.lead_id = la2.lead_id 
                                 AND ( la1.updated_at < la2.updated_at 
                                        OR ( la1.updated_at = la2.updated_at 
                                             AND 
                                       la1.leadactivity_id < la2.leadactivity_id 
                                           ) ) 
                               ) 
               INNER JOIN security_user u 
                       ON cl.user_id = u.user_id 
        WHERE  cl.is_deleted=0 AND cl.status NOT IN ('Deal_Lost','Deal_Closed') AND la2.leadactivity_id IS NULL GROUP BY 
       assigneename, 
       stagnantdays) AS tx 
GROUP  BY assigneename; 