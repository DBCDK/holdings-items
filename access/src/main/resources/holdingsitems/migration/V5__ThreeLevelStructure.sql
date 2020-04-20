CREATE OR REPLACE FUNCTION note_of(_agencyid NUMERIC(6,0), _bibliographicrecordid TEXT) RETURNS TEXT AS $$
  DECLARE
    ret TEXT;
  BEGIN
    SELECT note INTO ret FROM holdingsitemscollection WHERE agencyid = _agencyid AND bibliographicrecordid = _bibliographicrecordid LIMIT 1;
    IF ret IS NULL THEN
      ret := '';
    END IF;
    RETURN ret;
  END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION firstAccessionDate_of(_agencyid NUMERIC(6,0), _bibliographicrecordid TEXT) RETURNS DATE AS $$
  DECLARE
    ret DATE;
  BEGIN
    SELECT MIN(accessionDate) INTO ret FROM holdingsitemsitem WHERE agencyid = _agencyid AND bibliographicrecordid = _bibliographicrecordid;
    IF ret IS NULL THEN
      ret := now();
    END IF;
    RETURN ret;
  END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION modified_of(_agencyid NUMERIC(6,0), _bibliographicrecordid TEXT) RETURNS TIMESTAMP WITH TIME ZONE AS $$
  DECLARE
    ret TIMESTAMP WITH TIME ZONE;
  BEGIN
    SELECT MAX(modified) INTO ret FROM holdingsitemsitem WHERE agencyid = _agencyid AND bibliographicrecordid = _bibliographicrecordid;
    IF ret IS NULL THEN
      ret := now();
    END IF;
    RETURN ret;
  END
$$ LANGUAGE plpgsql;



CREATE TABLE bibliographicitem AS
  SELECT agencyid, bibliographicrecordid,
         note_of(agencyid, bibliographicrecordid) AS note,
         firstAccessionDate_of(agencyid, bibliographicrecordid) AS firstaccessiondate,
         modified_of(agencyid, bibliographicrecordid) AS modified,
         CAST('migrate' AS VARCHAR(256)) AS trackingId,
         0 AS version
   FROM holdingsitemscollection GROUP BY agencyid, bibliographicrecordid;
-- 36 / 32
-- 15:51.136 / 16:36.570

ALTER TABLE bibliographicitem
  ALTER COLUMN agencyid SET NOT NULL;
ALTER TABLE bibliographicitem
  ALTER COLUMN bibliographicrecordid SET NOT NULL;
ALTER TABLE bibliographicitem
  ALTER COLUMN note SET NOT NULL;
ALTER TABLE bibliographicitem
  ALTER COLUMN firstaccessiondate SET NOT NULL;
ALTER TABLE bibliographicitem
  ALTER COLUMN firstaccessiondate SET DEFAULT timeofday()::date;
ALTER TABLE bibliographicitem
  ALTER COLUMN modified SET NOT NULL;
ALTER TABLE bibliographicitem
  ALTER COLUMN modified SET DEFAULT timeofday()::timestamp;
ALTER TABLE bibliographicitem
    ALTER COLUMN trackingId SET DEFAULT '';
ALTER TABLE bibliographicitem
  ALTER COLUMN trackingid SET NOT NULL;
ALTER TABLE bibliographicitem
    ALTER COLUMN version SET NOT NULL;
ALTER TABLE bibliographicitem
    ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE bibliographicitem
  ADD CONSTRAINT bibliographicitem_pkey
    PRIMARY KEY (agencyid, bibliographicrecordid);

CREATE INDEX bibliographicitem_agency
  ON bibliographicitem(agencyId);
CREATE INDEX bibliographicitem_bibliographic
  ON bibliographicitem(bibliographicRecordId);

DROP FUNCTION note_of(NUMERIC(6,0), TEXT);
DROP FUNCTION firstAccessionDate_of(NUMERIC(6,0), TEXT);
DROP FUNCTION modified_of(NUMERIC(6,0), TEXT);

ALTER TABLE holdingsitemscollection
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE holdingsitemscollection
  ADD CONSTRAINT issue_agencyidbibliographicrecordid_fk
    FOREIGN KEY (agencyid, bibliographicrecordid)
      REFERENCES bibliographicitem(agencyid, bibliographicrecordid);


ALTER TABLE holdingsitemscollection DROP COLUMN note;
ALTER TABLE holdingsitemscollection RENAME TO issue;

ALTER TABLE holdingsitemsitem
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE holdingsitemsitem RENAME TO item;

