CREATE TABLE item_loanRestriction (
    loanRestriction VARCHAR(1) NOT NULL,
    CONSTRAINT item_loanRestriction_pk PRIMARY KEY (loanRestriction)
);

INSERT INTO item_loanRestriction (loanRestriction) VALUES ('');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('a');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('b');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('c');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('d');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('e');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('f');
INSERT INTO item_loanRestriction (loanRestriction) VALUES ('g');

ALTER TABLE item ADD COLUMN branchId TEXT NOT NULL DEFAULT '';
ALTER TABLE item ADD COLUMN loanRestriction VARCHAR(1) NOT NULL DEFAULT '';
ALTER TABLE item ADD CONSTRAINT item_loanRestriction_fk FOREIGN KEY (loanRestriction) REFERENCES item_loanRestriction (loanRestriction);
