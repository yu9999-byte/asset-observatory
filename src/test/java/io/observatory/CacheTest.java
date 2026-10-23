package io.observatory;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CacheTest {
 @Test void sameKeyWaitIsBoundedAndCapacityPreservesOriginalSnapshot() throws Exception {
  var clock=new MutableClock();var cache=new SnapshotCache<List<String>>(1,clock,Duration.ofMillis(40));
  var calls=new AtomicInteger();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
  var good=new ApiModels.Envelope<>(List.of("fixture"),"ok","test","https://example.test",clock.instant(),null,"sample",null,null);
  var bad=new ApiModels.Envelope<List<String>>(List.of(),"unavailable","test","https://example.test",null,null,"sample","unavailable",null);
  cache.get("same",Duration.ofSeconds(30),()->good,()->bad);clock.now=clock.now.plusSeconds(31);
  try(var workers=Executors.newSingleThreadExecutor()) {
   var leader=workers.submit(()->cache.get("same",Duration.ofSeconds(30),()->{
    calls.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return good;
   },()->bad));
   try {
    assertThat(entered.await(1,TimeUnit.SECONDS)).isTrue();
    var follower=cache.get("same",Duration.ofSeconds(30),()->{calls.incrementAndGet();return bad;},()->bad);
    assertThat(follower.status()).isEqualTo("stale");
    assertThat(follower.fetchedAt()).isEqualTo(good.fetchedAt());
    assertThat(follower.data()).containsExactly("fixture");
    assertThat(follower.reason()).contains("超时");
    var saturated=cache.get("other",Duration.ofSeconds(30),()->{calls.incrementAndGet();return good;},()->bad);
    assertThat(saturated.status()).isEqualTo("unavailable");
    assertThat(saturated.reason()).contains("已满");assertThat(calls.get()).isEqualTo(1);
   } finally {release.countDown();}
   assertThat(leader.get(1,TimeUnit.SECONDS).status()).isEqualTo("ok");
   assertThat(cache.size()).isEqualTo(1);
  }
 }
 @Test void slowKeyDoesNotBlockAnotherKey() throws Exception {
  var clock=new MutableClock();var cache=new SnapshotCache<List<String>>(2,clock);
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
  var good=new ApiModels.Envelope<>(List.of("fixture"),"ok","test","https://example.test",clock.instant(),null,"sample",null,null);
  var bad=new ApiModels.Envelope<List<String>>(List.of(),"unavailable","test","https://example.test",null,null,"sample","unavailable",null);
  try(var workers=Executors.newFixedThreadPool(2)) {
   var slow=workers.submit(()->cache.get("slow",Duration.ofSeconds(30),()->{
    entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return good;
   },()->bad));
   try {
    assertThat(entered.await(1,TimeUnit.SECONDS)).isTrue();
    var fast=workers.submit(()->cache.get("fast",Duration.ofSeconds(30),()->good,()->bad));
    assertThat(fast.get(500,TimeUnit.MILLISECONDS).status()).isEqualTo("ok");
   } finally { release.countDown(); }
   assertThat(slow.get(1,TimeUnit.SECONDS).status()).isEqualTo("ok");
  }
 }
 @Test void preservesLastGoodSnapshotOnFailureAndBoundsKeys() {
  var calls=new AtomicInteger();var clock=new MutableClock();
  var cache=new SnapshotCache<List<String>>(2,clock);
  var good=new ApiModels.Envelope<>(List.of("real"),"ok","test","https://example.test",clock.instant(),null,"sample",null,null);
  var bad=new ApiModels.Envelope<List<String>>(List.of(),"unavailable","test","https://example.test",null,null,"sample","HTTP 503",null);
  var first=cache.get("a",Duration.ofSeconds(30),()->{calls.incrementAndGet();return good;},()->bad);
  cache.get("a",Duration.ofSeconds(30),()->{calls.incrementAndGet();return bad;},()->bad);
  assertThat(calls.get()).isEqualTo(1);assertThat(first.data()).containsExactly("real");
  clock.now=clock.now.plusSeconds(31);
  assertThat(cache.get("a",Duration.ofSeconds(30),()->bad,()->bad).status()).isEqualTo("stale");
  cache.get("b",Duration.ofSeconds(30),()->good,()->bad);cache.get("c",Duration.ofSeconds(30),()->good,()->bad);
  assertThat(cache.size()).isEqualTo(2);
 }
 static class MutableClock extends Clock {
  Instant now=Instant.parse("2026-10-21T00:00:00Z");
  public Instant instant(){return now;}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}
 }
}
