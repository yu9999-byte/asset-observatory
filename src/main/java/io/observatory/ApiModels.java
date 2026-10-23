package io.observatory;
import java.time.Instant;
import java.util.List;
public final class ApiModels {
 private ApiModels() {}
 public record Envelope<T>(T data,String status,String source,String sourceUrl,Instant fetchedAt,Instant sourceAt,String coverage,String reason,String nextCursor) {
  public Envelope<T> stale(String reason) {return new Envelope<>(data,"stale",source,sourceUrl,fetchedAt,sourceAt,coverage,reason,nextCursor);}
 }
 public record Market(String assetId,String instrument,String lastPrice,String open24h,String change24hPct,String volume24hBase,String turnover24hQuote,String quote,Instant sourceAt) {}
 public record MarketQueryPage(List<Market> data,String nextCursor,Integer total,String completeness,Instant snapshotAt,String source,String sourceUrl,Instant fetchedAt,Instant sourceAt,String coverage,String reason) {}
 public record Derivative(String assetId,String instrument,String openInterestContracts,String openInterestUsd,String openInterestRaw,String openInterestUnit,Instant sourceAt) {}
 public record Markets(List<Catalog.Asset> assets,Envelope<List<Market>> market,Envelope<List<Derivative>> derivatives) {}
 public record Holder(String address,String balance,String rawBalance,int decimals,boolean isContract) {}
 public record Transfer(String id,String hash,long logIndex,long blockNumber,String from,String to,String direction,String amount,String rawAmount,int decimals,Instant sourceAt) {}
 public record Liquidation(String id,String sequenceId,String instrument,String side,String eventType,String sizeRaw,String sizeUnit,String price,Instant sourceAt) {}
}
