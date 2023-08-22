package de.juplo.kafka.chat.backend.persistence.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.domain.ShardNotOwnedException;
import de.juplo.kafka.chat.backend.persistence.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.persistence.kafka.messages.CommandCreateChatRoomTo;
import de.juplo.kafka.chat.backend.persistence.kafka.messages.EventChatMessageReceivedTo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.*;
import java.util.stream.IntStream;


@Slf4j
public class ChatRoomChannel implements Runnable, ConsumerRebalanceListener
{
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
  private final Map<UUID, ChatRoom>[] chatrooms;

  private boolean running;
  @Getter
  private volatile boolean loadInProgress;


  public ChatRoomChannel(
    String topic,
    Producer<String, AbstractMessageTo> producer,
    Consumer<String, AbstractMessageTo> consumer,
    ZoneId zoneId,
    int numShards,
    int bufferSize,
    Clock clock)
  {
    log.debug(
        "Creating ChatRoomChannel for topic {} with {} partitions",
        topic,
        numShards);
    this.topic = topic;
    this.consumer = consumer;
    this.producer = producer;
    this.zoneId = zoneId;
    this.numShards = numShards;
    this.bufferSize = bufferSize;
    this.clock = clock;
    this.isShardOwned = new boolean[numShards];
    this.currentOffset = new long[numShards];
    this.nextOffset = new long[numShards];
    this.chatrooms = new Map[numShards];
    IntStream
        .range(0, numShards)
        .forEach(shard -> this.chatrooms[shard] = new HashMap<>());
  }



  Mono<ChatRoomInfo> sendCreateChatRoomRequest(
      UUID chatRoomId,
      String name)
  {
    CommandCreateChatRoomTo createChatRoomRequestTo = CommandCreateChatRoomTo.of(name);
    return Mono.create(sink ->
    {
      ProducerRecord<String, AbstractMessageTo> record =
          new ProducerRecord<>(
              topic,
              chatRoomId.toString(),
              createChatRoomRequestTo);

      producer.send(record, ((metadata, exception) ->
      {
        if (metadata != null)
        {
          log.info("Successfully send chreate-request for chat room: {}", createChatRoomRequestTo);
          ChatRoomInfo chatRoomInfo = new ChatRoomInfo(chatRoomId, name, record.partition());
          createChatRoom(chatRoomInfo);
          sink.success(chatRoomInfo);
        }
        else
        {
          // On send-failure
          log.error(
              "Could not send create-request for chat room (id={}, name={}): {}",
              chatRoomId,
              name,
              exception);
          sink.error(exception);
        }
      }));
    });
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
        if (metadata != null)
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
      log.info("Partition revoked: {} - next={}", partition, nextOffset[partition]);
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
        ConsumerRecords<String, AbstractMessageTo> records = consumer.poll(Duration.ofMinutes(5));
        log.info("Fetched {} messages", records.count());

        if (loadInProgress)
        {
          loadChatRoom(records);

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

  private void loadChatRoom(ConsumerRecords<String, AbstractMessageTo> records)
  {
    for (ConsumerRecord<String, AbstractMessageTo> record : records)
    {
      UUID chatRoomId = UUID.fromString(record.key());

      switch (record.value().getType())
      {
        case COMMAND_CREATE_CHATROOM:
          createChatRoom(
              chatRoomId,
              (CommandCreateChatRoomTo) record.value(),
              record.partition());
          break;

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

  private void createChatRoom(
      UUID chatRoomId,
      CommandCreateChatRoomTo createChatRoomRequestTo,
      int partition)
  {
    log.info("Loading ChatRoom {} with buffer-size {}", chatRoomId, bufferSize);
    KafkaChatRoomService service = new KafkaChatRoomService(this, chatRoomId);
    ChatRoom chatRoom = new ChatRoom(
        chatRoomId,
        createChatRoomRequestTo.getName(),
        partition,
        clock,
        service,
        bufferSize);
    putChatRoom(chatRoom);
  }


  private void createChatRoom(ChatRoomInfo chatRoomInfo)
  {
    UUID id = chatRoomInfo.getId();
    String name = chatRoomInfo.getName();
    int shard = chatRoomInfo.getShard();
    log.info("Creating ChatRoom {} with buffer-size {}", id, bufferSize);
    KafkaChatRoomService service = new KafkaChatRoomService(this, id);
    ChatRoom chatRoom = new ChatRoom(id, name, shard, clock, service, bufferSize);
    putChatRoom(chatRoom);
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

    ChatRoom chatRoom = chatrooms[partition].get(chatRoomId);
    KafkaChatRoomService kafkaChatRoomService =
        (KafkaChatRoomService) chatRoom.getChatRoomService();

    kafkaChatRoomService.persistMessage(message);
  }

  private boolean isLoadingCompleted()
  {
    return IntStream
        .range(0, numShards)
        .filter(shard -> isShardOwned[shard])
        .allMatch(shard -> nextOffset[shard] >= currentOffset[shard]);
  }

  private void pauseAllOwnedPartions()
  {
    consumer.pause(IntStream
        .range(0, numShards)
        .filter(shard -> isShardOwned[shard])
        .mapToObj(shard -> new TopicPartition(topic, shard))
        .toList());
  }


  private void putChatRoom(ChatRoom chatRoom)
  {
    Integer partition = chatRoom.getShard();
    UUID chatRoomId = chatRoom.getId();
    if (chatrooms[partition].containsKey(chatRoomId))
    {
      log.warn("Ignoring existing chat-room: " + chatRoom);
    }
    else
    {
      log.info(
          "Adding new chat-room to partition {}: {}",
          partition,
          chatRoom);

      chatrooms[partition].put(chatRoomId, chatRoom);
    }
  }

  int[] getOwnedShards()
  {
    return IntStream
        .range(0, numShards)
        .filter(shard -> isShardOwned[shard])
        .toArray();
  }

  Mono<ChatRoom> getChatRoom(int shard, UUID id)
  {
    if (loadInProgress)
    {
      throw new LoadInProgressException(shard);
    }

    if (!isShardOwned[shard])
    {
      throw new ShardNotOwnedException(shard);
    }

    return Mono.justOrEmpty(chatrooms[shard].get(id));
  }

  Flux<ChatRoom> getChatRooms()
  {
    return Flux
        .fromStream(IntStream.range(0, numShards).mapToObj(i -> Integer.valueOf(i)))
        .filter(shard -> isShardOwned[shard])
        .flatMap(shard -> Flux.fromIterable(chatrooms[shard].values()));
  }
}