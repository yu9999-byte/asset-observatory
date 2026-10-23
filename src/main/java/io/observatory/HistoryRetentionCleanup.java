package io.observatory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
final class HistoryRetentionCleanup {
 private static final Logger log=LoggerFactory.getLogger(HistoryRetentionCleanup.class);
 private final HistoryStore history;

 HistoryRetentionCleanup(HistoryStore history){this.history=history;}

 @Scheduled(fixedDelay=60000,initialDelay=60000)
 void purgeExpiredHolderObservations(){
  var result=history.purgeExpiredObservations(1000);
  if(result.batchesDeleted()>0)log.info("Expired holder history removed: batches={}, observations={}",result.batchesDeleted(),result.observationsDeleted());
 }
}
