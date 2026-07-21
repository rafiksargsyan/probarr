package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.app.dto.AdminApiKeyDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.AdminProfileDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminApiKey;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminProfile;
import com.rsargsyan.probarr.main_ctx.core.exception.ApiKeyNotDisabledException;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminApiKeyRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.AdminProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {
  private final AdminProfileRepository adminProfileRepository;
  private final AdminApiKeyRepository adminApiKeyRepository;

  @Autowired
  public AdminService(AdminProfileRepository adminProfileRepository,
                      AdminApiKeyRepository adminApiKeyRepository) {
    this.adminProfileRepository = adminProfileRepository;
    this.adminApiKeyRepository = adminApiKeyRepository;
  }

  public AdminProfileDTO signup(String externalId, String fullName) {
    return adminProfileRepository.findByExternalId(externalId)
        .map(AdminProfileDTO::from)
        .orElseGet(() -> {
          AdminProfile profile = new AdminProfile(externalId, fullName);
          return AdminProfileDTO.from(adminProfileRepository.save(profile));
        });
  }

  public AdminApiKeyDTO createApiKey(Long adminProfileId, String description) {
    AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
        .orElseThrow(ResourceNotFoundException::new);
    AdminApiKey adminApiKey = adminProfile.createApiKey(description);
    String rawKey = adminApiKey.getKey();
    AdminApiKey saved = adminApiKeyRepository.save(adminApiKey);
    return AdminApiKeyDTO.from(saved, rawKey);
  }

  @Transactional(readOnly = true)
  public List<AdminApiKeyDTO> listApiKeys(Long adminProfileId) {
    AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
        .orElseThrow(ResourceNotFoundException::new);
    return adminProfile.getApiKeys().stream().map(AdminApiKeyDTO::from).toList();
  }

  public void disableApiKey(Long adminProfileId, Long keyId) {
    AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
        .orElseThrow(ResourceNotFoundException::new);
    AdminApiKey adminApiKey = adminProfile.getApiKeyById(keyId);
    adminApiKey.disable();
    adminApiKeyRepository.save(adminApiKey);
  }

  public void enableApiKey(Long adminProfileId, Long keyId) {
    AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
        .orElseThrow(ResourceNotFoundException::new);
    AdminApiKey adminApiKey = adminProfile.getApiKeyById(keyId);
    adminApiKey.enable();
    adminApiKeyRepository.save(adminApiKey);
  }

  public void deleteApiKey(Long adminProfileId, Long keyId) {
    AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
        .orElseThrow(ResourceNotFoundException::new);
    AdminApiKey adminApiKey = adminProfile.getApiKeyById(keyId);
    if (!adminApiKey.isDisabled()) throw new ApiKeyNotDisabledException();
    adminProfile.removeApiKey(adminApiKey);
    adminProfileRepository.save(adminProfile);
  }
}
