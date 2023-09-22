package de.juplo.kafka.chat.backend.implementation.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class ConsumerTaskRunner
{
  private final ConsumerTaskExecutor infoChannelConsumerTaskExecutor;
  private final ConsumerTaskExecutor dataChannelConsumerTaskExecutor;
  private final InfoChannel infoChannel;

  public void executeConsumerTasks()
  {
    infoChannelConsumerTaskExecutor.executeConsumerTask();
    dataChannelConsumerTaskExecutor.executeConsumerTask();
  }

  public void joinConsumerTasks() throws InterruptedException
  {
    dataChannelConsumerTaskExecutor.joinConsumerTaskJob();
    while (infoChannel.loadInProgress())
    {
      log.info("Waiting for {} to finish loading...", infoChannel);
      Thread.sleep(1000);
    }
    infoChannelConsumerTaskExecutor.joinConsumerTaskJob();
  }
}
