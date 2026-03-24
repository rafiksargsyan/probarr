package com.rsargsyan.probarr.main_ctx.core.exception;

public abstract class DomainException extends RuntimeException {
  public DomainException(String message) {
    super(message);
  }
}
