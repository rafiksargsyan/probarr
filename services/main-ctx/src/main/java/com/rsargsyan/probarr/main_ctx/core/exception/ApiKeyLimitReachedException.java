package com.rsargsyan.probarr.main_ctx.core.exception;

public class ApiKeyLimitReachedException extends DomainException {
  public ApiKeyLimitReachedException() {
    super("API key limit reached");
  }
}
