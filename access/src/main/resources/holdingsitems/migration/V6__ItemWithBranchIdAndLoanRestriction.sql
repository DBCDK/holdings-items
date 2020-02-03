ALTER TABLE item ADD COLUMN branchId TEXT; -- Will eventually become NOT NULL
ALTER TABLE item ADD COLUMN loanRestriction TEXT NOT NULL DEFAULT '';
