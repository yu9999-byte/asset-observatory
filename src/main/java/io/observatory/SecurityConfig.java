package io.observatory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
class SecurityConfig {
 @Bean SecurityFilterChain security(HttpSecurity http,@Value("${app.external}") boolean external,
 @Value("${server.address}") String bind,@Value("${app.password}")String password,
 @Value("${server.servlet.session.cookie.secure}")boolean secure,@Value("${app.sources.blockscout-legacy-approved}")boolean legacy) throws Exception {
  if(!external&&!bind.equals("127.0.0.1")&&!bind.equals("::1"))throw new IllegalStateException("非本机绑定必须启用 APP_EXTERNAL");
  if(external&&(password.length()<16||!secure||legacy))throw new IllegalStateException("外部模式需要16位以上密码、安全cookie且不得使用legacy链上源");
  http.authorizeHttpRequests(a->{
   if(external)a.requestMatchers("/","/index.html","/assets/**","/favicon.ico","/api/csrf").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/accounts").permitAll()
    .anyRequest().authenticated();
   else a.anyRequest().permitAll();
  });
  http.csrf(c->c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()));
  http.httpBasic(Customizer.withDefaults());
  http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")));
  return http.build();
 }
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
}
