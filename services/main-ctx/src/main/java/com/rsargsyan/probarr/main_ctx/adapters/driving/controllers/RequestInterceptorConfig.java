package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.AuthService;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminApiKeyRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class RequestInterceptorConfig implements WebMvcConfigurer {

  private final AuthService authService;
  private final AdminProfileRepository adminProfileRepository;
  private final AdminApiKeyRepository adminApiKeyRepository;

  @Autowired
  public RequestInterceptorConfig(AuthService authService,
                                  AdminProfileRepository adminProfileRepository,
                                  AdminApiKeyRepository adminApiKeyRepository) {
    this.authService = authService;
    this.adminProfileRepository = adminProfileRepository;
    this.adminApiKeyRepository = adminApiKeyRepository;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new UserContextInterceptor(authService));
    registry.addInterceptor(new AdminContextInterceptor(adminProfileRepository, adminApiKeyRepository))
        .addPathPatterns("/admin/**");
  }
}
