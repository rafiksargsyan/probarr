package com.rsargsyan.probarr.main_ctx.core.app;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rsargsyan.probarr.main_ctx.Config;
import java.time.Duration;

@Slf4j
@Configuration
public class MovieEnrichmentTask {

  @Bean
  public RecurringTask<Void> movieEnricher(MovieEnrichmentService movieEnrichmentService, Config config) {
    return Tasks.recurring("movie-enricher", Schedules.fixedDelay(Duration.ofSeconds(config.movieEnrichmentIntervalSeconds)))
        .execute((instance, ctx) -> {
          log.info("Running movie enrichment");
          movieEnrichmentService.enrichDueMovies();
        });
  }
}
