
CREATE TABLE queue_rules (
    supplier TEXT NOT NULL,
    consumer TEXT NOT NULL,
    postpone INTEGER,
    PRIMARY KEY (supplier, consumer)
);
