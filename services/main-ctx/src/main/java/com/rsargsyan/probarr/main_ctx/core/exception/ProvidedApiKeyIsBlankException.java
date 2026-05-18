package com.rsargsyan.probarr.main_ctx.core.exception;

public class ProvidedApiKeyIsBlankException extends DomainException {
  public ProvidedApiKeyIsBlankException() {
    super("Provided API key is blank");
  }
}
