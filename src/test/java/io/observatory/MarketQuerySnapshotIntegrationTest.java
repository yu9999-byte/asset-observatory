package io.observatory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:mysql://127.0.0.1:3307/observatory_test?connectionTimeZone=UTC&characterEncoding=UTF-8}","spring.datasource.username=${TEST_DATABASE_USER:observatory}","spring.datasource.password=${TEST_DATABASE_PASSWORD:local-only-not-for-deployment}","app.sources.enabled=false"})
class MarketQuerySnapshotIntegrationTest {
 @Autowired ResearchService service;
 @Autowired MarketQuerySnapshotStore store;
 @Autowired SourceHttp http;
 @Autowired ObjectMapper mapper;
 @Autowired JdbcTemplate db;

 @AfterEach void cleanup() { db.update("DELETE FROM market_query_snapshots"); }

 @Test void cursorContinuesFromAnotherServiceInstanceUsingPersistedSnapshot() {
  var first=service.marketQuery(null,"assetId","asc",3,null);
  var otherInstance=new ResearchService(http,mapper,false,false,false,store);
  var continued=otherInstance.marketQuery(null,"assetId","asc",3,first.nextCursor());
  assertThat(continued.data()).extracting(ApiModels.Market::assetId).containsExactly("LINK","UNI","USDC");
  assertThat(continued.snapshotAt()).isEqualTo(first.snapshotAt());
  assertThat(continued.reason()).isEqualTo(first.reason());
 }

 @Test void cursorBindsQueryAndRejectsExpiredSnapshot() {
  var page=service.marketQuery(null,"assetId","asc",3,null);
  assertThatThrownBy(()->service.marketQuery("JCT","assetId","asc",3,page.nextCursor()))
   .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("查询条件不匹配");
  String token=decodeToken(page.nextCursor());
  db.update("UPDATE market_query_snapshots SET snapshot_at=UTC_TIMESTAMP(6)-INTERVAL 121 SECOND WHERE token=?",token);
  assertThatThrownBy(()->service.marketQuery(null,"assetId","asc",3,page.nextCursor()))
   .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("已过期");
  assertThat(db.queryForObject("SELECT COUNT(*) FROM market_query_snapshots WHERE token=?",Integer.class,token)).isZero();
 }

 @Test void concurrentContinuationIsDeterministicAndSnapshotCleanupIsBounded() throws Exception {
  var page=service.marketQuery(null,"assetId","asc",3,null);
  try(var pool=Executors.newFixedThreadPool(2)) {
   Callable<Object> query=()->service.marketQuery(null,"assetId","asc",3,page.nextCursor());
   var a=pool.submit(query);var b=pool.submit(query);
   assertThat(a.get(3,TimeUnit.SECONDS)).isEqualTo(b.get(3,TimeUnit.SECONDS));
  }
  for(int i=0;i<40;i++)service.marketQuery("JCT","assetId","asc",1,null);
  assertThat(db.queryForObject("SELECT COUNT(*) FROM market_query_snapshots",Integer.class)).isLessThanOrEqualTo(32);
 }

 private String decodeToken(String cursor) {
  try { return mapper.readTree(java.util.Base64.getUrlDecoder().decode(cursor)).path("token").asText(); }
  catch(Exception e) { throw new AssertionError(e); }
 }
}
