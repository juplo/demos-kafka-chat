package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoomData;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.domain.ShardingPublisherStrategy;
import de.juplo.kafka.chat.backend.domain.exceptions.ShardNotOwnedException;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.data.EventChatMessageReceivedTo;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;


@ToString(of = { "topic", "instanceId" })
@Slf4j
public class DataChannel implements Channel, ConsumerRebalanceListener
{
  private final String instanceId;
  private final String topic;
  private final Producer<String, AbstractMessageTo> producer;
  private final Consumer<String, AbstractMessageTo> consumer;
  private final ZoneId zoneId;
  private final int numShards;
  private final Duration pollingInterval;
  private final int bufferSize;
  private final Clock clock;
  private final boolean[] isShardOwned;
  private final long[] currentOffset;
  private final long[] nextOffset;
  private final Map<UUID, ChatRoomData>[] chatRoomData;
  private final ChannelMediator channelMediator;
  private final ShardingPublisherStrategy shardingPublisherStrategy;

  private boolean running;
  @Getter
  private volatile ChannelState channelState = ChannelState.STARTING;


  public DataChannel(
    String instanceId,
    String topic,
    Producer<String, AbstractMessageTo> producer,
    Consumer<String, AbstractMessageTo> dataChannelConsumer,
    ZoneId zoneId,
    int numShards,
    Duration pollingInterval,
    int bufferSize,
    Clock clock,
    ChannelMediator channelMediator,
    ShardingPublisherStrategy shardingPublisherStrategy)
  {
    log.debug(
        "{}: Creating DataChannel for topic {} with {} partitions",
        instanceId,
        topic,
        numShards);
    this.instanceId = instanceId;
    this.topic = topic;
    this.consumer = dataChannelConsumer;
    this.producer = producer;
    this.zoneId = zoneId;
    this.numShards = numShards;
    this.pollingInterval = pollingInterval;
    this.bufferSize = bufferSize;
    this.clock = clock;
    this.isShardOwned = new boolean[numShards];
    this.currentOffset = new long[numShards];
    this.nextOffset = new long[numShards];
    this.chatRoomData = new Map[numShards];
    IntStream
        .range(0, numShards)
        .forEach(shard -> this.chatRoomData[shard] = new HashMap<>());
    this.channelMediator = channelMediator;
    this.shardingPublisherStrategy = shardingPublisherStrategy;
  }



  Mono<Message> sendChatMessage(
      UUID chatRoomId,
      Message.MessageKey key,
      LocalDateTime timestamp,
      String text)
  {
    ZonedDateTime zdt = ZonedDateTime.of(timestamp, zoneId);
    return Mono.create(sink ->
    {
      ProducerRecord<String, AbstractMessageTo> record =
          new ProducerRecord<>(
              topic,
              null,
              zdt.toEpochSecond(),
              chatRoomId.toString(),
              EventChatMessageReceivedTo.of(key.getUsername(), key.getMessageId(), text));

      producer.send(record, ((metadata, exception) ->
      {
        if (exception == null)
        {
          // On successful send
          Message message = new Message(key, metadata.offset(), timestamp, text);
          log.info("Successfully send message {}", message);
          sink.success(message);
        }
        else
        {
          // On send-failure
          log.error(
              "Could not send message for chat-room={}, key={}, timestamp={}, text={}: {}",
              chatRoomId,
              key,
              timestamp,
              text,
              exception);
          sink.error(exception);
        }
      }));
    });
  }

  @Override
  public void onPartitionsAssigned(Collection<TopicPartition> partitions)
  {
    log.info("Newly assigned partitions! Pausing normal operations...");
    channelState = ChannelState.LOAD_IN_PROGRESS;

    consumer.endOffsets(partitions).forEach((topicPartition, currentOffset) ->
    {
      int partition = topicPartition.partition();
      isShardOwned[partition] =  true;
      this.currentOffset[partition] = currentOffset;

      log.info(
          "Partition assigned: {} - loading messages: next={} -> current={}",
          partition,
          nextOffset[partition],
          currentOffset);

      consumer.seek(topicPartition, nextOffset[partition]);
      channelMediator.shardAssigned(partition);
      shardingPublisherStrategy
          .publishOwnership(partition)
          .doOnSuccess(instanceId -> log.info(
              "Successfully published instance {} as owner of shard {}",
              instanceId,
              partition))
          .doOnError(throwable -> log.error(
              "Could not publish instance {} as owner of shard {}: {}",
              instanceId,
              partition,
              throwable.toString()))
          .onErrorComplete()
          .block();
    });

    consumer.resume(partitions);
  }

  @Override
  public void onPartitionsRevoked(Collection<TopicPartition> partitions)
  {
    partitions.forEach(topicPartition ->
    {
      int partition = topicPartition.partition();
      isShardOwned[partition] = false;
      nextOffset[partition] = consumer.position(topicPartition);
      log.info("Partition revoked: {} - next={}", partition, nextOffset[partition]);
      channelMediator.shardRevoked(partition);
    });
  }

  @Override
  public void onPartitionsLost(Collection<TopicPartition> partitions)
  {
    log.warn("Lost partitions: {}, partitions");
    // TODO: Muss auf den Verlust anders reagiert werden?
    onPartitionsRevoked(partitions);
  }

  @Override
  public void run()
  {
    running = true;

    while (running)
    {
      try
      {
        ConsumerRecords<String, AbstractMessageTo> records = consumer.poll(pollingInterval);
        log.info("Fetched {} messages", records.count());

        switch (channelState)
        {
          case LOAD_IN_PROGRESS ->
          {
            loadChatRoomData(records);

            if (isLoadingCompleted())
            {
              log.info("Loading of messages completed! Pausing all owned partitions...");
              pauseAllOwnedPartions();
              log.info("Resuming normal operations...");
              channelState = ChannelState.READY;
            }
          }
          case SHUTTING_DOWN -> log.info("Shutdown in progress: ignoring {} fetched messages.", records.count());
          default ->
          {
            if (!records.isEmpty())
            {
              throw new IllegalStateException("All owned partitions should be paused, when in state " + channelState);
            }
          }
        }
      }
      catch (WakeupException e)
      {
        log.info("Received WakeupException, exiting!");
        channelState = ChannelState.SHUTTING_DOWN;
        running = false;
      }
    }

    log.info("Exiting normally");
  }

  private void loadChatRoomData(ConsumerRecords<String, AbstractMessageTo> records)
  {
    for (ConsumerRecord<String, AbstractMessageTo> record : records)
    {
      UUID chatRoomId = UUID.fromString(record.key());

      switch (record.value().getType())
      {
        case EVENT_CHATMESSAGE_RECEIVED:
          Instant instant = Instant.ofEpochSecond(record.timestamp());
          LocalDateTime timestamp = LocalDateTime.ofInstant(instant, zoneId);
          loadChatMessage(
              chatRoomId,
              timestamp,
              record.offset(),
              (EventChatMessageReceivedTo) record.value(),
              record.partition());
          break;

        default:
          log.debug(
              "Ignoring message for chat-room {} with offset {}: {}",
              chatRoomId,
              record.offset(),
              record.value());
      }

      nextOffset[record.partition()] = record.offset() + 1;
    }
  }

  private void loadChatMessage(
      UUID chatRoomId,
      LocalDateTime timestamp,
      long offset,
      EventChatMessageReceivedTo chatMessageTo,
      int partition)
  {
    Message.MessageKey key = Message.MessageKey.of(chatMessageTo.getUser(), chatMessageTo.getId());
    Message message = new Message(key, offset, timestamp, chatMessageTo.getText());

    ChatRoomData chatRoomData = computeChatRoomData(chatRoomId, partition);
    KafkaChatMessageService kafkaChatRoomService =
        (KafkaChatMessageService) chatRoomData.getChatRoomService();

    log.debug(
        "Loaded message from partition={} at offset={}: {}",
        partition,
        offset,
        message);
    kafkaChatRoomService.persistMessage(message);
  }

  private boolean isLoadingCompleted()
  {
    return IntStream
        .range(0, numShards)
        .filter(shard -> isShardOwned[shard])
        .allMatch(shard ->
        {
          TopicPartition partition = new TopicPartition(topic, shard);
          long position = consumer.position(partition);
          return position >= currentOffset[shard];
        });
  }

  private void pauseAllOwnedPartions()
  {
    consumer.pause(IntStream
        .range(0, numShards)
        .filter(shard -> isShardOwned[shard])
        .mapToObj(shard -> new TopicPartition(topic, shard))
        .toList());
  }


  int[] getOwnedShards()
  {
    return IntStream
        .range(0, numShards)
        .filter(shard -> isShardOwned[shard])
        .toArray();
  }

  void createChatRoomData(ChatRoomInfo chatRoomInfo)
  {
    computeChatRoomData(chatRoomInfo.getId(), chatRoomInfo.getShard());
  }

  Mono<ChatRoomData> getChatRoomData(int shard, UUID id)
  {
    ChannelState capturedState = channelState;
    if (capturedState != ChannelState.READY)
    {
      return Mono.error(new ChannelNotReadyException(capturedState));
    }

    if (!isShardOwned[shard])
    {
      return Mono.error(new ShardNotOwnedException(instanceId, shard));
    }

    return Mono.justOrEmpty(chatRoomData[shard].get(id));
  }

  private ChatRoomData computeChatRoomData(UUID chatRoomId, int shard)
  {
    ChatRoomData chatRoomData = this.chatRoomData[shard].get(chatRoomId);

    if (chatRoomData != null)
    {
      log.info(
          "Ignoring request to create already existing ChatRoomData for {}",
          chatRoomId);
    }
    else
    {
      log.info("Creating ChatRoomData {} with buffer-size {}", chatRoomId, bufferSize);
      KafkaChatMessageService service = new KafkaChatMessageService(this, chatRoomId);
      chatRoomData = new ChatRoomData(clock, service, bufferSize);
      this.chatRoomData[shard].put(chatRoomId, chatRoomData);
    }

    return chatRoomData;
  }

  ConsumerGroupMetadata getConsumerGroupMetadata()
  {
    return consumer.groupMetadata();
  }
}
