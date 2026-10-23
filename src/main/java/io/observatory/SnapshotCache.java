package io.observatory;
import java.time.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import io.observatory.ApiModels.Envelope;

/** Bounded single-flight cache. Expiry does not erase the last successful snapshot. */
public final class SnapshotCache<T> {
 private record Entry<T>(Envelope<T> value,Instant retryAt) {}
 private final ConcurrentHashMap<String,Entry<T>> entries=new ConcurrentHashMap<>();
 private final ConcurrentHashMap<String,CompletableFuture<Envelope<T>>> inFlight=new ConcurrentHashMap<>();
 private final int capacity;
 private final Clock clock;
 private final Duration followerWait;
 public SnapshotCache(int capacity,Clock clock){this(capacity,clock,Duration.ofSeconds(10));}
 SnapshotCache(int capacity,Clock clock,Duration followerWait){
  if(capacity<1||followerWait.isNegative()||followerWait.isZero())throw new IllegalArgumentException();
  this.capacity=capacity;this.clock=clock;this.followerWait=followerWait;
 }
 public Envelope<T> get(String key,Duration ttl,Supplier<Envelope<T>> loader,Supplier<Envelope<T>> empty) {
  Instant now=clock.instant();var old=entries.get(key);
  if(old!=null&&now.isBefore(old.retryAt()))return age(old.value(),now,ttl);
  CompletableFuture<Envelope<T>> flight;boolean leader=false;
  synchronized(this){
   old=entries.get(key);now=clock.instant();
   if(old!=null&&now.isBefore(old.retryAt()))return age(old.value(),now,ttl);
   flight=inFlight.get(key);
   if(flight==null){
    if(inFlight.size()>=capacity)return fallback(old,empty.get(),"缓存请求已满，请稍后重试");
    flight=new CompletableFuture<>();inFlight.put(key,flight);leader=true;
   }
  }
  if(!leader){
   try{return age(flight.get(followerWait.toNanos(),TimeUnit.NANOSECONDS),clock.instant(),ttl);}
   catch(InterruptedException e){Thread.currentThread().interrupt();return fallback(old,empty.get(),"等待来源请求已中断");}
   catch(ExecutionException|TimeoutException e){return fallback(old,empty.get(),"等待来源请求超时，请稍后重试");}
  }
  try{
   Envelope<T> value;
   try{value=loader.get();}catch(Exception e){value=empty.get();}
   if(value.status().equals("unavailable"))value=fallback(old,value,value.reason());
   synchronized(this){
    if(entries.size()>=capacity&&!entries.containsKey(key))entries.keySet().stream().findFirst().ifPresent(entries::remove);
    entries.put(key,new Entry<>(value,clock.instant().plus(ttl)));
   }
   var result=age(value,clock.instant(),ttl);flight.complete(result);return result;
  }catch(Error|RuntimeException e){flight.completeExceptionally(e);throw e;}
  finally{inFlight.remove(key,flight);}
 }
 private Envelope<T> fallback(Entry<T> old,Envelope<T> empty,String reason){
  if(old!=null&&old.value().fetchedAt()!=null)return old.value().stale(reason);
  return new Envelope<>(empty.data(),empty.status(),empty.source(),empty.sourceUrl(),empty.fetchedAt(),empty.sourceAt(),empty.coverage(),reason,empty.nextCursor());
 }
 private Envelope<T> age(Envelope<T> value,Instant now,Duration ttl){
  if(value.fetchedAt()!=null&&Duration.between(value.fetchedAt(),now).compareTo(ttl.multipliedBy(3))>0)return value.stale(value.reason()==null?"超过数据新鲜度目标":value.reason());
  return value;
 }
 public int size(){return entries.size();}
}
