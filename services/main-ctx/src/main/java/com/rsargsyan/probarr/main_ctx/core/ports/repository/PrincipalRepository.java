package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Principal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrincipalRepository extends JpaRepository<Principal, Long> {
  Optional<Principal> findByExternalId(String externalId);
}
