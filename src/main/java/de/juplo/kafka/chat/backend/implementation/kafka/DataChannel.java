package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.*;
import de.juplo.kafka.chat.backend.domain.exceptions.LoadInProgressException;
import de.juplo.kafka.chat.backend.domain.exceptions.ShardNotOwnedException;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.data.EventChatMessageReceivedTo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.*;
import java.util.stream.IntStream;


@Slf4j
public class DataChannel implements Runnable, ConsumerRebalanceListener
{
  private final String instanceId;
  private final String topic;
  private final Producer<String, AbstractMessageTo> producer;
  private final Consumer<String, AbstractMessageTo> consumer;
  private final ZoneId zoneId;
  private final int numShards;
  private final int bufferSize;
  private final Clock clock;
  private final boolean[] isShardOwned;
  private final long[] currentOffset;
  private final long[] nextOffset;
  private final Map<UUID, ChatRoomData>[] chatRoomData;
  private final InfoChannel infoChannel;
  private final ShardingPublisherStrategy shardingPublisherStrategy;

  private boolean running;
  @Getter
  private volatile boolean loadInProgress;


  public DataChannel(
    String instanceId,
    String topic,
    Producer<String, AbstractMessageTo> producer,
    Consumer<String, AbstractMessageTo> dataChannelConsumer,
    ZoneId zoneId,
    int numShards,
    int bufferSize,
    Clock clock,
    InfoChannel infoChannel,
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
    this.bufferSize = bufferSize;
    this.clock = clock;
    this.isShardOwned = new boolean[numShards];
    this.currentOffset = new long[numShards];
    this.nextOffset = new long[numShards];
    this.chatRoomData = new Map[numShards];
    IntStream
        .range(0, numShards)
        .forEach(shard -> this.chatRoomData[shard] = new HashMap<>());
    this.infoChannel = infoChannel;
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
    loadInProgress = true;

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
      infoChannel.sendShardAssignedEvent(partition);
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
              throwable))
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
      infoChannel.sendShardRevokedEvent(partition);
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
        ConsumerRecords<String, AbstractMessageTo> records = consumer.poll(Duration.ofMinutes(1));
        log.info("Fetched {} messages", records.count());

        if (loadInProgress)
        {
          loadChatRoomData(records);

          if (isLoadingCompleted())
          {
            log.info("Loading of messages completed! Pausing all owned partitions...");
            pauseAllOwnedPartions();
            log.info("Resuming normal operations...");
            loadInProgress = false;
          }
        }
        else
        {
          if (!records.isEmpty())
          {
            throw new IllegalStateException("All owned partitions should be paused, when no load is in progress!");
          }
        }
      }
      catch (WakeupException e)
      {
        log.info("Received WakeupException, exiting!");
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

    ChatRoomData chatRoomData = this
        .chatRoomData[partition]
        .computeIfAbsent(chatRoomId, this::computeChatRoomData);
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

  Mono<ChatRoomData> getChatRoomData(int shard, UUID id)
  {
    if (loadInProgress)
    {
      return Mono.error(new LoadInProgressException());
    }

    if (!isShardOwned[shard])
    {
      return Mono.error(new ShardNotOwnedException(instanceId, shard));
    }

    return infoChannel
        .getChatRoomInfo(id)
        .map(chatRoomInfo ->
            chatRoomData[shard].computeIfAbsent(id, this::computeChatRoomData));
  }

  private ChatRoomData computeChatRoomData(UUID chatRoomId)
  {
    log.info("Creating ChatRoom {} with buffer-size {}", chatRoomId, bufferSize);
    KafkaChatMessageService service = new KafkaChatMessageService(this, chatRoomId);
    return new ChatRoomData(clock, service, bufferSize);
  }

  ConsumerGroupMetadata getConsumerGroupMetadata()
  {
    return consumer.groupMetadata();
  }
}
