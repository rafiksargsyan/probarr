package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
  Optional<AdminProfile> findByExternalId(String externalId);
}
