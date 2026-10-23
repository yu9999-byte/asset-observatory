package io.observatory;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Evidence parsers only; no provider is enabled or granted rights by this class. */
public final class DiscoveryMapping {
 private static final String TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
 private DiscoveryMapping() {}

 public record Quote(String assetId, String providerAssetId, String priceUsd,
  String change1hPct, String change24hPct, String change7dPct, String marketCapUsd,
  String volume24hUsd, String circulatingSupply, String totalSupply,
  Instant sourceAt, List<String> issues) {}

 public record ChainLog(String id, String assetId, String chain, String contract,
  String hash, long logIndex, long blockNumber, String blockHash, String from,
  String to, String amount, String rawAmount, int decimals, Instant sourceAt) {}

 public static Quote coinlore(Catalog.Asset asset, String expectedId, JsonNode row) {
  if (!expectedId.equals(text(row, "id")) || !asset.symbol().equalsIgnoreCase(text(row, "symbol"))
   || !asset.name().equalsIgnoreCase(text(row, "name")))
   throw new IllegalArgumentException("Provider directory identity mismatch");
  var issues = new ArrayList<String>();
  String price = number(row, "price_usd", issues, true);
  String h1 = number(row, "percent_change_1h", issues, false);
  String h24 = number(row, "percent_change_24h", issues, false);
  String d7 = number(row, "percent_change_7d", issues, false);
  String cap = number(row, "market_cap_usd", issues, true);
  String volume = number(row, "volume24", issues, true);
  String circulating = number(row, "csupply", issues, true);
  String total = number(row, "tsupply", issues, true);
  return new Quote(asset.id(), expectedId, price, h1, h24, d7, cap, volume,
   circulating, total, null, List.copyOf(issues));
 }

 private static String number(JsonNode row, String field, List<String> issues, boolean nonnegative) {
  JsonNode value = row.path(field);
  if (value.isMissingNode() || value.isNull()) { issues.add(field + ":missing"); return null; }
  try {
   if (!value.isNumber() && !value.isTextual()) throw new NumberFormatException();
   String input = value.asText();
   if (input.length() > 200 || !input.matches("-?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?"))
    throw new NumberFormatException();
   BigDecimal parsed = new BigDecimal(input);
   if ((nonnegative && parsed.signum() < 0) || Math.abs(parsed.scale()) > 100 || parsed.precision() > 100)
    throw new NumberFormatException();
   return parsed.stripTrailingZeros().toPlainString();
  } catch (NumberFormatException e) { issues.add(field + ":invalid"); return null; }
 }

 public static ChainLog routescanLog(Catalog.Asset asset, JsonNode row) {
  if (!"ethereum".equals(asset.chain()) || asset.contract() == null
   || !asset.contract().equalsIgnoreCase(text(row, "address")))
   throw new IllegalArgumentException("Unexpected chain or token contract");
  JsonNode topics = row.path("topics");
  if (!topics.isArray() || topics.size() != 3 || !TRANSFER.equalsIgnoreCase(topics.path(0).asText()))
   throw new IllegalArgumentException("Not a canonical ERC-20 Transfer log");
  String from = topicAddress(topics.path(1).asText());
  String to = topicAddress(topics.path(2).asText());
  String raw = new BigInteger(hexBytes(text(row, "data"), 32).substring(2), 16).toString();
  String hash = hexBytes(text(row, "transactionHash"), 32);
  String blockHash = hexBytes(text(row, "blockHash"), 32);
  long logIndex = hexLong(text(row, "logIndex"));
  long height = hexLong(text(row, "blockNumber"));
  Instant at;
  try { at = Instant.ofEpochSecond(hexLong(text(row, "timeStamp"))); }
  catch (RuntimeException e) { throw new IllegalArgumentException("Invalid event time", e); }
  return new ChainLog(asset.chain() + ":" + hash + ":" + logIndex, asset.id(), asset.chain(),
   asset.contract(), hash, logIndex, height, blockHash, from, to,
   Metrics.units(raw, asset.decimals()), raw, asset.decimals(), at);
 }

 public static boolean matches(ChainLog log, JsonNode indexed) {
  try {
   return log.hash().equalsIgnoreCase(text(indexed, "hash"))
    && log.blockHash().equalsIgnoreCase(text(indexed, "blockHash"))
    && log.contract().equalsIgnoreCase(text(indexed, "contractAddress"))
    && log.from().equalsIgnoreCase(text(indexed, "from"))
    && log.to().equalsIgnoreCase(text(indexed, "to"))
    && log.rawAmount().equals(new BigInteger(text(indexed, "value")).toString())
    && log.blockNumber() == Long.parseLong(text(indexed, "blockNumber"))
    && log.sourceAt().getEpochSecond() == Long.parseLong(text(indexed, "timeStamp"))
    && log.decimals() == Integer.parseInt(text(indexed, "tokenDecimal"));
  } catch (RuntimeException e) { return false; }
 }

 private static String topicAddress(String value) {
  String topic = hexBytes(value, 32);
  if (!topic.substring(2, 26).equals("0".repeat(24)))
   throw new IllegalArgumentException("Noncanonical address topic");
  return Catalog.address("0x" + topic.substring(26));
 }

 private static String hexBytes(String value, int size) {
  if (!value.matches("0x[0-9a-fA-F]{" + size * 2 + "}"))
   throw new IllegalArgumentException("Invalid fixed-width hex");
  return value.toLowerCase(Locale.ROOT);
 }

 private static long hexLong(String value) {
  if (!value.matches("0x[0-9a-fA-F]{1,16}")) throw new IllegalArgumentException("Invalid hex integer");
  try { return new BigInteger(value.substring(2), 16).longValueExact(); }
  catch (ArithmeticException e) { throw new IllegalArgumentException("Integer overflow", e); }
 }

 private static String text(JsonNode row, String field) {
  JsonNode value = row.path(field);
  if (value.isMissingNode() || value.isNull() || !value.isValueNode() || value.asText().isBlank())
   throw new IllegalArgumentException("Missing field: " + field);
  return value.asText();
 }
}
