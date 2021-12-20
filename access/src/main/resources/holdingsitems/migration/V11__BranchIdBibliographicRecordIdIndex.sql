CREATE INDEX IF NOT EXISTS item_agencyid_branchid_bibliographicrecordid ON item (agencyid, branchid, bibliographicrecordid) WHERE branchid <> '';
