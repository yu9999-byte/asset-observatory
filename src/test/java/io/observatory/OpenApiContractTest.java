package io.observatory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SchemaValidatorsConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:mysql://127.0.0.1:3307/observatory_test?connectionTimeZone=UTC&characterEncoding=UTF-8}","spring.datasource.username=${TEST_DATABASE_USER:observatory}","spring.datasource.password=${TEST_DATABASE_PASSWORD:local-only-not-for-deployment}","app.sources.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class OpenApiContractTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
 private JsonNode contract;
 private static final String ADDRESS="0x000000000000000000000000000000000000dead";

 @BeforeEach void readContract() throws Exception {
  Path file=Path.of("docs/openapi.json");
  assertTrue(Files.isRegularFile(file),"The machine-readable contract must exist before acceptance");
  contract=json.readTree(file.toFile());
 }

 private JsonSchema schema(JsonNode target) {
  ObjectNode root=json.createObjectNode();
  root.put("$schema","https://json-schema.org/draft/2020-12/schema");
  root.set("components",contract.get("components"));
  root.setAll((ObjectNode)target);
  return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(root,
   SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build());
 }
 private JsonSchema named(String name) {
  return schema(json.createObjectNode().put("$ref","#/components/schemas/"+name));
 }
 private JsonNode resolve(JsonNode node) {
  return node.has("$ref")?contract.at(node.get("$ref").asText().substring(1)):node;
 }

 @Test void conformsToOfficialOpenApiStructureAndJsonSchemaMetaSchema() throws Exception {
  var factory=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  try(var official=getClass().getResourceAsStream("/contracts/openapi-3.1-2022-10-07.json");
      var meta=JsonSchemaFactory.class.getResourceAsStream("/draft/2020-12/schema")) {
   assertNotNull(official);assertNotNull(meta);
   var documentSchema=factory.getSchema(official);
   assertEquals(Set.of(),documentSchema.validate(contract),"OpenAPI structure must be valid");
   ObjectNode invalid=contract.deepCopy();invalid.remove("info");
   assertFalse(documentSchema.validate(invalid).isEmpty(),"Validator must reject an invalid document");
   var schemaMeta=factory.getSchema(meta);
   contract.path("components").path("schemas").fields().forEachRemaining(entry->{
    assertEquals(Set.of(),schemaMeta.validate(entry.getValue()),entry.getKey());
    named(entry.getKey());
   });
  }
 }
 private JsonNode check(MockHttpServletRequestBuilder request,String path,String method,int expected) throws Exception {
  var response=mvc.perform(request.accept("application/json").with(user("openapi-contract"))).andReturn().getResponse();
  assertEquals(expected,response.getStatus(),response.getContentAsString());
  JsonNode declaration=resolve(contract.path("paths").path(path).path(method).path("responses").path(Integer.toString(expected)));
  assertFalse(declaration.isMissingNode(),"Undocumented response: "+method+" "+path+" "+expected);
  if(expected==204) {
   assertTrue(response.getContentAsString().isEmpty());
   assertFalse(declaration.has("content"));
   return null;
  }
  assertTrue(response.getContentType().startsWith("application/json"));
  var body=json.readTree(response.getContentAsByteArray());
  var target=declaration.path("content").path("application/json").path("schema");
  assertFalse(target.isMissingNode(),"JSON response schema required");
  assertEquals(Set.of(),schema(target).validate(body),method+" "+path);
  return body;
 }

 @Test void includesEveryRegisteredBusinessOperationAndSecurityMode() {
  assertEquals("3.1.0",contract.path("openapi").asText());
  Set<String> implemented=new HashSet<>(),documented=new HashSet<>(),ids=new HashSet<>();
  mappings.getHandlerMethods().forEach((info,handler)->{
   if(handler.getBeanType()==ApiController.class||handler.getBeanType()==TablePreferenceController.class)
    for(String path:info.getPatternValues())for(var method:info.getMethodsCondition().getMethods())
     implemented.add(method.name().toLowerCase()+" "+path);
  });
  contract.path("paths").fields().forEachRemaining(path->{
   path.getValue().fields().forEachRemaining(operation->{
    if(!List.of("get","put","delete","post","patch").contains(operation.getKey()))return;
    if(path.getKey().startsWith("/api/"))documented.add(operation.getKey()+" "+path.getKey());
    assertTrue(ids.add(operation.getValue().path("operationId").asText()),"Unique operation IDs required");
    if(List.of("put","delete").contains(operation.getKey())) {
     var security=operation.getValue().path("security");
     if(path.getKey().startsWith("/api/admin/")) {
      assertEquals(1,security.size(),"Admin writes always require an authenticated administrator");
      assertTrue(security.get(0).has("basicAuth")&&security.get(0).has("csrfToken")&&security.get(0).has("sessionCookie"));
     } else {
      assertEquals(2,security.size(),"Local and external modes must require CSRF");
      assertTrue(security.get(0).has("csrfToken")&&security.get(0).has("sessionCookie"));
      assertTrue(security.get(1).has("basicAuth")&&security.get(1).has("csrfToken")&&security.get(1).has("sessionCookie"));
     }
    }
   });
  });
  assertEquals(implemented,documented,"OpenAPI must track the actual controller inventory");
  assertTrue(contract.path("security").get(0).isEmpty(),"Loopback mode permits anonymous reads");
  assertTrue(contract.path("security").get(1).has("basicAuth"));
 }

 @Test void validatesActualReadsAndHonestMissingStates() throws Exception {
  check(get("/api/csrf"),"/api/csrf","get",200);
  check(get("/api/markets"),"/api/markets","get",200);
  var directory=check(get("/api/markets/query?sort=lastPrice&direction=desc&limit=3"),"/api/markets/query","get",200);
  assertEquals(7,directory.path("total").asInt());assertEquals("partial",directory.path("completeness").asText());assertEquals(3,directory.path("data").size());
  assertTrue(directory.path("nextCursor").isTextual());assertTrue(directory.path("coverage").asText().contains("not reconciled to the market universe"));
  var second=check(get("/api/markets/query?sort=lastPrice&direction=desc&limit=3&cursor="+directory.path("nextCursor").asText()),"/api/markets/query","get",200);
  for(var previous:directory.path("data"))for(var current:second.path("data"))assertNotEquals(previous.path("assetId").asText(),current.path("assetId").asText());
  check(get("/api/markets/query?sort=unknown"),"/api/markets/query","get",400);
  check(get("/api/markets/query?q=JCT&sort=lastPrice&direction=desc&limit=3&cursor="+directory.path("nextCursor").asText()),"/api/markets/query","get",400);
  for(String id:List.of("BTC","LINK","JCT"))check(get("/api/assets/"+id+"/holders"),"/api/assets/{id}/holders","get",200);
  check(get("/api/addresses/"+ADDRESS+"/transfers?asset=JCT"),"/api/addresses/{address}/transfers","get",200);
  check(get("/api/assets/JCT/liquidations"),"/api/assets/{id}/liquidations","get",200);
  check(get("/api/addresses/"+ADDRESS+"/transfers?asset=LINK"),"/api/addresses/{address}/transfers","get",200);
  check(get("/api/assets/LINK/liquidations"),"/api/assets/{id}/liquidations","get",200);
  var sources=check(get("/api/sources"),"/api/sources","get",200);
  var coindeskSource=java.util.stream.StreamSupport.stream(sources.spliterator(),false)
   .filter(row->row.path("name").asText().equals("CoinDesk Data API")).findFirst().orElseThrow();
  assertFalse(coindeskSource.path("enabled").asBoolean());
  assertFalse(coindeskSource.path("productionApproved").asBoolean());
  assertTrue(coindeskSource.path("terms").isNull());
  check(get("/api/workspace"),"/api/workspace","get",200);
  for(String id:List.of("markets","holders","transfers","liquidations","watches"))
   check(get("/api/table-preferences/"+id),"/api/table-preferences/{tableId}","get",200);
  check(get("/actuator/health"),"/actuator/health","get",200);
  check(get("/actuator/health/liveness"),"/actuator/health/liveness","get",200);
  check(get("/actuator/health/readiness"),"/actuator/health/readiness","get",200);
 }

 @Test void validatesWritesAndApplicationErrors() throws Exception {
  check(delete("/api/watches/asset/LINK").with(csrf()),"/api/watches/{kind}/{key}","delete",204);
  check(put("/api/watches/asset/LINK").with(csrf()),"/api/watches/{kind}/{key}","put",200);
  check(put("/api/watches/address/invalid").with(csrf()),"/api/watches/{kind}/{key}","put",400);
  String note="{\"text\":\"contract fixture\",\"visible\":false,\"version\":0}";
  var workspace=check(get("/api/workspace"),"/api/workspace","get",200);
  long version=0;
  for(var row:workspace.path("notes"))if(row.path("kind").asText().equals("asset")&&row.path("key").asText().equals("LINK"))version=row.path("version").asLong();
  check(put("/api/notes/asset/LINK").with(csrf()).contentType("application/json").content(note.replace("\"version\":0","\"version\":"+version)),"/api/notes/{kind}/{key}","put",200);
  check(put("/api/notes/asset/LINK").with(csrf()).contentType("application/json").content(note),"/api/notes/{kind}/{key}","put",409);
  check(put("/api/notes/asset/LINK").with(csrf()).contentType("application/json").content("{}"),"/api/notes/{kind}/{key}","put",400);
  var preference=check(get("/api/table-preferences/holders"),"/api/table-preferences/{tableId}","get",200);
  String settings="{\"hidden\":[],\"order\":[\"address\",\"chain\",\"balance\",\"proportion\",\"value\",\"change-1H\",\"change-24H\",\"change-3D\",\"change-7D\",\"change-1M\",\"change-1Y\",\"type\",\"actions\"],\"sortKey\":null,\"desc\":false,\"pageSize\":15,\"version\":"+preference.path("version").asLong()+"}";
  check(put("/api/table-preferences/holders").with(csrf()).contentType("application/json").content(settings),"/api/table-preferences/{tableId}","put",200);
  check(put("/api/table-preferences/holders").with(csrf()).contentType("application/json").content(settings),"/api/table-preferences/{tableId}","put",409);
  check(get("/api/assets/UNKNOWN/holders"),"/api/assets/{id}/holders","get",400);
  check(get("/api/addresses/invalid/transfers?asset=LINK"),"/api/addresses/{address}/transfers","get",400);
  var missing=mvc.perform(get("/api/addresses/"+ADDRESS+"/transfers").accept("application/json")).andReturn().getResponse();
  assertEquals(400,missing.getStatus());
  assertEquals(Set.of(),named("Error").validate(json.readTree(missing.getContentAsByteArray())));
 }

 @Test void validatesProjectFieldsAndLegacyCompatibilityAgainstActualResponses() throws Exception {
  String path="/api/notes/{kind}/{key}";
  String base="/api/notes/asset/JCT";
  var workspace=check(get("/api/workspace"),"/api/workspace","get",200);
  long version=0;
  for(var note:workspace.path("notes"))if(note.path("kind").asText().equals("asset")&&note.path("key").asText().equals("JCT"))version=note.path("version").asLong();
  var payload=json.createObjectNode().put("projectName","Private project").put("projectInfo","User evidence <script>inert</script>")
   .put("text","Separate custom note").put("visible",true).put("version",version);
  assertEquals(Set.of(),named("NoteInput").validate(payload));
  var created=check(put(base).with(csrf()).contentType("application/json").content(payload.toString()),path,"put",200);
  assertEquals("Private project",created.path("projectName").asText());
  assertEquals("User evidence <script>inert</script>",created.path("projectInfo").asText());
  payload.put("version",created.path("version").asLong()).remove("projectName");
  payload.putNull("projectInfo");
  payload.put("visible",false);
  assertEquals(Set.of(),named("NoteInput").validate(payload));
  var legacy=check(put(base).with(csrf()).contentType("application/json").content(payload.toString()),path,"put",200);
  assertEquals(created.path("projectName"),legacy.path("projectName"));
  assertEquals(created.path("projectInfo"),legacy.path("projectInfo"));
  payload.put("projectName","x".repeat(121));
  assertFalse(named("NoteInput").validate(payload).isEmpty());
  check(put(base).with(csrf()).contentType("application/json").content(payload.toString()),path,"put",400);
  var missing=(ObjectNode)created.deepCopy();missing.remove("projectInfo");
  assertFalse(named("Note").validate(missing).isEmpty(),"Responses must explicitly include project fields");
 }

 @Test void schemasRejectLossOfPrecisionAndProvenance() {
  Instant now=Instant.parse("2026-10-21T00:00:00Z");
  var market=new ApiModels.Market("LINK","LINKUSD","12.340000000000000001",null,null,"9007199254740993",null,"USD",null);
  var derivative=new ApiModels.Derivative("LINK","PF_LINKUSD",null,null,"9007199254740993","source-native",null);
  var holder=new ApiModels.Holder(ADDRESS,"0.000000000000000001","1",18,false);
  var transfer=new ApiModels.Transfer("ethereum:0x"+"a".repeat(64)+":1","0x"+"a".repeat(64),1,20000000,ADDRESS,ADDRESS,"SELF","0.000000000000000001","1",18,now);
  var liquidation=new ApiModels.Liquidation("fixture","1","PF_LINKUSD","sell","liquidation","10","source-native","12.34",now);
  for(var entry:List.of(java.util.Map.entry("Market",market),java.util.Map.entry("Derivative",derivative),java.util.Map.entry("Holder",holder),java.util.Map.entry("Transfer",transfer),java.util.Map.entry("Liquidation",liquidation)))
   assertEquals(Set.of(),named(entry.getKey()).validate(json.valueToTree(entry.getValue())));
  ObjectNode row=json.valueToTree(market);
  ObjectNode invalidTransfer=json.valueToTree(transfer);
  invalidTransfer.put("hash","0xabc");
  assertFalse(named("Transfer").validate(invalidTransfer).isEmpty());
  invalidTransfer=json.valueToTree(transfer);invalidTransfer.put("logIndex",9007199254740992L);
  assertFalse(named("Transfer").validate(invalidTransfer).isEmpty());
  row.put("lastPrice",12.34);
  assertFalse(named("Market").validate(row).isEmpty(),"Decimals must stay strings");
  ObjectNode envelope=json.valueToTree(new ApiModels.Envelope<>(List.of(market),"partial","fixture","https://example.invalid",now,null,"synthetic software fixture","unit unknown",null));
  assertEquals(Set.of(),named("MarketEnvelope").validate(envelope));
  envelope.remove("coverage");
  assertFalse(named("MarketEnvelope").validate(envelope).isEmpty(),"Provenance cannot be omitted");
  envelope.put("coverage","fixture");envelope.put("status","unavailable");
  assertFalse(named("MarketEnvelope").validate(envelope).isEmpty(),"Unavailable is empty with no successful fetchedAt");
  assertFalse(named("NoteInput").validate(json.createObjectNode().put("text","x").put("visible",true).put("version",-1)).isEmpty());
  assertFalse(named("TablePreferenceInput").validate(json.createObjectNode()).isEmpty());
  assertFalse(named("UtcTime").validate(json.getNodeFactory().textNode("2026-99-99T00:00:00Z")).isEmpty());
 }
}
