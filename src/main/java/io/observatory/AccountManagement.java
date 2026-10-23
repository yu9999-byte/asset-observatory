package io.observatory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

record CreateAccountRequest(
 @NotBlank @Pattern(regexp="[A-Za-z0-9._@-]{3,64}") String username,
 @NotBlank @Size(min=16,max=128) String password) {}
record PasswordResetRequest(@NotBlank @Size(min=16,max=128) String password) {}
record AccountEnabledRequest(@NotNull Boolean enabled) {}
record AccountView(String username,String role,boolean enabled,Instant createdAt) {}

@Service
class AccountManagement implements UserDetailsService, ApplicationRunner {
 private static final String SELECT_ACCOUNT = "SELECT username,password_hash,role,enabled FROM user_accounts WHERE username=?";
 private final JdbcTemplate jdbc;
 private final PasswordEncoder encoder;
 private final String bootstrapUser;
 private final String bootstrapPassword;
 private final boolean external;

 AccountManagement(JdbcTemplate jdbc,PasswordEncoder encoder,
  @Value("${app.user}") String bootstrapUser,
  @Value("${app.password}") String bootstrapPassword,
  @Value("${app.external}") boolean external) {
  this.jdbc=jdbc;this.encoder=encoder;this.bootstrapUser=bootstrapUser;
  this.bootstrapPassword=bootstrapPassword;this.external=external;
 }

 @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
  List<UserDetails> users=jdbc.query(SELECT_ACCOUNT,(rs,row)->user(rs),username);
  if(users.isEmpty())throw new UsernameNotFoundException("账户不存在");
  return users.getFirst();
 }

 private UserDetails user(ResultSet rs) throws SQLException {
  return User.withUsername(rs.getString("username")).password(rs.getString("password_hash"))
   .disabled(!rs.getBoolean("enabled"))
   .authorities(new SimpleGrantedAuthority("ROLE_"+rs.getString("role"))).build();
 }

 @Override @Transactional public void run(ApplicationArguments args) {
  if(!external)return;
  String hash=encoder.encode(bootstrapPassword);
  jdbc.update("""
   INSERT INTO user_accounts(username,password_hash,role,enabled)
   VALUES(?,?,'ADMIN',TRUE)
   ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash),role='ADMIN',enabled=TRUE,updated_at=UTC_TIMESTAMP(6)
   """,bootstrapUser,hash);
 }

 @Transactional(readOnly=true) List<AccountView> list() {
  return jdbc.query("SELECT username,role,enabled,created_at FROM user_accounts ORDER BY username",
   (rs,row)->view(rs));
 }

 @Transactional public AccountView create(CreateAccountRequest request) {
  try {
   jdbc.update("INSERT INTO user_accounts(username,password_hash,role,enabled) VALUES(?,?,'RESEARCHER',TRUE)",
    request.username(),encoder.encode(request.password()));
  } catch(DuplicateKeyException duplicate) {
   throw new ResponseStatusException(HttpStatus.CONFLICT,"用户名已存在");
  }
  return find(request.username());
 }

 @Transactional public void resetPassword(String username,PasswordResetRequest request) {
  int changed=jdbc.update("UPDATE user_accounts SET password_hash=?,updated_at=UTC_TIMESTAMP(6) WHERE username=?",
   encoder.encode(request.password()),username);
  if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"账户不存在");
 }

 @Transactional public AccountView setEnabled(String username,AccountEnabledRequest request,String actor) {
  AccountView target=find(username);
  if(!request.enabled()&&username.equals(actor))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"不能停用当前管理员账户");
  if(!request.enabled()&&target.role().equals("ADMIN")) {
   Integer otherAdmins=jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts WHERE role='ADMIN' AND enabled=TRUE AND username<>?",Integer.class,username);
   if(otherAdmins==null||otherAdmins==0)
    throw new ResponseStatusException(HttpStatus.CONFLICT,"不能停用最后一个启用的管理员");
  }
  jdbc.update("UPDATE user_accounts SET enabled=?,updated_at=UTC_TIMESTAMP(6) WHERE username=?",request.enabled(),username);
  return find(username);
 }

 private AccountView find(String username) {
  List<AccountView> accounts=jdbc.query("SELECT username,role,enabled,created_at FROM user_accounts WHERE username=?",
   (rs,row)->view(rs),username);
  if(accounts.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"账户不存在");
  return accounts.getFirst();
 }

 private AccountView view(ResultSet rs) throws SQLException {
  return new AccountView(rs.getString("username"),rs.getString("role"),rs.getBoolean("enabled"),rs.getTimestamp("created_at").toInstant());
 }
}
