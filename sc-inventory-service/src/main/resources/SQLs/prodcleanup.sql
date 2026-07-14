-- ================================================================
-- PRODUCTION DATA CLEANUP: trim leading/trailing spaces from
-- name/description fields across all tenant schemas.
--
-- Skipped conflicts (handle manually after running):
--   1. Product 'Tiles Adhesive Wall King Gold ' — duplicate exists
--   2. Location 'GOLD SOUK ' in drgtrdcntr      — duplicate exists
--   3. Location 'road ' in mnglmcity             — duplicate exists
--
-- Safe to re-run (idempotent — WHERE clause prevents no-op rows).
-- ================================================================

-- ----------------------------------------------------------------
-- STEP 1: Global entities in masterschema
-- ----------------------------------------------------------------

-- Product name (skip known conflict)
UPDATE masterschema.Product
SET product_name = TRIM(product_name)
WHERE product_name != TRIM(product_name)
  AND is_deleted = 0
  AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';

-- Product description
UPDATE masterschema.Product
SET productDescription = TRIM(productDescription)
WHERE productDescription IS NOT NULL
  AND productDescription != TRIM(productDescription)
  AND is_deleted = 0;

-- Product measurementUnit
UPDATE masterschema.Product
SET measurementUnit = TRIM(measurementUnit)
WHERE measurementUnit IS NOT NULL
  AND measurementUnit != TRIM(measurementUnit)
  AND is_deleted = 0;

-- Category name
UPDATE masterschema.Category
SET category_name = TRIM(category_name)
WHERE category_name != TRIM(category_name)
  AND is_deleted = 0;

-- Category description
UPDATE masterschema.Category
SET categoryDescription = TRIM(categoryDescription)
WHERE categoryDescription IS NOT NULL
  AND categoryDescription != TRIM(categoryDescription)
  AND is_deleted = 0;

-- Contact name (all schemas — table exists everywhere, data may vary per tenant)
UPDATE masterschema.contacts  SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE bextension.contacts    SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE bhaavbhumi.contacts    SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE citycenter.contacts    SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE dextension.contacts    SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE drgtrdcntr.contacts    SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE iseries.contacts       SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE mhvrtrdcntr.contacts   SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE mnglmcity.contacts     SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;
UPDATE smartcity.contacts     SET name = TRIM(name) WHERE name != TRIM(name) AND is_deleted=0;

-- Machinery name
UPDATE masterschema.Machinery
SET machinery_name = TRIM(machinery_name)
WHERE machinery_name != TRIM(machinery_name)
  AND is_deleted = 0;

-- Machinery description
UPDATE masterschema.Machinery
SET machinery_description = TRIM(machinery_description)
WHERE machinery_description IS NOT NULL
  AND machinery_description != TRIM(machinery_description)
  AND is_deleted = 0;

-- BuildingType name
UPDATE masterschema.building_type
SET building_type = TRIM(building_type)
WHERE building_type IS NOT NULL
  AND building_type != TRIM(building_type)
  AND is_deleted = 0;

-- BuildingType description
UPDATE masterschema.building_type
SET typeDescription = TRIM(typeDescription)
WHERE typeDescription IS NOT NULL
  AND typeDescription != TRIM(typeDescription)
  AND is_deleted = 0;

-- Firm name
UPDATE masterschema.Firm
SET firm_name = TRIM(firm_name)
WHERE firm_name != TRIM(firm_name)
  AND is_deleted = 0;

-- Firm description
UPDATE masterschema.Firm
SET firm_description = TRIM(firm_description)
WHERE firm_description IS NOT NULL
  AND firm_description != TRIM(firm_description)
  AND is_deleted = 0;

-- ----------------------------------------------------------------
-- STEP 2: Product synced copies in tenant schemas
-- Add/remove schema names below to match your production tenants.
-- ----------------------------------------------------------------

SET @schemas = 'bextension,bhaavbhumi,citycenter,dextension,drgtrdcntr,iseries,mhvrtrdcntr,mnglmcity,smartcity';

-- Product name (skip known conflict in all schemas)
UPDATE bextension.Product  SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE bhaavbhumi.Product  SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE citycenter.Product  SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE dextension.Product  SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE drgtrdcntr.Product  SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE iseries.Product     SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE mhvrtrdcntr.Product SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE mnglmcity.Product   SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';
UPDATE smartcity.Product   SET product_name = TRIM(product_name) WHERE product_name != TRIM(product_name) AND is_deleted=0 AND TRIM(product_name) != 'Tiles Adhesive Wall King Gold';

-- Product description (all tenant schemas)
UPDATE bextension.Product  SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE bhaavbhumi.Product  SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE citycenter.Product  SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE dextension.Product  SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE drgtrdcntr.Product  SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE iseries.Product     SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE mhvrtrdcntr.Product SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE mnglmcity.Product   SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;
UPDATE smartcity.Product   SET productDescription = TRIM(productDescription) WHERE productDescription IS NOT NULL AND productDescription != TRIM(productDescription) AND is_deleted=0;

-- ----------------------------------------------------------------
-- STEP 3: Tenant-specific entities (Warehouse, UsageArea, Location)
-- These live per-schema. Add new tenant schemas here as needed.
-- ----------------------------------------------------------------

-- Warehouse name
UPDATE bextension.Warehouse  SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE bhaavbhumi.Warehouse  SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE citycenter.Warehouse  SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE dextension.Warehouse  SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE drgtrdcntr.Warehouse  SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE iseries.Warehouse     SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE masterschema.Warehouse SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE mhvrtrdcntr.Warehouse SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE mnglmcity.Warehouse   SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;
UPDATE smartcity.Warehouse   SET warehouseName = TRIM(warehouseName) WHERE warehouseName != TRIM(warehouseName) AND is_deleted=0;

-- UsageArea name
UPDATE bextension.usage_area  SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE bhaavbhumi.usage_area  SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE citycenter.usage_area  SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE dextension.usage_area  SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE drgtrdcntr.usage_area  SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE iseries.usage_area     SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE masterschema.usage_area SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE mhvrtrdcntr.usage_area SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE mnglmcity.usage_area   SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;
UPDATE smartcity.usage_area   SET usagearea_name = TRIM(usagearea_name) WHERE usagearea_name IS NOT NULL AND usagearea_name != TRIM(usagearea_name) AND is_deleted=0;

-- UsageArea description
UPDATE bextension.usage_area  SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE bhaavbhumi.usage_area  SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE citycenter.usage_area  SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE dextension.usage_area  SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE drgtrdcntr.usage_area  SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE iseries.usage_area     SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE masterschema.usage_area SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE mhvrtrdcntr.usage_area SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE mnglmcity.usage_area   SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;
UPDATE smartcity.usage_area   SET usageAreaDescription = TRIM(usageAreaDescription) WHERE usageAreaDescription IS NOT NULL AND usageAreaDescription != TRIM(usageAreaDescription) AND is_deleted=0;

-- Location name (skip known conflicts)
UPDATE bextension.Usage_Location  SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE bhaavbhumi.Usage_Location  SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE citycenter.Usage_Location  SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE dextension.Usage_Location  SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE drgtrdcntr.Usage_Location  SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND TRIM(location_name) != 'GOLD SOUK' AND is_deleted=0;
UPDATE iseries.Usage_Location     SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE masterschema.Usage_Location SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE mhvrtrdcntr.Usage_Location SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;
UPDATE mnglmcity.Usage_Location   SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND TRIM(location_name) != 'road' AND is_deleted=0;
UPDATE smartcity.Usage_Location   SET location_name = TRIM(location_name) WHERE location_name IS NOT NULL AND location_name != TRIM(location_name) AND is_deleted=0;

-- Location description
UPDATE bextension.Usage_Location  SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE bhaavbhumi.Usage_Location  SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE citycenter.Usage_Location  SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE dextension.Usage_Location  SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE drgtrdcntr.Usage_Location  SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE iseries.Usage_Location     SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE masterschema.Usage_Location SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE mhvrtrdcntr.Usage_Location SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE mnglmcity.Usage_Location   SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;
UPDATE smartcity.Usage_Location   SET locationDescription = TRIM(locationDescription) WHERE locationDescription IS NOT NULL AND locationDescription != TRIM(locationDescription) AND is_deleted=0;

-- ================================================================
-- END OF SCRIPT
-- Remaining conflicts to resolve manually:
--   1. masterschema.Product — 'Tiles Adhesive Wall King Gold ' vs 'Tiles Adhesive Wall King Gold'
--   2. drgtrdcntr.Usage_Location — 'GOLD SOUK ' vs 'GOLD SOUK'
--   3. mnglmcity.Usage_Location  — 'road ' vs 'road'
-- For each: identify which record has active usage, soft-delete the other.
-- ================================================================