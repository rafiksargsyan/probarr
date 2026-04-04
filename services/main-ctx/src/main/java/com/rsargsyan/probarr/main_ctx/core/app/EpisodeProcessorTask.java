package com.rsargsyan.probarr.main_ctx.core.app;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class EpisodeProcessorTask {

  @Bean
  public RecurringTask<Void> episodeProcessor(EpisodeProcessorService episodeProcessorService) {
    return Tasks.recurring("episode-processor", Schedules.fixedDelay(Duration.ofMinutes(1)))
        .execute((instance, ctx) -> {
          log.info("Running episode processor");
          episodeProcessorService.processEpisodesWithPendingCandidates();
        });
  }
}
