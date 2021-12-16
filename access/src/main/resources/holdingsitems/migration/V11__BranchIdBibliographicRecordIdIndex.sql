DO
$$
DECLARE
  has BOOLEAN;
BEGIN
    SELECT true INTO has FROM pg_indexes
        WHERE tablename='item' AND
              indexname='item_branchid_bibliographicrecordid';
    IF has IS NULL THEN
        -- Can be made `CONCURRENTLY` ahead of time
        CREATE INDEX item_branchid_bibliographicrecordid ON item (branchid, bibliographicrecordid) WHERE branchid <> '';
    END IF;
END
$$;

