update `boq_inventory` set createdBy=404,lastModifiedBy=404;
update `Category` set createdBy=404,lastModifiedBy=404;
update `contacts` set createdBy=404,lastModifiedBy=404;
update `inventorynotification` set createdBy=404,lastModifiedBy=404;
update `inward_inventory` set createdBy=404,lastModifiedBy=404;
update `inward_outward_entries` set createdBy=404,lastModifiedBy=404;
update `lost_damaged_inventory` set createdBy=404,lastModifiedBy=404;
update `Machinery` set createdBy=404,lastModifiedBy=404;
update `machineryonrent` set createdBy=404,lastModifiedBy=404;
update `outward_inventory` set createdBy=404,lastModifiedBy=404;
update `Product` set createdBy=404,lastModifiedBy=404;
update `reject_inward_entries` set createdBy=404,lastModifiedBy=404;
update `reject_outward_entries` set createdBy=404,lastModifiedBy=404;
update `return_outward_entries` set createdBy=404,lastModifiedBy=404;
update `Stock` set createdBy=404,lastModifiedBy=404;
update `stock_history` set createdBy=404,lastModifiedBy=404;
update `usage_area`  set createdBy=404,lastModifiedBy=404;
update `Usage_Location` set createdBy=404,lastModifiedBy=404;
update `Warehouse` set createdBy=404,lastModifiedBy=404;
update stock_history set creationDate=current_timestamp();
update stock_history set lastModifiedDate=current_timestamp();
update security_user set tenants = 'egcity';
ALTER TABLE `boq_inventory` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `boq_inventory` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `boq_inventory` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `boq_inventory` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `building_type` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `building_type` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `building_type` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `building_type` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `Category` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `Category` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `Category` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `Category` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `contacts` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `contacts` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `contacts` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `contacts` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `inventorynotification` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `inventorynotification` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `inventorynotification` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `inventorynotification` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `inward_inventory` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `inward_inventory` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `inward_inventory` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `inward_inventory` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `inward_outward_entries` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `inward_outward_entries` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `inward_outward_entries` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `inward_outward_entries` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `lost_damaged_inventory` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `lost_damaged_inventory` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `lost_damaged_inventory` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `lost_damaged_inventory` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `Machinery` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `Machinery` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `Machinery` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `Machinery` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `machineryonrent` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `machineryonrent` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `machineryonrent` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `machineryonrent` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `outward_inventory` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `outward_inventory` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `outward_inventory` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `outward_inventory` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `Product` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `Product` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `Product` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `Product` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `reject_inward_entries` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `reject_inward_entries` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `reject_inward_entries` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `reject_inward_entries` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `reject_outward_entries` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `reject_outward_entries` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `reject_outward_entries` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `reject_outward_entries` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `return_outward_entries` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `return_outward_entries` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `return_outward_entries` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `return_outward_entries` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `Stock` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `Stock` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `Stock` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `Stock` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `stock_history` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `stock_history` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `stock_history` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `stock_history` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `usage_area` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `usage_area` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `usage_area` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `usage_area` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;

ALTER TABLE `Usage_Location` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `Usage_Location` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `Usage_Location` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `Usage_Location` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;


ALTER TABLE `Warehouse` CHANGE `creationDate` `creationDate` DATETIME NOT NULL ;
ALTER TABLE `Warehouse` CHANGE `createdBy` `createdBy` VARCHAR(255) NOT NULL ;
ALTER TABLE `Warehouse` CHANGE `lastModifiedDate` `lastModifiedDate` DATETIME NOT NULL ;
ALTER TABLE `Warehouse` CHANGE `lastModifiedBy` `lastModifiedBy` VARCHAR(255) NOT NULL ;