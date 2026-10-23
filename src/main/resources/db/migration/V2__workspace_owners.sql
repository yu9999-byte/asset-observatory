ALTER TABLE watches
  ADD COLUMN owner_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL DEFAULT 'local-workspace',
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (owner_id, kind, entity_key);
ALTER TABLE notes
  ADD COLUMN owner_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL DEFAULT 'local-workspace',
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (owner_id, kind, entity_key);
