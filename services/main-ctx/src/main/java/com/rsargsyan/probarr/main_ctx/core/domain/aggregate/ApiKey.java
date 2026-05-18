package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.ProvidedApiKeyIsBlankException;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Entity
@Table(name = "api_key")
public class ApiKey extends AggregateRoot {

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
  private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private static final int DESCRIPTION_MAX_LENGTH = 127;

  @Getter
  @Transient
  private String key;

  @Getter
  @Column(nullable = false, unique = true)
  private String hashedKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  @Getter
  private UserProfile userProfile;

  @Getter
  private boolean disabled = false;

  @Column(name = "last_access_time")
  @Getter
  private Instant lastAccessTime;

  @Getter
  @Column(length = DESCRIPTION_MAX_LENGTH)
  private String description;

  @SuppressWarnings("unused")
  ApiKey() {}

  ApiKey(UserProfile userProfile, String description) {
    if (description == null || description.isBlank() || description.length() > DESCRIPTION_MAX_LENGTH) {
      throw new IllegalArgumentException("Invalid API key description");
    }
    this.key = generateApiKey();
    this.hashedKey = passwordEncoder.encode(key);
    this.description = description;
    this.userProfile = userProfile;
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

  public void accessed() {
    lastAccessTime = Instant.now();
  }

  public boolean check(String key) {
    if (key == null || key.isBlank()) throw new ProvidedApiKeyIsBlankException();
    return passwordEncoder.matches(key, hashedKey);
  }

  private static String generateApiKey() {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes);
  }
}
