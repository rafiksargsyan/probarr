package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BlacklistCleanupService {

  private final MovieRepository movieRepository;
  private final EpisodeRepository episodeRepository;

  @Autowired
  public BlacklistCleanupService(MovieRepository movieRepository, EpisodeRepository episodeRepository) {
    this.movieRepository = movieRepository;
    this.episodeRepository = episodeRepository;
  }

  public void clearFileDownloadFailed() {
    List<Long> movieIds = movieRepository.findIdsWithFileDownloadFailedBlacklist();
    for (Long id : movieIds) clearMovieBlacklist(id);
    log.info("Cleared FILE_DOWNLOAD_FAILED blacklist entries for {} movie(s)", movieIds.size());

    List<Long> episodeIds = episodeRepository.findIdsWithFileDownloadFailedBlacklist();
    for (Long id : episodeIds) clearEpisodeBlacklist(id);
    log.info("Cleared FILE_DOWNLOAD_FAILED blacklist entries for {} episode(s)", episodeIds.size());
  }

  @Transactional
  public void clearMovieBlacklist(Long movieId) {
    movieRepository.findById(movieId).ifPresent(movie -> {
      movie.clearBlackListByReason(BlacklistReason.FILE_DOWNLOAD_FAILED);
      movieRepository.save(movie);
    });
  }

  @Transactional
  public void clearEpisodeBlacklist(Long episodeId) {
    episodeRepository.findById(episodeId).ifPresent(episode -> {
      episode.clearBlackListByReason(BlacklistReason.FILE_DOWNLOAD_FAILED);
      episodeRepository.save(episode);
    });
  }
}
