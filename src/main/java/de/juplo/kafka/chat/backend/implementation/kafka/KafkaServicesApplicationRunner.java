package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "kafka")
@Component
@Slf4j
public class KafkaServicesApplicationRunner implements ApplicationRunner
{
  @Autowired
  ChatBackendProperties properties;

  @Autowired
  ThreadPoolTaskExecutor taskExecutor;
  @Autowired
  ConfigurableApplicationContext context;

  @Autowired
  ChatRoomChannel chatRoomChannel;
  @Autowired
  Consumer<String, AbstractMessageTo> chatRoomChannelConsumer;

  CompletableFuture<Void> chatRoomChannelConsumerJob;


  @Override
  public void run(ApplicationArguments args) throws Exception
  {
    List<String> topics = List.of(properties.getKafka().getChatRoomChannelTopic());
    chatRoomChannelConsumer.subscribe(topics, chatRoomChannel);
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
}
