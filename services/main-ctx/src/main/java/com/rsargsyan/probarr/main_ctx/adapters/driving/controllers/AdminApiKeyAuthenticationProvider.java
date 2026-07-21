package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminApiKey;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminApiKeyRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminApiKeyAuthenticationProvider implements AuthenticationProvider {
  private final AdminApiKeyRepository adminApiKeyRepository;

  @Autowired
  public AdminApiKeyAuthenticationProvider(AdminApiKeyRepository adminApiKeyRepository) {
    this.adminApiKeyRepository = adminApiKeyRepository;
  }

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    AdminApiKeyAuthentication token = (AdminApiKeyAuthentication) auth;
    String apiKeyId = token.getApiKeyId();
    String apiKey = token.getApiKey();

    if (!TSID.isValid(apiKeyId)) throw new BadCredentialsException("Invalid admin API key");

    Optional<AdminApiKey> keyOpt = adminApiKeyRepository.findById(TSID.from(apiKeyId).toLong());
    if (keyOpt.isEmpty() || keyOpt.get().isDisabled() || !keyOpt.get().check(apiKey)) {
      throw new BadCredentialsException("Failed to validate provided admin API key");
    }

    token.setAuthenticated(true);
    return token;
  }

  @Override
  public boolean supports(Class<?> auth) {
    return AdminApiKeyAuthentication.class.isAssignableFrom(auth);
  }
}
