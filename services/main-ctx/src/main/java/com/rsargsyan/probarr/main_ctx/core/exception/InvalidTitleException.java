package com.rsargsyan.probarr.main_ctx.core.exception;

public class InvalidTitleException extends DomainException {
  public InvalidTitleException() {
    super("Title must not be blank");
  }
}
