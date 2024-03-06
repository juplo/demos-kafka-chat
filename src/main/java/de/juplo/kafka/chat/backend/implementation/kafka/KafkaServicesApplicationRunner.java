package de.juplo.kafka.chat.backend.implementation.kafka;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@ConditionalOnProperty(
    prefix = "chat.backend",
    name = "services",
    havingValue = "kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaServicesApplicationRunner implements ApplicationRunner
{
  private final ChannelTaskRunner channelTaskRunner;
  private final Consumer dataChannelConsumer;
  private final Consumer infoChannelConsumer;


  @Override
  public void run(ApplicationArguments args)
  {
    log.info("Executing channel-tasks");
    channelTaskRunner.executeChannels();
  }

  @PreDestroy
  public void joinChannels() throws InterruptedException
  {
    log.info("Closing consumers");
    dataChannelConsumer.close();
    infoChannelConsumer.close();
    log.info("Joining channel-tasks");
    channelTaskRunner.joinChannels();
  }
}
