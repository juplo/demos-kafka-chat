package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
@Slf4j
public class ChannelTaskExecutor
{
  private final ThreadPoolTaskExecutor taskExecutor;
  @Getter
  private final Channel channel;
  private final Consumer<String, AbstractMessageTo> consumer;
  private final WorkAssignor workAssignor;

  CompletableFuture<Void> channelTaskJob;


  public void executeChannelTask()
  {
    workAssignor.assignWork(consumer);
    log.info("Starting the consumer-task for {}", channel);
    channelTaskJob = taskExecutor
        .submitCompletable(channel)
        .exceptionally(e ->
        {
          log.error("The consumer-task for {} exited abnormally!", channel, e);
          return null;
        });
  }

  @PreDestroy
  public void join()
  {
    log.info("Signaling the consumer-task for {} to quit its work", channel);
    consumer.wakeup();
    log.info("Waiting for the consumer of {} to finish its work", channel);
    channelTaskJob.join();
    log.info("Joined the consumer-task for {}", channel);
  }
}
