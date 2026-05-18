package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.UserService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ApiKeyCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ApiKeyDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/signup")
  public ResponseEntity<UserDTO> signup() {
    UserContext ctx = UserContextHolder.get();
    return ResponseEntity.ok(userService.signUp(ctx.getExternalId()));
  }

  @GetMapping("/{userId}/api-key")
  public ResponseEntity<List<ApiKeyDTO>> listApiKeys(@PathVariable String userId) {
    String actingUserId = UserContextHolder.get().getUserProfileId();
    return ResponseEntity.ok(userService.listApiKeys(actingUserId, userId));
  }

  @PostMapping("/{userId}/api-key")
  public ResponseEntity<ApiKeyDTO> createApiKey(@PathVariable String userId,
                                                @RequestBody ApiKeyCreationDTO req) {
    String actingUserId = UserContextHolder.get().getUserProfileId();
    return new ResponseEntity<>(userService.createApiKey(actingUserId, userId, req), HttpStatus.CREATED);
  }

  @PutMapping("/{userId}/api-key/{keyId}/disable")
  public ResponseEntity<ApiKeyDTO> disableApiKey(@PathVariable String userId,
                                                 @PathVariable String keyId) {
    String actingUserId = UserContextHolder.get().getUserProfileId();
    return ResponseEntity.ok(userService.disableApiKey(actingUserId, userId, keyId));
  }

  @PutMapping("/{userId}/api-key/{keyId}/enable")
  public ResponseEntity<ApiKeyDTO> enableApiKey(@PathVariable String userId,
                                                @PathVariable String keyId) {
    String actingUserId = UserContextHolder.get().getUserProfileId();
    return ResponseEntity.ok(userService.enableApiKey(actingUserId, userId, keyId));
  }

  @DeleteMapping("/{userId}/api-key/{keyId}")
  public ResponseEntity<Void> deleteApiKey(@PathVariable String userId,
                                           @PathVariable String keyId) {
    String actingUserId = UserContextHolder.get().getUserProfileId();
    userService.deleteApiKey(actingUserId, userId, keyId);
    return ResponseEntity.noContent().build();
  }
}
