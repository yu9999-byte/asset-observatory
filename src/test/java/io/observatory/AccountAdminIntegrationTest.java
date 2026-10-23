package io.observatory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:mysql://127.0.0.1:3307/observatory_test?connectionTimeZone=UTC&characterEncoding=UTF-8}","spring.datasource.username=${TEST_DATABASE_USER:observatory}","spring.datasource.password=${TEST_DATABASE_PASSWORD:local-only-not-for-deployment}","app.sources.enabled=false","app.external=true","app.user=contract-operator","app.password=contract-test-password-2026","server.servlet.session.cookie.secure=true","app.sources.blockscout-legacy-approved=false"})
@AutoConfigureMockMvc
@Transactional
class AccountAdminIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;

 private static final String ADMIN = "contract-operator";
 private static final String ADMIN_PASSWORD = "contract-test-password-2026";
 private static final String RESEARCHER = "reader.one";

 @Test void onlyAdminsCanListAndProvisionAccountsAndSecretsNeverReturn() throws Exception {
  mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/accounts").with(httpBasic(ADMIN,ADMIN_PASSWORD)))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value(ADMIN))
   .andExpect(jsonPath("$[0].password").doesNotExist()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  mvc.perform(post("/api/admin/accounts").with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf())
    .contentType("application/json").content("{\"username\":\"reader.one\",\"password\":\"researcher-password-2026\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value(RESEARCHER))
   .andExpect(jsonPath("$.role").value("RESEARCHER")).andExpect(jsonPath("$.enabled").value(true))
   .andExpect(jsonPath("$.passwordHash").doesNotExist());
  mvc.perform(get("/api/admin/accounts").with(httpBasic(RESEARCHER,"researcher-password-2026")))
   .andExpect(status().isForbidden());
  mvc.perform(get("/api/markets").with(httpBasic(RESEARCHER,"researcher-password-2026")))
   .andExpect(status().isOk());
  mvc.perform(post("/api/admin/accounts").with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf())
    .contentType("application/json").content("{\"username\":\"reader.one\",\"password\":\"researcher-password-2026\"}"))
   .andExpect(status().isConflict());
 }

 @Test void resetAndDisableChangeAuthenticationWithoutDeletingTheOwnerIdentity() throws Exception {
  createResearcher();
  mvc.perform(put("/api/admin/accounts/{username}/password",RESEARCHER)
    .with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf()).contentType("application/json")
    .content("{\"password\":\"replacement-password-2026\"}"))
   .andExpect(status().isNoContent());
  mvc.perform(get("/api/markets").with(httpBasic(RESEARCHER,"researcher-password-2026")))
   .andExpect(status().isUnauthorized());
  mvc.perform(get("/api/markets").with(httpBasic(RESEARCHER,"replacement-password-2026")))
   .andExpect(status().isOk());
  mvc.perform(put("/api/admin/accounts/{username}/enabled",RESEARCHER)
    .with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf()).contentType("application/json")
    .content("{\"enabled\":false}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
  mvc.perform(get("/api/markets").with(httpBasic(RESEARCHER,"replacement-password-2026")))
   .andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/accounts").with(httpBasic(ADMIN,ADMIN_PASSWORD)))
   .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.username == 'reader.one')].enabled").value(false));
 }

 @Test void validationAndLastAdministratorProtectionAreEnforced() throws Exception {
  mvc.perform(post("/api/admin/accounts").with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf())
    .contentType("application/json").content("{\"username\":\"x\",\"password\":\"short\"}"))
   .andExpect(status().isBadRequest());
  mvc.perform(put("/api/admin/accounts/{username}/enabled",ADMIN)
    .with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf()).contentType("application/json")
    .content("{\"enabled\":false}"))
   .andExpect(status().isConflict());
 }

 private void createResearcher() throws Exception {
  mvc.perform(post("/api/admin/accounts").with(httpBasic(ADMIN,ADMIN_PASSWORD)).with(csrf())
    .contentType("application/json").content("{\"username\":\"reader.one\",\"password\":\"researcher-password-2026\"}"))
   .andExpect(status().isCreated());
 }
}
