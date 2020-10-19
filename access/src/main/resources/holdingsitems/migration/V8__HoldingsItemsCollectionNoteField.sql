DROP VIEW holdingsitemscollection;
CREATE OR REPLACE VIEW holdingsitemscollection AS
    SELECT issue.agencyid,
           issue.bibliographicrecordid,
           issue.issueid,
           issue.issuetext,
           issue.expecteddelivery,
           issue.readyforloan,
           issue.complete,
           issue.created,
           issue.modified,
           issue.trackingid,
           issue.updated,
           issue.version,
           bibliographicitem.note
    FROM issue
             JOIN bibliographicitem USING (agencyid, bibliographicrecordid);
