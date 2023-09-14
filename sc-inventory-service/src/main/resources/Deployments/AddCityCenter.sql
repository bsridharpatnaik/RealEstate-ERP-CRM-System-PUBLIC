create database newcitycenter;
INSERT INTO `common`.`tenant`
(`name`,
`is_crm`,
`is_inventory`,
`tenant_long_name`)
VALUES
('citycenter',
0,
1,
'CITY CENTER');

use citycenter; -- suncitynx,kalpavrish,riddhisiddhi,smartcity,businesspark;

-- All Inventory --
CREATE OR replace VIEW all_inventory
AS
  SELECT row_number()
           over (
             ORDER BY tx.date desc, tx.type desc, tx.keyid desc) as id,
         tx.type,
         tx.keyid,
         tx.entryid,
         tx.date, -- index
         tx.contactid,
         tx.warehouseid,
         tx.Productid,
         tx.quantity,
         tx.closingstock,
         tx.creationDate,
         tx.lastModifiedDate,
         tx.Product_name, -- index
         tx.category_name, -- index
         tx.measurementunit,
         c.name,
         c.mobileno,
         c.emailid,
         c.contacttype,
         tx.warehouse_id,
         tx.warehousename -- index
  FROM   (SELECT 'Inward'                         AS type,
                 ii.inwardid                      as keyid,
                 ioe.entryid                      as entryid,
                 Date_format(ii.DATE, "%Y-%m-%d") AS date,
                 ii.contactid                     AS contactid,
                 ii.warehouse_id                  AS warehouseid,
                 ioe.Productid                    AS Productid,
                 ioe.quantity,
                 ioe.closingstock,
                 ioe.creationDate,
                 ioe.lastModifiedDate,
                 p.Product_name,
                 cat.category_name,
                 p.measurementunit,
                 w.warehouse_id,
                 w.warehousename
          FROM   inward_inventory ii
                 inner join inwardinventory_entry iie
                         ON ii.inwardid = iie.inwardid
                 inner join inward_outward_entries ioe
                         ON iie.entryid = ioe.entryid
                 inner join Product p
                         on p.Productid = ioe.Productid
                 INNER JOIN Category cat
                         on p.categoryId = cat.categoryId
                 inner join Warehouse w
                         ON w.warehouse_id = ii.warehouse_id
          WHERE  ii.is_deleted = 0
          UNION ALL
          SELECT 'Outward'                        AS type,
                 oi.outwardid                     as keyid,
                 ioe.entryid                      as entryid,
                 Date_format(oi.DATE, "%Y-%m-%d") AS date,
                 oi.contactid                     AS contactid,
                 oi.warehouse_id                  AS warehouseid,
                 ioe.Productid                    AS Productid,
                 ioe.quantity,
                 ioe.closingstock,
                 ioe.creationDate,
                 ioe.lastModifiedDate,
                 p.Product_name,
                 cat.category_name,
                 p.measurementunit,
                 w.warehouse_id,
                 w.warehousename
          FROM   outward_inventory oi
                 inner join outwardinventory_entry oie
                         ON oi.outwardid = oie.outwardid
                 inner join inward_outward_entries ioe
                         ON oie.entryid = ioe.entryid
                 inner join Product p
                         on p.Productid = ioe.Productid
                 INNER JOIN Category cat
                         on p.categoryId = cat.categoryId
                 inner join Warehouse w
                         ON w.warehouse_id = oi.warehouse_id
          WHERE  oi.is_deleted = 0
          UNION ALL
          SELECT 'Lost-Damaged'                    AS type,
                 lostdamagedid                     as keyid,
                 lostdamagedid                     as entryid,
                 Date_format(ldi.DATE, "%Y-%m-%d") AS date,
                 ''                                AS contactid,
                 ldi.warehousename                 AS warehouseid,
                 ldi.Productid                     AS Productid,
                 ldi.quantity,
                 ldi.closingstock,
                 ldi.creationDate,
                 ldi.lastModifiedDate,
                 p.Product_name,
                 cat.category_name,
                 p.measurementunit,
                 w.warehouse_id,
                 w.warehousename
          FROM   lost_damaged_inventory ldi
                 inner join Product p
                         on p.Productid = ldi.Productid
                 INNER JOIN Category cat
                         on p.categoryId = cat.categoryId
                 inner join Warehouse w
                         ON w.warehouse_id = ldi.warehousename
          where  ldi.is_deleted = 0) AS tx
         left join contacts c
                ON c.contactid = tx.contactid;


  -- Backfill Closing Stock
  CREATE OR REPLACE VIEW backfill_closing_stock AS
  SELECT row_number()
             over (
               ORDER BY tx.date desc, tx.type desc, tx.keyid desc) as id,
           tx.type,
           tx.keyid,
           tx.entryid,
           tx.date,
           tx.warehouseid,
           tx.Productid,
           tx.quantity,
           tx.closingstock
    FROM   (SELECT 'Inward'                         AS type,
                   ii.inwardid                      as keyid,
                   ioe.entryid                      as entryid,
                   Date_format(ii.DATE, "%Y-%m-%d") AS date,
                   ii.warehouse_id                  AS warehouseid,
                   ioe.Productid                    AS Productid,
                   ioe.quantity,
                   ioe.closingstock
            FROM   inward_inventory ii
                   inner join inwardinventory_entry iie
                           ON ii.inwardid = iie.inwardid
                   inner join inward_outward_entries ioe
                           ON iie.entryid = ioe.entryid
            WHERE  ii.is_deleted = 0
            UNION ALL
            SELECT 'Outward'                        AS type,
                   oi.outwardid                     as keyid,
                   ioe.entryid                      as entryid,
                   Date_format(oi.DATE, "%Y-%m-%d") AS date,
                   oi.warehouse_id                  AS warehouseid,
                   ioe.Productid,
                   ioe.quantity,
                   ioe.closingstock
            FROM   outward_inventory oi
                   inner join outwardinventory_entry oie
                           ON oi.outwardid = oie.outwardid
                   inner join inward_outward_entries ioe
                           ON oie.entryid = ioe.entryid
            WHERE  oi.is_deleted = 0
            UNION ALL
            SELECT 'Lost-Damaged'                    AS type,
                   lostdamagedid                     as keyid,
                   lostdamagedid                     as entryid,
                   Date_format(ldi.DATE, "%Y-%m-%d") AS date,
                   ldi.warehousename                 AS warehouseid,
                   ldi.Productid                     AS Productid,
                   ldi.quantity,
                   ldi.closingstock
            FROM   lost_damaged_inventory ldi
            where  ldi.is_deleted = 0) AS tx;

-- Store Procedure to backfill stock
DELIMITER //
DROP PROCEDURE IF EXISTS update_closing_stock//
CREATE PROCEDURE update_closing_stock(id_list TEXT, editDate TEXT,triggerSource TEXT )
         BEGIN
			DECLARE done INT DEFAULT FALSE;
			DECLARE entryid1 decimal;
            DECLARE oldClosingStock decimal;
            DECLARE newClosingStock decimal;
            DECLARE cur CURSOR FOR SELECT ioe.entryid FROM inward_outward_entries ioe
					LEFT JOIN inwardinventory_entry iie on ioe.entryid=iie.entryid
					LEFT JOIN inward_inventory ii on ii.inwardid=iie.inwardid
					LEFT JOIN outwardinventory_entry oie on ioe.entryid=oie.entryid
					LEFT JOIN outward_inventory oi on oi.outwardid=oie.outwardid
					WHERE
                    CASE WHEN triggerSource='scheduler' THEN
						ioe.is_deleted=0 AND (ii.date>=editDate OR oi.date>=editDate)
					ELSE
						FIND_IN_SET(ioe.productId,id_list)>0 AND ioe.is_deleted=0 AND (ii.date>=editDate OR oi.date>=editDate)
					END;
            DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

--            START TRANSACTION;
--            TRUNCATE temp;
--            INSERT INTO temp VALUES(CONCAT('Procedure Started ',SYSDATE()));
--            INSERT INTO temp VALUES(CONCAT('Product List - ', id_list));
--            INSERT INTO temp VALUES(CONCAT('Date - ', editDate));
--            COMMIT;

            OPEN cur;
			ins_loop: LOOP
            FETCH cur INTO entryid1;

            IF done THEN
                LEAVE ins_loop;
            END IF;

--            START TRANSACTION;
--            INSERT INTO temp VALUES(CONCAT('ENTRY ID - ', entryid1 ,' ',SYSDATE()));
--            COMMIT;

            SELECT
            bs1.closingStock as oldClosingStock,
			CASE WHEN bs1.type='Inward' THEN(
			(SELECT CASE WHEN SUM(bs2.quantity) IS NULL THEN 0 ELSE SUM(bs2.quantity) END FROM backfill_closing_stock bs2
            WHERE bs2.id>=bs1.id AND bs2.type='Inward' AND bs1.Productid=bs2.productid AND bs2.warehouseid=bs1.warehouseid)
            -
            (SELECT CASE WHEN SUM(bs2.quantity)IS NULL THEN 0 ELSE SUM(bs2.quantity) END  FROM backfill_closing_stock bs2
            WHERE bs2.id>bs1.id AND bs2.type!='Inward' AND bs1.Productid=bs2.productid AND bs2.warehouseid=bs1.warehouseid)
			)
			ELSE (
			(SELECT CASE WHEN SUM(bs2.quantity) IS NULL THEN 0 ELSE SUM(bs2.quantity) END  FROM backfill_closing_stock bs2
            WHERE bs2.id>bs1.id AND bs2.type='Inward' AND bs1.Productid=bs2.productid AND bs2.warehouseid=bs1.warehouseid)
            -
            (SELECT CASE WHEN SUM(bs2.quantity) IS NULL THEN 0 ELSE SUM(bs2.quantity) END  FROM backfill_closing_stock bs2
            WHERE bs2.id>=bs1.id AND bs2.type!='Inward' AND bs1.Productid=bs2.productid AND bs2.warehouseid=bs1.warehouseid)
			)
			END AS closingStock
            INTO oldClosingStock,newClosingStock
			FROM backfill_closing_stock bs1 WHERE bs1.entryid=entryid1;


            IF newClosingStock>=0 AND newClosingStock<>oldClosingStock THEN
            START TRANSACTION;
				UPDATE inward_outward_entries SET closingStock = newClosingStock WHERE entryid=entryid1;
            COMMIT;
			END IF;
         END LOOP;
		 CLOSE cur;
END;


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


-- Inventory Report

CREATE OR REPLACE VIEW inventoryreport AS
SELECT
	t2.id,
    t1.month,
	t1.product_name,
    t1.measurementunit,
    t1.category_name,
    t1.warehousename,
    IF(total_inward-total_outward-total_lost_damaged=closing_stock,0,(t2.closing_stock+total_outward+total_lost_damaged-total_inward)) as opening_stock,
    t1.total_inward,
    total_outward,
    total_lost_damaged,
    t2.closing_stock
FROM
(
	SELECT
		DATE_FORMAT(date,'%Y-%m') as month,
		category_name,
		product_name,
        measurementunit,
        ai1.warehousename,
		SUM(IF(type='Inward',quantity,0)) as total_inward,
		SUM(IF(type='Outward',quantity,0)) as total_outward,
		SUM(IF(type='Lost-Damaged',quantity,0)) as total_lost_damaged
	FROM all_inventory ai1
	GROUP BY month,category_name,product_name,measurementunit,ai1.warehousename
	ORDER BY month,category_name,product_name,measurementunit, ai1.warehousename
) AS t1
INNER JOIN
(
	SELECT
    DATE_FORMAT(date,'%Y-%m') as month,
	ai.id,
	ai.closingStock AS closing_stock,
    ai.product_name,
	ai.category_name,
	ai.warehousename
FROM all_inventory ai
INNER JOIN (
		SELECT
			DATE_FORMAT(date,'%Y-%m') as month,
			product_name,
			category_name,
			warehousename,
			MIN(id) AS id
		FROM all_inventory
        GROUP BY month,product_name,category_name,warehousename
) latest ON latest.id = ai.id
) as t2 ON t1.month=t2.month AND t1.category_name=t2.category_name AND t1.product_name=t2.product_name AND t1.warehousename=t2.warehousename
ORDER BY t1.month desc,product_name,warehousename;


##### BOQ Status VIEW

CREATE OR REPLACE VIEW boq_status_view AS
select
  row_number() OVER (
    ORDER BY
      `bu`.`id`
  ) AS `id`,
  `p`.`productId` AS `productId`,
  sum(`bu`.`quantity`) AS `boqQuantity`,
  `bu`.`buildingTypeId` AS `buildingTypeId`,
  `bu`.`usageLocationId` AS `usageLocationId`,
  `ul`.`location_name` AS `buildingUnit`,
  `bt`.`building_type` AS `buildingType`,
  `p`.`product_name` AS `product`,
  `cg`.`category_name` AS `category`,
  0.0 AS `outwardQuantity`,
  0 AS `status`
from
  (
    (
      (
        (
          `BOQUpload` `bu`
          join `Usage_Location` `ul`
        )
        join `Product` `p`
      )
      join `Category` `cg`
    )
    join `building_type` `bt`
  )
where
  (
    (
      `bu`.`usageLocationId` = `ul`.`locationId`
    )
    and (
      `p`.`productId` = `bu`.`productId`
    )
    and (
      `cg`.`categoryId` = `p`.`categoryId`
    )
    and (
      `bu`.`buildingTypeId` = `bt`.`typeId`
    )
    and (
      `bu`.`buildingTypeId` = `bu`.`buildingTypeId`
    )
    and (
      `bu`.`usageLocationId` = `bu`.`usageLocationId`
    )
    and (`bu`.`is_deleted` = false)
  )
group by
	`bu`.`id`,
  `bu`.`buildingTypeId`,
  `bu`.`usageLocationId`,
  `bu`.`productId`,
  `ul`.`location_name`,
  `p`.`product_name`,
  `cg`.`category_name`,
  `p`.`productId`;


 -- Inventory Missing Pricing
 CREATE OR REPLACE VIEW MissingInventoryPricingByMonth AS
     SELECT row_number()
                over () as id,s.*
     FROM
     (
   	SELECT oi.*
      FROM
   	(
   	  SELECT
   	  DISTINCT
   			c.category_name as categoryName,
   			ioe.productId as productId,
              p.measurementUnit,
   			p.product_name productName,
   			DATE_FORMAT(oi.date,'%Y-%m') AS date
   	  FROM outward_inventory oi
   		 INNER JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid
   		  INNER JOIN inward_outward_entries ioe ON ioe.entryid = oie.entryid
   		  INNER JOIN Product p on p.productId = ioe.productId
   		  INNER JOIN Category c ON c.categoryId=p.categoryId
   		  WHERE oi.is_deleted=0 AND ioe.is_deleted=0
   	 ) as oi LEFT JOIN InventoryMonthPriceMapping imp ON imp.productId = oi.productId AND DATE_FORMAT(imp.date,'%Y-%m') = oi.date AND imp.is_deleted=0
        WHERE imp.date IS NULL
        ORDER BY oi.categoryName, oi.productId, oi.productName, oi.date
     ) s;


   -- InventoryMonthUsageInformation
   CREATE OR REPLACE view InventoryMonthUsageInformation AS
   SELECT

   t.*,imp.price,TRUNCATE((t.totalQuantity * imp.price),2) as totalPrice  FROM
   (
   SELECT
   DISTINCT
   	row_number()
              over (
                ORDER BY oi.locationId, ul.location_name, c.categoryId, c.category_name, ioe.productId, p.product_name,DATE_FORMAT(oi.date,'%y-%m')) as id,
   		oi.locationId,
           ul.location_name as locationName,
           c.categoryId,
           c.category_name as categoryName,
           ioe.productId,
           p.product_name as productName,
           DATE_FORMAT(oi.date,'%y-%m') as ym,
           TRUNCATE(SUM(ioe.quantity),2) as totalQuantity
   	FROM outward_inventory oi
   	INNER JOIN outwardinventory_entry oie on oi.outwardid=oie.outwardid
       INNER JOIN inward_outward_entries ioe on ioe.entryid = oie.entryId
       INNER JOIN Usage_Location ul on ul.locationId = oi.locationId
       INNER JOIN Product p on p.productId = ioe.productId
       INNER JOIN Category c on c.categoryId = p.categoryId
   WHERE oi.is_deleted = 0 AND ioe.is_deleted = 0
   GROUP BY oi.locationId, ul.location_name, c.categoryId, c.category_name, ioe.productId, p.product_name,DATE_FORMAT(oi.date,'%y-%m')
   ) as t
   LEFT JOIN InventoryMonthPriceMapping imp on DATE_FORMAT(imp.date,'%y-%m') = t.ym AND imp.productId=t.productId AND imp.is_deleted=0;


   #### BOQ New view
CREATE OR REPLACE VIEW boq_status_view2 AS
SELECT
		row_number() OVER () as id,
		o.buildingTypeId,
        o.building_type,
        o.location_id,
        o.location_name,
        o.productId,
        o.product_name,
        o.category_name,
		ROUND(SUM(o.boq_quantity),2) as total_boq_quantity,
		ROUND(SUM(o.outward_quanity),2) as total_outward_quantity,
        JSON_ARRAYAGG(JSON_OBJECT(
			'finalLocationId',o.final_location_id,
            'finalLocationName',o.final_location_name,
            'total_boq_quantity',o.boq_quantity,
            'total_outward_quantity',o.outward_quanity,
            'status',o.status
            )) as detailed,
		ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) as status,
        CASE
			WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <= 10 THEN '0-10 %'
			WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 10 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=20 THEN '10-20 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 20 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=30 THEN '20-30 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 30 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=40 THEN '30-40 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 40 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=50 THEN '40-50 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 50 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=60 THEN '50-60 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 60 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=70 THEN '60-70 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 70 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=80 THEN '70-80 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 80 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=90 THEN '80-90 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 90 AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=100 THEN '90-100 %'
            WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 100 THEN 'above 100 %'
		END as statusBucket
FROM
(
	SELECT
		t.buildingTypeId,
        btype.building_type,
        t.usageLocationId as location_id,
        ul.location_name,
        t.locationId as final_location_id,
        ua.usagearea_name as final_location_name,
        t.productId,
        p.product_name,
        c.category_name,
        t.boq_quantity,
        CASE WHEN t.outward_quanity IS NULL THEN 0 ELSE t.outward_quanity END as outward_quanity,
        CASE WHEN t.outward_quanity IS NULL THEN 0 ELSE ROUND((t.outward_quanity-t.boq_quantity)/t.boq_quantity*100,2) END AS status
    FROM
    (
		SELECT bu.buildingTypeId,bu.usageLocationId, bu.locationId, bu.productId, bu.boq_quantity, oi.outward_quanity FROM
		(
			SELECT bu.buildingTypeId, bu.usageLocationId, bu.locationId, bu.productId, quantity as boq_quantity FROM BOQUpload bu
			WHERE bu.is_deleted=0
		) bu LEFT JOIN
		(
			SELECT oi.locationId as usageLocationId, oi.usageAreaId as locationId,ioe.productId, SUM(quantity) as outward_quanity FROM outward_inventory oi
				INNER JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid
				INNER JOIN inward_outward_entries ioe ON ioe.entryid = oie.entryId
			WHERE oi.is_deleted=0
			GROUP BY oi.locationId, oi.usageAreaId,ioe.productId
		) oi ON bu.usageLocationId=oi.usageLocationId AND bu.locationId=oi.locationId AND bu.productId=oi.productId
	) t
	INNER JOIN building_type btype ON t.buildingTypeId = btype.typeId
    INNER JOIN Usage_Location ul ON t.usageLocationId = ul.locationId
    INNER JOIN usage_area ua ON ua.usageAreaId = t.locationId
    INNER JOIN Product p ON p.productId = t.productId
    INNER JOIN Category c ON c.categoryId = p.categoryId
    ORDER BY btype.building_type,ul.location_name, c.category_name, p.product_name
) o
GROUP BY o.buildingTypeId,
        o.building_type,
        o.location_id,
        o.location_name,
        o.productId,
        o.product_name,
        o.category_name