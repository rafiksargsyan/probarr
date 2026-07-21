package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.ApiKeyLimitReachedException;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin_profile")
public class AdminProfile extends AggregateRoot {
  @Getter
  @Column(name = "external_id", unique = true)
  private String externalId;

  @Getter
  @Column(name = "full_name", length = 127)
  private String fullName;

  @OneToMany(mappedBy = "adminProfile", cascade = CascadeType.ALL, orphanRemoval = true)
  @Getter
  private List<AdminApiKey> apiKeys = new ArrayList<>();

  @SuppressWarnings("unused")
  public AdminProfile() {}

  public AdminProfile(String externalId, String fullName) {
    this.externalId = externalId;
    this.fullName = fullName;
  }

  public AdminApiKey createApiKey(String description) {
    long activeCount = apiKeys.stream().filter(k -> !k.isDisabled()).count();
    if (activeCount >= 2) throw new ApiKeyLimitReachedException();
    AdminApiKey apiKey = new AdminApiKey(this, description);
    apiKeys.add(apiKey);
    touch();
    return apiKey;
  }

  public AdminApiKey getApiKeyById(Long id) {
    return apiKeys.stream()
        .filter(k -> k.getId().equals(id))
        .findFirst()
        .orElseThrow(ResourceNotFoundException::new);
  }

  public void removeApiKey(AdminApiKey apiKey) {
    if (apiKeys.remove(apiKey)) touch();
  }
}
