-- Læsekompas indexes


DO
$$
DECLARE
  has BOOLEAN;
BEGIN
    SELECT true INTO has FROM pg_indexes
        WHERE tablename='holdingsitemsitem' AND
              indexname='holdingsitemsitem_branch';
    IF has IS NULL THEN
        EXECUTE 'CREATE INDEX holdingsitemsitem_branch ON holdingsitemsitem(agencyid,bibliographicrecordid,branch)';
    END IF;
END
$$;

DO
$$
DECLARE
  has BOOLEAN;
BEGIN
    SELECT true INTO has FROM pg_indexes
        WHERE tablename='holdingsitemsitem' AND
              indexname='holdingsitemsitem_branchstatus';
    IF has IS NULL THEN
        EXECUTE 'CREATE INDEX holdingsitemsitem_branchstatus ON holdingsitemsitem(agencyid,bibliographicrecordid,branch,status)';
    END IF;
END
$$;

