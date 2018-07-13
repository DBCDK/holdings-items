ALTER TABLE queue ADD COLUMN stateChange TEXT DEFAULT '{}';
ALTER TABLE queue_error ADD COLUMN stateChange TEXT DEFAULT '{}';
