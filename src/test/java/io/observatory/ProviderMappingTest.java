package io.observatory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ProviderMappingTest {
 private final ObjectMapper mapper=new ObjectMapper();
 @Test void krakenDoesNotMislabelTodayOpenAsRolling24Hours() throws Exception {
  var row=mapper.readTree("{\"c\":[\"100\",\"1\"],\"v\":[\"2\",\"10\"],\"p\":[\"98\",\"99\"],\"o\":\"90\"}");
  var market=ResearchService.krakenMarket(Catalog.asset("BTC"),row);
  assertThat(market.lastPrice()).isEqualTo("100");
  assertThat(market.open24h()).isNull();assertThat(market.change24hPct()).isNull();
  assertThat(market.turnover24hQuote()).isEqualTo("990");
  assertThat(market.sourceAt()).isNull();
 }
 @Test void mapsTokenTransferWithExactQuantityAndLogIdentity() throws Exception {
  String address="0x0000000000000000000000000000000000000001";
  String hash="0x"+"a".repeat(64);
  var row=mapper.readTree("{\"from\":{\"hash\":\""+address+"\"},\"to\":{\"hash\":\"0x0000000000000000000000000000000000000002\"},\"transaction_hash\":\""+hash+"\",\"log_index\":12,\"block_number\":100,\"timestamp\":\"2026-10-01T00:00:00Z\",\"token\":{\"address_hash\":\"0x514910771af9ca656af840dff83e8264ecf986ca\"},\"total\":{\"value\":\"1234567890123456789\",\"decimals\":\"18\"}}");
  var event=ResearchService.transfer(Catalog.asset("LINK"),address,row);
  assertThat(event.amount()).isEqualTo("1.234567890123456789");
  assertThat(event.id()).isEqualTo("ethereum:"+hash+":12");assertThat(event.direction()).isEqualTo("OUT");
 }
 @Test void verifiedJctIdentityDoesNotInventVenueMappingsOrEnableChainAccess() throws Exception {
  var asset=Catalog.asset("JCT");
  assertThat(asset.name()).isEqualTo("Janction");
  assertThat(asset.chain()).isEqualTo("ethereum");
  assertThat(asset.contract()).isEqualTo("0xc477b6dfd26ec2460b3b92de18837fd476ea7549");
  assertThat(asset.decimals()).isEqualTo(18);
  assertThat(asset.spotInstrument()).isNull();assertThat(asset.swapInstrument()).isNull();
  SourceHttp http=mock(SourceHttp.class);
  when(http.get(anyString())).thenAnswer(call->mapper.readTree(call.getArgument(0,String.class).contains("/0/public/")
   ?"{\"error\":[],\"result\":{\"XXBTZUSD\":{\"c\":[\"100\"],\"v\":[\"1\",\"2\"],\"p\":[\"100\",\"100\"]},\"JCTUSD\":{\"c\":[\"999\"]}}}"
   :"{\"result\":\"success\",\"tickers\":[{\"symbol\":\"PF_JCTUSD\",\"openInterest\":\"999\"},{\"symbol\":\"PF_XBTUSD\",\"openInterest\":\"1\"}]}"));
  var service=new ResearchService(http,mapper,true,true,false,new InMemoryMarketQuerySnapshotStore());
  var markets=service.markets();
  assertThat(markets.assets()).contains(asset);
  assertThat(markets.market().data()).noneMatch(row->row.assetId().equals("JCT"));
  assertThat(markets.derivatives().data()).noneMatch(row->row.assetId().equals("JCT"));
  var holders=service.holders("JCT",null);
  assertThat(holders.status()).isEqualTo("unavailable");
  assertThat(holders.sourceUrl()).contains(asset.contract());
  assertThat(service.liquidations("JCT").status()).isEqualTo("unsupported");
  verify(http,times(2)).get(anyString());
 }
 @Test void configuredMarketDirectoryHasGlobalStablePagesAndKeepsMissingValuesLast() {
  var service=new ResearchService(mock(SourceHttp.class),mapper,false,false,false,new InMemoryMarketQuerySnapshotStore());
  var first=service.marketQuery(null,"lastPrice","desc",3,null);
  assertThat(first.completeness()).isEqualTo("partial");
  assertThat(first.total()).isEqualTo(7);
  assertThat(first.data()).hasSize(3);
  assertThat(first.data()).allMatch(row->row.lastPrice()==null);
  assertThat(first.nextCursor()).isNotBlank();
  var second=service.marketQuery(null,"lastPrice","desc",3,first.nextCursor());
  assertThat(second.data()).hasSize(3);
  assertThat(second.data()).noneMatch(row->first.data().stream().anyMatch(previous->previous.assetId().equals(row.assetId())));
  var third=service.marketQuery(null,"lastPrice","desc",3,second.nextCursor());
  assertThat(third.data()).hasSize(1);
  assertThat(third.nextCursor()).isNull();
  assertThrows(IllegalArgumentException.class,()->service.marketQuery("JCT","lastPrice","desc",3,first.nextCursor()));
  assertThrows(IllegalArgumentException.class,()->service.marketQuery(null,"notAColumn","asc",3,null));
  assertThrows(IllegalArgumentException.class,()->service.marketQuery(null,"assetId","asc",101,null));
 }
}
