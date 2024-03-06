package de.juplo.kafka.chat.backend.implementation.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class ChannelTaskRunner
{
  private final ChannelTaskExecutor infoChannelTaskExecutor;
  private final ChannelTaskExecutor dataChannelTaskExecutor;

  public void executeChannels()
  {
    infoChannelTaskExecutor.executeChannelTask();
    dataChannelTaskExecutor.executeChannelTask();
  }
}
