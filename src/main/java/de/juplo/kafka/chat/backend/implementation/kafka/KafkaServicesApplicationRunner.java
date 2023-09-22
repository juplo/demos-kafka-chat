package de.juplo.kafka.chat.backend.implementation.kafka;

import jakarta.annotation.PreDestroy;
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
  private final ConsumerTaskRunner consumerTaskRunner;


  @Override
  public void run(ApplicationArguments args) throws Exception
  {
    consumerTaskRunner.executeConsumerTasks();
  }

  @PreDestroy
  public void joinConsumerTasks() throws InterruptedException
  {
    consumerTaskRunner.joinConsumerTasks();
  }
}
