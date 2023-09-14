package de.juplo.kafka.chat.backend.implementation.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final ConsumerTaskExecutor chatRoomChannelTaskExecutor;


  @Override
  public void run(ApplicationArguments args) throws Exception
  {
    chatRoomChannelTaskExecutor.executeConsumerTask();
  }
}
