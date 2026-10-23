package io.observatory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:mysql://127.0.0.1:3307/observatory_test?connectionTimeZone=UTC&characterEncoding=UTF-8}","spring.datasource.username=${TEST_DATABASE_USER:observatory}","spring.datasource.password=${TEST_DATABASE_PASSWORD:local-only-not-for-deployment}","app.sources.enabled=false","app.external=true","app.user=contract-operator","app.password=contract-test-password-2026","server.servlet.session.cookie.secure=true","app.sources.blockscout-legacy-approved=false"})
@AutoConfigureMockMvc
@Transactional
class ExternalSecurityTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;

 @Test void basicCredentialsProtectResearchAndHealthRoutes() throws Exception {
  for(String path:new String[]{"/api/markets","/api/workspace","/actuator/health"}) {
   mvc.perform(get(path)).andExpect(status().isUnauthorized());
   mvc.perform(get(path).with(httpBasic("contract-operator","wrong"))).andExpect(status().isUnauthorized());
   mvc.perform(get(path).with(httpBasic("contract-operator","contract-test-password-2026"))).andExpect(status().isOk());
  }
  mvc.perform(get("/")).andExpect(status().isOk());
  mvc.perform(get("/index.html")).andExpect(status().isOk());
  mvc.perform(get("/api/csrf")).andExpect(status().isOk());
 }

 @Test void anonymousVisitorsCanReachRegistrationAndCreateAResearcherWithSessionCsrf() throws Exception {
  mvc.perform(get("/")).andExpect(status().isOk());
  mvc.perform(get("/index.html")).andExpect(status().isOk());
  mvc.perform(get("/api/csrf")).andExpect(status().isOk());
  mvc.perform(get("/api/markets")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/accounts").contentType("application/json")
    .content("{\"username\":\"public.reader\",\"password\":\"public-reader-password-2026\"}"))
   .andExpect(status().isForbidden());

  var csrfResult=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)csrfResult.getRequest().getSession(false);
  assertNotNull(session);
  var token=json.readTree(csrfResult.getResponse().getContentAsByteArray());
  mvc.perform(post("/api/accounts").session(session).header(token.path("headerName").asText(),token.path("token").asText())
    .contentType("application/json").content("{\"username\":\"public.reader\",\"password\":\"public-reader-password-2026\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("RESEARCHER"))
   .andExpect(jsonPath("$.passwordHash").doesNotExist());
  mvc.perform(get("/api/markets").with(httpBasic("public.reader","public-reader-password-2026")))
   .andExpect(status().isOk());
 }

 @Test void registrationRejectsMissingCsrfInvalidPasswordsAndDuplicateNames() throws Exception {
  mvc.perform(post("/api/accounts").contentType("application/json")
    .content("{\"username\":\"no.csrf\",\"password\":\"long-enough-password-2026\"}"))
   .andExpect(status().isForbidden());
  var csrfResult=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)csrfResult.getRequest().getSession(false);
  var token=json.readTree(csrfResult.getResponse().getContentAsByteArray());
  String header=token.path("headerName").asText(),value=token.path("token").asText();
  mvc.perform(post("/api/accounts").session(session).header(header,value).contentType("application/json")
    .content("{\"username\":\"short.pass\",\"password\":\"short\"}"))
   .andExpect(status().isBadRequest());
  String body="{\"username\":\"duplicate.reader\",\"password\":\"duplicate-reader-password-2026\",\"role\":\"ADMIN\"}";
  mvc.perform(post("/api/accounts").session(session).header(header,value).contentType("application/json").content(body))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("RESEARCHER"));
  mvc.perform(post("/api/accounts").session(session).header(header,value).contentType("application/json").content(body))
   .andExpect(status().isConflict());
 }

 @Test void registrationSupportsTheContractMaximumPasswordWithoutTruncation() throws Exception {
  String password="p".repeat(127)+"A";
  String body=json.writeValueAsString(Map.of("username","long.password.reader","password",password));
  mvc.perform(post("/api/accounts").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
    .contentType("application/json").content(body)).andExpect(status().isCreated());
  mvc.perform(get("/api/workspace").with(httpBasic("long.password.reader",password))).andExpect(status().isOk());
  mvc.perform(get("/api/workspace").with(httpBasic("long.password.reader","p".repeat(127)+"B"))).andExpect(status().isUnauthorized());
 }

 @Test void csrfTokenRequiresTheIssuingSessionEvenWithValidBasicCredentials() throws Exception {
  var result=mvc.perform(get("/api/csrf").with(httpBasic("contract-operator","contract-test-password-2026")))
   .andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  assertNotNull(session);
  var token=json.readTree(result.getResponse().getContentAsByteArray());
  String header=token.path("headerName").asText(),value=token.path("token").asText();
  mvc.perform(put("/api/watches/asset/LINK").with(httpBasic("contract-operator","contract-test-password-2026")))
   .andExpect(status().isForbidden());
  mvc.perform(put("/api/watches/asset/LINK").header(header,value).with(httpBasic("contract-operator","contract-test-password-2026")))
   .andExpect(status().isForbidden());
  mvc.perform(put("/api/watches/asset/LINK").session(session).header(header,value).with(httpBasic("contract-operator","contract-test-password-2026")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.key").value("LINK"));
 }
}
