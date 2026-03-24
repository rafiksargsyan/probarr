package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.ReleaseService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/release")
public class ReleaseController {

  private final ReleaseService releaseService;

  @Autowired
  public ReleaseController(ReleaseService releaseService) {
    this.releaseService = releaseService;
  }

  @PostMapping("/{candidateId}")
  public ResponseEntity<ReleaseDTO> createRelease(@PathVariable String candidateId,
                                                  @RequestBody ReleaseDTO dto) {
    return new ResponseEntity<>(releaseService.createRelease(candidateId, dto), HttpStatus.CREATED);
  }

  @GetMapping("/{candidateId}")
  public ResponseEntity<ReleaseDTO> getRelease(@PathVariable String candidateId) {
    return ResponseEntity.ok(releaseService.getRelease(candidateId));
  }
}
