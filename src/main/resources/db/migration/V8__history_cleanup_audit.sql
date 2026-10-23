CREATE TABLE history_cleanup_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 cleaned_at DATETIME(6) NOT NULL,
 batches_deleted INT NOT NULL,
 observations_deleted INT NOT NULL,
 oldest_persisted_at DATETIME(6) NOT NULL,
 newest_persisted_at DATETIME(6) NOT NULL,
 KEY cleanup_time (cleaned_at),
 CHECK (batches_deleted > 0),
 CHECK (observations_deleted BETWEEN 0 AND 1000)
) ENGINE=InnoDB;
