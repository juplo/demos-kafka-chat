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

  public void joinChannels() throws InterruptedException
  {
    joinChannel(dataChannelTaskExecutor);
    joinChannel(infoChannelTaskExecutor);
  }

  private void joinChannel(
      ChannelTaskExecutor channelTaskExecutor)
      throws InterruptedException
  {
    Channel channel = channelTaskExecutor.getChannel();
    while (channel.getChannelState() != ChannelState.SHUTTING_DOWN)
    {
      log.info("Waiting for {} to shut down...", channel);
      Thread.sleep(1000);
    }
    channelTaskExecutor.join();
  }
}
