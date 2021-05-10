SET foreign_key_checks = 0;
DROP TABLE IF EXISTS 
`address_AUD`,`broker_AUD`,`customer_deal_structure_AUD`,`customer_documents_AUD`,`customer_lead_AUD`,`customer_payment_schedule_AUD`,`file_information_AUD`,`property_name_AUD`,`property_type_AUD`,`property_type_name_AUD`,`source_AUD`;
SET foreign_key_checks = 1;

ALTER TABLE `lead_activity`
  RENAME TO `LeadActivity`; 


ALTER TABLE `address` DROP `state`;

ALTER TABLE `address` DROP `zip`;


ALTER TABLE `customer_deal_structure`
  CHANGE COLUMN  `phase` `phase` varchar(255) DEFAULT NULL AFTER `mode`;



ALTER TABLE `customer_lead`
  CHANGE COLUMN `property_type` `propertyType`
    varchar(255) DEFAULT NULL;

ALTER TABLE `customer_lead`
  CHANGE COLUMN `dateofbirth` `dateofbirth`
    datetime DEFAULT NULL;
 


ALTER TABLE `customer_payment_schedule`
  CHANGE COLUMN `is_received` `isReceived`
    bit(1) DEFAULT NULL;



ALTER TABLE `lead_activity_notification_history`
  CHANGE COLUMN `user_id` `userId`
    bigint(20) DEFAULT NULL;


ALTER TABLE `lead_activity_tags`
  CHANGE COLUMN `lead_activity_id` `leadActivityId`
    bigint(20) NOT NULL;



ALTER TABLE `LeadActivity`
  CHANGE COLUMN `activity_date_time` `activity_date_time` datetime NOT NULL;

ALTER TABLE `LeadActivity`
  CHANGE COLUMN `activity_type` `activityType` varchar(255) NOT NULL;

ALTER TABLE `LeadActivity`
  CHANGE COLUMN `creator_id` `creator_id` bigint(20) NOT NULL;
  
ALTER TABLE `LeadActivity`
  CHANGE COLUMN `closing_comment` `closingComment` varchar(255) DEFAULT NULL;
  
ALTER TABLE `LeadActivity`
  CHANGE COLUMN `is_open` `isOpen` tinyint(1) DEFAULT '1';

ALTER TABLE `LeadActivity`
  CHANGE COLUMN  `is_rescheduled`  `isRescheduled` bit(1) NOT NULL AFTER `isOpen`;

  
ALTER TABLE `note`
  CHANGE COLUMN `creator_id` `creatorId` bigint(20) DEFAULT NULL;



ALTER TABLE `property_name` DROP `is_booked`;



ALTER TABLE `property_type`
  CHANGE COLUMN `total_properties` `totalProperties` bigint(20) DEFAULT NULL;

ALTER TABLE `property_type`
  CHANGE COLUMN `booked_properties` `bookedProperties` bigint(20) DEFAULT NULL;

ALTER TABLE `property_type`
  CHANGE COLUMN  `property_type`  `propertyType` varchar(255) DEFAULT NULL AFTER `bookedProperties`;

insert into egcity.address_AUD select * from prod.address_aud;
insert into egcity.source_AUD select * from prod.source_aud;

