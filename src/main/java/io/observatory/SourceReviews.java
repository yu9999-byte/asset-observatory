package io.observatory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;

/** Curated technical evidence metadata; never raw provider content. */
final class SourceReviews {
 record Verification(Instant checkedAt, String transportStatus, String scope,
  String rightsStatus, String evidenceId) {}
 private static final Map<String, Verification> REVIEWS = load();
 private SourceReviews() {}

 static Verification get(String name) { return REVIEWS.get(name); }

 private static Map<String, Verification> load() {
  try (var stream = SourceReviews.class.getResourceAsStream("/source-reviews.json")) {
   if (stream == null) throw new IllegalStateException("Missing curated source review");
   Map<String, Verification> reviews = new ObjectMapper().findAndRegisterModules()
    .readValue(stream, new TypeReference<Map<String, Verification>>() {});
   for (var review : reviews.values()) {
    if (review.checkedAt() == null || !java.util.Set.of("reachable", "unreachable", "not_verified").contains(review.transportStatus())
     || review.scope() == null || review.rightsStatus() == null || review.evidenceId() == null)
     throw new IllegalStateException("Invalid source review metadata");
   }
   return Map.copyOf(reviews);
  } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read source review", e); }
 }
}
