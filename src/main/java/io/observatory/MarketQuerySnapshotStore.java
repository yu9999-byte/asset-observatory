package io.observatory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.observatory.ApiModels.Envelope;
import io.observatory.ApiModels.Market;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

interface MarketQuerySnapshotStore {
 record Snapshot(String token, Instant snapshotAt, List<Market> rows,
                Envelope<List<Market>> source, String filter, String sort,
                boolean desc, int limit) {}

 void save(Snapshot snapshot);
 Snapshot load(String token);
 void remove(String token);
 void prune(Instant now, int maxRows);
}

/** MySQL-backed cursor snapshots. Rows and provenance are retained together so a cursor is reproducible. */
@Repository
class JdbcMarketQuerySnapshotStore implements MarketQuerySnapshotStore {
 private final JdbcTemplate db;
 private final ObjectMapper mapper;

 JdbcMarketQuerySnapshotStore(JdbcTemplate db, ObjectMapper mapper) {
  this.db = db;
  this.mapper = mapper;
 }

 @Override public void save(Snapshot snapshot) {
  try {
   db.update("INSERT INTO market_query_snapshots(token,snapshot_at,filter_text,sort_field,direction_desc,page_size,rows_json,source_json) VALUES(?,?,?,?,?,?,?,?)",
    snapshot.token(), utc(snapshot.snapshotAt()), snapshot.filter(), snapshot.sort(), snapshot.desc(), snapshot.limit(),
    mapper.writeValueAsString(snapshot.rows()), mapper.writeValueAsString(snapshot.source()));
  } catch (JsonProcessingException e) {
   throw new IllegalStateException("无法序列化市场查询快照", e);
  }
 }

 @Override public Snapshot load(String token) {
  var rows = db.query("SELECT token,snapshot_at,filter_text,sort_field,direction_desc,page_size,rows_json,source_json FROM market_query_snapshots WHERE token=?",
   (rs, n) -> {
    try {
     Instant snapshotAt = rs.getTimestamp("snapshot_at").toInstant();
     List<Market> markets = mapper.readValue(rs.getString("rows_json"), new TypeReference<>() {});
     JavaType sourceType = mapper.getTypeFactory().constructParametricType(Envelope.class,
      mapper.getTypeFactory().constructCollectionType(List.class, Market.class));
     Envelope<List<Market>> source = mapper.readValue(rs.getString("source_json"), sourceType);
     return new Snapshot(rs.getString("token"), snapshotAt, List.copyOf(markets), source,
      rs.getString("filter_text"), rs.getString("sort_field"), rs.getBoolean("direction_desc"), rs.getInt("page_size"));
    } catch (Exception e) {
     throw new IllegalStateException("市场查询快照数据损坏", e);
    }
   }, token);
  return rows.isEmpty() ? null : rows.getFirst();
 }

 @Override public void remove(String token) {
  db.update("DELETE FROM market_query_snapshots WHERE token=?", token);
 }

 @Override public void prune(Instant now, int maxRows) {
  db.update("DELETE FROM market_query_snapshots WHERE snapshot_at < ?", utc(now.minusSeconds(120)));
  int count = db.queryForObject("SELECT COUNT(*) FROM market_query_snapshots", Integer.class);
  int excess = count - maxRows;
  if (excess <= 0) return;
  List<String> tokens = db.query("SELECT token FROM market_query_snapshots ORDER BY snapshot_at ASC, token ASC LIMIT ?",
   (rs, n) -> rs.getString(1), excess);
  if (!tokens.isEmpty()) db.batchUpdate("DELETE FROM market_query_snapshots WHERE token=?",
   tokens.stream().map(token -> new Object[]{token}).toList());
 }

 private static java.sql.Timestamp utc(Instant instant) {
  return java.sql.Timestamp.from(instant);
 }
}
