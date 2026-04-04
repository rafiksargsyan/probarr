package com.rsargsyan.probarr.main_ctx.core.app;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.rsargsyan.probarr.main_ctx.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class TVShowScannerTask {

  @Bean
  public RecurringTask<Void> tvShowScanner(TVShowScannerService tvShowScannerService, Config config) {
    return Tasks.recurring("tvshow-scanner",
            Schedules.fixedDelay(Duration.ofSeconds(config.tvShowScanIntervalSeconds)))
        .execute((instance, ctx) -> {
          log.info("Running TV show scanner");
          tvShowScannerService.scanDueShows();
        });
  }

  @Bean
  public RecurringTask<Void> tvShowEnricher(TVShowEnrichmentService tvShowEnrichmentService, Config config) {
    return Tasks.recurring("tvshow-enricher",
            Schedules.fixedDelay(Duration.ofSeconds(config.tvShowEnrichmentIntervalSeconds)))
        .execute((instance, ctx) -> {
          log.info("Running TV show enricher");
          tvShowEnrichmentService.enrichDueTvShows();
        });
  }
}
