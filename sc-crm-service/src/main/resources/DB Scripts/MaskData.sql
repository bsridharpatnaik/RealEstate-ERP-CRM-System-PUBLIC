use egcity;
set SQL_SAFE_UPDATES=0;
-- UPDATE customer_lead SET email_id=REPEAT('*', CHAR_LENGTH(email_id) - 4) ;
    -- UPDATE customer_lead SET primary_mobile = LPAD(FLOOR(RAND() * 10000000000), 10, '0');
-- UPDATE customer_lead SET secondary_mobile=CONCAT(SUBSTR(secondary_mobile, 1,4), REPEAT('*', CHAR_LENGTH(secondary_mobile) - 4));
-- UPDATE security_user SET password='$2a$04$gxWcB5j52mfIOm5QdNacV.ChN7MUmV4WRwXgYF2q7zFGjJafLdQPS';
-- UPDATE security_user SET email='test@test.com';