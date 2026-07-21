package com.rsargsyan.probarr.main_ctx.core.exception;

public class InvalidApiKeyDescriptionException extends DomainException {
  public InvalidApiKeyDescriptionException() {
    super("API key description must be between 1 and 127 characters");
  }
}
