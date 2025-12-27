-- ========================================
-- COMPLETE DATABASE SETUP SCRIPT
-- ========================================

-- WARNING: This will DROP all existing databases and recreate them!
-- Make sure you have backups before executing!

-- Step 1: DROP all databases
DROP DATABASE IF EXISTS suncitynxv2;
DROP DATABASE IF EXISTS kalpavrishv2;
DROP DATABASE IF EXISTS riddhisiddhiv2;
DROP DATABASE IF EXISTS smartcityv2;
DROP DATABASE IF EXISTS businessparkv2;
DROP DATABASE IF EXISTS drgtrdcntrv2;
DROP DATABASE IF EXISTS citycenterv2;
DROP DATABASE IF EXISTS schoolv2;
DROP DATABASE IF EXISTS bhaavbhumiv2;
DROP DATABASE IF EXISTS dhabbav2;
DROP DATABASE IF EXISTS mhvrtrdcntrv2;
DROP DATABASE IF EXISTS masterschema;

-- Step 2: CREATE all databases
CREATE DATABASE suncitynxv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE kalpavrishv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE riddhisiddhiv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE smartcityv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE businessparkv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE drgtrdcntrv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE citycenterv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE schoolv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE bhaavbhumiv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dhabbav2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE mhvrtrdcntrv2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE masterschema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ========================================
-- Define procedure to create all objects
-- ========================================

DELIMITER //

CREATE PROCEDURE setup_database_objects(IN db_name VARCHAR(64))
BEGIN
    SET @db = db_name;

    -- Switch to the specified database
    SET @sql = CONCAT('USE `', @db, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- Create all_inventory_view
    CREATE OR REPLACE VIEW all_inventory_view AS
    SELECT row_number() over (ORDER BY tx.date desc, tx.type desc, tx.keyid desc) as id,
           tx.type, tx.keyid, tx.entryid, tx.date,
           CASE WHEN tx.contactid = '' THEN 404 ELSE tx.contactid END as contactid,
           tx.warehouseid, tx.Productid, tx.quantity, tx.closingstock,
           tx.creationDate, tx.lastModifiedDate, tx.Product_name, tx.category_name,
           tx.measurementunit,
           CASE WHEN c.name IS NULL THEN '' ELSE c.name END as name,
           c.mobileno, c.emailid,
           CASE WHEN c.contacttype IS NULL THEN '' ELSE c.contacttype END as contacttype,
           tx.warehouse_id, tx.warehousename
    FROM (
        SELECT 'Inward' AS type, ii.inwardid as keyid, ioe.entryid as entryid,
               Date_format(ii.DATE, "%Y-%m-%d") AS date, ii.contactid AS contactid,
               ii.warehouse_id AS warehouseid, ioe.Productid AS Productid,
               ioe.quantity, ioe.closingstock, ioe.creationDate, ioe.lastModifiedDate,
               p.Product_name, cat.category_name, p.measurementunit, w.warehouse_id, w.warehousename
        FROM inward_inventory ii
        INNER JOIN inwardinventory_entry iie ON ii.inwardid = iie.inwardid
        INNER JOIN inward_outward_entries ioe ON iie.entryid = ioe.entryid
        INNER JOIN Product p on p.Productid = ioe.Productid
        INNER JOIN Category cat on p.categoryId = cat.categoryId
        INNER JOIN Warehouse w ON w.warehouse_id = ii.warehouse_id
        WHERE ii.is_deleted = 0
        UNION ALL
        SELECT 'Outward' AS type, oi.outwardid as keyid, ioe.entryid as entryid,
               Date_format(oi.DATE, "%Y-%m-%d") AS date, oi.contactid AS contactid,
               oi.warehouse_id AS warehouseid, ioe.Productid AS Productid,
               ioe.quantity, ioe.closingstock, ioe.creationDate, ioe.lastModifiedDate,
               p.Product_name, cat.category_name, p.measurementunit, w.warehouse_id, w.warehousename
        FROM outward_inventory oi
        INNER JOIN outwardinventory_entry oie ON oi.outwardid = oie.outwardid
        INNER JOIN inward_outward_entries ioe ON oie.entryid = ioe.entryid
        INNER JOIN Product p on p.Productid = ioe.Productid
        INNER JOIN Category cat on p.categoryId = cat.categoryId
        INNER JOIN Warehouse w ON w.warehouse_id = oi.warehouse_id
        WHERE oi.is_deleted = 0
        UNION ALL
        SELECT 'Lost-Damaged' AS type, lostdamagedid as keyid, lostdamagedid as entryid,
               Date_format(ldi.DATE, "%Y-%m-%d") AS date, '' AS contactid,
               ldi.warehousename AS warehouseid, ldi.Productid AS Productid,
               ldi.quantity, ldi.closingstock, ldi.creationDate, ldi.lastModifiedDate,
               p.Product_name, cat.category_name, p.measurementunit, w.warehouse_id, w.warehousename
        FROM lost_damaged_inventory ldi
        INNER JOIN Product p on p.Productid = ldi.Productid
        INNER JOIN Category cat on p.categoryId = cat.categoryId
        INNER JOIN Warehouse w ON w.warehouse_id = ldi.warehousename
        WHERE ldi.is_deleted = 0
    ) AS tx
    LEFT JOIN contacts c ON c.contactid = tx.contactid;

    -- Create stock_verification view
    CREATE OR REPLACE VIEW stock_verification AS
    SELECT inw.inventory, ROUND(total_inward,2) AS 'total_inward',
           ROUND(total_outward,2) AS 'total_outward', ROUND(current_stock,2) AS 'current_stock',
           ROUND(total_inward - (total_outward + current_stock),2) AS 'diff_in_Stock'
    FROM (
        SELECT p.product_name AS inventory, Sum(quantity) AS total_inward
        FROM inward_inventory ii
        INNER JOIN inwardinventory_entry iie ON iie.inwardid = ii.inwardid
        INNER JOIN inward_outward_entries ioe ON ioe.entryid = iie.entryid
        INNER JOIN Product p ON p.productid = ioe.productid
        WHERE ii.is_deleted = 0
        GROUP BY p.product_name
    ) AS inw
    INNER JOIN (
        SELECT p.product_name AS inventory, Sum(quantity) AS total_outward
        FROM outward_inventory oi
        INNER JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid
        INNER JOIN inward_outward_entries ioe ON ioe.entryid = oie.entryid
        INNER JOIN Product p ON p.productid = ioe.productid
        WHERE oi.is_deleted = 0
        GROUP BY p.product_name
    ) AS outw ON outw.inventory = inw.inventory
    INNER JOIN (
        SELECT p.product_name AS inventory, Sum(s.quantityinhand) AS current_stock
        FROM Stock s
        INNER JOIN Product p ON p.productid = s.productid
        GROUP BY p.product_name
    ) AS stock ON inw.inventory = stock.inventory
    WHERE ROUND(total_inward - (total_outward + current_stock),2) != 0;

    -- Create boq_status view
    CREATE OR REPLACE VIEW boq_status AS
    SELECT ex.*, ow.totalConsumedQuantity,
           FORMAT(CASE WHEN ow.totalConsumedQuantity/totalExpectedQuantity*100 IS NULL
                  THEN 0 ELSE ow.totalConsumedQuantity/totalExpectedQuantity*100 END,2)+0 as consumedPercent
    FROM (
        SELECT row_number() over (ORDER BY bt.typeId) as id, bt.typeId, bt.building_type,
               ul.locationId, ul.location_name, p.productId, p.product_name,
               CASE WHEN SUM(bi.quantity) IS NULL THEN 0 ELSE SUM(bi.quantity) END as totalExpectedQuantity
        FROM building_type bt
        INNER JOIN Usage_Location ul ON ul.typeId=bt.typeId AND ul.typeId IS NOT NULL AND ul.is_deleted=0
        INNER JOIN boq_inventory bi ON ((ul.locationId = bi.locationId OR ul.typeId = bi.typeId) AND bi.is_deleted = 0)
        INNER JOIN Product p on p.productId = bi.productId AND p.is_deleted = 0
        WHERE bt.is_deleted = 0
        GROUP BY bt.typeId, bt.building_type, ul.locationId, ul.location_name, p.productId, p.product_name
    ) as ex
    INNER JOIN (
        SELECT ul.locationId, ul.location_name, p.productId, p.product_name,
               CASE WHEN SUM(ioe.quantity) IS NULL THEN 0 ELSE SUM(ioe.quantity) END as totalConsumedQuantity
        FROM outward_inventory oi
        INNER JOIN outwardinventory_entry oie on oie.outwardid=oi.outwardid
        INNER JOIN inward_outward_entries ioe on ioe.entryid = oie.entryId AND ioe.is_deleted=0
        INNER JOIN Usage_Location ul ON oi.locationId=ul.locationId and ul.is_deleted=0
        INNER JOIN Product p on p.productId = ioe.productId AND p.is_deleted = 0
        GROUP BY locationId, location_name, productId, product_name
    ) as ow ON ex.locationId=ow.locationId AND ex.productId=ow.productId;

    -- Create inwardoutwardtrend view
    CREATE OR REPLACE VIEW inwardoutwardtrend AS
    SELECT t.date,
           CASE WHEN ii.count IS NULL THEN 0 ELSE ii.count END as inward_count,
           CASE WHEN oi.count IS NULL THEN 0 ELSE oi.count END as outward_count
    FROM (
        SELECT DATE_FORMAT(curdate(),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 1 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 2 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 3 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 4 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 5 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 6 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 7 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 8 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 9 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 10 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 11 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 12 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 13 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 14 day),'%d-%m-%Y') as date UNION
        SELECT DATE_FORMAT(DATE_SUB(curdate(), INTERVAL 15 day),'%d-%m-%Y') as date
    ) as t
    LEFT JOIN (
        SELECT ii.date as fulldate, DATE_FORMAT(ii.date,'%d-%m-%Y') as date, COUNT(ioe.entryid) as count
        FROM inward_inventory ii
        INNER JOIN inwardinventory_entry iie on iie.inwardid = ii.inwardid
        INNER JOIN inward_outward_entries ioe on ioe.entryid=iie.entryid
        WHERE ii.is_deleted=0 AND ioe.is_deleted=0 AND ii.date >= (CURDATE() - INTERVAL 15 DAY)
        GROUP BY ii.date, DATE_FORMAT(ii.date,'%d-%m-%Y')
        ORDER BY ii.date DESC
    ) as ii on ii.date=t.date
    LEFT JOIN (
        SELECT oi.date as fulldate, DATE_FORMAT(oi.date,'%d-%m-%Y') as date, COUNT(ioe.entryid) as count
        FROM outward_inventory oi
        INNER JOIN outwardinventory_entry oie on oie.outwardid = oi.outwardid
        INNER JOIN inward_outward_entries ioe on ioe.entryid=oie.entryid
        WHERE oi.is_deleted=0 AND ioe.is_deleted=0 AND oi.date >= (CURDATE() - INTERVAL 15 DAY)
        GROUP BY oi.date, DATE_FORMAT(oi.date,'%d-%m-%Y')
        ORDER BY oi.date DESC
    ) as oi on oi.date=t.date;

    -- Create inward_stats view
    CREATE OR REPLACE VIEW inward_stats AS
    SELECT totalInward.name as supplier_name, totalInward.count as inward_count,
           CASE WHEN leadTime.lead_time IS NULL THEN 0 ELSE leadTime.lead_time END as avg_lead_time,
           CASE WHEN rejectCount.reject_count IS NULL THEN 0 ELSE rejectCount.reject_count END as rejectCount
    FROM (
        SELECT c.name, count(inwardid) as count
        FROM inward_inventory ii
        INNER JOIN contacts c on c.contactid=ii.contactid
        WHERE ii.is_deleted=0
        GROUP BY c.name
    ) as totalInward
    LEFT JOIN (
        SELECT c.name, ROUND(AVG(DATEDIFF(ii.date,ii.purchaseOrderdate)),2) as lead_time
        FROM inward_inventory ii
        INNER JOIN contacts c on c.contactid=ii.contactid
        WHERE ii.is_deleted=0 AND ii.purchaseOrderdate IS NOT NULL AND ii.purchaseOrderdate <= ii.date
        GROUP BY c.name
    ) AS leadTime on totalInward.name=leadTime.name
    LEFT JOIN (
        SELECT c.name, COUNT(DISTINCT ii.inwardid) as reject_count
        FROM inward_inventory ii
        INNER JOIN contacts c on c.contactid=ii.contactid
        INNER JOIN rejectInward_entry rie on rie.inwardid=ii.inwardid
        INNER JOIN reject_inward_entries re on re.rejectentryid=rie.rejectentryid
        WHERE ii.is_deleted=0
        GROUP BY c.name
    ) as rejectCount on leadTime.name=rejectCount.name;

    -- Create outward_stats view
    CREATE OR REPLACE VIEW outward_stats AS
    SELECT totalOutward.name as contractor_name, totalOutward.total_count as total_count,
           CASE WHEN rejectCount.reject_count IS NULL THEN 0 ELSE rejectCount.reject_count END as reject_count
    FROM (
        SELECT c.name, COUNT(oi.outwardid) as total_count
        FROM outward_inventory oi
        INNER JOIN contacts c on c.contactId=oi.contactId
        WHERE oi.is_deleted=0
        GROUP BY c.name
    ) as totalOutward
    LEFT JOIN (
        SELECT c.name, COUNT(DISTINCT oi.outwardid) as reject_count
        FROM outward_inventory oi
        INNER JOIN contacts c on c.contactId=oi.contactId
        INNER JOIN rejectOutward_entry roe on roe.outwardid=oi.outwardid
        INNER JOIN reject_outward_entries rie ON rie.rejectentryid=roe.rejectentryid
        WHERE oi.is_deleted=0
        GROUP BY c.name
    ) as rejectCount on rejectCount.name=totalOutward.name;

    -- Create inventoryreport view
    CREATE OR REPLACE VIEW inventoryreport AS
    SELECT t2.id, t1.month, t1.product_name, t1.measurementunit, t1.category_name, t1.warehousename,
           IF(total_inward-total_outward-total_lost_damaged=closing_stock,0,
              (t2.closing_stock+total_outward+total_lost_damaged-total_inward)) as opening_stock,
           t1.total_inward, total_outward, total_lost_damaged, t2.closing_stock
    FROM (
        SELECT DATE_FORMAT(date,'%Y-%m') as month, category_name, product_name, measurementunit, ai1.warehousename,
               SUM(IF(type='Inward',quantity,0)) as total_inward,
               SUM(IF(type='Outward',quantity,0)) as total_outward,
               SUM(IF(type='Lost-Damaged',quantity,0)) as total_lost_damaged
        FROM all_inventory ai1
        GROUP BY month, category_name, product_name, measurementunit, ai1.warehousename
        ORDER BY month, category_name, product_name, measurementunit, ai1.warehousename
    ) AS t1
    INNER JOIN (
        SELECT DATE_FORMAT(date,'%Y-%m') as month, ai.id, ai.closingStock AS closing_stock,
               ai.product_name, ai.category_name, ai.warehousename
        FROM all_inventory ai
        INNER JOIN (
            SELECT DATE_FORMAT(date,'%Y-%m') as month, product_name, category_name, warehousename,
                   MIN(id) AS id
            FROM all_inventory
            GROUP BY month, product_name, category_name, warehousename
        ) latest ON latest.id = ai.id
    ) as t2 ON t1.month=t2.month AND t1.category_name=t2.category_name
               AND t1.product_name=t2.product_name AND t1.warehousename=t2.warehousename
    ORDER BY t1.month desc, product_name, warehousename;

    -- Create boq_status_view
    CREATE OR REPLACE VIEW boq_status_view AS
    SELECT row_number() OVER (ORDER BY `bu`.`id`) AS `id`,
           `p`.`productId` AS `productId`, SUM(`bu`.`quantity`) AS `boqQuantity`,
           `bu`.`buildingTypeId` AS `buildingTypeId`, `bu`.`usageLocationId` AS `usageLocationId`,
           `ul`.`location_name` AS `buildingUnit`, `bt`.`building_type` AS `buildingType`,
           `p`.`product_name` AS `product`, `cg`.`category_name` AS `category`,
           0.0 AS `outwardQuantity`, 0 AS `status`
    FROM `BOQUpload` `bu`
    JOIN `Usage_Location` `ul` ON `bu`.`usageLocationId` = `ul`.`locationId`
    JOIN `Product` `p` ON `p`.`productId` = `bu`.`productId`
    JOIN `Category` `cg` ON `cg`.`categoryId` = `p`.`categoryId`
    JOIN `building_type` `bt` ON `bu`.`buildingTypeId` = `bt`.`typeId`
    WHERE `bu`.`is_deleted` = false
    GROUP BY `bu`.`id`, `bu`.`buildingTypeId`, `bu`.`usageLocationId`, `bu`.`productId`,
             `ul`.`location_name`, `p`.`product_name`, `cg`.`category_name`, `p`.`productId`;

    -- Create MissingInventoryPricingByMonth view
    CREATE OR REPLACE VIEW MissingInventoryPricingByMonth AS
    SELECT row_number() over () as id, s.*
    FROM (
        SELECT oi.*
        FROM (
            SELECT DISTINCT c.category_name as categoryName, ioe.productId as productId,
                   p.measurementUnit, p.product_name productName,
                   DATE_FORMAT(oi.date,'%Y-%m') AS date
            FROM outward_inventory oi
            INNER JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid
            INNER JOIN inward_outward_entries ioe ON ioe.entryid = oie.entryid
            INNER JOIN Product p on p.productId = ioe.productId
            INNER JOIN Category c ON c.categoryId=p.categoryId
            WHERE oi.is_deleted=0 AND ioe.is_deleted=0
        ) as oi
        LEFT JOIN InventoryMonthPriceMapping imp
                  ON imp.productId = oi.productId
                  AND DATE_FORMAT(imp.date,'%Y-%m') = oi.date
                  AND imp.is_deleted=0
        WHERE imp.date IS NULL
        ORDER BY oi.categoryName, oi.productId, oi.productName, oi.date
    ) s;

    -- Create InventoryMonthUsageInformation view
    CREATE OR REPLACE VIEW InventoryMonthUsageInformation AS
    SELECT t.*, imp.price, TRUNCATE((t.totalQuantity * imp.price),2) as totalPrice
    FROM (
        SELECT DISTINCT
               row_number() over (ORDER BY oi.locationId, ul.location_name, c.categoryId,
                                 c.category_name, ioe.productId, p.product_name,
                                 DATE_FORMAT(oi.date,'%y-%m')) as id,
               oi.locationId, ul.location_name as locationName, c.categoryId,
               c.category_name as categoryName, ioe.productId, p.product_name as productName,
               DATE_FORMAT(oi.date,'%y-%m') as ym,
               TRUNCATE(SUM(ioe.quantity),2) as totalQuantity
        FROM outward_inventory oi
        INNER JOIN outwardinventory_entry oie on oi.outwardid=oie.outwardid
        INNER JOIN inward_outward_entries ioe on ioe.entryid = oie.entryId
        INNER JOIN Usage_Location ul on ul.locationId = oi.locationId
        INNER JOIN Product p on p.productId = ioe.productId
        INNER JOIN Category c on c.categoryId = p.categoryId
        WHERE oi.is_deleted = 0 AND ioe.is_deleted = 0
        GROUP BY oi.locationId, ul.location_name, c.categoryId, c.category_name,
                 ioe.productId, p.product_name, DATE_FORMAT(oi.date,'%y-%m')
    ) as t
    LEFT JOIN InventoryMonthPriceMapping imp
              on DATE_FORMAT(imp.date,'%y-%m') = t.ym
              AND imp.productId=t.productId
              AND imp.is_deleted=0;

    -- Create boq_status_view2
    CREATE OR REPLACE VIEW boq_status_view2 AS
    SELECT row_number() OVER () as id, o.buildingTypeId, o.building_type, o.location_id,
           o.location_name, o.productId, o.product_name, o.category_name,
           ROUND(SUM(o.boq_quantity),2) as total_boq_quantity,
           ROUND(SUM(o.outward_quanity),2) as total_outward_quantity,
           JSON_ARRAYAGG(JSON_OBJECT(
               'finalLocationId', o.final_location_id,
               'finalLocationName', o.final_location_name,
               'total_boq_quantity', o.boq_quantity,
               'total_outward_quantity', o.outward_quanity,
               'status', o.status
           )) as detailed,
           ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) as status,
           CASE
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <= 10 THEN '0-10 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 10
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=20 THEN '10-20 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 20
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=30 THEN '20-30 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 30
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=40 THEN '30-40 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 40
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=50 THEN '40-50 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 50
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=60 THEN '50-60 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 60
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=70 THEN '60-70 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 70
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=80 THEN '70-80 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 80
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=90 THEN '80-90 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 90
                    AND ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) <=100 THEN '90-100 %'
               WHEN ROUND(((SUM(o.outward_quanity) - SUM(o.boq_quantity))/SUM(o.boq_quantity) * 100),2) > 100 THEN 'above 100 %'
           END as statusBucket
    FROM (
        SELECT t.buildingTypeId, btype.building_type, t.usageLocationId as location_id,
               ul.location_name, t.locationId as final_location_id, ua.usagearea_name as final_location_name,
               t.productId, p.product_name, c.category_name, t.boq_quantity,
               CASE WHEN t.outward_quanity IS NULL THEN 0 ELSE t.outward_quanity END as outward_quanity,
               CASE WHEN t.outward_quanity IS NULL THEN 0
                    ELSE ROUND((t.outward_quanity-t.boq_quantity)/t.boq_quantity*100,2) END AS status
        FROM (
            SELECT bu.buildingTypeId, bu.usageLocationId, bu.locationId, bu.productId,
                   bu.boq_quantity, oi.outward_quanity
            FROM (
                SELECT bu.buildingTypeId, bu.usageLocationId, bu.locationId, bu.productId,
                       quantity as boq_quantity
                FROM BOQUpload bu
                WHERE bu.is_deleted=0
            ) bu
            LEFT JOIN (
                SELECT oi.locationId as usageLocationId, oi.usageAreaId as locationId,
                       ioe.productId, SUM(quantity) as outward_quanity
                FROM outward_inventory oi
                INNER JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid
                INNER JOIN inward_outward_entries ioe ON ioe.entryid = oie.entryId
                WHERE oi.is_deleted=0
                GROUP BY oi.locationId, oi.usageAreaId, ioe.productId
            ) oi ON bu.usageLocationId=oi.usageLocationId
                    AND bu.locationId=oi.locationId
                    AND bu.productId=oi.productId
        ) t
        INNER JOIN building_type btype ON t.buildingTypeId = btype.typeId
        INNER JOIN Usage_Location ul ON t.usageLocationId = ul.locationId
        INNER JOIN usage_area ua ON ua.usageAreaId = t.locationId
        INNER JOIN Product p ON p.productId = t.productId
        INNER JOIN Category c ON c.categoryId = p.categoryId
        ORDER BY btype.building_type, ul.location_name, c.category_name, p.product_name
    ) o
    GROUP BY o.buildingTypeId, o.building_type, o.location_id, o.location_name,
             o.productId, o.product_name, o.category_name;

    -- Create stockInformation view
    CREATE OR REPLACE VIEW stockInformation as
    SELECT p.productId as productId, p.product_name, p.reorderQuantity, p.measurementUnit,
           c.category_name, ROUND(SUM(s.quantityInHand),2) as totalQuantityInHand,
           CASE WHEN ROUND(SUM(s.quantityInHand),2)<=p.reorderQuantity THEN 'Low' ELSE 'High' END as stockStatus,
           JSON_ARRAYAGG(JSON_OBJECT(
               'warehouseName', w.warehouseName,
               'quantityInHand', s.quantityInHand,
               'measurementUnit', p.measurementUnit
           )) as detailedStock
    FROM Stock s
    INNER JOIN Product p on p.productId=s.productId
    INNER JOIN Category c on p.categoryId=c.categoryId
    INNER JOIN Warehouse w on w.warehouse_id = s.warehouseName
    WHERE s.is_deleted=0
    GROUP BY p.productId, p.product_name, p.reorderQuantity, p.measurementUnit, c.category_name;

    -- Create stock_report view
    CREATE OR REPLACE VIEW stock_report AS
    WITH product_stocks AS (
        SELECT s.productId, SUM(s.quantityInHand) as total_quantity
        FROM Stock s
        WHERE s.is_deleted = 0
        GROUP BY s.productId
        HAVING SUM(s.quantityInHand) > 0
    ),
    last_inward_dates AS (
        SELECT DISTINCT ioe.productId, i.date as last_inward_date, co.name as supplier_name
        FROM inward_outward_entries ioe
        JOIN inwardinventory_entry ie ON ioe.entryid = ie.entryId
        JOIN inward_inventory i ON ie.inwardid = i.inwardid
        JOIN contacts co ON i.contactId = co.contactId
        WHERE ioe.is_deleted = 0
        AND (ioe.productId, i.date) IN (
            SELECT ioe2.productId, MAX(i2.date)
            FROM inward_outward_entries ioe2
            JOIN inwardinventory_entry ie2 ON ioe2.entryid = ie2.entryId
            JOIN inward_inventory i2 ON ie2.inwardid = i2.inwardid
            WHERE ioe2.is_deleted = 0
            GROUP BY ioe2.productId
        )
    )
    SELECT ROW_NUMBER() OVER (ORDER BY COALESCE(lid.last_inward_date, '1900-01-01') ASC,
                                      c.category_name, p.product_name) as sr_no,
           lid.last_inward_date, lid.supplier_name, c.category_name,
           p.product_name as item_name, ps.total_quantity as quantity,
           p.measurementUnit as measurement_unit,
           CONCAT_WS(', ',
               CASE WHEN FLOOR(DATEDIFF(CURRENT_DATE, lid.last_inward_date)/365) > 0
                    THEN CONCAT(FLOOR(DATEDIFF(CURRENT_DATE, lid.last_inward_date)/365), ' years')
                    ELSE NULL END,
               CASE WHEN FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 365)/30) > 0
                    THEN CONCAT(FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 365)/30), ' months')
                    ELSE NULL END,
               CASE WHEN FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 30)/7) > 0
                    THEN CONCAT(FLOOR((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 30)/7), ' weeks')
                    ELSE NULL END,
               CASE WHEN (DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 7) > 0
                    THEN CONCAT((DATEDIFF(CURRENT_DATE, lid.last_inward_date) % 7), ' days')
                    ELSE NULL END
           ) as aging_period,
           '' as aging_reason, NULL as remark
    FROM product_stocks ps
    JOIN Product p ON ps.productId = p.productId
    JOIN Category c ON p.categoryId = c.categoryId
    LEFT JOIN last_inward_dates lid ON ps.productId = lid.productId;

    -- Create execution_history table
    CREATE TABLE IF NOT EXISTS execution_history (
        id INT AUTO_INCREMENT PRIMARY KEY,
        procedure_name VARCHAR(255) NOT NULL,
        last_execution DATETIME NOT NULL
    );

    -- Insert initial execution history
    INSERT IGNORE INTO execution_history (procedure_name, last_execution)
    VALUES ('update_all_inventory', '2010-01-01');

    -- Create all_inventory table
    CREATE TABLE IF NOT EXISTS all_inventory (
        id BIGINT PRIMARY KEY,
        category_name VARCHAR(255),
        closingstock DOUBLE,
        contactid BIGINT,
        contacttype VARCHAR(50),
        creationDate DATETIME,
        date DATE,
        emailid VARCHAR(255),
        entryid BIGINT,
        keyid BIGINT,
        lastModifiedDate DATETIME,
        measurementunit VARCHAR(50),
        mobileno VARCHAR(20),
        name VARCHAR(255),
        productid BIGINT,
        product_name VARCHAR(255),
        quantity DOUBLE,
        type VARCHAR(50),
        warehouse_id BIGINT,
        warehousename VARCHAR(255),
        INDEX idx_date (date),
        INDEX idx_product_name (product_name),
        INDEX idx_category_name (category_name),
        INDEX idx_warehousename (warehousename)
    );

    -- Drop existing stored procedures if they exist
    DROP PROCEDURE IF EXISTS update_closing_stock;
    DROP PROCEDURE IF EXISTS update_all_inventory;
END //

DELIMITER ;

-- ========================================
-- Execute setup for each database
-- ========================================

-- Setup suncitynxv2
CALL setup_database_objects('suncitynxv2');

-- Setup kalpavrishv2
CALL setup_database_objects('kalpavrishv2');

-- Setup riddhisiddhiv2
CALL setup_database_objects('riddhisiddhiv2');

-- Setup smartcityv2
CALL setup_database_objects('smartcityv2');

-- Setup businessparkv2
CALL setup_database_objects('businessparkv2');

-- Setup drgtrdcntrv2
CALL setup_database_objects('drgtrdcntrv2');

-- Setup citycenterv2
CALL setup_database_objects('citycenterv2');

-- Setup schoolv2
CALL setup_database_objects('schoolv2');

-- Setup bhaavbhumiv2
CALL setup_database_objects('bhaavbhumiv2');

-- Setup dhabbav2
CALL setup_database_objects('dhabbav2');

-- Setup mhvrtrdcntrv2
CALL setup_database_objects('mhvrtrdcntrv2');

-- Setup masterschema
CALL setup_database_objects('masterschema');

-- ========================================
-- Now create the stored procedures in each database
-- ========================================

DELIMITER //

CREATE PROCEDURE create_procedures_in_database(IN db_name VARCHAR(64))
BEGIN
    SET @db = db_name;

    -- Switch to database
    SET @sql = CONCAT('USE `', @db, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- Create update_closing_stock procedure
    DROP PROCEDURE IF EXISTS update_closing_stock;

    CREATE PROCEDURE update_closing_stock()
    BEGIN
        CREATE TEMPORARY TABLE IF NOT EXISTS TempCumulativeStock (
            entryid BIGINT,
            oldClosingStock DOUBLE,
            calculatedClosingStock DOUBLE
        );

        INSERT INTO TempCumulativeStock (entryid, oldClosingStock, calculatedClosingStock)
        SELECT sr.entryid, sr.oldClosingStock,
               SUM(CASE WHEN tx.type = 'Inward' THEN tx.quantity ELSE 0 END) OVER (
                   PARTITION BY sr.warehouse_id, sr.productid
                   ORDER BY sr.row_num
                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
               ) -
               SUM(CASE WHEN tx.type IN ('Outward', 'Lost-Damaged') THEN tx.quantity ELSE 0 END) OVER (
                   PARTITION BY sr.warehouse_id, sr.productid
                   ORDER BY sr.row_num
                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
               ) AS calculatedClosingStock
        FROM (
            SELECT tx.entryid, tx.date, tx.warehouse_id AS warehouse_id, tx.productid AS productid,
                   tx.quantity, tx.closingstock AS oldClosingStock,
                   ROW_NUMBER() OVER (
                       PARTITION BY tx.warehouse_id, tx.productid
                       ORDER BY tx.date ASC, tx.type ASC, tx.keyid DESC
                   ) AS row_num
            FROM all_inventory tx
        ) sr
        JOIN all_inventory tx ON sr.entryid = tx.entryid;

        UPDATE inward_outward_entries e
        JOIN TempCumulativeStock ccs ON e.entryid = ccs.entryid
        SET e.closingstock = ccs.calculatedClosingStock
        WHERE ccs.oldClosingStock <> ccs.calculatedClosingStock;

        DROP TEMPORARY TABLE IF EXISTS TempCumulativeStock;
    END;

    -- Create update_all_inventory procedure
    DROP PROCEDURE IF EXISTS update_all_inventory;

    CREATE PROCEDURE update_all_inventory()
    BEGIN
        DECLARE last_execution DATETIME DEFAULT '2010-01-01 00:00:00';
        DECLARE min_modified_id BIGINT DEFAULT 9223372036854775807;
        DECLARE min_deleted_keyid BIGINT DEFAULT 9223372036854775807;
        DECLARE min_id BIGINT;

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
            SELECT 1 FROM all_inventory_view aiv WHERE ai.keyid = aiv.keyid
        );

        SET min_id = LEAST(min_modified_id, min_deleted_keyid);

        DELETE FROM all_inventory WHERE id >= min_id;

        INSERT INTO all_inventory (
            id, category_name, closingstock, contactid, contacttype, creationDate, date,
            emailid, entryid, keyid, lastModifiedDate, measurementunit, mobileno, name,
            productid, product_name, quantity, type, warehouse_id, warehousename
        )
        SELECT id, category_name, closingstock, contactid, contacttype, creationDate, date,
               emailid, entryid, keyid, lastModifiedDate, measurementunit, mobileno, name,
               productid, product_name, quantity, type, warehouse_id, warehousename
        FROM all_inventory_view
        WHERE id >= min_id;

        INSERT INTO execution_history (last_execution, procedure_name)
        VALUES (NOW(), 'update_all_inventory');

        COMMIT;
    END;
END //

DELIMITER ;

-- Create procedures in each database
CALL create_procedures_in_database('suncitynxv2');
CALL create_procedures_in_database('kalpavrishv2');
CALL create_procedures_in_database('riddhisiddhiv2');
CALL create_procedures_in_database('smartcityv2');
CALL create_procedures_in_database('businessparkv2');
CALL create_procedures_in_database('drgtrdcntrv2');
CALL create_procedures_in_database('citycenterv2');
CALL create_procedures_in_database('schoolv2');
CALL create_procedures_in_database('bhaavbhumiv2');
CALL create_procedures_in_database('dhabbav2');
CALL create_procedures_in_database('mhvrtrdcntrv2');
CALL create_procedures_in_database('masterschema');

-- Clean up temporary procedures
DROP PROCEDURE IF EXISTS setup_database_objects;
DROP PROCEDURE IF EXISTS create_procedures_in_database;

-- ========================================
-- Setup complete!
-- ========================================
SELECT 'Database setup complete!' as Status;