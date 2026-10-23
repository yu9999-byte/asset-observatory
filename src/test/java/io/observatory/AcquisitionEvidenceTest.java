package io.observatory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Offline checks against captured observations, never a live provider or runtime fallback. */
class AcquisitionEvidenceTest {
 private static final Path RUN = Path.of("docs/evidence/acquisition/20261021T162534395Z-direct");
 private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

 @Test void capturedBodiesMatchRecordedHashesAndProbeDoesNotGrantApproval() throws Exception {
  var report = json.readTree(RUN.resolve("report.json").toFile());
  assertThat(report.path("localVerificationOnly").asBoolean()).isTrue();
  for (String field : List.of("productionApproved", "continuousCoverageVerified",
   "independentSourceReconciliation", "externalReleaseApproved"))
   assertThat(report.path(field).asBoolean(true)).as(field).isFalse();
  int captured = 0, failed = 0;
  for (var request : report.path("requests")) {
   assertThat(request.path("method").asText()).isEqualTo("GET");
   Instant.parse(request.path("startedAt").asText());
   Instant.parse(request.path("endedAt").asText());
   if (request.path("fetchStatus").asText().equals("captured")) {
    Path raw = RUN.resolve(request.path("rawFile").asText()).normalize();
    assertThat(raw.getParent()).isEqualTo(RUN);
    byte[] bytes = Files.readAllBytes(raw);
    assertThat(bytes.length).isEqualTo(request.path("bytes").asInt());
    assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
     .isEqualTo(request.path("bodySha256").asText());
    assertThat(request.path("schemaStatus").asText()).isEqualTo("checked");
    captured++;
   } else {
    assertThat(request.path("fetchStatus").asText()).isEqualTo("transport_error");
    assertThat(request.path("rootErrorType").asText()).isEqualTo("ConnectException");
    failed++;
   }
  }
  assertThat(captured).isEqualTo(6);
  assertThat(failed).isEqualTo(1);
 }

 @Test void realQuoteSamplePreservesMissingSupplyAndUnknownSourceTime() throws Exception {
  var raw = json.readTree(RUN.resolve("coinlore-quotes.raw").toFile());
  assertThat(raw.size()).isEqualTo(7);
  JsonNode row = null;
  for (var candidate : raw) if (candidate.path("id").asText().equals("183005")) row = candidate;
  assertThat(row).isNotNull();
  var quote = DiscoveryMapping.coinlore(Catalog.asset("JCT"), "183005", row);
  assertThat(quote.priceUsd()).isEqualTo("0.004352");
  assertThat(quote.volume24hUsd()).isEqualTo("32935038.60094091");
  assertThat(quote.marketCapUsd()).isNull();
  assertThat(quote.circulatingSupply()).isNull();
  assertThat(quote.totalSupply()).isNull();
  assertThat(quote.sourceAt()).isNull();
  assertThat(quote.issues()).containsExactly("market_cap_usd:invalid", "csupply:invalid", "tsupply:missing");
 }

 @Test void fourRealIndexedTransfersUniquelyMatchRawLogIdentityAndExactAmounts() throws Exception {
  var report = json.readTree(RUN.resolve("report.json").toFile());
  var logs = new ArrayList<DiscoveryMapping.ChainLog>();
  for (var request : report.path("requests"))
   if (request.path("id").asText().startsWith("routescan-logs"))
    for (var row : json.readTree(RUN.resolve(request.path("rawFile").asText()).toFile()).path("result"))
     logs.add(DiscoveryMapping.routescanLog(Catalog.asset("JCT"), row));
  assertThat(logs).hasSize(7);
  assertThat(logs.stream().map(DiscoveryMapping.ChainLog::id)).doesNotHaveDuplicates();
  assertThat(logs).anyMatch(log -> log.to().equals("0x" + "0".repeat(40)));
  var indexed = json.readTree(RUN.resolve("routescan-transfers.raw").toFile()).path("result");
  assertThat(indexed.size()).isEqualTo(4);
  for (var row : indexed) {
   var matches = logs.stream().filter(log -> DiscoveryMapping.matches(log, row)).toList();
   assertThat(matches).hasSize(1);
   var saved = report.path("sameProviderReconciliation");
   boolean recorded = false;
   for (var result : saved)
    if (result.path("transactionHash").asText().equals(row.path("hash").asText())) {
     assertThat(result.path("status").asText()).isEqualTo("unique_exact_match");
     assertThat(result.path("matchingEventIds").path(0).asText()).isEqualTo(matches.getFirst().id());
     recorded = true;
    }
   assertThat(recorded).isTrue();
  }
 }

 @Test void reachableReviewIsNotActivationOrProductionApprovalAndDoesNotFetch() {
  var http = mock(SourceHttp.class);
  var service = new ResearchService(http, json, false, false, false, mock(MarketQuerySnapshotStore.class));
  for (String name : List.of("CoinLore", "Routescan")) {
   Map<String, Object> source = service.sources().stream()
    .filter(item -> name.equals(item.get("name"))).findFirst().orElseThrow();
   assertThat(source.get("enabled")).isEqualTo(false);
   assertThat(source.get("productionApproved")).isEqualTo(false);
   var review = (SourceReviews.Verification) source.get("verification");
   assertThat(review.transportStatus()).isEqualTo("reachable");
   assertThat(evidenceExists(review.evidenceId())).as("evidence for %s", name).isTrue();
   assertThat(review.rightsStatus()).isNotBlank();
  }
  verifyNoInteractions(http);
 }

 private static boolean evidenceExists(String evidenceId) {
  Path root = Path.of("docs/evidence");
  return Files.isRegularFile(root.resolve(evidenceId).resolve("report.json"))
   || Files.isRegularFile(root.resolve(evidenceId + ".json"))
   || Files.isRegularFile(root.resolve(evidenceId + ".md"));
 }

 @Test void updatedTermsAndCorrectedCandidateRemainSeparateFromDataApproval() throws Exception {
  var http = mock(SourceHttp.class);
  var service = new ResearchService(http, json, false, false, false, mock(MarketQuerySnapshotStore.class));
  var coinlore = service.sources().stream().filter(row -> "CoinLore".equals(row.get("name"))).findFirst().orElseThrow();
  var review = (SourceReviews.Verification) coinlore.get("verification");
  assertThat(review.rightsStatus()).contains("书面", "再分发");
  assertThat(review.scope()).contains("16:26:07", "行情时点未知");
  assertThat(review.checkedAt()).isAfter(Instant.parse("2026-10-21T18:00:00Z"));
  assertThat(evidenceExists(review.evidenceId())).isTrue();
  var mexc = service.sources().stream().filter(row -> "MEXC candidate".equals(row.get("name"))).findFirst().orElseThrow();
  assertThat(mexc.get("enabled")).isEqualTo(false);
  assertThat(mexc.get("productionApproved")).isEqualTo(false);
  var mexcReview = (SourceReviews.Verification) mexc.get("verification");
  assertThat(mexcReview.transportStatus()).isEqualTo("unreachable");
  assertThat(mexcReview.scope()).contains("contract.mexc.com", "JCT");
  assertThat(evidenceExists(mexcReview.evidenceId())).isTrue();
  verifyNoInteractions(http);
 }

 @Test void debankAddressHistoryIsListedButRemainsDisabledPendingExternalUseRights() throws Exception {
  var http = mock(SourceHttp.class);
  var service = new ResearchService(http, json, false, false, false, mock(MarketQuerySnapshotStore.class));
  var debank = service.sources().stream().filter(row -> "DeBank Cloud".equals(row.get("name"))).findFirst().orElseThrow();
  assertThat(debank.get("enabled")).isEqualTo(false);
  assertThat(debank.get("productionApproved")).isEqualTo(false);
  assertThat(debank.get("documentation")).isEqualTo("https://docs.cloud.debank.com/en/readme/api-pro-reference/user.md");
  assertThat(debank.get("terms")).isEqualTo("https://docs.cloud.debank.com/en/terms-of-service.md");
  assertThat(debank.get("gap").toString()).contains("history_list", "14", "4(m)", "外部展示");
  assertThat(evidenceExists("debank-address-api-review-2026-10-22")).isTrue();
  verifyNoInteractions(http);
 }

 @Test void coindeskDataCandidateIsVisibleButRemainsDisabledWithUnknownApiRights() throws Exception {
  var http = mock(SourceHttp.class);
  var service = new ResearchService(http, json, false, false, false, mock(MarketQuerySnapshotStore.class));
  var source = service.sources().stream().filter(row -> "CoinDesk Data API".equals(row.get("name"))).findFirst().orElseThrow();
  assertThat(source.get("enabled")).isEqualTo(false);
  assertThat(source.get("productionApproved")).isEqualTo(false);
  assertThat(source.get("documentation")).isEqualTo("https://developers.coindesk.com/documentation");
  assertThat(source.get("terms")).isNull();
  assertThat(source.get("gap").toString()).contains("授权未知", "免费计划", "外部展示");
  var review = (SourceReviews.Verification) source.get("verification");
  assertThat(review.transportStatus()).isEqualTo("reachable");
  assertThat(review.scope()).contains("7,596", "ERR_CONNECTION_TIMED_OUT");
  assertThat(review.rightsStatus()).contains("未知");
  assertThat(review.evidenceId()).isEqualTo("coindesk-data-api-access-review-2026-10-22");
  verifyNoInteractions(http);
 }
}
