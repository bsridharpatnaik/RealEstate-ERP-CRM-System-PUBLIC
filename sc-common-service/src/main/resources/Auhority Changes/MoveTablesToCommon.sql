-- Create table security_user in common schema
CREATE TABLE `common`.`security_user` (
  `user_id` bigint NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `password_expired` bit(1) NOT NULL,
  `status` bit(1) NOT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `tenants` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK_dj2f84clj2q183f13g8hh8nhi` (`user_name`),
  UNIQUE KEY `UKdj2f84clj2q183f13g8hh8nhi` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Create table tenant in common schema
CREATE TABLE `common`.`tenant` (
  `name` varchar(50) NOT NULL,
  `is_crm` bit(1) DEFAULT NULL,
  `is_inventory` bit(1) DEFAULT NULL,
  `tenant_long_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Create table tenant_authorization_mapping in common schema
CREATE TABLE `common`.`tenant_authorization_mapping` (
  `mapping_id` bigint NOT NULL,
  `authorization` varchar(255) DEFAULT NULL,
  `tenant_name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`mapping_id`),
  KEY `FK47qgws6eeag1fxkkbct2qpld1` (`tenant_name`),
  CONSTRAINT `FK47qgws6eeag1fxkkbct2qpld1` FOREIGN KEY (`tenant_name`) REFERENCES `common`.`tenant` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Create table role in common schema
CREATE TABLE `common`.`role` (
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Create table user_role in common schema
CREATE TABLE `common`.`user_role` (
  `user_id` bigint NOT NULL,
  `role_name` varchar(50) NOT NULL,
  PRIMARY KEY (`user_id`,`role_name`),
  KEY `FKn6r4465stkbdy93a9p8cw7u24` (`role_name`),
  CONSTRAINT `FKag2tat8o2o7yvedewuh0tosbq` FOREIGN KEY (`user_id`) REFERENCES `common`.`security_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKn6r4465stkbdy93a9p8cw7u24` FOREIGN KEY (`role_name`) REFERENCES `common`.`role` (`name`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Create table user_tenant_mapping in common schema
CREATE TABLE `common`.`user_tenant_mapping` (
  `user_id` bigint NOT NULL,
  `mapping_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`mapping_id`),
  KEY `FK3eihkaxbsm8cgiti0q1ddaiev` (`mapping_id`),
  CONSTRAINT `FK3eihkaxbsm8cgiti0q1ddaiev` FOREIGN KEY (`mapping_id`) REFERENCES `common`.`tenant_authorization_mapping` (`mapping_id`),
  CONSTRAINT `FK6e63nv26mdn534mauwh6ny1ug` FOREIGN KEY (`user_id`) REFERENCES `common`.`security_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- Copy data from tenant
INSERT INTO `common`.`tenant`
SELECT * FROM `egcity`.`tenant`;

-- Copy data from security_user
INSERT INTO `common`.`security_user`
SELECT * FROM `egcity`.`security_user`;

-- Copy data from role
INSERT INTO `common`.`role`
SELECT * FROM `egcity`.`role`;

-- Copy data from tenant_authorization_mapping
INSERT INTO `common`.`tenant_authorization_mapping`
SELECT * FROM `egcity`.`tenant_authorization_mapping`;

-- Copy data from user_role
INSERT INTO `common`.`user_role`
SELECT * FROM `egcity`.`user_role`;

-- Copy data from user_tenant_mapping
INSERT INTO `common`.`user_tenant_mapping`
SELECT * FROM `egcity`.`user_tenant_mapping`;
