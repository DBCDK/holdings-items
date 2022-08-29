
CREATE TABLE supersedes (
    superseded TEXT NOT NULL PRIMARY KEY,
    superseding TEXT NOT NULL
);

CREATE INDEX supersedes_superseding_idx ON supersedes(superseding);
