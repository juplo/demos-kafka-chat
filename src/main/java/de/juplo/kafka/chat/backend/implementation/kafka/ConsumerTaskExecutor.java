package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
@Slf4j
public class ConsumerTaskExecutor
{
  private final ThreadPoolTaskExecutor taskExecutor;
  private final Runnable consumerTask;
  private final Consumer<String, AbstractMessageTo> consumer;
  private final WorkAssignor workAssignor;

  CompletableFuture<Void> consumerTaskJob;


  public void executeConsumerTask()
  {
    workAssignor.assignWork(consumer);
    log.info("Starting the consumer-task for {}", consumerTask);
    consumerTaskJob = taskExecutor
        .submitCompletable(consumerTask)
        .exceptionally(e ->
        {
          log.error("The consumer-task for {} exited abnormally!", consumerTask, e);
          return null;
        });
  }

  @PreDestroy
  public void joinConsumerTaskJob()
  {
    log.info("Signaling the consumer-task for {} to quit its work", consumerTask);
    consumer.wakeup();
    log.info("Waiting for the consumer of {} to finish its work", consumerTask);
    consumerTaskJob.join();
    log.info("Joined the consumer-task for {}", consumerTask);
  }


  public interface WorkAssignor
  {
    void assignWork(Consumer<?, ?> consumer);
  }
}
