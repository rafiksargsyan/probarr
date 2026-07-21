package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${cors.allowed-origins}")
  private List<String> allowedOrigins;

  private final CustomApiKeyAuthenticationProvider apiKeyAuthenticationProvider;
  private final AdminApiKeyAuthenticationProvider adminApiKeyAuthenticationProvider;

  @Autowired
  public SecurityConfig(CustomApiKeyAuthenticationProvider apiKeyAuthenticationProvider,
                        AdminApiKeyAuthenticationProvider adminApiKeyAuthenticationProvider) {
    this.apiKeyAuthenticationProvider = apiKeyAuthenticationProvider;
    this.adminApiKeyAuthenticationProvider = adminApiKeyAuthenticationProvider;
  }

  @Bean("adminJwtDecoder")
  public JwtDecoder adminJwtDecoder(@Value("${admin.firebase.project-id}") String adminProjectId) {
    return JwtDecoders.fromIssuerLocation("https://securetoken.google.com/" + adminProjectId);
  }

  @Bean
  @Order(1)
  public SecurityFilterChain adminSecurityFilterChain(
      HttpSecurity http,
      @Qualifier("adminJwtDecoder") JwtDecoder adminJwtDecoder) throws Exception {
    AuthenticationManager adminAuthManager = new ProviderManager(adminApiKeyAuthenticationProvider);
    return http
        .securityMatcher("/admin/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(adminJwtDecoder)))
        .addFilterBefore(new AdminApiKeyAuthenticationFilter(adminAuthManager),
            BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
        .addFilterBefore(new ApiKeyAuthenticationFilter(new ProviderManager(apiKeyAuthenticationProvider)),
            BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
