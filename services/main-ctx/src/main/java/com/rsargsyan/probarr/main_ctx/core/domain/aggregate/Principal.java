package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

@Entity
public class Principal extends AggregateRoot {

  @Column(name = "external_id", unique = true)
  @Getter
  private String externalId;

  @SuppressWarnings("unused")
  Principal() {}

  public Principal(String externalId) {
    if (externalId == null || externalId.isBlank()) throw new IllegalArgumentException("externalId is blank");
    this.externalId = externalId;
  }
}
