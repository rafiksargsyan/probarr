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
public class BlacklistCleanupTask {

  @Bean
  public RecurringTask<Void> blacklistCleanup(BlacklistCleanupService blacklistCleanupService, Config config) {
    return Tasks.recurring("blacklist-cleanup",
            Schedules.fixedDelay(Duration.ofSeconds(config.blacklistClearIntervalSeconds)))
        .execute((instance, ctx) -> {
          log.info("Running blacklist cleanup");
          blacklistCleanupService.clearFileDownloadFailed();
        });
  }
}
