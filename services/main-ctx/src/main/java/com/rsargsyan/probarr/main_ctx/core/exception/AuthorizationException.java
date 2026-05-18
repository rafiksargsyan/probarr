package com.rsargsyan.probarr.main_ctx.core.exception;

public class AuthorizationException extends DomainException {
  public AuthorizationException() {
    super("Not authorized");
  }
}
