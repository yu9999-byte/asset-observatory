ALTER TABLE ingest_streams
 ADD COLUMN policy_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL;
ALTER TABLE ingest_batches ADD COLUMN policy_evidence JSON NULL;
