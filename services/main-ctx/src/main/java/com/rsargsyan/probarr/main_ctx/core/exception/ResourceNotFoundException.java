package com.rsargsyan.probarr.main_ctx.core.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException() {
    super("Resource not found");
  }
}
