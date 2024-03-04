package de.juplo.kafka.chat.backend.implementation.kafka;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
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
public class KafkaServicesApplicationRunner implements ApplicationRunner
{
  private final ChannelTaskRunner channelTaskRunner;


  @Override
  public void run(ApplicationArguments args)
  {
    channelTaskRunner.executeChannels();
  }

  @PreDestroy
  public void joinChannels() throws InterruptedException
  {
    channelTaskRunner.joinChannels();
  }
}
