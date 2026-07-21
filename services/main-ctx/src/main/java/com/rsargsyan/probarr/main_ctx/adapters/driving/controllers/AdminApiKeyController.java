package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.AdminService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.AdminApiKeyCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.AdminApiKeyDTO;
import com.rsargsyan.probarr.main_ctx.core.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api-key")
public class AdminApiKeyController {
  private final AdminService adminService;

  @Autowired
  public AdminApiKeyController(AdminService adminService) {
    this.adminService = adminService;
  }

  @PostMapping
  public ResponseEntity<AdminApiKeyDTO> createApiKey(@RequestBody AdminApiKeyCreationDTO req) {
    Long adminProfileId = AdminContextHolder.get().adminProfileId();
    return new ResponseEntity<>(adminService.createApiKey(adminProfileId, req.description()), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<AdminApiKeyDTO>> listApiKeys() {
    Long adminProfileId = AdminContextHolder.get().adminProfileId();
    return ResponseEntity.ok(adminService.listApiKeys(adminProfileId));
  }

  @PutMapping("/{keyId}/disable")
  public ResponseEntity<Void> disableApiKey(@PathVariable String keyId) {
    Long adminProfileId = AdminContextHolder.get().adminProfileId();
    adminService.disableApiKey(adminProfileId, Util.validateTSID(keyId));
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{keyId}/enable")
  public ResponseEntity<Void> enableApiKey(@PathVariable String keyId) {
    Long adminProfileId = AdminContextHolder.get().adminProfileId();
    adminService.enableApiKey(adminProfileId, Util.validateTSID(keyId));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{keyId}")
  public ResponseEntity<Void> deleteApiKey(@PathVariable String keyId) {
    Long adminProfileId = AdminContextHolder.get().adminProfileId();
    adminService.deleteApiKey(adminProfileId, Util.validateTSID(keyId));
    return ResponseEntity.noContent().build();
  }
}
