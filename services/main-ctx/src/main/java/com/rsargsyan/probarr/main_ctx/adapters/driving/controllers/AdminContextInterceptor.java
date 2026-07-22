package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminApiKey;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminProfile;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminApiKeyRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminProfileRepository;
import io.hypersistence.tsid.TSID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;

public class AdminContextInterceptor implements HandlerInterceptor {
  private final AdminProfileRepository adminProfileRepository;
  private final AdminApiKeyRepository adminApiKeyRepository;

  public AdminContextInterceptor(AdminProfileRepository adminProfileRepository,
                                 AdminApiKeyRepository adminApiKeyRepository) {
    this.adminProfileRepository = adminProfileRepository;
    this.adminApiKeyRepository = adminApiKeyRepository;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return true;

    if (auth instanceof AdminApiKeyAuthentication adminKeyAuth) {
      String apiKeyId = adminKeyAuth.getApiKeyId();
      AdminApiKey adminApiKey = adminApiKeyRepository.findByIdWithProfile(TSID.from(apiKeyId).toLong())
          .orElseThrow(ResourceNotFoundException::new);
      AdminProfile adminProfile = adminApiKey.getAdminProfile();
      adminApiKey.markAccessed();
      adminApiKeyRepository.save(adminApiKey);
      AdminContextHolder.set(new AdminContext(adminProfile.getId(), adminProfile.getExternalId(),
          adminProfile.getFullName()));
    } else if (auth.getPrincipal() instanceof Jwt jwt) {
      Map<String, Object> claims = jwt.getClaims();
      String externalId = (String) claims.get("sub");
      String fullName = (String) claims.get("name");

      boolean isSignup = request.getMethod().equalsIgnoreCase("POST")
          && request.getRequestURI().equals("/admin/signup");

      Optional<AdminProfile> profileOpt = adminProfileRepository.findByExternalId(externalId);
      if (profileOpt.isPresent()) {
        AdminProfile adminProfile = profileOpt.get();
        AdminContextHolder.set(new AdminContext(adminProfile.getId(), externalId, fullName));
      } else if (!isSignup) {
        throw new ResourceNotFoundException();
      }
    }

    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    AdminContextHolder.clear();
  }
}
