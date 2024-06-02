use egcity;
set @dbname='egcity';
set @q=concat('CREATE OR REPLACE VIEW payments_page AS
SELECT
	row_number() over (
    ORDER BY cps.payment_date,cl.lead_id,
    cl.name,
    cds.deal_id,
    cps.amount,
    su.user_name,
    pt.propertyType,
    pn.name) as id,
	cl.lead_id,
    cl.name as customerName,
    cds.deal_id as dealStructureId,
    CASE WHEN cps.payment_date IS NULL THEN \'\' ELSE cps.payment_date END as paymentDate,
    cps.amount,
    cps.isReceived,
    cps.isCustomerPayment,
    su.user_name,
    su.user_id,
    pt.propertyType,
    pn.name as propertyName
FROM customer_deal_structure cds
INNER JOIN customer_lead cl ON cds.lead_id=cl.lead_id
INNER JOIN ',@dbname,'.security_user su on su.user_id=cl.user_id
INNER JOIN customer_payment_schedule cps ON cps.deal_id=cds.deal_id AND cps.is_deleted=0
INNER JOIN property_type pt ON pt.property_type_id=cds.property_type_id
INNER JOIN property_name pn ON pn.property_name_id=cds.property_name_id
WHERE cds.is_Deleted=0 ;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

