package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.AdminService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.AdminProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminSignupController {
  private final AdminService adminService;

  @Autowired
  public AdminSignupController(AdminService adminService) {
    this.adminService = adminService;
  }

  @PostMapping("/signup")
  public ResponseEntity<AdminProfileDTO> signup() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) auth.getPrincipal();
    Map<String, Object> claims = jwt.getClaims();
    String externalId = (String) claims.get("sub");
    String fullName = (String) claims.get("name");
    return ResponseEntity.ok(adminService.signup(externalId, fullName));
  }
}
