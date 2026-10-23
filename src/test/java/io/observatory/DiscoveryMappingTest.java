package io.observatory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import static org.assertj.core.api.Assertions.*;

class DiscoveryMappingTest {
 private final ObjectMapper mapper = new ObjectMapper();
 private static final String FROM = "0xa68f0255c0eeb35e3cf558d58cfbbfa8811b9b84";
 private static final String TO = "0x58e53cad56180743ace00349ef3e1bfe4eff5732";
 private static final String HASH = "0x" + "ab".repeat(32);
 private static final String BLOCK = "0x" + "cd".repeat(32);

 @Test void invalidQuoteFieldsAreMissingIndividuallyAndNeverBecomeZero() throws Exception {
  var row = mapper.readTree("""
   {"id":"183005","symbol":"JCT","name":"Janction","price_usd":"0.004220",
    "percent_change_24h":"-8.08","volume24":"32050202.094882596",
    "market_cap_usd":"0?","csupply":"0?","tsupply":null}
   """);
  var result = DiscoveryMapping.coinlore(Catalog.asset("JCT"), "183005", row);
  assertThat(result.priceUsd()).isEqualTo("0.00422");
  assertThat(result.change24hPct()).isEqualTo("-8.08");
  assertThat(result.volume24hUsd()).isEqualTo("32050202.094882596");
  assertThat(result.marketCapUsd()).isNull();
  assertThat(result.circulatingSupply()).isNull();
  assertThat(result.sourceAt()).isNull();
  assertThat(result.issues()).anyMatch(s -> s.startsWith("market_cap_usd:invalid"));
 }

 @Test void genuineZeroIsPreservedAndNonFiniteNegativeAmountsAreRejected() throws Exception {
  var row = mapper.readTree("""
   {"id":"183005","symbol":"JCT","name":"Janction","price_usd":"NaN",
    "percent_change_24h":"0","volume24":"-1","market_cap_usd":"0","csupply":"0"}
   """);
  var result = DiscoveryMapping.coinlore(Catalog.asset("JCT"), "183005", row);
  assertThat(result.priceUsd()).isNull();
  assertThat(result.volume24hUsd()).isNull();
  assertThat(result.change24hPct()).isEqualTo("0");
  assertThat(result.marketCapUsd()).isEqualTo("0");
  assertThat(result.circulatingSupply()).isEqualTo("0");
 }

 @Test void quoteMappingRejectsTickerOnlyOrWrongAssetIdentity() throws Exception {
  var wrong = mapper.readTree("{\"id\":\"183005\",\"symbol\":\"JCT\",\"name\":\"Another Token\"}");
  assertThatThrownBy(() -> DiscoveryMapping.coinlore(Catalog.asset("JCT"), "183005", wrong))
   .isInstanceOf(IllegalArgumentException.class);
  var right = mapper.readTree("{\"id\":\"183005\",\"symbol\":\"JCT\",\"name\":\"Janction\"}");
  assertThatThrownBy(() -> DiscoveryMapping.coinlore(Catalog.asset("JCT"), "999", right))
   .isInstanceOf(IllegalArgumentException.class);
 }

 @Test void uint256AndDistinctLogsInOneTransactionRetainExactIdentity() throws Exception {
  String max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE).toString();
  var first = DiscoveryMapping.routescanLog(Catalog.asset("JCT"), log("19b", max));
  var second = DiscoveryMapping.routescanLog(Catalog.asset("JCT"), log("19c", max));
  assertThat(first.rawAmount()).isEqualTo(max);
  assertThat(first.amount()).isEqualTo(Metrics.units(max, 18));
  assertThat(first.id()).isEqualTo("ethereum:" + HASH + ":411");
  assertThat(second.id()).isNotEqualTo(first.id());
  assertThat(first.blockHash()).isEqualTo(BLOCK);
  assertThat(first.sourceAt().toString()).isEqualTo("2026-05-16T13:25:11Z");
 }

 @Test void malformedOrForeignLogsCannotBecomeEvents() throws Exception {
  for (String field : new String[]{"logIndex", "blockHash", "transactionHash", "timeStamp"}) {
   var node = log("19b", "1"); node.remove(field);
   assertThatThrownBy(() -> DiscoveryMapping.routescanLog(Catalog.asset("JCT"), node))
    .isInstanceOf(IllegalArgumentException.class);
  }
  var foreign = log("19b", "1"); foreign.put("address", Catalog.asset("LINK").contract());
  assertThatThrownBy(() -> DiscoveryMapping.routescanLog(Catalog.asset("JCT"), foreign))
   .isInstanceOf(IllegalArgumentException.class);
  var nft = log("19b", "1"); nft.withArray("topics").add("0x" + "00".repeat(32));
  assertThatThrownBy(() -> DiscoveryMapping.routescanLog(Catalog.asset("JCT"), nft))
   .isInstanceOf(IllegalArgumentException.class);
 }

 @Test void nonCanonicalAddressTopicsAndOversizeValuesAreRejected() throws Exception {
  var dirty = log("19b", "1"); dirty.withArray("topics").set(1, mapper.getNodeFactory().textNode("0x" + "ff".repeat(12) + FROM.substring(2)));
  assertThatThrownBy(() -> DiscoveryMapping.routescanLog(Catalog.asset("JCT"), dirty))
   .isInstanceOf(IllegalArgumentException.class);
  var oversized = log("19b", "1"); oversized.put("data", "0x" + "ff".repeat(33));
  assertThatThrownBy(() -> DiscoveryMapping.routescanLog(Catalog.asset("JCT"), oversized))
   .isInstanceOf(IllegalArgumentException.class);
 }

 @Test void reconciliationChecksTimeBlockAndExactAmountWithoutInventingLogIndex() throws Exception {
  var event = DiscoveryMapping.routescanLog(Catalog.asset("JCT"), log("19b", "1234567890123456789"));
  var indexed = mapper.createObjectNode();
  indexed.put("hash", HASH).put("blockHash", BLOCK).put("blockNumber", "25108012")
   .put("timeStamp", "1778937911").put("from", FROM).put("to", TO)
   .put("contractAddress", Catalog.asset("JCT").contract()).put("value", "1234567890123456789")
   .put("tokenDecimal", "18").put("transactionIndex", "80");
  assertThat(DiscoveryMapping.matches(event, indexed)).isTrue();
  indexed.put("value", "1234567890123456788");
  assertThat(DiscoveryMapping.matches(event, indexed)).isFalse();
  indexed.put("value", "1234567890123456789").put("blockHash", "0x" + "ef".repeat(32));
  assertThat(DiscoveryMapping.matches(event, indexed)).isFalse();
 }

 private com.fasterxml.jackson.databind.node.ObjectNode log(String index, String raw) {
  var node = mapper.createObjectNode();
  node.put("address", Catalog.asset("JCT").contract()).put("transactionHash", HASH)
   .put("blockHash", BLOCK).put("blockNumber", "0x17f1e2c").put("logIndex", "0x" + index)
   .put("timeStamp", "0x6a087037").put("data", "0x" + String.format("%064x", new BigInteger(raw)));
  node.putArray("topics").add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef")
   .add("0x" + "00".repeat(12) + FROM.substring(2)).add("0x" + "00".repeat(12) + TO.substring(2));
  return node;
 }
}
