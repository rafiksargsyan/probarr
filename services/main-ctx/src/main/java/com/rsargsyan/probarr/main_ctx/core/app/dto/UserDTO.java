package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.UserProfile;

public record UserDTO(String id, String accountId) {
  public static UserDTO from(UserProfile userProfile) {
    return new UserDTO(userProfile.getStrId(), userProfile.getAccount().getStrId());
  }
}
