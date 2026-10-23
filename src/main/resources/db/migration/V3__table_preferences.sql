CREATE TABLE table_preferences (
  owner_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
  table_id VARCHAR(32) COLLATE utf8mb4_bin NOT NULL,
  config JSON NOT NULL,
  version BIGINT NOT NULL,
  updated_at VARCHAR(40) NOT NULL,
  PRIMARY KEY (owner_id, table_id)
);
