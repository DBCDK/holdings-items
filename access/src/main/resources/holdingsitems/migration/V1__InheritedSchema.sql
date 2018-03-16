
DO
$$
DECLARE
    b BOOLEAN;
BEGIN
    SELECT COUNT(*) = 1 INTO b FROM pg_tables WHERE schemaname='public' AND tablename='holdingsitemscollection';
    IF b = true THEN
        DROP FUNCTION IF EXISTS dequeue(worker_ VARCHAR(128), no_ INT);
        DROP FUNCTION IF EXISTS dequeue(worker_ VARCHAR(128));
        DROP FUNCTION IF EXISTS enqueue(bibliographicRecordId_ TEXT, agencyId_ NUMERIC(6), issueId_ TEXT, additionalData_ TEXT, provider_ VARCHAR(32));
        DROP FUNCTION IF EXISTS enqueue(bibliographicRecordId_ TEXT, agencyId_ NUMERIC(6), issueId_ TEXT, provider_ VARCHAR(32));
        DROP FUNCTION IF EXISTS lockagency(agencyId_ NUMERIC(6));
        DROP TABLE IF EXISTS queue CASCADE;
        DROP TABLE IF EXISTS messagequeuerules CASCADE;
        DROP TABLE IF EXISTS queuerules CASCADE;
        DROP TABLE IF EXISTS jobdiag CASCADE;
        DROP TABLE IF EXISTS queue CASCADE;
        DROP TABLE IF EXISTS queueworkers CASCADE;
        DROP TABLE IF EXISTS agencylock CASCADE;

    ELSE
        CREATE TABLE version (
               version NUMERIC(6) NOT NULL PRIMARY KEY,
               warning TEXT DEFAULT NULL
        );
        -- Compatible versions
        INSERT INTO version VALUES(7, 'Please upgrade dk.dbc:holdingsitems-acces to 1.1.1-SNAPSHOT');
        INSERT INTO version VALUES(8, 'Please upgrade dk.dbc:holdingsitems-acces to 1.1.1-SNAPSHOT');
        INSERT INTO version VALUES(9, 'Please upgrade dk.dbc:holdingsitems-acces to 1.1.1-SNAPSHOT');
        INSERT INTO version VALUES(10, 'Please upgrade dk.dbc:holdingsitems-acces to 1.1.1-SNAPSHOT');
        INSERT INTO version VALUES(11, 'Please upgrade dk.dbc:holdingsitems-acces to 1.1.1-SNAPSHOT');
        INSERT INTO version VALUES(12, 'Please upgrade dk.dbc:holdingsitems-acces to 1.1.1-SNAPSHOT');
        INSERT INTO version VALUES(13);
        INSERT INTO version VALUES(14);

        CREATE TABLE holdingsitems_status (
               status VARCHAR(64) NOT NULL,
               CONSTRAINT holdingsitems_status_pk PRIMARY KEY (status)
        );
        INSERT INTO holdingsitems_status (status) VALUES ('NotForLoan');
        INSERT INTO holdingsitems_status (status) VALUES ('OnLoan');
        INSERT INTO holdingsitems_status (status) VALUES ('OnOrder');
        INSERT INTO holdingsitems_status (status) VALUES ('OnShelf');
        INSERT INTO holdingsitems_status (status) VALUES ('Online');
        INSERT INTO holdingsitems_status (status) VALUES ('Decommissioned');

        CREATE TABLE holdingsitemscollection (
               agencyId NUMERIC(6) NOT NULL,
               bibliographicRecordId TEXT NOT NULL,
               issueId TEXT NOT NULL,
               issueText TEXT NOT NULL,
               expectedDelivery DATE,
               readyForLoan INTEGER NOT NULL, -- number of units available for loan - -1 none available for loan ever
               note TEXT NOT NULL DEFAULT '',
               complete TIMESTAMP NOT NULL DEFAULT timeofday()::timestamp,
               modified TIMESTAMP NOT NULL,
               created TIMESTAMP NOT NULL DEFAULT timeofday()::timestamp,
               updated TIMESTAMP NOT NULL DEFAULT timeofday()::timestamp,
               trackingId VARCHAR(256) NOT NULL DEFAULT '',
               CONSTRAINT holdingsitemscollection_pk PRIMARY KEY (agencyId, bibliographicRecordId, issueId)
        );
        CREATE TABLE holdingsitemsitem (
               agencyId NUMERIC(6) NOT NULL,
               bibliographicRecordId TEXT NOT NULL,
               issueId TEXT NOT NULL,
               itemId TEXT NOT NULL,
               branch TEXT NOT NULL,
               department TEXT NOT NULL,
               location TEXT NOT NULL,
               subLocation TEXT NOT NULL,
               circulationRule TEXT NOT NULL,
               accessionDate DATE NOT NULL,
               status VARCHAR(64) NOT NULL,
               created TIMESTAMP NOT NULL DEFAULT timeofday()::timestamp,
               modified TIMESTAMP NOT NULL,
               trackingId VARCHAR(256) NOT NULL DEFAULT '',
               CONSTRAINT holdingsitemsitem_pk PRIMARY KEY (agencyId, bibliographicRecordId, issueId, itemId),
               CONSTRAINT holdingsitemsitem_fk_collection FOREIGN KEY (agencyId, bibliographicRecordId, issueId) REFERENCES holdingsitemscollection (agencyId, bibliographicRecordId, issueId),
               CONSTRAINT holdingsitemsitem_fk_status FOREIGN KEY (status) REFERENCES holdingsitems_status (status)
        );

        CREATE INDEX holdingsitemscollection_trackingid ON holdingsitemscollection (trackingId);
        CREATE INDEX holdingsitemscollection_bibliographicRecordId ON holdingsitemscollection (bibliographicRecordId);
        CREATE INDEX holdingsitemsitem_trackingid ON holdingsitemsitem (trackingId);
        CREATE INDEX holdingsitemsitem_agency_record ON holdingsitemsitem (agencyId, bibliographicRecordId);
        CREATE INDEX holdingsitemsitem_record ON holdingsitemsitem (bibliographicRecordId);

        CREATE TABLE q (
            worker TEXT NOT NULL,
            queued TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp(),
            agencyId NUMERIC(6,0) NOT NULL,
            bibliographicRecordId TEXT NOT NULL,
            issueId TEXT NOT NULL DEFAULT '',
            additionalData TEXT NOT NULL DEFAULT '',
            trackingId TEXT NOT NULL DEFAULT '',
            takenBy TEXT,
            takenAt TIMESTAMP WITH TIME ZONE,
            PRIMARY KEY (worker, queued, agencyId, bibliographicRecordId, issueId)
        );

        CREATE INDEX q_wt ON q (worker, takenby);

        CREATE INDEX q_takenBy ON q (takenBy) WHERE takenBy IS NOT NULL;
        CREATE INDEX q_takenAt ON q (takenAt) WHERE takenAt IS NOT NULL;
        CREATE INDEX q_take ON q (worker, queued) WHERE takenBy IS NULL;


        CREATE TABLE diag (
            worker TEXT NOT NULL,
            queued TIMESTAMP WITH TIME ZONE NOT NULL,
            agencyId NUMERIC(6,0) NOT NULL,
            bibliographicRecordId TEXT NOT NULL,
            issueId TEXT NOT NULL DEFAULT '',
            additionalData TEXT NOT NULL DEFAULT '',
            trackingId TEXT NOT NULL DEFAULT '',
            error TEXT NOT NULL,
            PRIMARY KEY (worker, queued, agencyId, bibliographicRecordId, issueId)
        );


    END IF;
END
$$;

CREATE TABLE queue (
    agencyId NUMERIC(6) NOT NULL,
    bibliographicRecordId TEXT NOT NULL,
    additionalData TEXT NOT NULL DEFAULT '',
    trackingId TEXT NOT NULL DEFAULT ''
);

CREATE TABLE queue_error(
    agencyId NUMERIC(6) NOT NULL,
    bibliographicRecordId TEXT NOT NULL,
    additionalData TEXT NOT NULL DEFAULT '',
    trackingId TEXT NOT NULL DEFAULT ''
);
