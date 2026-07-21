package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminApiKey;

import java.time.Instant;

public record AdminApiKeyDTO(String id, String key, String description, boolean disabled,
                             Instant lastAccessTime, Instant createdAt) {
  public static AdminApiKeyDTO from(AdminApiKey adminApiKey, String key) {
    return new AdminApiKeyDTO(
        adminApiKey.getStrId(),
        key,
        adminApiKey.getDescription(),
        adminApiKey.isDisabled(),
        adminApiKey.getLastAccessTime(),
        adminApiKey.getCreatedAt()
    );
  }

  public static AdminApiKeyDTO from(AdminApiKey adminApiKey) {
    return from(adminApiKey, null);
  }
}
