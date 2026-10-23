package io.observatory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryMarketQuerySnapshotStore implements MarketQuerySnapshotStore {
 private final Map<String, Snapshot> snapshots=new ConcurrentHashMap<>();
 @Override public void save(Snapshot snapshot){snapshots.put(snapshot.token(),snapshot);}
 @Override public Snapshot load(String token){return snapshots.get(token);}
 @Override public void remove(String token){snapshots.remove(token);}
 @Override public void prune(Instant now,int maxRows){
  snapshots.entrySet().removeIf(entry->entry.getValue().snapshotAt().plusSeconds(120).isBefore(now));
  while(snapshots.size()>maxRows)snapshots.keySet().stream().findFirst().ifPresent(snapshots::remove);
 }
}
