package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.exception.DomainException;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  public record ErrorResponse(String code, String message) {}

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFoundException(ResourceNotFoundException e) {
    return new ResponseEntity<>(new ErrorResponse(e.getClass().getSimpleName(), e.getMessage()),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleGenericDomainException(DomainException e) {
    return new ResponseEntity<>(new ErrorResponse(e.getClass().getSimpleName(), e.getMessage()),
        HttpStatus.BAD_REQUEST);
  }
}
