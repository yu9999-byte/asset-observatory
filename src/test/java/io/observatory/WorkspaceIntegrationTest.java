package io.observatory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:mysql://127.0.0.1:3307/observatory_test?connectionTimeZone=UTC&characterEncoding=UTF-8}","spring.datasource.username=${TEST_DATABASE_USER:observatory}","spring.datasource.password=${TEST_DATABASE_PASSWORD:local-only-not-for-deployment}","app.sources.enabled=false"})
@AutoConfigureMockMvc
class WorkspaceIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate db;
 @Test void projectFieldsPersistPrivatelyAndLegacyUpdatesPreserveThem() throws Exception {
  String record="{\"projectName\":\"Janction research\",\"projectInfo\":\"User evidence, not a provider label\",\"text\":\"Verify flows\",\"visible\":true,\"version\":0}";
  mvc.perform(put("/api/notes/asset/JCT").with(user("project-alice")).with(csrf()).contentType("application/json").content(record))
   .andExpect(status().isOk()).andExpect(jsonPath("$.projectName").value("Janction research"))
   .andExpect(jsonPath("$.projectInfo").value("User evidence, not a provider label")).andExpect(jsonPath("$.version").value(1));
  mvc.perform(get("/api/workspace").with(user("project-bob"))).andExpect(jsonPath("$.notes").isEmpty());
  mvc.perform(put("/api/notes/asset/JCT").with(user("project-bob")).with(csrf()).contentType("application/json").content(record.replace("\"version\":0","\"version\":1")))
   .andExpect(status().isConflict());
  mvc.perform(put("/api/notes/asset/JCT").with(user("project-alice")).with(csrf()).contentType("application/json").content("{\"text\":\"Legacy client update\",\"visible\":false,\"version\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.projectName").value("Janction research"))
   .andExpect(jsonPath("$.projectInfo").value("User evidence, not a provider label")).andExpect(jsonPath("$.version").value(2));
  mvc.perform(get("/api/workspace?owner=project-bob").with(user("project-alice")))
   .andExpect(jsonPath("$.notes[0].text").value("Legacy client update"))
   .andExpect(jsonPath("$.notes[0].projectInfo").value("User evidence, not a provider label"));
 }
 @Test void projectFieldsClearExplicitlyAndStaleEditsCannotPartiallyApply() throws Exception {
  String record="{\"projectName\":\"Original\",\"projectInfo\":\"Evidence\",\"text\":\"Custom\",\"visible\":true,\"version\":0}";
  mvc.perform(put("/api/notes/asset/JCT").with(csrf()).contentType("application/json").content(record)).andExpect(status().isOk());
  String address="0xa68f0255c0eeb35e3cf558d58cfbbfa8811b9b84";
  mvc.perform(put("/api/notes/address/"+address).with(csrf()).contentType("application/json").content(record.replace("Original","Address record"))).andExpect(status().isOk());
  mvc.perform(put("/api/notes/asset/JCT").with(csrf()).contentType("application/json").content("{\"projectName\":\"\",\"projectInfo\":null,\"text\":\"Updated\",\"visible\":false,\"version\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.projectName").value(""))
   .andExpect(jsonPath("$.projectInfo").value("Evidence")).andExpect(jsonPath("$.version").value(2));
  mvc.perform(put("/api/notes/asset/JCT").with(csrf()).contentType("application/json").content(record.replace("Original","Stale overwrite").replace("\"version\":0","\"version\":1")))
   .andExpect(status().isConflict());
  mvc.perform(get("/api/workspace"))
   .andExpect(jsonPath("$.notes[?(@.kind=='asset')].projectName").value(org.hamcrest.Matchers.contains("")))
   .andExpect(jsonPath("$.notes[?(@.kind=='address')].projectName").value(org.hamcrest.Matchers.contains("Address record")));
 }
 @Test void legacyDatabaseInsertDefaultsNewFieldsAndPreservesOriginalBodyVersionAndTime() throws Exception {
  db.update("INSERT INTO notes(owner_id,kind,entity_key,body,visible,version,updated_at) VALUES('local-workspace','asset','LINK','Pre-upgrade research',false,7,'2026-10-21T00:00:00Z')");
  mvc.perform(get("/api/workspace")).andExpect(status().isOk())
   .andExpect(jsonPath("$.notes[0].projectName").value(""))
   .andExpect(jsonPath("$.notes[0].projectInfo").value(""))
   .andExpect(jsonPath("$.notes[0].text").value("Pre-upgrade research"))
   .andExpect(jsonPath("$.notes[0].version").value(7))
   .andExpect(jsonPath("$.notes[0].updatedAt").value("2026-10-21T00:00:00Z"));
 }
 @Test void validatesProjectFieldLimitsWithoutSavingPartialRecords() throws Exception {
  var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
  for(String field:java.util.List.of("projectName","projectInfo")) {
   int limit=field.equals("projectName")?120:4000;
   var value=mapper.createObjectNode().put("text","Custom").put("visible",true).put("version",0);
   value.put(field,"x".repeat(limit+1));
   mvc.perform(put("/api/notes/asset/JCT").with(csrf()).contentType("application/json").content(value.toString())).andExpect(status().isBadRequest());
   value.put(field,"\uD83D\uDD0E".repeat(limit/2+1));
   mvc.perform(put("/api/notes/asset/JCT").with(csrf()).contentType("application/json").content(value.toString())).andExpect(status().isBadRequest());
  }
  mvc.perform(get("/api/workspace")).andExpect(jsonPath("$.notes").isEmpty());
  var valid=mapper.createObjectNode().put("text","<script>inert</script>").put("visible",true).put("version",0)
   .put("projectName","\uD83D\uDD0E".repeat(60)).put("projectInfo","x".repeat(4000));
  mvc.perform(put("/api/notes/asset/JCT").with(csrf()).contentType("application/json").content(valid.toString()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.projectName").value("\uD83D\uDD0E".repeat(60)))
   .andExpect(jsonPath("$.projectInfo").value("x".repeat(4000)));
 }
 @Test void upgradesLegacyTableColumnsWithoutDiscardingSavedPreferencesOrWritingOnRead() throws Exception {
  String legacy="{\"hidden\":[\"oi\",\"retired\"],\"order\":[\"price\",\"asset\",\"change\",\"volume\",\"turnover\",\"oi\",\"coverage\",\"actions\",\"retired\"],\"sortKey\":\"retired\",\"desc\":true,\"pageSize\":30}";
  String owner="user:"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest("legacy-layout".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  db.update("INSERT INTO table_preferences(owner_id,table_id,config,version,updated_at) VALUES(?,?,?,7,?)",owner,"markets",legacy,"2026-10-21T00:00:00Z");
  var result=mvc.perform(get("/api/table-preferences/markets").with(user("legacy-layout")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(7))
   .andExpect(jsonPath("$.order.length()").value(11)).andExpect(jsonPath("$.order[0]").value("price"))
   .andExpect(jsonPath("$.order[10]").value("funding"))
   .andExpect(jsonPath("$.hidden.length()").value(1)).andExpect(jsonPath("$.hidden[0]").value("oi"))
   .andExpect(jsonPath("$.sortKey").isEmpty()).andExpect(jsonPath("$.pageSize").value(30)).andReturn();
  org.junit.jupiter.api.Assertions.assertEquals(7L,db.queryForObject("SELECT version FROM table_preferences WHERE owner_id=?",Long.class,owner));
  var json=new com.fasterxml.jackson.databind.ObjectMapper();
  org.junit.jupiter.api.Assertions.assertEquals(json.readTree(legacy),json.readTree(db.queryForObject("SELECT config FROM table_preferences WHERE owner_id=?",String.class,owner)));
  var payload=(com.fasterxml.jackson.databind.node.ObjectNode)new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
  payload.remove(java.util.List.of("tableId","updatedAt"));
  mvc.perform(put("/api/table-preferences/markets").with(user("legacy-layout")).with(csrf()).contentType("application/json").content(payload.toString()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(8));
 }
 @Test void persistsTablePreferencesAndRejectsStaleVersions() throws Exception {
  mvc.perform(get("/api/table-preferences/markets").with(user("preferences-alice")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.pageSize").value(15));
  String settings="{\"hidden\":[\"oi\"],\"order\":[\"price\",\"asset\",\"change\",\"volume\",\"turnover\",\"oi\",\"coverage\",\"actions\",\"marketCap\",\"fdv\",\"funding\"],\"sortKey\":\"price\",\"desc\":true,\"pageSize\":30,\"version\":0}";
  mvc.perform(put("/api/table-preferences/markets").with(user("preferences-alice")).with(csrf()).contentType("application/json").content(settings))
   .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(get("/api/table-preferences/markets?owner=preferences-alice").with(user("preferences-bob")))
   .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.hidden").isEmpty());
  mvc.perform(get("/api/table-preferences/markets").with(user("preferences-alice")))
   .andExpect(jsonPath("$.order[0]").value("price")).andExpect(jsonPath("$.hidden[0]").value("oi")).andExpect(jsonPath("$.desc").value(true)).andExpect(jsonPath("$.pageSize").value(30));
  mvc.perform(put("/api/table-preferences/markets").with(user("preferences-alice")).with(csrf()).contentType("application/json").content(settings)).andExpect(status().isConflict());
  mvc.perform(put("/api/table-preferences/markets").with(user("preferences-alice")).with(csrf()).contentType("application/json").content(settings.replace("\"version\":0","\"version\":1")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
 }
 @Test void validatesTablePreferencesAndRequiresCsrf() throws Exception {
  String settings="{\"hidden\":[],\"order\":[\"address\",\"chain\",\"balance\",\"proportion\",\"value\",\"change-1H\",\"change-24H\",\"change-3D\",\"change-7D\",\"change-1M\",\"change-1Y\",\"type\",\"actions\"],\"sortKey\":null,\"desc\":false,\"pageSize\":15,\"version\":0}";
  mvc.perform(put("/api/table-preferences/holders").contentType("application/json").content(settings)).andExpect(status().isForbidden());
  for(String invalid:java.util.List.of(
   settings.replace("\"pageSize\":15","\"pageSize\":100000"),
   settings.replace("\"hidden\":[]","\"hidden\":[\"address\",\"chain\",\"balance\",\"proportion\",\"value\",\"change-1H\",\"change-24H\",\"change-3D\",\"change-7D\",\"change-1M\",\"change-1Y\",\"type\",\"actions\"]"),
   settings.replace("\"sortKey\":null","\"sortKey\":\"unknown\""),
   settings.replace("\"balance\"","\"address\""),
   settings.replace("\"hidden\":[]","\"hidden\":[\"unknown\"]"))) {
   mvc.perform(put("/api/table-preferences/holders").with(csrf()).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
  }
  mvc.perform(get("/api/table-preferences/unknown")).andExpect(status().isBadRequest());
 }
 @BeforeEach void resetTestWorkspace() {
  db.update("DELETE FROM table_preferences");
  db.update("DELETE FROM notes");
  db.update("DELETE FROM watches");
 }
 @Test void servesFrontendBundleAsJavascriptRatherThanAssetPage() throws Exception {
  var resources=new org.springframework.core.io.support.PathMatchingResourcePatternResolver().getResources("classpath:/static/assets/*.js");
  org.junit.jupiter.api.Assertions.assertTrue(resources.length>0,"Build frontend before integration tests");
  mvc.perform(get("/assets/"+resources[0].getFilename())).andExpect(status().isOk()).andExpect(header().string("Content-Type",org.hamcrest.Matchers.containsString("javascript")));
 }
 @Test void isolatesReadsWritesDeletesAndVersionsByAuthenticatedUser() throws Exception {
  mvc.perform(put("/api/watches/asset/LINK").with(user("alice")).with(csrf())).andExpect(status().isOk());
  mvc.perform(put("/api/notes/asset/LINK").with(user("alice")).with(csrf()).contentType("application/json").content("{\"text\":\"Alice private\",\"visible\":true,\"version\":0}")).andExpect(status().isOk());
  mvc.perform(get("/api/workspace").with(user("bob"))).andExpect(jsonPath("$.watches").isEmpty()).andExpect(jsonPath("$.notes").isEmpty());
  mvc.perform(put("/api/notes/asset/LINK").with(user("bob")).with(csrf()).contentType("application/json").content("{\"text\":\"intrusion\",\"visible\":false,\"version\":1}")).andExpect(status().isConflict());
  mvc.perform(put("/api/notes/asset/LINK").with(user("bob")).with(csrf()).contentType("application/json").content("{\"text\":\"Bob private\",\"visible\":false,\"version\":0}")).andExpect(status().isOk());
  mvc.perform(delete("/api/watches/asset/LINK").with(user("bob")).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/workspace?owner=bob").with(user("alice"))).andExpect(jsonPath("$.watches[0].key").value("LINK")).andExpect(jsonPath("$.notes[0].text").value("Alice private"));
  mvc.perform(get("/api/workspace")).andExpect(jsonPath("$.watches").isEmpty()).andExpect(jsonPath("$.notes").isEmpty());
 }
 @Test void csrfRequiredAndInvalidAddressesRejected() throws Exception {
  mvc.perform(put("/api/watches/asset/BTC")).andExpect(status().isForbidden());
  mvc.perform(put("/api/watches/address/bad").with(csrf())).andExpect(status().isBadRequest());
 }
 @Test void persistsWatchAndUsesOptimisticNoteVersion() throws Exception {
  mvc.perform(put("/api/watches/asset/LINK").with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.key").value("LINK"));
  mvc.perform(put("/api/notes/asset/LINK").with(csrf()).contentType("application/json").content("{\"text\":\"research\",\"visible\":true,\"version\":0}")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(put("/api/notes/asset/LINK").with(csrf()).contentType("application/json").content("{\"text\":\"conflict\",\"visible\":false,\"version\":0}")).andExpect(status().isConflict());
  mvc.perform(get("/api/workspace")).andExpect(status().isOk()).andExpect(jsonPath("$.watches[0].key").value("LINK")).andExpect(jsonPath("$.notes[0].text").value("research"));
  mvc.perform(delete("/api/watches/asset/LINK").with(csrf())).andExpect(status().isNoContent());
 }
 @Test void unavailableDataIsEmptyWithReasonAndApi404IsNotSpa() throws Exception {
  mvc.perform(get("/api/markets")).andExpect(status().isOk()).andExpect(jsonPath("$.market.status").value("unavailable")).andExpect(jsonPath("$.market.data").isEmpty()).andExpect(jsonPath("$.market.reason").isNotEmpty());
  mvc.perform(get("/api/does-not-exist")).andExpect(status().isNotFound());
  mvc.perform(get("/api/assets/BTC/holders")).andExpect(jsonPath("$.status").value("unsupported"));
 }
 @Test void rejectsInvalidNotesAndUnknownAssets() throws Exception {
  mvc.perform(put("/api/notes/asset/UNKNOWN").with(csrf()).contentType("application/json").content("{\"text\":\"x\",\"visible\":true,\"version\":0}")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/addresses/0x123/transfers?asset=LINK")).andExpect(status().isBadRequest());
 }
}
