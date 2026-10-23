package io.observatory;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in development investigation. Passing this test does not approve sources. */
@EnabledIfSystemProperty(named = "probe.live", matches = "true")
class AcquisitionProbeLiveTest {
 private static final String JCT = "0xc477b6dfd26ec2460b3b92de18837fd476ea7549";
 private static final String ADDRESS = "0xa68f0255c0eeb35e3cf558d58cfbbfa8811b9b84";
 private static final String ROUTE = "https://api.routescan.io/v2/network/mainnet/evm/1/etherscan/api?";
 private static final Set<String> HOSTS = Set.of("api.coinlore.net", "api.routescan.io",
  "eth.blockscout.com", "api.ethplorer.io", "api.bitget.com", "contract.mexc.com",
  "api.gateio.ws", "api.dexscreener.com", "api.kraken.com", "futures.kraken.com");
 private record Target(String id, String url, boolean core) {}
 private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
  .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

 private static List<Target> targets() {
  return List.of(
   new Target("coinlore-quotes", "https://api.coinlore.net/api/ticker/?id=90,80,2751,47305,33285,33422,183005", true),
   new Target("coinlore-jct-venues", "https://api.coinlore.net/api/coin/markets/?id=183005", false),
   new Target("routescan-transfers", ROUTE + "module=account&action=tokentx&address=" + ADDRESS + "&contractaddress=" + JCT + "&page=1&offset=5&sort=desc", true),
   new Target("routescan-logs", ROUTE + "module=logs&action=getLogs&fromBlock=25108012&toBlock=25108012&address=" + JCT + "&page=1&offset=100", true),
   new Target("blockscout-token", "https://eth.blockscout.com/api/v2/tokens/" + JCT, false),
   new Target("blockscout-holders", "https://eth.blockscout.com/api/v2/tokens/" + JCT + "/holders", true),
   new Target("ethplorer-token", "https://api.ethplorer.io/getTokenInfo/" + JCT + "?apiKey=freekey", false),
   new Target("ethplorer-holders", "https://api.ethplorer.io/getTopTokenHolders/" + JCT + "?apiKey=freekey&limit=30", false),
   new Target("bitget-coins", "https://api.bitget.com/api/v3/market/coins?coin=JCT", false),
   new Target("bitget-instruments", "https://api.bitget.com/api/v3/market/instruments?category=USDT-FUTURES&symbol=JCTUSDT", false),
   new Target("mexc-contracts", "https://contract.mexc.com/api/v1/contract/detail", false),
   new Target("gate-contracts", "https://api.gateio.ws/api/v4/futures/usdt/contracts", false),
   new Target("dex-ethereum", "https://api.dexscreener.com/token-pairs/v1/ethereum/" + JCT, false),
   new Target("dex-bnb", "https://api.dexscreener.com/token-pairs/v1/bsc/0xea37a8de1de2d9d10772eeb569e28bfa5cb17707", false),
   new Target("kraken-spot", "https://api.kraken.com/0/public/Ticker?pair=XBTUSD,ETHUSD,LINKUSD,UNIUSD,USDCUSD,WBTCUSD", false),
   new Target("kraken-futures", "https://futures.kraken.com/derivatives/api/v3/tickers", false));
 }

 @Test void captureEvidenceWithoutEnablingSources() throws Exception {
  Path output = Path.of(System.getProperty("probe.output", "target/acquisition-probe"));
  Files.createDirectories(output);
  Path reportFile = output.resolve("report.json");
  assertFalse(Files.exists(reportFile), "Use a new output directory; do not overwrite prior evidence");
  String proxy = System.getProperty("probe.proxy", "");
  var client = SourceHttp.client(proxy);
  ObjectNode report = mapper.createObjectNode();
  report.put("startedAt", Instant.now().toString()).put("javaVersion", System.getProperty("java.version"))
   .put("os", System.getProperty("os.name")).put("transport", proxy.isBlank() ? "direct" : "explicit-existing-http-proxy")
   .put("scope", System.getProperty("probe.scope", "core")).put("localVerificationOnly", true)
   .put("productionApproved", false).put("perRequestBudgetSeconds", 10)
   .put("rightsStatus", "unresolved; no external display/storage approval inferred")
   .put("historicalProbeBlock", 25108012).put("sampleAddress", ADDRESS);
  ArrayNode records = report.putArray("requests");
  Map<String, JsonNode> captured = new HashMap<>();
  var queue = new ArrayList<>(targets());
  String selected = System.getProperty("probe.targets", "");
  Set<String> selectedIds = selected.isBlank() ? Set.of() : Set.of(selected.split(","));
  assertTrue(selectedIds.stream().allMatch(id -> queue.stream().anyMatch(target -> target.id().equals(id))),
   "Unknown probe target; choose a documented target ID");
  report.set("selectedTargetIds", mapper.valueToTree(new TreeSet<>(selectedIds)));
  Set<Long> logBlocks = new HashSet<>(Set.of(25108012L));
  for (int targetIndex = 0; targetIndex < queue.size(); targetIndex++) {
   Target target = queue.get(targetIndex);
   if (!selectedIds.isEmpty() ? !selectedIds.contains(target.id())
    : !"all".equals(System.getProperty("probe.scope", "core")) && !target.core()) continue;
   URI uri = URI.create(target.url());
   assertTrue(HOSTS.contains(uri.getHost())); assertEquals("https", uri.getScheme());
   long start = System.nanoTime();
   ObjectNode record = records.addObject();
   record.put("id", target.id()).put("url", target.url()).put("startedAt", Instant.now().toString())
    .put("method", "GET").put("permissionStatus", "not-approved-for-production");
   var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
    .header("Accept", "application/json").header("User-Agent", "AssetObservatory/0.1 read-only-feasibility")
    .GET().build();
   var pending = client.sendAsync(request, info -> new SourceHttp.LimitedBody(12_000_000));
   try {
    var response = pending.get(10, TimeUnit.SECONDS);
    byte[] raw = response.body();
    record.put("httpStatus", response.statusCode()).put("bytes", raw.length)
     .put("bodySha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)));
    String filename = target.id() + ".raw";
    Files.write(output.resolve(filename), raw, StandardOpenOption.CREATE_NEW);
    record.put("rawFile", filename);
    response.headers().firstValue("content-type").ifPresent(v -> record.put("contentType", v));
    if (response.statusCode() != 200) record.put("fetchStatus", "http_error");
    else {
     record.put("fetchStatus", "captured");
     try {
      JsonNode root = mapper.readTree(raw);
      if (root == null) throw new IllegalArgumentException("Empty response body");
      record.set("schema", summarize(target.id(), root));
      record.put("schemaStatus", "checked");
      captured.put(target.id(), root);
      if (target.id().equals("routescan-transfers")) {
       for (JsonNode row : root.path("result")) {
        long height = Long.parseLong(row.path("blockNumber").asText());
        if (height < 0) throw new IllegalArgumentException("Invalid indexed block number");
        if (logBlocks.size() < 5 && logBlocks.add(height))
         queue.add(new Target("routescan-logs-" + height, ROUTE + "module=logs&action=getLogs&fromBlock="
          + height + "&toBlock=" + height + "&address=" + JCT + "&page=1&offset=100", true));
       }
      }
     } catch (Exception e) {
      record.put("schemaStatus", "invalid").put("schemaError", e.getClass().getSimpleName() + ": " + e.getMessage());
     }
    }
   } catch (Exception e) {
    record.put("fetchStatus", "transport_error").put("errorType", e.getClass().getSimpleName());
    Throwable root = e; while (root.getCause() != null) root = root.getCause();
    record.put("rootErrorType", root.getClass().getSimpleName());
   } finally {
    if (!pending.isDone()) pending.cancel(true);
    record.put("endedAt", Instant.now().toString()).put("elapsedMs", (System.nanoTime() - start) / 1_000_000);
    mapper.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), report);
    System.out.println("PROBE " + target.id() + " " + record.path("fetchStatus").asText()
     + " HTTP=" + record.path("httpStatus").asText("n/a") + " ms=" + record.path("elapsedMs").asText());
   }
   Thread.sleep(1200);
  }
  ArrayNode reconciliation = report.putArray("sameProviderReconciliation");
  if (captured.containsKey("routescan-transfers") && captured.containsKey("routescan-logs")) {
   var logs = new ArrayList<DiscoveryMapping.ChainLog>();
   for (var capture : captured.entrySet()) if (capture.getKey().startsWith("routescan-logs"))
    for (JsonNode row : capture.getValue().path("result"))
     logs.add(DiscoveryMapping.routescanLog(Catalog.asset("JCT"), row));
   for (JsonNode row : captured.get("routescan-transfers").path("result")) {
    List<String> matches = logs.stream().filter(log -> DiscoveryMapping.matches(log, row)).map(DiscoveryMapping.ChainLog::id).toList();
    reconciliation.addObject().put("transactionHash", row.path("hash").asText())
     .put("status", matches.size() == 1 ? "unique_exact_match" : matches.isEmpty()
      ? logs.stream().anyMatch(log -> log.hash().equalsIgnoreCase(row.path("hash").asText()))
       ? "fields_mismatch" : "outside_captured_logs" : "ambiguous")
     .set("matchingEventIds", mapper.valueToTree(matches));
   }
  }
  report.put("endedAt", Instant.now().toString()).put("continuousCoverageVerified", false)
   .put("independentSourceReconciliation", false).put("externalReleaseApproved", false);
  mapper.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), report);
  assertTrue(records.size() > 0);
  System.out.println("EVIDENCE " + reportFile.toAbsolutePath());
 }

 private JsonNode summarize(String id, JsonNode root) {
  ObjectNode out = mapper.createObjectNode();
  if (id.equals("coinlore-quotes")) {
   if (!root.isArray()) throw new IllegalArgumentException("Expected quote array");
   var ids = Map.of("90", "BTC", "80", "ETH", "2751", "LINK", "47305", "UNI",
    "33285", "USDC", "33422", "WBTC", "183005", "JCT");
   ArrayNode rows = out.putArray("quotes");
   for (JsonNode row : root) {
    String providerId = row.path("id").asText();
    if (!ids.containsKey(providerId)) throw new IllegalArgumentException("Unexpected provider asset ID");
    rows.add(mapper.valueToTree(DiscoveryMapping.coinlore(Catalog.asset(ids.get(providerId)), providerId, row)));
   }
   return out.put("count", rows.size()).put("providerContractMappingVerified", false);
  }
  if (id.startsWith("routescan-")) {
   if (!root.path("status").asText().equals("1") || !root.path("result").isArray())
    throw new IllegalArgumentException("Routescan provider error/empty: " + root.path("message").asText());
   out.put("count", root.path("result").size()).put("scope", "bounded historical sample; not continuous/full history");
   if (id.startsWith("routescan-logs")) {
    ArrayNode logs = out.putArray("verifiedLogs");
    for (JsonNode row : root.path("result")) logs.add(mapper.valueToTree(DiscoveryMapping.routescanLog(Catalog.asset("JCT"), row)));
   }
   return out;
  }
  if (id.equals("blockscout-holders") || id.equals("ethplorer-holders")) {
   String key = id.startsWith("blockscout") ? "items" : "holders";
   if (!root.path(key).isArray()) throw new IllegalArgumentException("Missing holder array/provider error");
   out.put("count", root.path(key).size()).put("fixedBlockSnapshotVerified", false);
   out.set("nextPageParams", root.path("next_page_params"));
   return out;
  }
  if (id.equals("blockscout-token") || id.equals("ethplorer-token")) {
   if (!JCT.equalsIgnoreCase(root.path("address_hash").asText(root.path("address").asText()))
    || !root.path("decimals").asText().equals("18")) throw new IllegalArgumentException("Token identity/decimals mismatch");
   out.put("contract", JCT).put("decimals", 18).put("snapshotTimeVerified", false);
   out.set("supplyRaw", root.path(id.startsWith("blockscout") ? "total_supply" : "totalSupply"));
   return out;
  }
  if (id.startsWith("bitget-") && !root.path("code").asText().equals("00000"))
   throw new IllegalArgumentException("Bitget provider error: " + root.path("code").asText());
  if (id.equals("mexc-contracts") && !root.path("success").asBoolean())
   throw new IllegalArgumentException("MEXC provider error");
  if (id.equals("kraken-spot") && root.path("error").size() > 0) throw new IllegalArgumentException("Kraken provider error");
  if (id.equals("kraken-futures") && !root.path("result").asText().equals("success")) throw new IllegalArgumentException("Kraken provider error");
  JsonNode rows = root.isArray() ? root : root.path(id.equals("kraken-futures") ? "tickers" : "data");
  if (rows.isArray()) {
   ArrayNode matches = out.putArray("jctCandidates");
   for (JsonNode row : rows) if (row.path("symbol").asText().contains("JCT") || row.path("name").asText().equals("JCT_USDT")
    || row.path("coin").asText().equals("JCT")) matches.add(row);
   out.put("count", rows.size()).put("assetContractMappingVerified", false);
  } else out.put("objectKeysObserved", root.size());
  return out.put("unitsAndRightsVerified", false);
 }
}
