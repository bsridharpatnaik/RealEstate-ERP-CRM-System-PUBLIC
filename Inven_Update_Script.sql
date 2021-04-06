use egcity;

SET SQL_SAFE_UPDATES = 0;

drop view if exists all_inventory;
drop view if exists allcontacts;
drop view if exists stock_verification;
drop view if exists supplier;
drop view if exists contractor;

ALTER TABLE `category`
  RENAME TO `Category`;

ALTER TABLE `Category`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `category_id`,
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER `is_deleted`,
  ADD `descss` varchar(255) DEFAULT NULL
    AFTER `category_name`;
	
	
ALTER TABLE `Category`
  CHANGE COLUMN `category_id` `categoryId`
    bigint(20) NOT NULL;	
	
ALTER TABLE `Category`
  CHANGE COLUMN `category_description` `categoryDescription`
    varchar(255) DEFAULT NULL;
	
ALTER TABLE `Category`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `Category`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

-- DROP TABLE table `contacts`;

ALTER TABLE `contact`
DROP FOREIGN KEY `FKqdqofwoj3p6a2rmo4302awcyy`;

ALTER TABLE `contact` DROP `addr_id`;

ALTER TABLE `contact`                 
  RENAME TO `contacts`;  

ALTER TABLE `contacts`
  CHANGE COLUMN  `contact_id` `contactId` bigint(20) NOT NULL;
  
ALTER TABLE `contacts`
  ADD `addr_line1` varchar(255) DEFAULT NULL
    AFTER  `contactId`;

ALTER TABLE `contacts`
  ADD `addr_line2` varchar(255) DEFAULT NULL
    AFTER  `addr_line1`;
	
ALTER TABLE `contacts`
  ADD `city` varchar(255) DEFAULT NULL
    AFTER  `addr_line2`;	

ALTER TABLE `contacts`
  ADD `contactPerson` varchar(255) DEFAULT NULL
    AFTER  `city`;	

ALTER TABLE `contacts`
  ADD `contactPersonMobileNo` varchar(255) DEFAULT NULL
    AFTER  `contactPerson`;

ALTER TABLE `contacts`
  CHANGE COLUMN  `contact_type` `contactType` varchar(255) NOT NULL AFTER `contactPersonMobileNo`;

ALTER TABLE `contacts`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `contactType`;

ALTER TABLE `contacts`
  CHANGE COLUMN  `created_at`  `creationDate` datetime(6) DEFAULT NULL AFTER `createdBy`;  

ALTER TABLE `contacts`
  CHANGE COLUMN  `email_id`  `emailId` varchar(255) DEFAULT NULL AFTER `creationDate`;

ALTER TABLE `contacts`
  ADD `gst_number` varchar(255) DEFAULT NULL
    AFTER  `emailId`;

ALTER TABLE `contacts`
  CHANGE COLUMN  `is_deleted`  `is_deleted` tinyint(1) DEFAULT '1' AFTER `gst_number`; 
  
ALTER TABLE `contacts`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `contacts`
  CHANGE COLUMN  `updated_at`  `lastModifiedDate` datetime(6) DEFAULT NULL AFTER `lastModifiedBy`; 

ALTER TABLE `contacts`
  CHANGE COLUMN  `mobile_no`  `mobileNo` varchar(255) NOT NULL AFTER `lastModifiedDate`;
  
ALTER TABLE `contacts`
  ADD `state` varchar(255) DEFAULT NULL
    AFTER  `name`;

ALTER TABLE `contacts`
  ADD `zip` varchar(255) DEFAULT NULL
    AFTER  `state`;


UPDATE `contacts` c set `contactPerson` =  (select `contact_person` from `contact_info` ci where ci.`contact_id` = c.`contactId`);

UPDATE `contacts` c set `contactPersonMobileNo` =  (select `contact_person_mobile_no` from `contact_info` ci where ci.`contact_id` = c.`contactId`);

UPDATE `contacts` c set `gst_number` =  (select `gst_number` from `contact_info` ci where ci.`contact_id` = c.`contactId`);
 
alter table contacts change column mobileNo mobileNo varchar(255) DEFAULT NULL;

ALTER TABLE `files`
  CHANGE COLUMN  `file_name`  `fileName` varchar(255) DEFAULT NULL;
  
ALTER TABLE `files`
  CHANGE COLUMN  `file_type`  `fileType` varchar(255) DEFAULT NULL;
  

ALTER TABLE `inventorynotification`
  CHANGE COLUMN  `warehouse_name`  `warehouseName` varchar(255) DEFAULT NULL;
  
ALTER TABLE `inventorynotification`
  CHANGE COLUMN  `updated_by` `updatedBy` varchar(255) DEFAULT NULL;
  
ALTER TABLE `inventorynotification`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `inventorynotification`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

ALTER TABLE `inventorynotification`
  CHANGE COLUMN  `product_id` `productId` bigint(20) NOT NULL AFTER `warehouseName`;

ALTER TABLE `inventorynotification`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `id`;

ALTER TABLE `inventorynotification`
  ADD  `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;


ALTER TABLE `inventorynotification`
  ADD KEY `FKg9ukqr4ugl77ldgs1y5nyf44c` (`productId`);	
  

ALTER TABLE `inwardinventory_entry`
  CHANGE COLUMN  `entry_id` `entryId` bigint(20) NOT NULL;

ALTER TABLE `inwardinventory_entry`
  ADD KEY `FKpw1ey2pwn2ueglvqs70rek0fl` (`entryId`);	
  

ALTER TABLE `inward_inventory`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `inwardid`;
	
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

ALTER TABLE `inward_inventory`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `additional_info` `additionalInfo` varchar(255) DEFAULT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `date` `date` datetime(6) NOT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `invoice_received` `invoiceReceived` bit(1) NOT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `our_slip_no`  `ourSlipNo` varchar(255) DEFAULT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `supplier_slip_no` `supplierSlipNo` varchar(255) DEFAULT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `vehicle_no` `vehicleNo` varchar(255) DEFAULT NULL;
  
ALTER TABLE `inward_inventory`
  CHANGE COLUMN  `contact_id` `contactId` bigint(20) NOT NULL;
  
ALTER TABLE `inward_inventory`
  ADD `purchaseOrder` varchar(255) DEFAULT NULL
    AFTER  `warehouse_id`;
	
ALTER TABLE `inward_inventory`
  ADD `purchaseOrderdate` datetime DEFAULT NULL
    AFTER  `purchaseOrder`;
	
ALTER TABLE `inward_inventory`
  ADD KEY `FKgrt6nkjsu00y3n0mvdprkpa5v` (`contactId`),
  ADD KEY `FKdb00k45jc2jkxuukhkr2uc4m1` (`warehouse_id`);
  

  
ALTER TABLE `inward_outward_entries`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `entryid`;

ALTER TABLE `inward_outward_entries`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `inward_outward_entries`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `inward_outward_entries`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;

ALTER TABLE `inward_outward_entries`
  CHANGE COLUMN  `closing_stock`  `closingStock` double DEFAULT NULL;
  
ALTER TABLE `inward_outward_entries`
  CHANGE COLUMN  `product_id` `productId` bigint(20) NOT NULL;
  
ALTER TABLE `inward_outward_entries`
  ADD KEY `FK5v6i0e7bbvi1tcukgjp7w1bke` (`productId`);
    

ALTER TABLE `lost_damaged_inventory`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `lostdamagedid`;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `lost_damaged_inventory`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `closing_stock`  `closingStock` double DEFAULT NULL;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `date` `date` datetime(6) NOT NULL;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `location_of_theft`   `locationOfTheft` varchar(255) DEFAULT NULL;
 
ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `product_id` `productId` bigint(20) NOT NULL;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `warehouse_name` `warehouseName` bigint(20) NOT NULL;

ALTER TABLE `lost_damaged_inventory`
  CHANGE COLUMN  `additional_comment`  `additionalComment` varchar(255) DEFAULT NULL AFTER `lastModifiedDate`;
  
ALTER TABLE `lost_damaged_inventory`
 ADD KEY `FKe7wlv2m2ir9ev5gx07pns1w57` (`productId`),
 ADD KEY `FKf30lroe2r7jk4rnpkww0m3vg3` (`warehouseName`);

ALTER TABLE `machinery`
  RENAME TO `Machinery`; 

ALTER TABLE `Machinery`
  CHANGE COLUMN  `machinery_id` `machineryId` bigint(20) NOT NULL;

ALTER TABLE `Machinery`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `machineryId`;

ALTER TABLE `Machinery`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

ALTER TABLE `Machinery`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;
	
ALTER TABLE `Machinery`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `Machinery`
  CHANGE COLUMN  `machinery_description` `machineryDescription` varchar(255) DEFAULT NULL;

ALTER TABLE `Machinery`
  CHANGE COLUMN  `machinery_name` `machineryName` varchar(255) DEFAULT NULL;
 

ALTER TABLE `machinery_on_rent`
  RENAME TO `machineryonrent`;

ALTER TABLE `machineryonrent`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `morid`;
	
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `created_at` `creationDate` datetime DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;
	
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `additional_notes` `additionalNotes` varchar(255) DEFAULT NULL;

ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `amount_charged` `amountCharged` double DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `end_date` `endDate` datetime DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  ADD `endDateTime` datetime DEFAULT NULL
    AFTER  `endDate`;

ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `end_meter_reading`  `endMeterReading` double DEFAULT NULL;

ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `initial_meter_reading` `initialMeterReading` double DEFAULT NULL;

update `machineryonrent` set `mode` = '1' where lower(`mode`) = 'daily';

update `machineryonrent` set `mode` = '2' where lower(`mode`) = 'meter';


ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `mode` `mode` int(11) DEFAULT NULL;

ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `no_of_trips` `noOfTrips` double DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  ADD `rate` double DEFAULT NULL
    AFTER  `noOfTrips`;  
  
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `start_date` `startDate` datetime DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  ADD `startDateTime` datetime DEFAULT NULL
    AFTER  `startDate`; 
	
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `vehicle_no` `vehicleNo` varchar(255) DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `contact_id` `contractorId` bigint(20) DEFAULT NULL AFTER `vehicleNo`;
  
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `machinery_id` `machineryId` bigint(20) NOT NULL;
  
ALTER TABLE `machineryonrent`
  ADD `supplierId` bigint(20) DEFAULT NULL
    AFTER  `machineryId`; 
	
ALTER TABLE `machineryonrent`
  CHANGE COLUMN  `location_id` `locationId` bigint(20) DEFAULT NULL;
  
ALTER TABLE `machineryonrent`
 ADD  KEY `FK4xqaucbvyoj3adncmodiyqq3q` (`contractorId`),
 ADD  KEY `FKhmvyb1lnb11rypmeelmnn31mw` (`supplierId`);

ALTER TABLE `machineryonrent`
  CHANGE COLUMN `contractorId` `contractorId1` bigint(20) DEFAULT NULL;
  ALTER TABLE `machineryonrent`
  CHANGE COLUMN `supplierId` `contractorId` bigint(20) DEFAULT NULL;
   ALTER TABLE `machineryonrent`
  CHANGE COLUMN `contractorId1` `supplierId` bigint(20) DEFAULT NULL;

ALTER TABLE `outwardinventory_entry`
  CHANGE COLUMN  `entry_id` `entryId` bigint(20) NOT NULL;
  

ALTER TABLE `outward_inventory`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `outwardid`;

ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `outward_inventory`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `additional_info` `additionalInfo` varchar(255) DEFAULT NULL;
  
ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `slip_no`  `slipNo` varchar(255) DEFAULT NULL;

ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `date` `date` datetime(6) NOT NULL;

ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `contact_id`   `contactId` bigint(20) NOT NULL;
 
ALTER TABLE `outward_inventory`
DROP FOREIGN KEY `FK1yaj0breeb939ifo16bd5wutu`;

ALTER TABLE `outward_inventory`
DROP INDEX `FK1yaj0breeb939ifo16bd5wutu`;

ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `usage_area_id` `usageAreaId` bigint(20) DEFAULT NULL;
  
ALTER TABLE `outward_inventory`
DROP FOREIGN KEY `FKod8b0fr383w6y41ololmujyi1`;

ALTER TABLE `outward_inventory`
DROP INDEX `FKod8b0fr383w6y41ololmujyi1`;

ALTER TABLE `outward_inventory`
  CHANGE COLUMN  `location_id` `locationId` bigint(20) NOT NULL;


ALTER TABLE `outward_inventory`
  ADD KEY `FK1yaj0breeb939ifo16bd5wutu` (`usageAreaId`),
  ADD KEY `FKod8b0fr383w6y41ololmujyi1` (`locationId`);


ALTER TABLE `outward_inventory`
 ADD KEY `FKmtmli3513g49y8biiink1ckmd` (`contactId`);
  

ALTER TABLE `product`
  RENAME TO `Product`; 
  
ALTER TABLE `Product`
  CHANGE COLUMN  `product_id` `productId` bigint(20) NOT NULL;
  
ALTER TABLE `Product`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `productId`;

ALTER TABLE `Product`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `Product`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `Product`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `Product`
  CHANGE COLUMN  `measurement_unit` `measurementUnit` varchar(255) DEFAULT NULL;
  
ALTER TABLE `Product`
  CHANGE COLUMN  `product_description` `productDescription` varchar(255) DEFAULT NULL;

ALTER TABLE `Product`
  CHANGE COLUMN  `reorder_quantity` `reorderQuantity` double DEFAULT NULL;

ALTER TABLE `Product`
  CHANGE COLUMN  `category_id` `categoryId` bigint(20) NOT NULL;
 

ALTER TABLE `return_outward_entries`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `returnentryid`;

ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `return_outward_entries`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL;
  
ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `closing_stock` `closingStock` double DEFAULT NULL;
  
ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `old_quantity` `oldQuantity` double DEFAULT NULL;

ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `return_date` `returnDate` datetime(6) NOT NULL;

ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `return_quantity` `returnQuantity` double DEFAULT NULL;
 
ALTER TABLE `return_outward_entries`
  CHANGE COLUMN  `product_id` `productId` bigint(20) NOT NULL;
  
ALTER TABLE `revinfo`
  RENAME TO `REVINFO`;
  
ALTER TABLE `REVINFO`
  CHANGE COLUMN  `rev` `REV` int(11) NOT NULL;
  
ALTER TABLE `REVINFO`
  CHANGE COLUMN  `revtstmp` `REVTSTMP` bigint(20) DEFAULT NULL;
  
ALTER TABLE `REVINFO`
  CHANGE COLUMN  `userid` `USERID` bigint(20) NOT NULL;

ALTER TABLE `REVINFO`
  CHANGE COLUMN  `username` `USERNAME` varchar(255) NOT NULL AFTER `USERID`;
 
ALTER TABLE `stock`
  RENAME TO `Stock`;  

ALTER TABLE `Stock`
  CHANGE COLUMN  `stock_id` `stockId` bigint(20) NOT NULL;

ALTER TABLE `Stock`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `stockId`;

ALTER TABLE `Stock`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;
    
ALTER TABLE `Stock`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `Stock`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL AFTER `lastModifiedBy`;
  
ALTER TABLE `Stock`
  CHANGE COLUMN  `quantity_in_hand` `quantityInHand` double DEFAULT NULL;
  
ALTER TABLE `Stock`
  CHANGE COLUMN  `product_id` `productId` bigint(20) NOT NULL;

ALTER TABLE `Stock`
  CHANGE COLUMN  `warehouse_name` `warehouseName` bigint(20) NOT NULL;
  

ALTER TABLE `stock_history`
  CHANGE COLUMN  `stock_history_id` `stockHistoryId` bigint(20) NOT NULL;

ALTER TABLE `stock_history`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `stockHistoryId`;

ALTER TABLE `stock_history`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL AFTER `createdBy`;

ALTER TABLE `stock_history`
  CHANGE COLUMN  `is_deleted` `is_deleted` tinyint(1) DEFAULT 1 AFTER `creationDate`;
  
ALTER TABLE `stock_history`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `stock_history`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL AFTER `lastModifiedBy`;
  
ALTER TABLE `stock_history`
  CHANGE COLUMN  `measurement_unit` `measurementUnit` varchar(255) DEFAULT NULL;
  
ALTER TABLE `stock_history`
  CHANGE COLUMN  `product_id` `productId` bigint(20) DEFAULT NULL;

ALTER TABLE `stock_history`
  CHANGE COLUMN  `total_stock`  `totalStock` varchar(255) DEFAULT NULL;

ALTER TABLE `stock_history`
  CHANGE COLUMN  `warehouse_stock` `warehouseStock` double DEFAULT NULL;
  

ALTER TABLE `usage_area`
  CHANGE COLUMN  `usage_area_id` `usageAreaId` bigint(20) NOT NULL;

ALTER TABLE `usage_area`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `usageAreaId`;

ALTER TABLE `usage_area`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

ALTER TABLE `usage_area`
  CHANGE COLUMN  `is_deleted` `is_deleted` tinyint(1) DEFAULT 1 AFTER `creationDate`;
  
ALTER TABLE `usage_area`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `usage_area`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL AFTER `lastModifiedBy`;
  
ALTER TABLE `usage_area`
  CHANGE COLUMN  `usage_area_description` `usageAreaDescription` varchar(255) DEFAULT NULL;
  
ALTER TABLE `usage_location`
  RENAME TO `Usage_Location`;
  
ALTER TABLE `Usage_Location`
  CHANGE COLUMN  `location_id` `locationId` bigint(20) NOT NULL;

ALTER TABLE `Usage_Location`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `locationId`;

ALTER TABLE `Usage_Location`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

ALTER TABLE `Usage_Location`
  CHANGE COLUMN  `is_deleted` `is_deleted` tinyint(1) DEFAULT 1 AFTER `creationDate`;
  
ALTER TABLE `Usage_Location`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;

ALTER TABLE `Usage_Location`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL AFTER `lastModifiedBy`;
  
ALTER TABLE `Usage_Location`
  CHANGE COLUMN  `location_description` `locationDescription` varchar(255) DEFAULT NULL;

ALTER TABLE `Usage_Location`
  ADD `typeId` bigint(20) DEFAULT NULL
    AFTER  `location_name`;

ALTER TABLE `Usage_Location`
 ADD KEY `FK5h73wggk1ohou9dri09xwv2ak` (`typeId`);
 
ALTER TABLE `warehouse`
  RENAME TO `Warehouse`;

ALTER TABLE `Warehouse`
  ADD `createdBy` varchar(255) DEFAULT NULL
    AFTER  `warehouse_id`;

ALTER TABLE `Warehouse`
  CHANGE COLUMN  `created_at` `creationDate` datetime(6) DEFAULT NULL;

ALTER TABLE `Warehouse`
  ADD `lastModifiedBy` varchar(255) DEFAULT NULL
    AFTER  `is_deleted`;
	
ALTER TABLE `Warehouse`
  CHANGE COLUMN  `updated_at` `lastModifiedDate` datetime(6) DEFAULT NULL AFTER `lastModifiedBy`;
    
ALTER TABLE `Warehouse`
  CHANGE COLUMN  `warehouse_name` `warehouseName` varchar(50) NOT NULL;
  
  
ALTER TABLE `outward_inventory`
  ADD CONSTRAINT `FK1yaj0breeb939ifo16bd5wutu` FOREIGN KEY (`usageAreaId`) REFERENCES `usage_area` (`usageAreaId`),
  ADD CONSTRAINT `FKod8b0fr383w6y41ololmujyi1` FOREIGN KEY (`locationId`) REFERENCES `Usage_Location` (`locationId`),
  ADD CONSTRAINT `FKmtmli3513g49y8biiink1ckmd` FOREIGN KEY (`contactId`) REFERENCES `contacts` (`contactId`);  
  

ALTER TABLE `machineryonrent`
  ADD CONSTRAINT `FK4xqaucbvyoj3adncmodiyqq3q` FOREIGN KEY (`contractorId`) REFERENCES `contacts` (`contactId`),
  ADD CONSTRAINT `FKhmvyb1lnb11rypmeelmnn31mw` FOREIGN KEY (`supplierId`) REFERENCES `contacts` (`contactId`);  
  
    
  
ALTER TABLE `inward_inventory`
  ADD CONSTRAINT `FKgrt6nkjsu00y3n0mvdprkpa5v` FOREIGN KEY (`contactId`) REFERENCES `contacts` (`contactId`);  
    
  
SET foreign_key_checks = 0;
DROP TABLE IF EXISTS `address_aud`,`all_inventory_transactions_aud`,`broker_aud`,`category_aud`,`contacts_aud`,`contractor_aud`,`customer_deal_structure_aud`,`customer_documents_aud`,`customer_lead_aud`,`customer_payment_schedule_aud`,`file_information_aud`,`inventorynotification_aud`,`inward_fileinformation_aud`,`inward_inventory_aud`,`inward_outward_entries_aud`,`inwardinventory_entry_aud`,`lost_damaged_inventory_aud`,`lostdamaged_fileinformation_aud`,`machinery_aud`,`machinery_on_rent_aud`,`mor_fileinformation_aud`,`outward_fileinformation_aud`,`outward_inventory_aud`;
SET foreign_key_checks = 1;


SET foreign_key_checks = 0;
DROP TABLE IF EXISTS `outwardinventory_entry_aud`,`product_aud`,`property_name_aud`,`property_type_aud`,`property_type_name_aud`,`return_outward_entries_aud`,`return_outward_entry_aud`,`source_aud`,`stock_aud`,`stock_history_aud`,`supplier_aud`,`usage_area_aud`,`usage_location_aud`,`warehouse_aud`;
SET foreign_key_checks = 1;