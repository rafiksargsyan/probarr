package com.rsargsyan.probarr.main_ctx.core.exception;

public class CandidateAlreadyIndexedException extends DomainException {
  public CandidateAlreadyIndexedException() {
    super("Release candidate has already been indexed");
  }
}
