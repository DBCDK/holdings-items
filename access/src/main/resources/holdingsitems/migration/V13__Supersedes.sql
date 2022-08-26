
CREATE TABLE Supersedes (
    overtaken TEXT NOT NULL PRIMARY KEY,
    owner TEXT NOT NULL
);

CREATE INDEX supersedes_owner_idx ON supersedes(owner);