
INSERT INTO holdingsitems_status (status) VALUES ('Lost');
INSERT INTO holdingsitems_status (status) VALUES ('Discarded');

-- Should be not null for 'Online' items
ALTER TABLE item ADD COLUMN lastLoanDate DATE;
