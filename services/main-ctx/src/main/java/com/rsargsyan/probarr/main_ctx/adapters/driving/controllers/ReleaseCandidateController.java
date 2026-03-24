package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.ReleaseCandidateService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseCandidateCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseCandidateDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/release-candidate")
public class ReleaseCandidateController {

  private final ReleaseCandidateService releaseCandidateService;

  @Autowired
  public ReleaseCandidateController(ReleaseCandidateService releaseCandidateService) {
    this.releaseCandidateService = releaseCandidateService;
  }

  @GetMapping
  public ResponseEntity<List<ReleaseCandidateDTO>> listCandidates(
      @RequestParam(required = false) String movieId,
      @RequestParam(required = false) String episodeId) {
    if (movieId != null) {
      return ResponseEntity.ok(releaseCandidateService.listByMovie(movieId));
    } else if (episodeId != null) {
      return ResponseEntity.ok(releaseCandidateService.listByEpisode(episodeId));
    }
    return ResponseEntity.badRequest().build();
  }

  @PostMapping
  public ResponseEntity<ReleaseCandidateDTO> createCandidate(
      @RequestBody ReleaseCandidateCreationDTO dto) {
    return new ResponseEntity<>(releaseCandidateService.createCandidate(dto), HttpStatus.CREATED);
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ReleaseCandidateDTO> updateStatus(@PathVariable String id,
                                                          @RequestBody CandidateStatus status) {
    return ResponseEntity.ok(releaseCandidateService.updateStatus(id, status));
  }
}
