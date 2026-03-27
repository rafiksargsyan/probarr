package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {
  Optional<Release> findByInfoHash(String infoHash);
  boolean existsByInfoHash(String infoHash);
}
