CREATE DATABASE IF NOT EXISTS suncitynxv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS kalpavrishv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS riddhisiddhiv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS smartcityv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS businessparkv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS drgtrdcntrv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS citycenterv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS schoolv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS bhaavbhumiv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS dhabbav2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS mhvrtrdcntrv2
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;


use bhaavbhumiv2;
DELIMITER $$

DROP TRIGGER IF EXISTS category_ai $$
CREATE TRIGGER category_ai
AFTER INSERT ON bhaavbhumiv2.Category
FOR EACH ROW
BEGIN
    INSERT INTO suncitynxv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO kalpavrishv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO riddhisiddhiv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO smartcityv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO businessparkv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO drgtrdcntrv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO citycenterv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO schoolv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO dhabbav2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );

    INSERT INTO mhvrtrdcntrv2.Category VALUES (
        NEW.categoryId, NEW.createdBy, NEW.creationDate, NEW.is_deleted,
        NEW.lastModifiedBy, NEW.lastModifiedDate,
        NEW.categoryDescription, NEW.category_name
    );
END$$

DELIMITER ;

DELIMITER $$

DROP TRIGGER IF EXISTS category_au $$
CREATE TRIGGER category_au
AFTER UPDATE ON bhaavbhumiv2.Category
FOR EACH ROW
BEGIN
    UPDATE suncitynxv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE kalpavrishv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE riddhisiddhiv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE smartcityv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE businessparkv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE drgtrdcntrv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE citycenterv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE schoolv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE dhabbav2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;

    UPDATE mhvrtrdcntrv2.Category SET
        createdBy = NEW.createdBy,
        creationDate = NEW.creationDate,
        is_deleted = NEW.is_deleted,
        lastModifiedBy = NEW.lastModifiedBy,
        lastModifiedDate = NEW.lastModifiedDate,
        categoryDescription = NEW.categoryDescription,
        category_name = NEW.category_name
    WHERE categoryId = NEW.categoryId;
END$$

DELIMITER ;

