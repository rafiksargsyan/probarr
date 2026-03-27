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
public class MovieScannerTask {

  @Bean
  public RecurringTask<Void> movieScanner(MovieScannerService movieScannerService, Config config) {
    return Tasks.recurring("movie-scanner",
//            Schedules.fixedDelay(Duration.ofHours(config.movieScanIntervalHours)))
            Schedules.fixedDelay(Duration.ofSeconds(10)))
        .execute((instance, ctx) -> {
          log.info("Running movie scanner");
          movieScannerService.scanDueMovies();
        });
  }
}
