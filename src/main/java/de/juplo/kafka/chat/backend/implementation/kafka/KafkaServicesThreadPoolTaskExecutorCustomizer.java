package de.juplo.kafka.chat.backend.implementation.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


/**
 * Customizer for the auto-configured Bean {@code applicationTaskExecutor}.
 *
 * This customization is necessary, because otherwise, the bean is part
 * of the lowest phase of the {@link org.springframework.context.SmartLifecycle}
 * and, hence, destroyed first during shutdown.
 *
 * Without this customization, the shutdown of the thread-pool, that is triggered
 * this way does not succeed, blocking the furthershutdown for 30 seconds.
 */
@Slf4j
public class KafkaServicesThreadPoolTaskExecutorCustomizer implements ThreadPoolTaskExecutorCustomizer
{
  @Override
  public void customize(ThreadPoolTaskExecutor taskExecutor)
  {
    log.info("Customizing the auto-configured ThreadPoolTaskExecutor");
    taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    taskExecutor.setAwaitTerminationSeconds(10);
  }
}
