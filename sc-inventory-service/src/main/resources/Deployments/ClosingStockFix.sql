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
