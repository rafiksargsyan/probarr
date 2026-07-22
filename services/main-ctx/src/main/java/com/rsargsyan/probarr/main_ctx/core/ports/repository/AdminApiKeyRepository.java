package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminApiKeyRepository extends JpaRepository<AdminApiKey, Long> {

  @Query("SELECT k FROM AdminApiKey k JOIN FETCH k.adminProfile WHERE k.id = :id")
  Optional<AdminApiKey> findByIdWithProfile(@Param("id") Long id);
}
