use businesspark; -- suncitynx,kalpavrish,riddhisiddhi,smartcity,businesspark;
set @dbname='common';

-- Fetch lead with latest activity by date --

SELECT cl.*,la.* from customer_lead cl
INNER  JOIN LeadActivity la on la.lead_id=cl.lead_id
LEFT OUTER JOIN LeadActivity la2 on
	(cl.lead_id=la2.lead_id AND
		(la.created_at < la2.created_at OR
        (la.created_at = la2.created_at AND la.leadactivity_id<la2.leadactivity_id)))
WHERE la2.leadactivity_id IS NULL
AND cl.is_deleted=false;


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
               
               
               
               