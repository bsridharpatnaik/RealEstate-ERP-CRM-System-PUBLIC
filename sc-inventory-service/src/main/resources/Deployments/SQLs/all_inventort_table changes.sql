drop view IF EXISTS all_inventory;
CREATE OR replace VIEW all_inventory_view
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

CREATE TABLE IF NOT EXISTS all_inventory (
    id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    type VARCHAR(12) NOT NULL,
    keyid BIGINT NOT NULL DEFAULT 0,
    entryid BIGINT NOT NULL DEFAULT 0,
    date VARCHAR(10) DEFAULT NULL,
    contactid VARCHAR(20) NOT NULL,
    warehouseid BIGINT NOT NULL DEFAULT 0,
    Productid BIGINT NOT NULL DEFAULT 0,
    quantity DOUBLE DEFAULT NULL,
    closingstock DOUBLE DEFAULT NULL,
    creationDate DATETIME NOT NULL,
    lastModifiedDate DATETIME NOT NULL,
    Product_name VARCHAR(255) DEFAULT NULL,
    category_name VARCHAR(255) DEFAULT NULL,
    measurementunit VARCHAR(255) DEFAULT NULL,
    name VARCHAR(255) DEFAULT NULL,
    mobileno VARCHAR(255) DEFAULT NULL,
    emailid VARCHAR(255) DEFAULT NULL,
    contacttype VARCHAR(255) DEFAULT NULL,
    warehouse_id BIGINT NOT NULL DEFAULT 0,
    warehousename VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS last_execution (
    id INT AUTO_INCREMENT PRIMARY KEY,
    procedure_name VARCHAR(255) NOT NULL,
    last_exec_time DATETIME NOT NULL
);

INSERT IGNORE INTO `last_execution`
(
`procedure_name`,
`last_exec_time`)
VALUES
(
'update_all_inventory',
'2010-01-01');


-- Store procedure to update table from view
DROP PROCEDURE IF EXISTS update_all_inventory;

DELIMITER //

CREATE PROCEDURE update_all_inventory()
BEGIN
    DECLARE last_exec_time DATETIME;

    -- Start a new transaction
    START TRANSACTION;

    -- Get the last execution time for the procedure 'update_all_inventory'
    SELECT last_exec_time INTO last_exec_time
    FROM last_execution
    WHERE procedure_name = 'update_all_inventory'
    ORDER BY id DESC
    LIMIT 1;

    -- Ensure last_exec_time has a value, default to '2010-01-01' if not
    IF last_exec_time IS NULL THEN
        SET last_exec_time = '2010-01-01';
    END IF;

    -- Delete records from the main table that are present in the temporary table
    DELETE FROM all_inventory
    WHERE lastModifiedDate > last_exec_time;

    -- Insert updated records from the temporary table into the main table
    INSERT INTO all_inventory
    SELECT * FROM all_inventory_view
    WHERE lastModifiedDate > last_exec_time;

    -- Update the last execution time for 'update_all_inventory'
    INSERT INTO last_execution (last_exec_time, procedure_name)
    VALUES (NOW(), 'update_all_inventory')
    ON DUPLICATE KEY UPDATE
        last_exec_time = VALUES(last_exec_time);

    -- Commit the transaction
    COMMIT;
END //

DELIMITER ;
