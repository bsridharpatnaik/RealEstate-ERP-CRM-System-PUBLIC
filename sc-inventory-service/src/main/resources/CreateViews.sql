use egcity; -- suncitynx,kalpavrish,riddhisiddhi,smartcity,businesspark;

-- All Inventory --
use egcity;
CREATE OR replace VIEW all_inventory AS
SELECT
		-- convert(func_inc_var_session(),char) AS id,
        row_number() over (ORDER BY tx.date desc,tx.type desc,tx.keyid desc) as id,
		tx.*,
		p.Product_name,
        cat.category_name,
        p.measurementunit,
         c.name,
         c.mobileno,
         c.emailid,
         c.contacttype,
         w.warehouse_id,
         w.warehousename
  FROM   (SELECT 'Inward'                         AS type,
				ii.inwardid as keyid ,
				ioe.entryid as entryid,
                 Date_format(ii.DATE, "%Y-%m-%d") AS date,
                 ii.contactid                    AS contactid,
                 ii.warehouse_id                  AS warehouseid,
                 ioe.Productid                   AS Productid,
                 ioe.quantity,
                 ioe.closingstock,
                 ioe.creationDate,
                 ioe.lastModifiedDate
          FROM   inward_inventory ii
                 inner join inwardinventory_entry iie
                         ON ii.inwardid = iie.inwardid
                 inner join inward_outward_entries ioe
                         ON iie.entryid = ioe.entryid
          WHERE  ii.is_deleted = 0
          UNION ALL
          SELECT 'Outward'                        AS type,
                  oi.outwardid as keyid,
                  ioe.entryid as entryid,
                 Date_format(oi.DATE, "%Y-%m-%d") AS date,
                 oi.contactid                    AS contactid,
                 oi.warehouse_id                  AS warehouseid,
                 ioe.Productid                   AS Productid,
                 ioe.quantity,
                 ioe.closingstock,
                 ioe.creationDate,
                 ioe.lastModifiedDate
          FROM   outward_inventory oi
                 inner join outwardinventory_entry oie
                         ON oi.outwardid = oie.outwardid
                 inner join inward_outward_entries ioe
                         ON oie.entryid = ioe.entryid
          WHERE  oi.is_deleted = 0
          UNION ALL
          SELECT 'Lost-Damaged'                    AS type,
                lostdamagedid as keyid,
                lostdamagedid as entryid,
                 Date_format(ldi.DATE, "%Y-%m-%d") AS date,
                 ''                                AS contactid,
                 ldi.warehousename                AS warehouseid,
                 ldi.Productid                    AS Productid,
                 ldi.quantity,
                 ldi.closingstock,
                 ldi.creationDate,
                 ldi.lastModifiedDate
          FROM   lost_damaged_inventory ldi
			where ldi.is_deleted = 0) AS tx
         left join contacts c
                ON c.contactid = tx.contactid
         inner join Warehouse w
                 ON w.warehouse_id = tx.warehouseid
		inner join Product p on p.Productid = tx.Productid
        INNER JOIN Category cat on p.categoryId=cat.categoryId
 ORDER  BY tx.date desc,tx.type desc,tx.keyid desc;
  
  
 -- BOQ STATUS -------
CREATE OR REPLACE view boq_status AS
SELECT 
	b.*,
	CASE WHEN SUM(ioe.quantity) IS NULL THEN 0 ELSE SUM(ioe.quantity) END as totalConsumedQuantity,
    FORMAT(CASE WHEN SUM(ioe.quantity) /totalExpectedQuantity*100 IS NULL THEN 0 ELSE SUM(ioe.quantity) /totalExpectedQuantity*100 END,2)+0 as consumedPercent
FROM 
(SELECT 
	row_number() over (ORDER BY bt.typeId) as id,
	bt.typeId,
    bt.building_type,
    ul.locationId,
    ul.location_name,
    p.productId,
	p.product_name,
	CASE WHEN SUM(bi.quantity) IS NULL THEN 0 ELSE SUM(bi.quantity) END as totalExpectedQuantity
FROM
	building_type bt 
LEFT JOIN Usage_Location ul ON ul.typeId=bt.typeId AND ul.typeId IS NOT NULL AND ul.is_deleted=0
LEFT JOIN boq_inventory bi  ON ((ul.locationId = bi.locationId
  OR ul.typeId = bi.typeId) AND bi.is_deleted=0)
LEFT JOIN Product p on p.productId=bi.productId AND p.is_deleted=0
WHERE bt.is_deleted=0
GROUP BY 
    bt.typeId,
    bt.building_type,
    ul.locationId,
    ul.location_name,
    p.productId,
	p.product_name) as b
LEFT JOIN outward_inventory oi on oi.locationId=b.locationId AND oi.is_deleted=0
LEFT JOIN outwardinventory_entry oie on oie.outwardid=oi.outwardid
LEFT JOIN inward_outward_entries ioe on ioe.entryId=oie.entryId AND ioe.is_deleted=0
GROUP BY typeId,
		building_type,
		locationId,
		location_name,
		productId,
		product_name,
        totalExpectedQuantity;
	
	
  -- --------- Stock Verification ------------
  create or replace view stock_verification as
SELECT inw.inventory, 
       ROUND(total_inward,2)                                     AS 'total_inward', 
       ROUND(total_outward  ,2)                                  AS 'total_outward', 
       ROUND(current_stock ,2)                                   AS 'current_stock', 
       ROUND(total_inward - ( total_outward + current_stock ),2) AS 
       'diff_in_Stock' 
FROM   (SELECT p.product_name AS inventory, 
               Sum(quantity)  AS total_inward 
        FROM   inward_inventory ii 
               INNER JOIN inwardinventory_entry iie 
                       ON iie.inwardid = ii.inwardid 
               INNER JOIN inward_outward_entries ioe 
                       ON ioe.entryid = iie.entryid 
               INNER JOIN Product p 
                       ON p.productid = ioe.productid 
        WHERE  ii.is_deleted = 0 
        GROUP  BY p.product_name) AS inw 
       INNER JOIN (SELECT p.product_name AS inventory, 
                          Sum(quantity)  AS total_outward 
                   FROM   outward_inventory oi 
                          INNER JOIN outwardinventory_entry oie 
                                  ON oie.outwardid = oi.outwardid 
                          INNER JOIN inward_outward_entries ioe 
                                  ON ioe.entryid = oie.entryid 
                          INNER JOIN Product p 
                                  ON p.productid = ioe.productid 
                   WHERE  oi.is_deleted = 0 
                   GROUP  BY p.product_name) AS outw 
               ON outw.inventory = inw.inventory 
       INNER JOIN (SELECT p.product_name          AS inventory, 
                          Sum(s.quantityinhand) AS current_stock 
                   FROM   Stock s 
                          INNER JOIN Product p 
                                  ON p.productid = s.productid 
                   GROUP  BY p.product_name) AS stock 
               ON inw.inventory = stock.inventory WHERE ROUND(total_inward - ( total_outward + current_stock ),2)!=0;

###################  STOCk STATUS #############

CREATE OR REPLACE VIEW stockInformation as
	SELECT
		p.productId as productId,
        p.product_name,
        p.reorderQuantity,
        p.measurementUnit,
        c.category_name,
        ROUND(SUM(s.quantityInHand),2) as totalQuantityInHand,
        CASE WHEN ROUND(SUM(s.quantityInHand),2)<=p.reorderQuantity THEN 'Low' ELSE 'High' END as stockStatus,
        JSON_ARRAYAGG(JSON_OBJECT(
			'warehouseName',w.warehouseName,
            'quantityInHand',s.quantityInHand,
            'measurementUnit',p.measurementUnit
            )) as detailedStock
	FROM Stock s
	INNER JOIN Product p on p.productId=s.productId
	INNER JOIN Category c on p.categoryId=c.categoryId
    INNER JOIN Warehouse w on w.warehouse_id = s.warehouseName
	WHERE s.is_deleted=0
	GROUP BY p.productId,p.product_name,p.reorderQuantity,p.measurementUnit,c.category_name;


##########
USE kalpavrish;
CREATE OR REPLACE view boq_status AS
SELECT
	ex.*,
    ow.totalConsumedQuantity,
    FORMAT(CASE WHEN ow.totalConsumedQuantity /totalExpectedQuantity*100 IS NULL THEN 0 ELSE  ow.totalConsumedQuantity /totalExpectedQuantity*100 END,2)+0 as consumedPercent
FROM
(
SELECT
	row_number() over (ORDER BY bt.typeId) as id,
	bt.typeId,
    bt.building_type,
    ul.locationId,
    ul.location_name,
    p.productId,
	p.product_name,
    CASE WHEN SUM(bi.quantity) IS NULL THEN 0 ELSE SUM(bi.quantity) END as totalExpectedQuantity
FROM
	building_type bt
INNER JOIN Usage_Location ul ON ul.typeId=bt.typeId AND ul.typeId IS NOT NULL AND ul.is_deleted=0
INNER JOIN boq_inventory bi  ON ((ul.locationId = bi.locationId OR ul.typeId = bi.typeId) AND bi.is_deleted = 0)
INNER JOIN Product p on p.productId = bi.productId AND p.is_deleted = 0
WHERE bt.is_deleted = 0
GROUP BY
    bt.typeId,
    bt.building_type,
    ul.locationId,
    ul.location_name,
    p.productId,
	p.product_name
    ) as ex
    INNER JOIN
    (SELECT
	ul.locationId,
    ul.location_name,
    p.productId,
	p.product_name,
	CASE WHEN SUM(ioe.quantity) IS NULL THEN 0 ELSE SUM(ioe.quantity) END as totalConsumedQuantity
FROM
	outward_inventory oi
INNER JOIN outwardinventory_entry oie on oie.outwardid=oi.outwardid
INNER JOIN inward_outward_entries ioe on ioe.entryid = oie.entryId AND ioe.is_deleted=0
INNER JOIN Usage_Location ul ON oi.locationId=ul.locationId and ul.is_deleted=0
INNER JOIN Product p on p.productId = ioe.productId AND p.is_deleted = 0
GROUP BY locationId,location_name,productId,product_name) as ow
ON ex.locationId=ow.locationId AND ex.productId=ow.productId;

-- Historical Closing Stock #################
use egcity;
select
		ai1.ProductId as productId,
        ai1.Product_name as product_name,
        p.reorderQuantity,
        p.measurementUnit,
        c.category_name,
        ROUND(SUM(ai1.closingstock),2) as totalQuantityInHand,
        CASE WHEN ROUND(SUM(ai1.closingstock),2)<=p.reorderQuantity THEN 'Low' ELSE 'High' END as stockStatus,
        JSON_ARRAYAGG(JSON_OBJECT(
			'warehouseName',ai1.warehousename,
            'quantityInHand',ai1.closingstock,
            'measurementUnit',p.measurementUnit
            )) as detailedStock
FROM all_inventory ai1
INNER JOIN
	(SELECT
		Productid,
        warehouseid,
        MIN(id) as id
	FROM all_inventory ai
    WHERE ai.date<='2021-03-01'
    GROUP BY Productid,warehouseid
    ) AS ai2  ON ai1.id=ai2.id
INNER JOIN Product p on p.productId=ai1.ProductId
INNER JOIN Category c on p.categoryId=c.categoryId
GROUP BY ai1.ProductId,ai1.Product_name,p.reorderQuantity,p.measurementUnit,c.category_name;


-- Inward outward trend
CREATE OR REPLACE VIEW inwardoutwardtrend AS
SELECT
	t.date,
    CASE WHEN ii.count IS NULL THEN 0 ELSE ii.count END as inward_count ,
    CASE WHEN oi.count IS NULL THEN 0 ELSE oi.count END as outward_count
    FROM
(
	SELECT
		DATE_FORMAT(curdate(),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 1 day),'%d-%m-%Y') as date     union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 2 day),'%d-%m-%Y') as date     union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 3 day),'%d-%m-%Y') as date     union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 4 day),'%d-%m-%Y') as date     union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 5 day),'%d-%m-%Y') as date     union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 6 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 7 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 8 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 9 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 10 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 11 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 12 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 13 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 14 day),'%d-%m-%Y') as date    union
		select DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 15 day),'%d-%m-%Y') as date
) as t
LEFT JOIN
(
	SELECT ii.date as fulldate,DATE_FORMAT(ii.date,'%d-%m-%Y') as date,COUNT(ioe.entryid) as count
		FROM inward_inventory ii
        INNER JOIN inwardinventory_entry iie on iie.inwardid = ii.inwardid
        INNER JOIN inward_outward_entries ioe on ioe.entryid=iie.entryid
		WHERE ii.is_deleted=0 AND ioe.is_deleted=0
		AND ii.date >= ( CURDATE() - INTERVAL 15 DAY )
	GROUP BY ii.date,DATE_FORMAT(ii.date,'%d-%m-%Y')
	ORDER BY ii.date DESC
) as ii on ii.date=t.date

LEFT JOIN
(
	SELECT oi.date as fulldate,DATE_FORMAT(oi.date,'%d-%m-%Y') as date,COUNT(ioe.entryid) as count
		FROM outward_inventory oi
        INNER JOIN outwardinventory_entry oie on oie.outwardid = oi.outwardid
        INNER JOIN inward_outward_entries ioe on ioe.entryid=oie.entryid
		WHERE oi.is_deleted=0  AND ioe.is_deleted=0
		AND oi.date >= ( CURDATE() - INTERVAL 15 DAY )
	GROUP BY oi.date,DATE_FORMAT(oi.date,'%d-%m-%Y')
	ORDER BY oi.date DESC
) as oi on oi.date=t.date;

-- inward stats for dashboard
CREATE OR REPLACE VIEW inward_stats AS
SELECT
	totalInward.name as supplier_name,
    totalInward.count as inward_count,
    CASE WHEN leadTime.lead_time IS NULL THEN 0 ELSE leadTime.lead_time END as avg_lead_time,
    CASE WHEN rejectCount.reject_count IS NULL THEN 0 ELSE rejectCount.reject_count END as rejectCount
FROM

(
SELECT c.name,count(inwardid) as count FROM inward_inventory ii
	INNER JOIN contacts c on c.contactid=ii.contactid
    WHERE ii.is_deleted=0
GROUP BY c.name
) as totalInward

LEFT JOIN
(
SELECT c.name,
	ROUND(AVG(DATEDIFF(ii.date,ii.purchaseOrderdate)),2) as lead_time
FROM inward_inventory ii
	INNER JOIN contacts c on c.contactid=ii.contactid
WHERE ii.is_deleted=0
    AND ii.purchaseOrderdate IS NOT NULL
	AND ii.purchaseOrderdate <= ii.date
GROUP BY c.name
) AS leadTime on totalInward.name=leadTime.name
LEFT JOIN
(
SELECT c.name,COUNT(DISTINCT ii.inwardid) as reject_count FROM inward_inventory ii
	INNER JOIN contacts c on c.contactid=ii.contactid
	INNER JOIN rejectInward_entry rie on rie.inwardid=ii.inwardid
    INNER JOIN reject_inward_entries re on re.rejectentryid=rie.rejectentryid
WHERE ii.is_deleted=0
GROUP BY c.name
) as rejectCount on leadTime.name=rejectCount.name;


-- Outward Stats
CREATE OR REPLACE VIEW outward_stats AS
SELECT
	totalOutward.name as contractor_name,
    totalOutward.total_count as total_count,
    CASE WHEN rejectCount.reject_count IS NULL THEN 0 ELSE rejectCount.reject_count END  as reject_count

FROM

(
	SELECT
		c.name,
		COUNT(oi.outwardid) as total_count
	FROM outward_inventory oi
	INNER JOIN contacts c on c.contactId=oi.contactId
	WHERE oi.is_deleted=0
	GROUP BY c.name
) as totalOutward
LEFT JOIN
(
	SELECT c.name,COUNT(DISTINCT oi.outwardid) as reject_count FROM outward_inventory oi
    INNER JOIN contacts c on c.contactId=oi.contactId
	INNER JOIN rejectOutward_entry roe on roe.outwardid=oi.outwardid
	INNER JOIN reject_outward_entries rie ON rie.rejectentryid=roe.rejectentryid
	WHERE oi.is_deleted=0
    GROUP BY c.name
) as rejectCount on rejectCount.name=totalOutward.name;
