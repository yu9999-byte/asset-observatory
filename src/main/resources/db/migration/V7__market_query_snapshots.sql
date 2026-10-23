CREATE TABLE market_query_snapshots (
  token CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  snapshot_at DATETIME(6) NOT NULL,
  filter_text VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  sort_field VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  direction_desc BOOLEAN NOT NULL,
  page_size INT NOT NULL,
  rows_json JSON NOT NULL,
  source_json JSON NOT NULL,
  PRIMARY KEY (token),
  KEY market_snapshot_time (snapshot_at),
  CHECK (page_size BETWEEN 1 AND 100)
) ENGINE=InnoDB;
