CREATE INDEX IF NOT EXISTS item_branchid_bibliographicrecordid ON item (branchid, bibliographicrecordid) WHERE branchid <> '';
