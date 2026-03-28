package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.AudioTrackDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.SubtitleTrackDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Edition;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.ReleaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReleaseService {

  private final ReleaseRepository releaseRepository;
  private final MovieRepository movieRepository;

  @Autowired
  public ReleaseService(ReleaseRepository releaseRepository, MovieRepository movieRepository) {
    this.releaseRepository = releaseRepository;
    this.movieRepository = movieRepository;
  }

  @Transactional
  public ReleaseDTO createRelease(String movieIdStr, String infoHash, ReleaseCreationDTO dto) {
    Long movieId = Util.validateTSID(movieIdStr);
    Movie movie = movieRepository.findById(movieId).orElseThrow(ResourceNotFoundException::new);

    boolean candidateExists = movie.getReleaseCandidates().stream()
        .anyMatch(rc -> rc.infoHash().equals(infoHash));
    if (!candidateExists) {
      throw new ResourceNotFoundException();
    }

    if (releaseRepository.existsByInfoHash(infoHash)) {
      throw new IllegalStateException("Release already exists for infoHash: " + infoHash);
    }

    List<AudioTrack> audioTracks = dto.audioTracks() != null
        ? dto.audioTracks().stream().map(AudioTrackDTO::toEntity).toList()
        : List.of();
    List<SubtitleTrack> subtitleTracks = dto.subtitleTracks() != null
        ? dto.subtitleTracks().stream().map(SubtitleTrackDTO::toEntity).toList()
        : List.of();

    Edition edition = dto.edition() != null ? dto.edition() : Edition.UNKNOWN;
    Release release = new Release(movie, infoHash, dto.filePath(), dto.fileSizeBytes(),
        dto.videoCodec(), dto.resolution(), dto.ripType(), edition, dto.runtimeSeconds(),
        audioTracks, subtitleTracks);
    releaseRepository.save(release);

    movie.onScanCompleted();
    movieRepository.save(movie);

    return ReleaseDTO.from(release);
  }

  public ReleaseDTO getRelease(String infoHash) {
    return ReleaseDTO.from(releaseRepository.findByInfoHash(infoHash)
        .orElseThrow(ResourceNotFoundException::new));
  }
}
