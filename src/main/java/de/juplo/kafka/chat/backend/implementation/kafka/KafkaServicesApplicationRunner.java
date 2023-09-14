package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaServicesApplicationRunner implements ApplicationRunner
{
  private final ThreadPoolTaskExecutor taskExecutor;
  private final ChatRoomChannel chatRoomChannel;
  private final Consumer<String, AbstractMessageTo> chatRoomChannelConsumer;
  private final WorkAssignor workAssignor;

  CompletableFuture<Void> chatRoomChannelConsumerJob;


  @Override
  public void run(ApplicationArguments args) throws Exception
  {
    workAssignor.assignWork(chatRoomChannelConsumer);
    log.info("Starting the consumer for the ChatRoomChannel");
    chatRoomChannelConsumerJob = taskExecutor
        .submitCompletable(chatRoomChannel)
        .exceptionally(e ->
        {
          log.error("The consumer for the ChatRoomChannel exited abnormally!", e);
          return null;
        });
  }

  @PreDestroy
  public void joinChatRoomChannelConsumerJob()
  {
    log.info("Signaling the consumer of the CahtRoomChannel to quit its work");
    chatRoomChannelConsumer.wakeup();
    log.info("Waiting for the consumer of the ChatRoomChannel to finish its work");
    chatRoomChannelConsumerJob.join();
    log.info("Joined the consumer of the ChatRoomChannel");
  }


  interface WorkAssignor
  {
    void assignWork(Consumer<?, ?> consumer);
  }
}
