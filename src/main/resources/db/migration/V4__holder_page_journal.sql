CREATE TABLE source_policies (
 id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 provider VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 dataset VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 revision VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 audience VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 evidence_url VARCHAR(2048) NOT NULL,
 evidence_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 retention_days INT NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY policy_identity (provider,dataset,revision),
 CHECK (retention_days BETWEEN 1 AND 400)
) ENGINE=InnoDB;

CREATE TABLE ingest_streams (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 policy_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 asset_id VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 chain_id VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 contract_address CHAR(42) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 decimals_value INT NOT NULL,
 checkpoint VARCHAR(1024),
 version BIGINT NOT NULL DEFAULT 0,
 lease_token CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
 lease_until DATETIME(6),
 UNIQUE KEY stream_identity (policy_id,chain_id,contract_address),
 FOREIGN KEY (policy_id) REFERENCES source_policies(id),
 CHECK (decimals_value BETWEEN 0 AND 255)
) ENGINE=InnoDB;

CREATE TABLE ingest_batches (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 stream_id BIGINT NOT NULL,
 observation_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 content_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 source_at DATETIME(6),
 fetched_at DATETIME(6) NOT NULL,
 persisted_at DATETIME(6) NOT NULL,
 coverage VARCHAR(1000) NOT NULL,
 row_count INT NOT NULL,
 checkpoint_before VARCHAR(1024),
 checkpoint_after VARCHAR(1024) NOT NULL,
 UNIQUE KEY batch_identity (stream_id,observation_key),
 KEY stream_fetch (stream_id,fetched_at),
 FOREIGN KEY (stream_id) REFERENCES ingest_streams(id),
 CHECK (row_count BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE holder_observations (
 batch_id BIGINT NOT NULL,
 address CHAR(42) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 raw_quantity VARCHAR(78) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 PRIMARY KEY (batch_id,address),
 KEY address_batches (address,batch_id),
 FOREIGN KEY (batch_id) REFERENCES ingest_batches(id)
) ENGINE=InnoDB;
