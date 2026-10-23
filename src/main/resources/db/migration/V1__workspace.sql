CREATE TABLE watches (
  kind VARCHAR(16) NOT NULL,
  entity_key VARCHAR(64) NOT NULL,
  created_at VARCHAR(40) NOT NULL,
  PRIMARY KEY (kind, entity_key)
);
CREATE TABLE notes (
  kind VARCHAR(16) NOT NULL,
  entity_key VARCHAR(64) NOT NULL,
  body TEXT NOT NULL,
  visible BOOLEAN NOT NULL,
  version BIGINT NOT NULL,
  updated_at VARCHAR(40) NOT NULL,
  PRIMARY KEY (kind, entity_key)
);
