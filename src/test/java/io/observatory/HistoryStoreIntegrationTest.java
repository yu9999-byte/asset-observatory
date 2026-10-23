package io.observatory;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:mysql://127.0.0.1:3307/observatory_test?connectionTimeZone=UTC&characterEncoding=UTF-8}","spring.datasource.username=${TEST_DATABASE_USER:observatory}","spring.datasource.password=${TEST_DATABASE_PASSWORD:local-only-not-for-deployment}","app.sources.enabled=false"})
class HistoryStoreIntegrationTest {
 @Autowired JdbcTemplate db;
 @Autowired PlatformTransactionManager manager;
 HistoryStore store;
 final AtomicReference<Instant> now=new AtomicReference<>();
 static final String ADDRESS="0x0000000000000000000000000000000000000001";
 static final String HASH="a".repeat(64);
 @BeforeEach void setup() {
  now.set(Instant.parse("2026-10-21T00:00:00Z"));
  store=new HistoryStore(db,manager,new Clock(){
   public ZoneId getZone(){return ZoneOffset.UTC;}
   public Clock withZone(ZoneId zone){return this;}
   public Instant instant(){return now.get();}
  });
  db.update("DELETE FROM holder_observations");
  db.update("DELETE FROM ingest_batches");
  db.update("DELETE FROM history_cleanup_audit");
  db.update("DELETE FROM ingest_streams");
  db.update("DELETE FROM source_policies");
  db.update("INSERT INTO source_policies(id,provider,dataset,revision,audience,evidence_url,evidence_sha256,expires_at,retention_days,enabled) VALUES('fixture','test-only','holders','v1','personal','https://example.org/test-policy',?, ?,30,true)",HASH,java.sql.Timestamp.from(now.get().plusSeconds(3600)));
 }
 @AfterEach void clearHistoryFixtures() {
  db.update("DELETE FROM holder_observations");db.update("DELETE FROM ingest_batches");db.update("DELETE FROM history_cleanup_audit");
  db.update("DELETE FROM ingest_streams");db.update("DELETE FROM source_policies");
 }
 HistoryStore.Page page(String key,String raw) {
  return new HistoryStore.Page(key,HASH,null,now.get(),"cursor:next","one observed page; continuity unknown",List.of(new HistoryStore.Balance(ADDRESS,raw)));
 }
 long stream(){return store.open("fixture","LINK");}
 int count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}

 @Test void preservesUint256NullSourceTimeAndExactRetry() {
  long id=stream(); var lease=store.claim(id); String max=BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE).toString();
  var input=page("observation-1",max); long batch=store.commit(lease,input);
  assertEquals(max,db.queryForObject("SELECT raw_quantity FROM holder_observations WHERE batch_id=?",String.class,batch));
  assertNull(db.queryForObject("SELECT source_at FROM ingest_batches WHERE id=?",Object.class,batch));
  long version=db.queryForObject("SELECT version FROM ingest_streams WHERE id=?",Long.class,id);
  assertEquals(batch,store.commit(lease,input)); assertEquals(1,count("ingest_batches"));
  assertEquals(version,db.queryForObject("SELECT version FROM ingest_streams WHERE id=?",Long.class,id));
  assertEquals("cursor:next",db.queryForObject("SELECT checkpoint FROM ingest_streams WHERE id=?",String.class,id));
  assertThrows(IllegalStateException.class,()->store.commit(lease,page("observation-1","2")));
 }
 @Test void transactionRollbackNeverAdvancesCheckpointWithoutObservations() {
  long id=stream();var lease=store.claim(id);
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).execute(status->{store.commit(lease,page("rollback","123"));throw new IllegalStateException("simulated crash before outer commit");}));
  assertEquals(0,count("ingest_batches")); assertEquals(0,count("holder_observations"));
  assertNull(db.queryForObject("SELECT checkpoint FROM ingest_streams WHERE id=?",String.class,id));
  assertEquals(lease.version(),db.queryForObject("SELECT version FROM ingest_streams WHERE id=?",Long.class,id));
  assertTrue(store.commit(lease,page("rollback","123"))>0);
 }
 @Test void revokedExpiredAndWrongDatasetPoliciesDenyStorage() {
  assertThrows(IllegalStateException.class,()->store.open("absent","LINK"));
  long id=stream();var lease=store.claim(id);
  db.update("UPDATE source_policies SET enabled=false WHERE id='fixture'");
  assertThrows(IllegalStateException.class,()->store.commit(lease,page("revoked","1")));
  db.update("UPDATE source_policies SET enabled=true,expires_at=? WHERE id='fixture'",java.sql.Timestamp.from(now.get()));
  assertThrows(IllegalStateException.class,()->store.claim(id));
  assertThrows(IllegalStateException.class,()->store.open("fixture","LINK"));
  db.update("UPDATE source_policies SET expires_at=?,dataset='transfers' WHERE id='fixture'",java.sql.Timestamp.from(now.get().plusSeconds(60)));
  assertThrows(IllegalStateException.class,()->store.open("fixture","LINK")); assertEquals(0,count("ingest_batches"));
 }
 @Test void expiredLeaseCannotCommitAfterAnotherWorkerClaims() {
  long id=stream();var old=store.claim(id);
  assertThrows(IllegalStateException.class,()->store.claim(id));
  now.updateAndGet(t->t.plusSeconds(31));var current=store.claim(id);
  assertTrue(current.version()>old.version());
  assertThrows(IllegalStateException.class,()->store.commit(old,page("lost","1")));
  assertEquals(0,count("ingest_batches"));
  assertTrue(store.commit(current,page("current","2"))>0);
 }
 @Test void databaseLeaseAllowsOnlyOneConcurrentWorker() throws Exception {
  long id=stream(); var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)) {
   Callable<Boolean> task=()->{ready.countDown();assertTrue(start.await(3,TimeUnit.SECONDS));try{store.claim(id);return true;}catch(IllegalStateException busy){return false;}};
   var a=pool.submit(task);var b=pool.submit(task);assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();
   assertNotEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));
  }
 }
 @Test void rejectsInvalidPagesWithoutPartialWrites() {
  long id=stream();var lease=store.claim(id);var base=page("invalid","1");
  for(String raw:List.of("-1","1.5","01",BigInteger.ONE.shiftLeft(256).toString()))
   assertThrows(IllegalArgumentException.class,()->store.commit(lease,page("invalid",raw)));
  assertThrows(IllegalArgumentException.class,()->store.commit(lease,new HistoryStore.Page(base.key(),HASH,null,now.get(),base.checkpoint(),base.coverage(),Collections.nCopies(101,new HistoryStore.Balance(ADDRESS,"1")))));
  assertThrows(IllegalArgumentException.class,()->store.commit(lease,new HistoryStore.Page(base.key(),HASH,null,now.get(),base.checkpoint(),base.coverage(),List.of(new HistoryStore.Balance(ADDRESS,"1"),new HistoryStore.Balance(ADDRESS,"2")))));
  assertThrows(IllegalArgumentException.class,()->store.commit(lease,new HistoryStore.Page(base.key(),HASH,now.get().plusSeconds(1),now.get(),base.checkpoint(),base.coverage(),base.balances())));
  assertThrows(IllegalArgumentException.class,()->store.commit(lease,new HistoryStore.Page(base.key(),HASH,null,now.get().plusSeconds(1),base.checkpoint(),base.coverage(),base.balances())));
  assertThrows(IllegalArgumentException.class,()->store.open("fixture","BTC"));
  assertEquals(0,count("ingest_batches"));assertEquals(0,count("holder_observations"));
 }
 @Test void retryIdentityIncludesProvenanceAndBalancesButNotPageOrder() {
  long id=stream();var lease=store.claim(id);var base=page("ordered","1");
  var balances=List.of(new HistoryStore.Balance(ADDRESS,"1"),new HistoryStore.Balance("0x0000000000000000000000000000000000000002","2"));
  var input=new HistoryStore.Page(base.key(),HASH,null,now.get(),base.checkpoint(),base.coverage(),balances);
  long batch=store.commit(lease,input);
  assertEquals(batch,store.commit(lease,new HistoryStore.Page(base.key(),HASH,null,now.get(),base.checkpoint(),base.coverage(),balances.reversed())));
  for(var changed:List.of(
   new HistoryStore.Page(base.key(),"b".repeat(64),null,now.get(),base.checkpoint(),base.coverage(),balances),
   new HistoryStore.Page(base.key(),HASH,null,now.get(),"different",base.coverage(),balances),
   new HistoryStore.Page(base.key(),HASH,null,now.get(),base.checkpoint(),"different",balances),
   new HistoryStore.Page(base.key(),HASH,now.get(),now.get(),base.checkpoint(),base.coverage(),balances)))
   assertThrows(IllegalStateException.class,()->store.commit(lease,changed));
  assertEquals(1,count("ingest_batches"));assertEquals(2,count("holder_observations"));
 }
 @Test void changedPolicyRevisionCannotRewriteCommittedEvidence() {
  long id=stream();var lease=store.claim(id);long batch=store.commit(lease,page("audited","1"));
  assertEquals(HASH,db.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(policy_evidence,'$.evidenceSha256')) FROM ingest_batches WHERE id=?",String.class,batch));
  db.update("UPDATE source_policies SET evidence_sha256=? WHERE id='fixture'","b".repeat(64));
  assertThrows(IllegalStateException.class,()->store.claim(id));
  assertThrows(IllegalStateException.class,()->store.open("fixture","LINK"));
  assertEquals(HASH,db.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(policy_evidence,'$.evidenceSha256')) FROM ingest_batches WHERE id=?",String.class,batch));
 }
 @Test void incompletePermissionEvidenceNeverOpensStream() {
  for(String mutation:List.of("audience='public'","evidence_url='http://example.org'","evidence_sha256='unknown'","enabled=false")) {
   db.update("UPDATE source_policies SET "+mutation+" WHERE id='fixture'");
   assertThrows(IllegalStateException.class,()->store.open("fixture","LINK"));
   db.update("UPDATE source_policies SET audience='personal',evidence_url='https://example.org/test-policy',evidence_sha256=?,enabled=true WHERE id='fixture'",HASH);
  }
  assertEquals(0,count("ingest_streams"));
 }

 @Test void retentionCleanupUsesPolicyAgeAndAWholeBatchRowBound() {
  long id=stream();
  for(int i=0;i<3;i++){
   var lease=store.claim(id);store.commit(lease,page("retention-"+i,Integer.toString(i+1)));
  }
  db.update("UPDATE ingest_batches SET persisted_at=? WHERE observation_key IN ('retention-0','retention-1')",java.sql.Timestamp.from(now.get().minus(Duration.ofDays(31))));
  db.update("UPDATE source_policies SET enabled=false WHERE id='fixture'");

  var first=store.purgeExpiredObservations(1);
  assertEquals(1,first.observationsDeleted());assertEquals(1,first.batchesDeleted());
  assertEquals(2,count("ingest_batches"));assertEquals(2,count("holder_observations"));
  assertEquals(1,count("history_cleanup_audit"));
  assertEquals(1,db.queryForObject("SELECT observations_deleted FROM history_cleanup_audit",Integer.class));

  var second=store.purgeExpiredObservations(1);
  assertEquals(1,second.observationsDeleted());assertEquals(1,second.batchesDeleted());
  var noOp=store.purgeExpiredObservations(1);
  assertEquals(0,noOp.observationsDeleted());assertEquals(0,noOp.batchesDeleted());
  assertEquals(1,count("ingest_batches"));assertEquals(1,count("holder_observations"));
  assertEquals("retention-2",db.queryForObject("SELECT observation_key FROM ingest_batches",String.class));
 }

 @Test void retentionCleanupRollsBackDeletesAndAuditAsOneTransaction() {
  long id=stream();var lease=store.claim(id);store.commit(lease,page("rollback-cleanup","9"));
  db.update("UPDATE ingest_batches SET persisted_at=?",java.sql.Timestamp.from(now.get().minus(Duration.ofDays(31))));
  assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).execute(status->{
   var result=store.purgeExpiredObservations(10);
   assertEquals(1,result.observationsDeleted());
   throw new IllegalStateException("simulated outer transaction rollback");
  }));
  assertEquals(1,count("ingest_batches"));assertEquals(1,count("holder_observations"));
  assertEquals(0,count("history_cleanup_audit"));
 }

 @Test void retentionCleanupKeepsBatchesWithinTheirOwnPolicyWindow() {
  long normalPolicyStream=stream();var normalLease=store.claim(normalPolicyStream);
  store.commit(normalLease,page("normal-retention","1"));
  db.update("INSERT INTO source_policies(id,provider,dataset,revision,audience,evidence_url,evidence_sha256,expires_at,retention_days,enabled) VALUES('short-fixture','short-test','holders','v1','personal','https://example.org/short-test-policy',?, ?,1,true)",HASH,java.sql.Timestamp.from(now.get().plusSeconds(3600)));
  long shortPolicyStream=store.open("short-fixture","LINK");var shortLease=store.claim(shortPolicyStream);
  store.commit(shortLease,page("short-retention","2"));
  db.update("UPDATE source_policies SET enabled=false,expires_at=? WHERE id='short-fixture'",java.sql.Timestamp.from(now.get().minusSeconds(1)));
  now.updateAndGet(t->t.plus(Duration.ofDays(10)));

  var result=store.purgeExpiredObservations(10);
  assertEquals(1,result.observationsDeleted());assertEquals(1,result.batchesDeleted());
  assertEquals("normal-retention",db.queryForObject("SELECT observation_key FROM ingest_batches",String.class));
 }

 @Test void retentionCleanupRejectsInvalidBounds() {
  assertThrows(IllegalArgumentException.class,()->store.purgeExpiredObservations(0));
  assertThrows(IllegalArgumentException.class,()->store.purgeExpiredObservations(1001));
 }
}
