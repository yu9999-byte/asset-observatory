package io.observatory;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
final class HistoryStore {
 record Balance(String address,String rawQuantity) {}
 record Page(String key,String payloadSha256,Instant sourceAt,Instant fetchedAt,
             String checkpoint,String coverage,List<Balance> balances) {}
 record Lease(long streamId,long version,String token) {}
 record CleanupResult(int batchesDeleted,int observationsDeleted) {}
 private record ExpiredBatch(long id,int rowCount,Instant persistedAt) {}
 private record Policy(String provider,String dataset,String revision,String audience,
                       String evidenceUrl,String evidenceSha256,String expiresAt,int retentionDays) {}
 private record Stream(String policy,String asset,String chain,String contract,int decimals,
                       String checkpoint,long version,String token,Instant until,String policyFingerprint) {}
 private static final ObjectMapper JSON=new ObjectMapper();
 private static final BigInteger MAX=BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);
 private final JdbcTemplate db;
 private final TransactionTemplate tx;
 private final Clock clock;
 @Autowired HistoryStore(JdbcTemplate db,PlatformTransactionManager manager){this(db,manager,Clock.systemUTC());}
 HistoryStore(JdbcTemplate db,PlatformTransactionManager manager,Clock clock){this.db=db;this.tx=new TransactionTemplate(manager);this.clock=clock;}

 long open(String policyId,String assetId) {
  requireKey(policyId,128);var asset=Catalog.asset(assetId);
  if(asset.contract()==null||!"ethereum".equals(asset.chain()))throw new IllegalArgumentException("Verified ERC-20 catalog identity required");
  return tx.execute(status->{
   Policy policy=policy(policyId);
   db.update("INSERT INTO ingest_streams(policy_id,asset_id,chain_id,contract_address,decimals_value,policy_fingerprint) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id=id",policyId,asset.id(),asset.chain(),asset.contract(),asset.decimals(),sha256(json(policy).getBytes(StandardCharsets.UTF_8)));
   long id=db.queryForObject("SELECT id FROM ingest_streams WHERE policy_id=? AND chain_id=? AND contract_address=?",Long.class,policyId,asset.chain(),asset.contract());
   verifyPolicy(stream(id),policy);return id;
  });
 }

 Lease claim(long streamId) {
  return tx.execute(status->{
   Policy policy=policy(policyId(streamId));var stream=stream(streamId);verifyIdentity(stream);verifyPolicy(stream,policy);
   Instant now=clock.instant();
   if(stream.until()!=null&&stream.until().isAfter(now))throw new IllegalStateException("Stream already leased");
   String token=UUID.randomUUID().toString();
   db.update("UPDATE ingest_streams SET lease_token=?,lease_until=?,version=version+1 WHERE id=?",token,utc(now.plusSeconds(30)),streamId);
   return new Lease(streamId,stream.version()+1,token);
  });
 }

 long commit(Lease lease,Page input) {
  Objects.requireNonNull(lease);Page page=validate(input);String digest=fingerprint(page);
  return tx.execute(status->{
   // All operations lock policy before stream, including open and claim.
   Policy policy=policy(policyId(lease.streamId()));var stream=stream(lease.streamId());verifyIdentity(stream);verifyPolicy(stream,policy);
   var old=db.query("SELECT id,content_sha256 FROM ingest_batches WHERE stream_id=? AND observation_key=?",
    (rs,n)->Map.entry(rs.getLong("id"),rs.getString("content_sha256")),lease.streamId(),page.key());
   if(!old.isEmpty()){
    if(!old.getFirst().getValue().equals(digest))throw new IllegalStateException("Observation key reused with different content");
    return old.getFirst().getKey();
   }
   Instant now=clock.instant();
   if(!Instant.parse(policy.expiresAt()).isAfter(now))throw new IllegalStateException("Policy expired before commit");
   if(stream.version()!=lease.version()||!Objects.equals(stream.token(),lease.token())||stream.until()==null||!stream.until().isAfter(now))
    throw new IllegalStateException("Lease lost or expired");
   var keys=new GeneratedKeyHolder();
   db.update(connection->{
    var statement=connection.prepareStatement("INSERT INTO ingest_batches(stream_id,observation_key,payload_sha256,content_sha256,source_at,fetched_at,persisted_at,coverage,row_count,checkpoint_before,checkpoint_after,policy_evidence) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
    statement.setLong(1,lease.streamId());statement.setString(2,page.key());statement.setString(3,page.payloadSha256());statement.setString(4,digest);
    statement.setObject(5,utc(page.sourceAt()));statement.setObject(6,utc(page.fetchedAt()));statement.setObject(7,utc(now));
    statement.setString(8,page.coverage());statement.setInt(9,page.balances().size());statement.setString(10,stream.checkpoint());statement.setString(11,page.checkpoint());
    statement.setString(12,json(policy));
    return statement;
   },keys);
   long batch=Objects.requireNonNull(keys.getKey()).longValue();
   for(var balance:page.balances())db.update("INSERT INTO holder_observations(batch_id,address,raw_quantity) VALUES(?,?,?)",batch,balance.address(),balance.rawQuantity());
   if(!stream.until().isAfter(clock.instant())||!Instant.parse(policy.expiresAt()).isAfter(clock.instant()))
    throw new IllegalStateException("Lease or policy expired while persisting; retry from committed checkpoint");
   db.update("UPDATE ingest_streams SET checkpoint=?,version=version+1,lease_token=NULL,lease_until=NULL WHERE id=?",page.checkpoint(),lease.streamId());
   return batch;
  });
 }

 CleanupResult purgeExpiredObservations(int maxRows) {
  if(maxRows<1||maxRows>1000)throw new IllegalArgumentException("Cleanup row bound must be between 1 and 1000");
  return tx.execute(status->{
   Instant now=clock.instant();
   List<ExpiredBatch> candidates=db.query("SELECT b.id,b.row_count,b.persisted_at FROM ingest_batches b JOIN ingest_streams s ON s.id=b.stream_id JOIN source_policies p ON p.id=s.policy_id WHERE b.persisted_at<TIMESTAMPADD(DAY,-p.retention_days,?) AND b.row_count<=? ORDER BY b.persisted_at ASC,b.id ASC LIMIT 1000 FOR UPDATE SKIP LOCKED",
    (rs,n)->new ExpiredBatch(rs.getLong("id"),rs.getInt("row_count"),rs.getTimestamp("persisted_at").toInstant()),utc(now),maxRows);
   var selected=new ArrayList<ExpiredBatch>();int selectedRows=0;
   for(var batch:candidates){if(selectedRows+batch.rowCount()>maxRows)continue;selected.add(batch);selectedRows+=batch.rowCount();}
   if(selected.isEmpty())return new CleanupResult(0,0);
   var ids=selected.stream().map(ExpiredBatch::id).toList();
   String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
   int observations=db.update("DELETE FROM holder_observations WHERE batch_id IN ("+placeholders+")",ids.toArray());
   if(observations!=selectedRows)throw new IllegalStateException("Holder cleanup row count changed during deletion");
   int batches=db.update("DELETE FROM ingest_batches WHERE id IN ("+placeholders+")",ids.toArray());
   if(batches!=selected.size())throw new IllegalStateException("Holder cleanup batch count changed during deletion");
   Instant oldest=selected.getFirst().persistedAt();Instant newest=selected.getLast().persistedAt();
   db.update("INSERT INTO history_cleanup_audit(cleaned_at,batches_deleted,observations_deleted,oldest_persisted_at,newest_persisted_at) VALUES(?,?,?,?,?)",utc(now),batches,observations,utc(oldest),utc(newest));
   return new CleanupResult(batches,observations);
  });
 }

 private String policyId(long id){
  var rows=db.query("SELECT policy_id FROM ingest_streams WHERE id=?",(rs,n)->rs.getString(1),id);
  if(rows.isEmpty())throw new IllegalStateException("Unknown stream");return rows.getFirst();
 }
 private Policy policy(String id) {
  var rows=db.query("SELECT * FROM source_policies WHERE id=? FOR UPDATE",(rs,n)->{
   Instant expiry=rs.getObject("expires_at",LocalDateTime.class).toInstant(ZoneOffset.UTC);
   boolean evidence;
   try{URI uri=URI.create(rs.getString("evidence_url"));evidence="https".equals(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null;}
   catch(IllegalArgumentException invalid){evidence=false;}
   boolean allowed=rs.getBoolean("enabled")&&"holders".equals(rs.getString("dataset"))&&"personal".equals(rs.getString("audience"))
    &&expiry.isAfter(clock.instant())&&rs.getInt("retention_days")>=1&&rs.getInt("retention_days")<=400
    &&evidence&&rs.getString("evidence_sha256").matches("[0-9a-f]{64}");
   if(!allowed)throw new IllegalStateException("No active eligible holder storage policy");
   return new Policy(rs.getString("provider"),rs.getString("dataset"),rs.getString("revision"),rs.getString("audience"),rs.getString("evidence_url"),rs.getString("evidence_sha256"),expiry.toString(),rs.getInt("retention_days"));
  },id);
  if(rows.isEmpty())throw new IllegalStateException("No active eligible holder storage policy");return rows.getFirst();
 }
 private Stream stream(long id){
  var rows=db.query("SELECT * FROM ingest_streams WHERE id=? FOR UPDATE",(rs,n)->{
   LocalDateTime until=rs.getObject("lease_until",LocalDateTime.class);
   return new Stream(rs.getString("policy_id"),rs.getString("asset_id"),rs.getString("chain_id"),rs.getString("contract_address"),rs.getInt("decimals_value"),rs.getString("checkpoint"),rs.getLong("version"),rs.getString("lease_token"),until==null?null:until.toInstant(ZoneOffset.UTC),rs.getString("policy_fingerprint"));
  },id);
  if(rows.isEmpty())throw new IllegalStateException("Unknown stream");return rows.getFirst();
 }
 private static void verifyIdentity(Stream stream){
  var asset=Catalog.asset(stream.asset());
  if(!Objects.equals(asset.chain(),stream.chain())||!Objects.equals(asset.contract(),stream.contract())||asset.decimals()!=stream.decimals())
   throw new IllegalStateException("Catalog identity changed; review stream before collection");
 }
 private static void verifyPolicy(Stream stream,Policy policy){
  if(!sha256(json(policy).getBytes(StandardCharsets.UTF_8)).equals(stream.policyFingerprint()))
   throw new IllegalStateException("Policy revision changed or has no fingerprint; create a reviewed revision and stream");
 }
 private Page validate(Page page){
  if(page==null)throw new IllegalArgumentException("Page required");requireKey(page.key(),128);
  if(page.payloadSha256()==null||!page.payloadSha256().matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Payload SHA256 required");
  if(page.checkpoint()==null||page.checkpoint().length()>1024||page.coverage()==null||page.coverage().isBlank()||page.coverage().length()>1000)
   throw new IllegalArgumentException("Bounded checkpoint and coverage required");
  if(page.fetchedAt()==null||page.fetchedAt().isAfter(clock.instant())||page.sourceAt()!=null&&page.sourceAt().isAfter(page.fetchedAt()))
   throw new IllegalArgumentException("Invalid observation times");
  if(page.balances()==null||page.balances().size()>100)throw new IllegalArgumentException("At most 100 balances per page");
  var balances=new TreeMap<String,Balance>();
  for(var balance:page.balances()){
   if(balance==null||balance.rawQuantity()==null||!balance.rawQuantity().matches("0|[1-9][0-9]{0,77}")||new BigInteger(balance.rawQuantity()).compareTo(MAX)>0)
    throw new IllegalArgumentException("Canonical uint256 required");
   String address=Catalog.address(balance.address());
   if(balances.putIfAbsent(address,new Balance(address,balance.rawQuantity()))!=null)throw new IllegalArgumentException("Duplicate holder address");
  }
  return new Page(page.key(),page.payloadSha256(),micros(page.sourceAt()),micros(page.fetchedAt()),page.checkpoint(),page.coverage(),List.copyOf(balances.values()));
 }
 private static void requireKey(String key,int max){if(key==null||key.isBlank()||key.length()>max||!key.matches("[A-Za-z0-9_.:-]+"))throw new IllegalArgumentException("Invalid bounded identity key");}
 private static Instant micros(Instant instant){return instant==null?null:instant.truncatedTo(ChronoUnit.MICROS);}
 private static LocalDateTime utc(Instant instant){return instant==null?null:LocalDateTime.ofInstant(micros(instant),ZoneOffset.UTC);}
 private static String fingerprint(Page page){
  try{
   var bytes=new ByteArrayOutputStream();var data=new DataOutputStream(bytes);
   for(String field:List.of(page.payloadSha256(),Objects.toString(page.sourceAt(),"null"),page.fetchedAt().toString(),page.checkpoint(),page.coverage()))write(data,field);
   data.writeInt(page.balances().size());for(var balance:page.balances()){write(data,balance.address());write(data,balance.rawQuantity());}
   return sha256(bytes.toByteArray());
  }catch(IOException impossible){throw new IllegalStateException(impossible);}
 }
 private static String json(Policy policy){try{return JSON.writeValueAsString(policy);}catch(com.fasterxml.jackson.core.JsonProcessingException impossible){throw new IllegalStateException(impossible);}}
 private static String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
 private static void write(DataOutputStream data,String value)throws IOException{byte[] encoded=value.getBytes(StandardCharsets.UTF_8);data.writeInt(encoded.length);data.write(encoded);}
}
