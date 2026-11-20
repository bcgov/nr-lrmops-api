package ca.bc.gov.nrs.lrmops.configuration;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Web security for the LRM Ops API.
 *
 * <p>A pure bearer-token API (no browser session, no cookies), so CSRF is disabled and sessions
 * are stateless. Every request that isn't actuator health/metrics or the OpenAPI docs must carry
 * a valid
 * Keycloak-issued JWT; the {@code JwtDecoder} is auto-configured from
 * {@code spring.security.oauth2.resourceserver.jwt.*} and performs RS256 signature, issuer and
 * expiry validation.
 *
 * <p>OAuth2 scopes from the token's {@code scope} claim are exposed as {@code SCOPE_*} authorities
 * by Spring's default converter; endpoints are gated per operation with {@code @PreAuthorize} (see
 * {@link ca.bc.gov.nrs.lrmops.security.ApiScopes}). Which clients may call which endpoints is
 * therefore decided in Keycloak, by the scopes assigned to each calling client.
 *
 * <h3>CORS</h3>
 * Several applications use this API. Their backends call it server to server, where CORS does not
 * apply; a browser app calling it directly needs its origin in {@code ALLOWED_ORIGINS}. Empty, no
 * cross-origin browser call is allowed.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

  /**
   * Paths reachable without authentication: the OpenShift probes, the Prometheus scrape (keep in
   * step with management.endpoints.web.exposure.include) and the OpenAPI docs / Swagger UI.
   */
  private static final String[] PUBLIC_PATHS = {
      "/actuator/health",
      "/actuator/health/**",
      "/actuator/prometheus",
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-ui/**"
  };

  @Value("${ca.bc.gov.nrs.cors.allowed-origins:}")
  private String allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        // Uses the corsConfigurationSource bean below; answers preflights before authentication.
        .cors(Customizer.withDefaults())
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }

  /**
   * The browser origins allowed to call the API, from {@code ALLOWED_ORIGINS} (comma-separated,
   * scheme and host, e.g. {@code https://app.gov.bc.ca}). Read-only API, so GET only.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(parseOrigins(allowedOrigins));
    configuration.setAllowedMethods(List.of("GET", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    // No credentials: tokens ride the Authorization header, set by the calling app. There is no
    // cookie or session to send.
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /** Comma-separated origins, each trimmed, blanks dropped. */
  static List<String> parseOrigins(String origins) {
    if (origins == null) {
      return List.of();
    }
    return Arrays.stream(origins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList();
  }
}
