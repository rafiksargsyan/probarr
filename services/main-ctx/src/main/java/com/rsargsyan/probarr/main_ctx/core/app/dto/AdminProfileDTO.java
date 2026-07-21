package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminProfile;

import java.time.Instant;

public record AdminProfileDTO(String id, String externalId, String fullName, Instant createdAt) {
  public static AdminProfileDTO from(AdminProfile adminProfile) {
    return new AdminProfileDTO(
        adminProfile.getStrId(),
        adminProfile.getExternalId(),
        adminProfile.getFullName(),
        adminProfile.getCreatedAt()
    );
  }
}
