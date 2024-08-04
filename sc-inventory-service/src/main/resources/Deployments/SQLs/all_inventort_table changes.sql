DROP PROCEDURE IF EXISTS update_closing_stock;

DELIMITER //

DROP PROCEDURE IF EXISTS update_closing_stock;
CREATE PROCEDURE update_closing_stock()
BEGIN
    -- Calculate cumulative closing stock
    WITH SortedRecords AS (
        SELECT
            tx.entryid,
            tx.date,
            tx.warehouseid,
            tx.Productid,
            tx.quantity,
            tx.closingstock AS oldClosingStock,
            -- Assign row numbers based on the updated sorting order
            ROW_NUMBER() OVER (
                PARTITION BY tx.warehouseid, tx.Productid
                ORDER BY tx.date ASC, tx.type ASC, tx.keyid DESC
            ) AS row_num
        FROM all_inventory tx
    ),
    CumulativeStock AS (
        SELECT
            sr.entryid,
            sr.oldClosingStock,
            -- Calculate cumulative inward and outward quantities
            SUM(CASE
                    WHEN tx.type = 'Inward' THEN tx.quantity
                    ELSE 0
                END) OVER (
                    PARTITION BY tx.warehouseid, tx.Productid
                    ORDER BY sr.row_num
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                ) AS cumulative_inward,
            SUM(CASE
                    WHEN tx.type IN ('Outward', 'Lost-Damaged') THEN tx.quantity
                    ELSE 0
                END) OVER (
                    PARTITION BY tx.warehouseid, tx.Productid
                    ORDER BY sr.row_num
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                ) AS cumulative_outward
        FROM SortedRecords sr
        JOIN all_inventory tx ON sr.entryid = tx.entryid
    )
    -- Update the closingstock field where discrepancies are found
    UPDATE inward_outward_entries e
    JOIN (
        SELECT
            entryid,
            oldClosingStock,
            cumulative_inward - cumulative_outward AS calculatedClosingStock
        FROM CumulativeStock
    ) ccs ON e.entryid = ccs.entryid
    SET e.closingstock = ccs.calculatedClosingStock
    WHERE ccs.oldClosingStock <> ccs.calculatedClosingStock;
END //

DELIMITER ;

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


CREATE TABLE IF NOT EXISTS execution_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    procedure_name VARCHAR(255) NOT NULL,
    last_execution DATETIME NOT NULL
);

INSERT IGNORE INTO `execution_history`
(
`procedure_name`,
`last_execution`)
VALUES
(
'update_all_inventory',
'2010-01-01');


-- Store procedure to update table from view
DROP PROCEDURE IF EXISTS update_all_inventory;

DELIMITER //

CREATE PROCEDURE update_all_inventory()
BEGIN
    DECLARE last_execution DATETIME DEFAULT '2010-01-01 00:00:00';
    DECLARE min_modified_id BIGINT DEFAULT 9223372036854775807;
    DECLARE min_deleted_keyid BIGINT DEFAULT 9223372036854775807;
    DECLARE min_id BIGINT;

    -- Start a new transaction
    START TRANSACTION;

    -- Get the last execution time for the procedure 'update_all_inventory'
    SELECT COALESCE(MAX(last_execution), '2010-01-01 00:00:00')
    INTO last_execution
    FROM execution_history
    WHERE procedure_name = 'update_all_inventory';

    -- Find the minimum id of the records that have been modified since the last execution time
    SELECT COALESCE(MIN(id), 9223372036854775807)
    INTO min_modified_id
    FROM all_inventory_view
    WHERE lastModifiedDate > last_execution;

    -- Find the minimum keyid of the records that have been deleted since the last execution time
    SELECT COALESCE(MIN(keyid), 9223372036854775807)
    INTO min_deleted_keyid
    FROM all_inventory ai
    WHERE NOT EXISTS (
        SELECT 1
        FROM all_inventory_view aiv
        WHERE ai.keyid = aiv.keyid
    );

    -- Determine the minimum id between modified and deleted records
    SET min_id = LEAST(min_modified_id, min_deleted_keyid);

    -- Delete records from the main table that have id >= min_id
    DELETE FROM all_inventory
    WHERE id >= min_id;

    -- Insert updated records from the view into the main table
    INSERT INTO all_inventory
    SELECT * FROM all_inventory_view
    WHERE id >= min_id;

    -- Update the last execution time for 'update_all_inventory'
    INSERT INTO execution_history (last_execution, procedure_name)
    VALUES (NOW(), 'update_all_inventory');

    -- Commit the transaction
    COMMIT;
END //

DELIMITER ;


