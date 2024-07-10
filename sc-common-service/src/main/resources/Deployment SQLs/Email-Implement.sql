use egcity;
alter table security_user ADD COLUMN email varchar(255);
set @dbname='common';

set @q=concat('CREATE OR REPLACE view userdetails AS SELECT t.user_id, t.user_name,t.email, t.roles, u.tenants FROM
(
SELECT su.user_id,su.user_name,su.email, group_concat(ur.role_name SEPARATOR \',\') as roles FROM ',@dbname,'.security_user su
INNER JOIN ',@dbname,'.user_role ur on ur.user_id = su.user_id
INNER JOIN ',@dbname,'.role r on r.name = ur.role_name
WHERE su.status=true
GROUP BY su.user_id,su.user_name, su.email
) as t
LEFT JOIN
(
SELECT su.user_id,su.user_name,su.email,group_concat(tam.tenant_name SEPARATOR \',\') as tenants FROM ',@dbname,'.security_user su
INNER JOIN ',@dbname,'.user_role ur on ur.user_id = su.user_id
LEFT JOIN ',@dbname,'.user_tenant_mapping utm on su.user_id =utm.user_id
INNER JOIN ',@dbname,'.tenant_authorization_mapping tam ON tam.mapping_id=utm.mapping_id
WHERE su.status=true
GROUP BY su.user_id,su.user_name, su.email
) as u ON t.user_id=u.user_id;');
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;