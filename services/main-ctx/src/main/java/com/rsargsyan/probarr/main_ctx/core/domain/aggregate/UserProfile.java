package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.ApiKeyLimitReachedException;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profile")
public class UserProfile extends AggregateRoot {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  @Getter
  private Account account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "principal_id", nullable = false)
  @Getter
  private Principal principal;

  @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ApiKey> apiKeys = new ArrayList<>();

  @SuppressWarnings("unused")
  public UserProfile() {}

  public UserProfile(Account account, Principal principal) {
    if (account == null) throw new IllegalArgumentException("account is null");
    if (principal == null) throw new IllegalArgumentException("principal is null");
    this.account = account;
    this.principal = principal;
  }

  public String createApiKey(String description) {
    if (apiKeys.size() >= 2) throw new ApiKeyLimitReachedException();
    ApiKey apiKey = new ApiKey(this, description);
    apiKeys.add(apiKey);
    touch();
    return apiKey.getKey();
  }

  public ApiKey getApiKeyByKey(String key) {
    for (ApiKey apiKey : apiKeys) {
      if (apiKey.check(key)) return apiKey;
    }
    return null;
  }
}
