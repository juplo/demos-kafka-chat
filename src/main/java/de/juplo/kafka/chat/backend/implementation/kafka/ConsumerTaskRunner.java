package de.juplo.kafka.chat.backend.implementation.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class ConsumerTaskRunner
{
  private final ConsumerTaskExecutor infoChannelConsumerTaskExecutor;
  private final ConsumerTaskExecutor dataChannelConsumerTaskExecutor;

  public void executeConsumerTasks()
  {
    infoChannelConsumerTaskExecutor.executeConsumerTask();
    dataChannelConsumerTaskExecutor.executeConsumerTask();
  }

  public void joinConsumerTasks()
  {
    dataChannelConsumerTaskExecutor.joinConsumerTaskJob();
    infoChannelConsumerTaskExecutor.joinConsumerTaskJob();
  }
}
