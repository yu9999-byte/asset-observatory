package io.observatory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferValidationTest {
 private final ObjectMapper json = new ObjectMapper();
 private static final String OWNER = "0x0000000000000000000000000000000000000001";
 private static final String HASH = "0x" + "ab".repeat(32);
 private static final long SAFE_INTEGER = 9007199254740991L;

 private ObjectNode row() {
  var row = json.createObjectNode().put("transaction_hash", HASH).put("log_index", 12)
   .put("block_number", 100).put("timestamp", "2026-10-01T00:00:00Z");
  row.putObject("from").put("hash", OWNER);
  row.putObject("to").put("hash", "0x" + "0".repeat(40));
  row.putObject("token").put("address_hash", Catalog.asset("JCT").contract());
  row.putObject("total").put("value", "1234567890123456789").put("decimals", "18");
  return row;
 }

 private ApiModels.Transfer map(ObjectNode row) {
  return ResearchService.transfer(Catalog.asset("JCT"), OWNER, row);
 }

 @Test void rejectsMalformedHashInsteadOfGivingItAnEventIdentity() throws Exception {
  for (String value : List.of("\"0xabc\"", "\"0x" + "g".repeat(64) + "\"", "null", "true", "123")) {
   var row = row(); row.set("transaction_hash", json.readTree(value));
   assertThrows(IllegalArgumentException.class, () -> map(row), value);
  }
 }

 @Test void integerIdentitiesDoNotCoerceFractionsBooleansOrOverflow() throws Exception {
  for (String field : List.of("log_index", "block_number")) {
   for (String value : List.of("1.5", "1.0", "true", "{}", "null", "-1", "9007199254740992",
     "9223372036854775808", "\"1e2\"", "\"1.5\"", "\" 1\"", "\"+1\"", "\"01\"")) {
    var row = row(); row.set(field, json.readTree(value));
    assertThrows(IllegalArgumentException.class, () -> map(row), field + "=" + value);
   }
   var missing = row(); missing.remove(field);
   assertThrows(IllegalArgumentException.class, () -> map(missing), field);
  }
  var boundary = row().put("log_index", Long.toString(SAFE_INTEGER)).put("block_number", SAFE_INTEGER);
  assertEquals(SAFE_INTEGER, map(boundary).logIndex());
  assertEquals(SAFE_INTEGER, map(boundary).blockNumber());
 }

 @Test void decimalsMustExactlyMatchVerifiedPrecision() throws Exception {
  for (String value : List.of("18.1", "18.0", "\"18.1\"", "true", "null", "17", "4294967314")) {
   var row = row(); ((ObjectNode) row.path("total")).set("decimals", json.readTree(value));
   assertThrows(IllegalArgumentException.class, () -> map(row), value);
  }
  var integral = row(); ((ObjectNode) integral.path("total")).put("decimals", 18);
  assertEquals(18, map(integral).decimals());
 }

 @Test void preservesUint256MaximumAndValidZeroBurnOrSelfEvents() throws Exception {
  String max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE).toString();
  var row = row(); ((ObjectNode) row.path("total")).put("value", max);
  assertEquals(max, map(row).rawAmount());
  assertEquals("115792089237316195423570985008687907853269984665640564039457.584007913129639935", map(row).amount());
  ((ObjectNode) row.path("total")).put("value", "0");
  assertEquals("0", map(row).amount()); assertEquals("OUT", map(row).direction());
  ((ObjectNode) row.path("to")).put("hash", OWNER);
  assertEquals("SELF", map(row).direction());
  for (String value : List.of("\"" + BigInteger.ONE.shiftLeft(256) + "\"", "\"" + "1".repeat(79) + "\"",
    "\"-1\"", "\"1.5\"", "1", "null")) {
   var invalid = row(); ((ObjectNode) invalid.path("total")).set("value", json.readTree(value));
   assertThrows(IllegalArgumentException.class, () -> map(invalid), value);
  }
 }

 @Test void absentEventTimeStaysMissingButMalformedPresentTimeIsRejected() throws Exception {
  var missing = row(); missing.remove("timestamp"); assertNull(map(missing).sourceAt());
  assertNull(map(row().putNull("timestamp")).sourceAt());
  assertEquals(Instant.parse("2026-10-01T00:00:00Z"), map(row().put("timestamp", "2026-10-01T08:00:00+08:00")).sourceAt());
  for (String value : List.of("\"\"", "\"invalid\"", "\"2026-02-30T00:00:00Z\"", "true", "{}", "123")) {
   var invalid = row(); invalid.set("timestamp", json.readTree(value));
   assertThrows(IllegalArgumentException.class, () -> map(invalid), value);
  }
 }

 private ApiModels.Envelope<List<ApiModels.Transfer>> page(JsonNode... rows) throws Exception {
  SourceHttp http = mock(SourceHttp.class);
  var response = json.createObjectNode(); var items = response.putArray("items");
  for (var row : rows) items.add(row);
  when(http.get(anyString())).thenReturn(response);
  return new ResearchService(http, json, true, false, true, new InMemoryMarketQuerySnapshotStore())
   .transfers(OWNER, "JCT", null);
 }

 @Test void canonicalDuplicatesCollapseButConflictingEventsInvalidateTheNewPage() throws Exception {
  var alias = row().put("transaction_hash", "0x" + "AB".repeat(32));
  assertEquals(HASH, map(alias).hash());
  var duplicate = page(row(), alias, row());
  assertEquals("partial", duplicate.status()); assertEquals(1, duplicate.data().size());
  for (String field : List.of("amount", "block", "time", "to")) {
   var conflict = row();
   switch (field) {
    case "amount" -> ((ObjectNode) conflict.path("total")).put("value", "1");
    case "block" -> conflict.put("block_number", 101);
    case "time" -> conflict.put("timestamp", "2026-10-02T00:00:00Z");
    case "to" -> ((ObjectNode) conflict.path("to")).put("hash", OWNER);
   }
   var invalid = page(row(), conflict);
   assertEquals("unavailable", invalid.status(), field); assertTrue(invalid.data().isEmpty(), field);
  }
 }

 @Test void mixedValidInvalidPageCannotSilentlyDropTheInvalidRow() throws Exception {
  var invalid = row().put("transaction_hash", "0xabc");
  var page = page(row(), invalid);
  assertEquals("unavailable", page.status()); assertTrue(page.data().isEmpty()); assertNull(page.fetchedAt());
 }
}
