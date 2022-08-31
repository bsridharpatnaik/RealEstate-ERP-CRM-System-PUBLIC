create table smsdeliverylist (
type varchar(32) PRIMARY KEY,
numbers varchar(1000));

use egcity;
INSERT INTO `smsdeliverylist`
(`type`,
`numbers`)
VALUES
('EgcityIOStats',
'918600033031');

use newsuncitynx;
INSERT INTO `smsdeliverylist`
(`type`,
`numbers`)
VALUES
('SuncityIOStats',
'918600033031');
