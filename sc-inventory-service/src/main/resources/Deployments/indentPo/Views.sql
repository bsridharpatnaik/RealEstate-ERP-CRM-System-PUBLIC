CREATE OR REPLACE view IndentsForInward AS
SELECT
	ii.indent_id,
    ii.indent_date,
    ii.indent_status,
    ii.createdBy as indentCreatedBy,
    ii.tenant,
    iie.line_item_code,
    iie.line_item_status,
    iie.productId,
    p.product_name,
    p.product_code,
    p.measurementUnit,
    iie.purchaseOrderId,
    iie.quantity,
    iie.remarks,
	iie.inward_id,
    po.po_date,
    po.purchase_order_id,
    po.grandTotal,
    po.status  as po_status,
    c.contactId as supplier_id,
    c.name as supplier_name
    FROM masterschema.indent_inventory ii
INNER JOIN masterschema.indent_inventory_entries iie on ii.indent_id=iie.indent_id
INNER JOIN masterschema.Product p on p.productId = iie.productId
INNER JOIN masterschema.purchase_order po on po.purchase_order_id = iie.purchaseOrderId
INNER JOIN masterschema.contacts c ON po.supplier_id = c.contactId
WHERE
	iie.line_item_status = 'PO Created' AND
    ii.is_deleted=0 AND
    iie.is_deleted=0 AND
    po.is_deleted=0;