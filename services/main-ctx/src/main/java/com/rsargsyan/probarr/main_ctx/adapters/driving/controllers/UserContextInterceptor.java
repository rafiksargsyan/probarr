package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserContextInterceptor implements HandlerInterceptor {

  private final AuthService authService;

  public UserContextInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return true;

    Object principal = auth.getPrincipal();
    if (principal instanceof Jwt jwt) {
      String externalId = jwt.getSubject();
      String userProfileId = authService.getUserProfileIdByExternalId(externalId);
      UserContextHolder.set(UserContext.builder()
          .externalId(externalId)
          .userProfileId(userProfileId)
          .build());
    } else if (auth instanceof CustomApiKey customApiKey) {
      AuthService.UserProfileContext ctx = authService.getUserContextByApiKey(customApiKey.getApiKeyId());
      if (ctx != null) {
        UserContextHolder.set(UserContext.builder()
            .userProfileId(ctx.userProfileId())
            .externalId(ctx.externalId())
            .build());
      }
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    UserContextHolder.clear();
  }
}
