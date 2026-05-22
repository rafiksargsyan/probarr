package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.ApiKey;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Principal;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.UserProfile;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.ApiKeyRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.PrincipalRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.UserProfileRepository;
import io.hypersistence.tsid.TSID;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

  private final ApiKeyRepository apiKeyRepository;
  private final PrincipalRepository principalRepository;
  private final UserProfileRepository userProfileRepository;

  @Autowired
  public AuthService(ApiKeyRepository apiKeyRepository,
                     PrincipalRepository principalRepository,
                     UserProfileRepository userProfileRepository) {
    this.apiKeyRepository = apiKeyRepository;
    this.principalRepository = principalRepository;
    this.userProfileRepository = userProfileRepository;
  }

  @Transactional(readOnly = true)
  public String getUserProfileIdByExternalId(String externalId) {
    return principalRepository.findByExternalId(externalId)
        .flatMap(p -> userProfileRepository.findByPrincipalId(p.getId()))
        .map(UserProfile::getStrId)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public boolean validateApiKey(String apiKeyId, String apiKey) {
    if (!TSID.isValid(apiKeyId)) return false;
    Optional<ApiKey> apiKeyFromDB = apiKeyRepository.findById(TSID.from(apiKeyId).toLong());
    return apiKeyFromDB.isPresent() && !apiKeyFromDB.get().isDisabled() && apiKeyFromDB.get().check(apiKey);
  }

  @Transactional
  public UserProfileContext getUserContextByApiKey(String apiKeyId) {
    if (!TSID.isValid(apiKeyId)) return null;
    Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(TSID.from(apiKeyId).toLong());
    if (apiKeyOpt.isEmpty()) return null;
    ApiKey apiKey = apiKeyOpt.get();
    UserProfile userProfile = apiKey.getUserProfile();
    apiKeyRepository.updateLastAccessTime(apiKey.getId(), Instant.now());
    return new UserProfileContext(
        userProfile.getStrId(),
        userProfile.getPrincipal().getExternalId()
    );
  }

  public record UserProfileContext(String userProfileId, String externalId) {}
}
