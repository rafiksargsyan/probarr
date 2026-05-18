package com.rsargsyan.probarr.main_ctx.core.exception;

public class ApiKeyNotDisabledException extends DomainException {
  public ApiKeyNotDisabledException() {
    super("API key must be disabled before deletion");
  }
}
