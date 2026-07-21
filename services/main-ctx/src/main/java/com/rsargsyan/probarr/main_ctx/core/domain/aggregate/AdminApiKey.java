package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.InvalidApiKeyDescriptionException;
import com.rsargsyan.probarr.main_ctx.core.exception.ProvidedApiKeyIsBlankException;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Entity
@Table(name = "admin_api_key")
public class AdminApiKey extends AggregateRoot {
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
  private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private static final int DESCRIPTION_MAX_LENGTH = 127;

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admin_profile_id", nullable = false)
  private AdminProfile adminProfile;

  @Getter
  @Transient
  private String key;

  @Getter
  @Column(name = "hashed_key", nullable = false, unique = true)
  private String hashedKey;

  @Getter
  private boolean disabled = false;

  @Getter
  @Column(name = "last_access_time")
  private Instant lastAccessTime;

  @Getter
  @Column(length = DESCRIPTION_MAX_LENGTH)
  private String description;

  private static String generateApiKey() {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes);
  }

  public boolean disable() {
    if (this.disabled) return false;
    this.disabled = true;
    touch();
    return true;
  }

  public boolean enable() {
    if (!this.disabled) return false;
    this.disabled = false;
    touch();
    return true;
  }

  public void markAccessed() {
    lastAccessTime = Instant.now();
  }

  public boolean check(String rawKey) {
    if (rawKey == null || rawKey.isBlank()) throw new ProvidedApiKeyIsBlankException();
    return passwordEncoder.matches(rawKey, hashedKey);
  }

  @SuppressWarnings("unused")
  AdminApiKey() {}

  AdminApiKey(AdminProfile adminProfile, String description) {
    this.adminProfile = adminProfile;
    this.key = generateApiKey();
    this.hashedKey = passwordEncoder.encode(key);
    this.description = validateDescription(description);
  }

  private String validateDescription(String description) {
    if (description == null || description.isBlank() || description.length() > DESCRIPTION_MAX_LENGTH) {
      throw new InvalidApiKeyDescriptionException();
    }
    return description;
  }
}
