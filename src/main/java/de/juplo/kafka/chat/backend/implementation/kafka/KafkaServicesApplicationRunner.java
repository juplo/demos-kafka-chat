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
  private final ConsumerTaskRunner consumerTaskRunner;


  @Override
  public void run(ApplicationArguments args)
  {
    consumerTaskRunner.executeConsumerTasks();
  }

  @PreDestroy
  public void joinConsumerTasks() throws InterruptedException
  {
    consumerTaskRunner.joinConsumerTasks();
  }
}
