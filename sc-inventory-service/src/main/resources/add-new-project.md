# Adding a New Project (Tenant)

This doc walks through all steps to onboard a new project/tenant.  
Example project used: **D Extension** (`dextension`)

---

## Step 1 — Create Database

Run in MySQL:

```sql
CREATE DATABASE <schema_name>;
```

Example:
```sql
CREATE DATABASE dextension;
```

---

## Step 2 — Backend Config Changes

### `application-sc-local-v2.properties` (and other env property files as needed)

Add the new schema to `schemas.map`:

```properties
schemas.map=...,dextension:DX
```

- Key: schema name (must match DB name)
- Value: short code (used internally as tenant prefix, pick 2–3 chars, unique)

---

## Step 3 — Start the Server

Start the backend server. Hibernate will auto-create all tables in the new schema via JPA (DDL), now that the schema is registered in `schemas.map`.

---

## Step 4 — Seed Master Data

Run in MySQL after server start (tables must exist first):

```sql
INSERT INTO dextension.Category
SELECT * FROM masterschema.Category;
```

```sql
INSERT INTO dextension.Product
SELECT * FROM masterschema.Product;
```

```sql
INSERT INTO dextension.Machinery
SELECT * FROM masterschema.Machinery;
```

### Contacts — Dynamic Insert

`contacts` column order differs between schemas — `masterschema` uses creation order, new schemas use alphabetical order.  
**Do not use `SELECT *`** — positional mapping corrupts data (e.g. `createdBy` → `creationDate`).

Use this dynamic approach instead — column list is fetched from `INFORMATION_SCHEMA` at runtime:

```sql
SET @target = 'dextension';  -- change this for each new project

SET @cols = (
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = 'masterschema' AND TABLE_NAME = 'contacts'
);

SET @sql = CONCAT(
  'INSERT INTO ', @target, '.contacts (', @cols, ') ',
  'SELECT ', @cols, ' FROM masterschema.contacts'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

Only change `@target` for each new project. Column list is always current — no manual updates needed when columns are added.

---

## Step 5 — Create Views

Execute `sc-inventory-service/src/main/resources/SQLs/CreateViews.sql` against the new schema to create the views (e.g. `all_inventory_view`) and supporting procedures.

---

## Step 6 — Register Tenant

```sql
INSERT INTO `common`.`tenant`
(`name`, `is_crm`, `is_inventory`, `tenant_long_name`)
VALUES
('<schema_name>', 0, 1, '<Display Name>');
```

Example:
```sql
INSERT INTO `common`.`tenant`
(`name`, `is_crm`, `is_inventory`, `tenant_long_name`)
VALUES
('dextension', 0, 1, 'D Extension');
```

Also add this row to `sc-inventory-service/src/main/resources/SQLs/InitialSQL` so it is part of the seed script.

This makes the tenant visible to the UI.

---

## Step 7 — Frontend Changes

### `SC UI/src/Modules/Projects/index.js`

Add logo mapping for the new project:

```js
const logoMap = {
  // ... existing entries ...
  dextension: "/suncitynx.jpg",  // use appropriate image from public folder
};
```

Place the project logo image in the `public/` folder of the UI before this step.

---

## Step 8 — Assign Project to User

Assign the new project/tenant to the relevant user(s) so they can access it (via user management / project assignment in the UI or DB).

---

## Checklist

| Step | Action | File / Location |
|------|--------|----------------|
| 1 | Create DB | MySQL |
| 2 | Add to schemas.map | `application-sc-local-v2.properties` |
| 3 | Start server (DDL creates tables) | — |
| 4a | Seed Category | `<schema>.Category` ← `masterschema.Category` |
| 4b | Seed contacts | explicit column INSERT (see above) |
| 4c | Seed Product | `<schema>.Product` ← `masterschema.Product` |
| 4d | Seed Machinery | `<schema>.Machinery` ← `masterschema.Machinery` |
| 5 | Run CreateViews.sql on new schema | `SQLs/CreateViews.sql` |
| 6 | Insert tenant row | `common.tenant` + `SQLs/InitialSQL` |
| 7 | Add logo mapping | `SC UI/src/Modules/Projects/index.js` |
| 8 | Assign project to user | UI / DB |

---

## Contacts — No Maintenance Needed

The dynamic INSERT in Step 4 reads column list from `INFORMATION_SCHEMA` at runtime.  
New columns added to `contacts` are picked up automatically — no doc or script updates required.
