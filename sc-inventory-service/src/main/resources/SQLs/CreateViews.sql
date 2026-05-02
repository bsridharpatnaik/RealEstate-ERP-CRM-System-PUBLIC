-- use drgtrdcntr,bhaavbhumi,citycenter,mnglmcity,mhvrtrdcntr,iseries, smartcity
use iseries;

CREATE TABLE IF NOT EXISTS execution_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    procedure_name VARCHAR(255) NOT NULL,
    last_execution DATETIME NOT NULL DEFAULT '2010-01-01 00:00:00',
    UNIQUE KEY uk_execution_history_procedure (procedure_name)
);


CREATE OR REPLACE VIEW all_inventory_view AS
SELECT
    ROW_NUMBER() OVER (
        ORDER BY
            q.date        DESC,
            q.sort_order  DESC,
            q.entryid     ASC     -- ← ASC
    ) AS id,
    q.type,
    q.keyid,
    q.entryid,
    q.date,
    q.contactid,
    q.productid,
    q.quantity,

    SUM(CASE
        WHEN q.type IN ('Inward', 'Transfer-In', 'Excess-Found')   THEN  q.quantity
        WHEN q.type IN ('Transfer-Out', 'Outward', 'Lost-Damaged') THEN -q.quantity
        ELSE 0
    END) OVER (
        PARTITION BY q.warehouse_id, q.productid
        ORDER BY q.date ASC, q.sort_order ASC, q.entryid ASC      -- ← ASC
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS closingstock,

    q.creationDate,
    q.lastModifiedDate,
    q.product_name,
    q.category_name,
    q.measurementunit,
    q.sort_order,
    c.name,
    c.mobileno,
    c.emailid,
    c.contacttype,
    q.warehouse_id,
    q.warehousename
FROM (

    /* === INWARD (NORMAL) — sort_order 1 === */
    SELECT
        'Inward'                AS type,
        ii.inwardid             AS keyid,
        ioe.entryid             AS entryid,
        DATE(ii.date)           AS date,
        ii.contactid,
        ioe.productid,
        ioe.quantity,
        ioe.creationDate,
        ioe.lastModifiedDate,
        p.product_name,
        cat.category_name,
        p.measurementunit,
        w.warehouse_id,
        w.warehousename,
        1                       AS sort_order
    FROM inward_inventory ii
    JOIN inwardinventory_entry iie  ON ii.inwardid    = iie.inwardid
    JOIN inward_outward_entries ioe ON iie.entryid    = ioe.entryid
    JOIN Product p                  ON p.productid    = ioe.productid
    JOIN Category cat               ON cat.categoryid = p.categoryid
    JOIN Warehouse w                ON w.warehouse_id = ioe.warehouse_id
    WHERE ii.is_deleted = 0

    UNION ALL

    /* === TRANSFER-IN — sort_order 2 === */
    SELECT
        'Transfer-In'               AS type,
        it.transferId               AS keyid,
        iti.transferItemId * 2      AS entryid,
        DATE(it.transfer_date)      AS date,
        NULL                        AS contactid,
        iti.productId               AS productid,
        iti.quantity,
        iti.creationDate,
        iti.lastModifiedDate,
        iti.productName             AS product_name,
        cat.category_name,
        iti.measurementUnit         AS measurementunit,
        it.target_warehouse_id      AS warehouse_id,
        it.target_warehouse_name    AS warehousename,
        2                           AS sort_order
    FROM inventory_transfer it
    JOIN inventory_transfer_item iti ON iti.transfer_id = it.transferId
    JOIN Product p                   ON p.productid     = iti.productId
    JOIN Category cat                ON cat.categoryid  = p.categoryid
    WHERE it.is_deleted  = 0
      AND iti.is_deleted = 0
      AND it.target_tenant = DATABASE()

    UNION ALL

    /* === TRANSFER-OUT — sort_order 3 === */
    SELECT
        'Transfer-Out'              AS type,
        it.transferId               AS keyid,
        iti.transferItemId * 2 + 1  AS entryid,
        DATE(it.transfer_date)      AS date,
        NULL                        AS contactid,
        iti.productId               AS productid,
        iti.quantity,
        iti.creationDate,
        iti.lastModifiedDate,
        iti.productName             AS product_name,
        cat.category_name,
        iti.measurementUnit         AS measurementunit,
        it.source_warehouse_id      AS warehouse_id,
        it.source_warehouse_name    AS warehousename,
        3                           AS sort_order
    FROM inventory_transfer it
    JOIN inventory_transfer_item iti ON iti.transfer_id = it.transferId
    JOIN Product p                   ON p.productid     = iti.productId
    JOIN Category cat                ON cat.categoryid  = p.categoryid
    WHERE it.is_deleted  = 0
      AND iti.is_deleted = 0
      AND it.source_tenant = DATABASE()

    UNION ALL

    /* === OUTWARD (NORMAL) — sort_order 4 === */
    SELECT
        'Outward'               AS type,
        oi.outwardid            AS keyid,
        ioe.entryid             AS entryid,
        DATE(oi.date)           AS date,
        oi.contactid,
        ioe.productid,
        ioe.quantity,
        ioe.creationDate,
        ioe.lastModifiedDate,
        p.product_name,
        cat.category_name,
        p.measurementunit,
        w.warehouse_id,
        w.warehousename,
        4                       AS sort_order
    FROM outward_inventory oi
    JOIN outwardinventory_entry oie ON oi.outwardid   = oie.outwardid
    JOIN inward_outward_entries ioe ON oie.entryid    = ioe.entryid
    JOIN Product p                  ON p.productid    = ioe.productid
    JOIN Category cat               ON cat.categoryid = p.categoryid
    JOIN Warehouse w                ON w.warehouse_id = oi.warehouse_id
    WHERE oi.is_deleted = 0

    UNION ALL

    /* === LOST / DAMAGED — sort_order 5 === */
    SELECT
        'Lost-Damaged'          AS type,
        ldi.lostdamagedid       AS keyid,
        ldi.lostdamagedid       AS entryid,
        DATE(ldi.date)          AS date,
        NULL                    AS contactid,
        ldi.productid,
        ldi.quantity,
        ldi.creationDate,
        ldi.lastModifiedDate,
        p.product_name,
        cat.category_name,
        p.measurementunit,
        w.warehouse_id,
        w.warehousename,
        5                       AS sort_order
    FROM lost_damaged_inventory ldi
    JOIN Product p    ON p.productid    = ldi.productid
    JOIN Category cat ON cat.categoryid = p.categoryid
    JOIN Warehouse w  ON w.warehouse_id = ldi.warehousename
    WHERE ldi.is_deleted = 0 AND (ldi.entry_type = 'LOST_DAMAGED' OR ldi.entry_type IS NULL)

    UNION ALL

    /* === EXCESS FOUND — sort_order 5 === */
    SELECT
        'Excess-Found'          AS type,
        ldi.lostdamagedid       AS keyid,
        ldi.lostdamagedid       AS entryid,
        DATE(ldi.date)          AS date,
        NULL                    AS contactid,
        ldi.productid,
        ldi.quantity,
        ldi.creationDate,
        ldi.lastModifiedDate,
        p.product_name,
        cat.category_name,
        p.measurementunit,
        w.warehouse_id,
        w.warehousename,
        5                       AS sort_order
    FROM lost_damaged_inventory ldi
    JOIN Product p    ON p.productid    = ldi.productid
    JOIN Category cat ON cat.categoryid = p.categoryid
    JOIN Warehouse w  ON w.warehouse_id = ldi.warehousename
    WHERE ldi.is_deleted = 0 AND ldi.entry_type = 'EXCESS_FOUND'

) q
LEFT JOIN contacts c ON c.contactid = q.contactid;


DROP PROCEDURE IF EXISTS update_closing_stock;
DELIMITER //

CREATE PROCEDURE update_closing_stock()
BEGIN
    CREATE TEMPORARY TABLE IF NOT EXISTS TempCumulativeStock (
        entryid BIGINT,
        type VARCHAR(20),
        oldClosingStock DOUBLE,
        calculatedClosingStock DOUBLE
    );
    TRUNCATE TABLE TempCumulativeStock;

    INSERT INTO TempCumulativeStock (entryid, type, oldClosingStock, calculatedClosingStock)
    SELECT
        sr.entryid,
        sr.type,
        sr.oldClosingStock,
        SUM(
            CASE WHEN sr.type IN ('Inward', 'Transfer-In', 'Excess-Found') THEN sr.quantity ELSE 0 END
        ) OVER (
            PARTITION BY sr.warehouse_id, sr.productid
            ORDER BY sr.row_num
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        )
        -
        SUM(
            CASE WHEN sr.type IN ('Outward', 'Transfer-Out', 'Lost-Damaged') THEN sr.quantity ELSE 0 END
        ) OVER (
            PARTITION BY sr.warehouse_id, sr.productid
            ORDER BY sr.row_num
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS calculatedClosingStock
    FROM (
        SELECT
            ai.entryid,
            ai.type,
            ai.date,
            ai.warehouse_id,
            ai.productid,
            ai.quantity,
            ai.closingstock AS oldClosingStock,
            ROW_NUMBER() OVER (
                PARTITION BY ai.warehouse_id, ai.productid
                ORDER BY
                    ai.date       ASC,
                    ai.sort_order ASC,
                    ai.entryid    ASC    -- ← ASC to match view
            ) AS row_num
        FROM all_inventory ai
    ) sr;

    UPDATE inward_outward_entries e
    JOIN TempCumulativeStock tcs ON e.entryid = tcs.entryid
    SET e.closingstock = tcs.calculatedClosingStock
    WHERE tcs.oldClosingStock <> tcs.calculatedClosingStock;

    UPDATE inventory_transfer_item iti
    JOIN TempCumulativeStock tcs ON iti.transferItemId = tcs.entryid / 2
    SET iti.target_closing_stock = tcs.calculatedClosingStock
    WHERE tcs.type = 'Transfer-In'
      AND iti.target_closing_stock <> tcs.calculatedClosingStock;

    UPDATE inventory_transfer_item iti
    JOIN TempCumulativeStock tcs ON iti.transferItemId = (tcs.entryid - 1) / 2
    SET iti.source_closing_stock = tcs.calculatedClosingStock
    WHERE tcs.type = 'Transfer-Out'
      AND iti.source_closing_stock <> tcs.calculatedClosingStock;

    DROP TEMPORARY TABLE IF EXISTS TempCumulativeStock;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS update_all_inventory;

DELIMITER //

CREATE PROCEDURE update_all_inventory()
proc_end: BEGIN
    DECLARE last_execution DATETIME DEFAULT '2010-01-01 00:00:00';
    DECLARE min_modified_id BIGINT DEFAULT 9223372036854775807;
    DECLARE min_deleted_keyid BIGINT DEFAULT 9223372036854775807;
    DECLARE min_id BIGINT;
    DECLARE lock_acquired INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DO RELEASE_LOCK('update_all_inventory_lock');
        RESIGNAL;
    END;

    SELECT GET_LOCK('update_all_inventory_lock', 10) INTO lock_acquired;

    IF lock_acquired = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'update_all_inventory is already running';
    END IF;

    START TRANSACTION;

    SELECT COALESCE(MAX(last_execution), '2010-01-01 00:00:00')
    INTO last_execution
    FROM execution_history
    WHERE procedure_name = 'update_all_inventory';

    SELECT COALESCE(MIN(id), 9223372036854775807)
    INTO min_modified_id
    FROM all_inventory_view
    WHERE lastModifiedDate > last_execution;

    SELECT COALESCE(MIN(keyid), 9223372036854775807)
    INTO min_deleted_keyid
    FROM all_inventory ai
    WHERE NOT EXISTS (
        SELECT 1 FROM all_inventory_view aiv
        WHERE ai.keyid = aiv.keyid
    );

    SET min_id = LEAST(min_modified_id, min_deleted_keyid);

    IF min_id = 9223372036854775807 THEN
        INSERT INTO execution_history (procedure_name, last_execution)
        VALUES ('update_all_inventory', NOW())
        ON DUPLICATE KEY UPDATE last_execution = VALUES(last_execution);
        COMMIT;
        DO RELEASE_LOCK('update_all_inventory_lock');
        LEAVE proc_end;
    END IF;

    DELETE FROM all_inventory WHERE id >= min_id;

    INSERT INTO all_inventory (
        id,
        category_name,
        closingstock,
        contactid,
        contacttype,
        creationDate,
        date,
        emailid,
        entryid,
        keyid,
        lastModifiedDate,
        measurementunit,
        mobileno,
        name,
        productid,
        product_name,
        quantity,
        sort_order,
        type,
        warehouse_id,
        warehousename
    )
    SELECT
        id,
        category_name,
        closingstock,
        contactid,
        contacttype,
        creationDate,
        date,
        emailid,
        entryid,
        keyid,
        lastModifiedDate,
        measurementunit,
        mobileno,
        name,
        productid,
        product_name,
        quantity,
        sort_order,
        type,
        warehouse_id,
        warehousename
    FROM all_inventory_view
    WHERE id >= min_id;

    INSERT INTO execution_history (procedure_name, last_execution)
    VALUES ('update_all_inventory', NOW())
    ON DUPLICATE KEY UPDATE last_execution = VALUES(last_execution);

    COMMIT;
    DO RELEASE_LOCK('update_all_inventory_lock');

END //

DELIMITER ;


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
        warehouse_id,
        MIN(id) as id
	FROM all_inventory ai
    WHERE ai.date<='2021-03-01'
    GROUP BY Productid,warehouse_id
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
    IF(total_inward+total_excess_found-total_outward-total_lost_damaged=closing_stock,0,(t2.closing_stock+total_outward+total_lost_damaged-total_inward-total_excess_found)) as opening_stock,
    t1.total_inward,
    total_outward,
    total_lost_damaged,
    total_excess_found,
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
		SUM(IF(type='Lost-Damaged',quantity,0)) as total_lost_damaged,
		SUM(IF(type='Excess-Found',quantity,0)) as total_excess_found
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
SELECT
  row_number() OVER (
    ORDER BY `bu`.`id`
  ) AS `id`,
  `p`.`productId` AS `productId`,
  SUM(`bu`.`quantity`) AS `boqQuantity`,
  `bu`.`buildingTypeId` AS `buildingTypeId`,
  `bu`.`usageLocationId` AS `usageLocationId`,
  `ul`.`location_name` AS `buildingUnit`,
  `bt`.`building_type` AS `buildingType`,
  `p`.`product_name` AS `product`,
  `cg`.`category_name` AS `category`,
  0.0 AS `outwardQuantity`,
  0 AS `status`
FROM
  `BOQUpload` `bu`
  JOIN `Usage_Location` `ul` ON `bu`.`usageLocationId` = `ul`.`locationId`
  JOIN `Product` `p` ON `p`.`productId` = `bu`.`productId`
  JOIN `Category` `cg` ON `cg`.`categoryId` = `p`.`categoryId`
  JOIN `building_type` `bt` ON `bu`.`buildingTypeId` = `bt`.`typeId`
WHERE
  `bu`.`is_deleted` = false
GROUP BY
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
        o.category_name;

/* =====================================================
   SAFE INDEX CREATION (IGNORE IF ALREADY EXISTS)
   ===================================================== */

-- -------- all_inventory.lastModifiedDate --------
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'all_inventory'
  AND index_name = 'idx_all_inventory_last_modified';

SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_all_inventory_last_modified ON all_inventory (lastModifiedDate)',
    'SELECT ''idx_all_inventory_last_modified already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- -------- all_inventory.keyid --------
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'all_inventory'
  AND index_name = 'idx_all_inventory_keyid';

SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_all_inventory_keyid ON all_inventory (keyid)',
    'SELECT ''idx_all_inventory_keyid already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- -------- inward_outward_entries.lastModifiedDate --------
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'inward_outward_entries'
  AND index_name = 'idx_ioe_last_modified';

SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_ioe_last_modified ON inward_outward_entries (lastModifiedDate)',
    'SELECT ''idx_ioe_last_modified already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


DELIMITER //

DROP PROCEDURE IF EXISTS create_indexes_safe//

CREATE PROCEDURE create_indexes_safe()
BEGIN
    -- Only handle composite status/date indexes

    -- po_status_history
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
        AND table_name = 'po_status_history'
        AND index_name = 'idx_po_history_status_date'
    ) THEN
        DROP INDEX idx_po_history_status_date ON po_status_history;
    END IF;

    CREATE INDEX idx_po_history_status_date
    ON po_status_history (newStatus, changedAt);

    -- indent_status_history
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
        AND table_name = 'indent_status_history'
        AND index_name = 'idx_indent_history_status_date'
    ) THEN
        DROP INDEX idx_indent_history_status_date ON indent_status_history;
    END IF;

    CREATE INDEX idx_indent_history_status_date
    ON indent_status_history (newStatus, changedAt);

END//

DELIMITER ;


CALL create_indexes_safe();
DROP PROCEDURE create_indexes_safe;


-- -------- inventory_transfer_item.lastModifiedDate --------
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'inventory_transfer_item'
  AND index_name = 'idx_transfer_item_last_modified';

SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_transfer_item_last_modified ON inventory_transfer_item (lastModifiedDate)',
    'SELECT ''idx_transfer_item_last_modified already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- -------- lost_damaged_inventory.lastModifiedDate --------
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'lost_damaged_inventory'
  AND index_name = 'idx_lost_damaged_last_modified';

SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_lost_damaged_last_modified ON lost_damaged_inventory (lastModifiedDate)',
    'SELECT ''idx_lost_damaged_last_modified already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- Store procedure to update table from view
DROP PROCEDURE IF EXISTS update_all_inventory;

DELIMITER //

CREATE PROCEDURE update_all_inventory()
proc_end: BEGIN
    DECLARE last_execution DATETIME DEFAULT '2010-01-01 00:00:00';
    DECLARE min_modified_id BIGINT DEFAULT 9223372036854775807;
    DECLARE min_deleted_keyid BIGINT DEFAULT 9223372036854775807;
    DECLARE min_id BIGINT;
    DECLARE lock_acquired INT DEFAULT 0;

    /* =====================================================
       SAFETY: ENSURE LOCK RELEASE + ROLLBACK ON ERROR
       ===================================================== */
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DO RELEASE_LOCK('update_all_inventory_lock');
        RESIGNAL;
    END;

    /* =====================================================
       ACQUIRE GLOBAL LOCK (PREVENT PARALLEL RUNS)
       ===================================================== */
    SELECT GET_LOCK('update_all_inventory_lock', 10)
    INTO lock_acquired;

    IF lock_acquired = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'update_all_inventory is already running';
    END IF;

    /* =====================================================
       BEGIN TRANSACTION
       ===================================================== */
    START TRANSACTION;

    /* =====================================================
       FETCH LAST EXECUTION TIME
       ===================================================== */
    SELECT COALESCE(MAX(last_execution), '2010-01-01 00:00:00')
    INTO last_execution
    FROM execution_history
    WHERE procedure_name = 'update_all_inventory';

    /* =====================================================
       FIND EARLIEST MODIFIED RECORD
       ===================================================== */
    SELECT COALESCE(MIN(id), 9223372036854775807)
    INTO min_modified_id
    FROM all_inventory_view
    WHERE lastModifiedDate > last_execution;

    /* =====================================================
       FIND EARLIEST DELETED RECORD
       ===================================================== */
    SELECT COALESCE(MIN(keyid), 9223372036854775807)
    INTO min_deleted_keyid
    FROM all_inventory ai
    WHERE NOT EXISTS (
        SELECT 1
        FROM all_inventory_view aiv
        WHERE ai.keyid = aiv.keyid
    );

    /* =====================================================
       DETERMINE REFRESH START POINT
       ===================================================== */
    SET min_id = LEAST(min_modified_id, min_deleted_keyid);

    /* =====================================================
       NOTHING CHANGED → ONLY UPDATE EXECUTION HISTORY
       ===================================================== */
    IF min_id = 9223372036854775807 THEN
        INSERT INTO execution_history (procedure_name, last_execution)
        VALUES ('update_all_inventory', NOW())
        ON DUPLICATE KEY UPDATE
            last_execution = VALUES(last_execution);

        COMMIT;
        DO RELEASE_LOCK('update_all_inventory_lock');
        LEAVE proc_end;
    END IF;

    /* =====================================================
       DELETE STALE RECORDS
       ===================================================== */
    DELETE FROM all_inventory
    WHERE id >= min_id;

    /* =====================================================
       INSERT UPDATED RECORDS
       ===================================================== */
    INSERT INTO all_inventory (
        id,
        category_name,
        closingstock,
        contactid,
        contacttype,
        creationDate,
        date,
        emailid,
        entryid,
        keyid,
        lastModifiedDate,
        measurementunit,
        mobileno,
        name,
        productid,
        product_name,
        quantity,
        type,
        warehouse_id,
        warehousename
    )
    SELECT
        id,
        category_name,
        closingstock,
        contactid,
        contacttype,
        creationDate,
        date,
        emailid,
        entryid,
        keyid,
        lastModifiedDate,
        measurementunit,
        mobileno,
        name,
        productid,
        product_name,
        quantity,
        type,
        warehouse_id,
        warehousename
    FROM all_inventory_view
    WHERE id >= min_id;

    /* =====================================================
       UPDATE EXECUTION HISTORY
       ===================================================== */
    INSERT INTO execution_history (procedure_name, last_execution)
    VALUES ('update_all_inventory', NOW())
    ON DUPLICATE KEY UPDATE
        last_execution = VALUES(last_execution);

    /* =====================================================
       COMMIT + RELEASE LOCK
       ===================================================== */
    COMMIT;
    DO RELEASE_LOCK('update_all_inventory_lock');

END //

DELIMITER ;


###################  STOCk STATUS #############

CREATE OR REPLACE VIEW stockInformation as
	SELECT
		p.productId as productId,
        p.product_name,
        p.product_code,
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
    INNER JOIN Warehouse w on w.warehouse_id = s.warehouseId
	WHERE s.is_deleted=0
	GROUP BY p.productId,p.product_name,p.product_code,p.reorderQuantity,p.measurementUnit,c.category_name;

-- Stock Report
CREATE OR REPLACE VIEW stock_report AS
WITH product_stocks AS (
    SELECT
        s.productId,
        SUM(s.quantityInHand) as total_quantity
    FROM Stock s
    WHERE s.is_deleted = 0
    GROUP BY s.productId
    HAVING SUM(s.quantityInHand) > 0
),
last_inward_dates AS (
    SELECT DISTINCT
        ioe.productId,
        i.date as last_inward_date,
        co.name as supplier_name
    FROM inward_outward_entries ioe
    JOIN inwardinventory_entry ie ON ioe.entryid = ie.entryId
    JOIN inward_inventory i ON ie.inwardid = i.inwardid
    JOIN contacts co ON i.contactId = co.contactId
    WHERE ioe.is_deleted = 0
    AND (ioe.productId, i.date) IN (
        SELECT
            ioe2.productId,
            MAX(i2.date)
        FROM inward_outward_entries ioe2
        JOIN inwardinventory_entry ie2 ON ioe2.entryid = ie2.entryId
        JOIN inward_inventory i2 ON ie2.inwardid = i2.inwardid
        WHERE ioe2.is_deleted = 0
        GROUP BY ioe2.productId
    )
)
SELECT
    ROW_NUMBER() OVER (ORDER BY COALESCE(lid.last_inward_date, '1900-01-01') ASC, c.category_name, p.product_name) as sr_no,
    lid.last_inward_date,
    lid.supplier_name,
    c.category_name,
    p.product_name as item_name,
    ps.total_quantity as quantity,
    p.measurementUnit as measurement_unit,
    CONCAT_WS(', ',
        CASE
            WHEN FLOOR(DATEDIFF(CURRENT_DATE, lid.last_inward_date)/365) > 0
            THEN CONCAT(FLOOR(DATEDIFF(CURRENT_DATE, lid.last_inward_date)/365), ' years')
            ELSE NULL
        END,
        CASE
            WHEN FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 365)/30) > 0
            THEN CONCAT(FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 365)/30), ' months')
            ELSE NULL
        END,
        CASE
            WHEN FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 30)/7) > 0
            THEN CONCAT(FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 30)/7), ' weeks')
            ELSE NULL
        END,
        CASE
            WHEN (DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 7) > 0
            THEN CONCAT((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 7), ' days')
            ELSE NULL
        END
    ) as aging_period,
    '' as aging_reason,
    NULL as remark
FROM product_stocks ps
JOIN Product p ON ps.productId = p.productId
JOIN Category c ON p.categoryId = c.categoryId
LEFT JOIN last_inward_dates lid ON ps.productId = lid.productId;


-- Get indents for inward
CREATE OR REPLACE VIEW masterschema.IndentsForInward AS
SELECT
    iie.line_item_code AS lineItemCode,
    ii.indent_id,
    ii.indent_date,
    ii.indent_status,
    ii.createdBy AS indentCreatedBy,
    ii.tenant,
    iie.line_item_status,
    iie.productId,
    p.product_name,
    p.product_code,
    p.measurementUnit,
    iie.purchaseOrderId,
    iie.quantity,
    iie.remarks,
    iie.specification,
    po.po_date,
    po.purchase_order_id,
    po.grandTotal,
    po.status AS po_status,
    c.contactId AS supplier_id,
    c.name AS supplier_name,
    COALESCE(iip.total_inward_quantity, 0) AS total_inward_quantity,
    COALESCE(pol.quantity, iie.quantity)   AS poLineQuantity,
    COALESCE(pol.tolerance_percent, 0)     AS tolerancePercent

FROM masterschema.indent_inventory ii
INNER JOIN masterschema.indent_inventory_entries iie
    ON ii.indent_id = iie.indent_id
INNER JOIN masterschema.Product p
    ON p.productId = iie.productId
INNER JOIN masterschema.purchase_order po
    ON po.purchase_order_id = iie.purchaseOrderId
INNER JOIN masterschema.contacts c
    ON po.supplier_id = c.contactId
LEFT JOIN (
    SELECT indent_entry_id, SUM(inward_quantity) AS total_inward_quantity
    FROM masterschema.indent_inward_mapping
    GROUP BY indent_entry_id
) iip ON iie.entryid = iip.indent_entry_id
LEFT JOIN masterschema.purchase_order_line pol
    ON pol.po_id = po.purchase_order_id
    AND pol.product_id = iie.productId
WHERE
    iie.line_item_status IN ('PO Created', 'INWARD PARTIAL', 'INWARD COMPLETE')
    AND ii.is_deleted = 0
    AND iie.is_deleted = 0
    AND po.is_deleted = 0
    AND p.is_deleted = 0
    AND c.is_deleted = 0;




 SELECT COUNT(*) INTO @idx_exists
 FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND table_name = 'indent_inventory'
   AND index_name = 'idx_indent_stale_dashboard';

 SET @sql = IF(@idx_exists = 0,
     'CREATE INDEX idx_indent_stale_dashboard
      ON indent_inventory (last_status_updated_at, indent_status, tenant)',
     'SELECT ''Index already exists''');

 PREPARE stmt FROM @sql;
 EXECUTE stmt;
 DEALLOCATE PREPARE stmt;