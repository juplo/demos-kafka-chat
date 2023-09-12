package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.info.EventChatRoomCreated;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;


@Slf4j
public class InfoChannel implements Runnable
{
  private final String topic;
  private final Producer<String, AbstractMessageTo> producer;
  private final Consumer<String, AbstractMessageTo> consumer;
  private final int numShards;
  private final long[] currentOffset;
  private final long[] nextOffset;
  private final Map<UUID, ChatRoomInfo> chatRoomInfo;

  private boolean running;


  public InfoChannel(
    String topic,
    Producer<String, AbstractMessageTo> producer,
    Consumer<String, AbstractMessageTo> infoChannelConsumer)
  {
    log.debug(
        "Creating InfoChannel for topic {}",
        topic);
    this.topic = topic;
    this.consumer = infoChannelConsumer;
    this.producer = producer;
    this.chatRoomInfo = new HashMap<>();

    this.numShards = consumer
        .partitionsFor(topic)
        .size();
    this.currentOffset = new long[numShards];
    this.nextOffset = new long[numShards];
    IntStream
        .range(0, numShards)
        .forEach(partition -> this.nextOffset[partition] = -1l);
  }


  boolean loadInProgress()
  {
    return IntStream
        .range(0, numShards)
        .anyMatch(partition -> nextOffset[partition] < currentOffset[partition]);
  }

  Mono<ChatRoomInfo> sendChatRoomCreatedEvent(
      UUID chatRoomId,
      String name,
      int shard)
  {
    EventChatRoomCreated to = EventChatRoomCreated.of(chatRoomId, name, shard);
    return Mono.create(sink ->
    {
      ProducerRecord<String, AbstractMessageTo> record =
          new ProducerRecord<>(
              topic,
              Integer.toString(shard),
              to);

      producer.send(record, ((metadata, exception) ->
      {
        if (metadata != null)
        {
          log.info("Successfully sent chreate-request for chat room: {}", to);
          ChatRoomInfo chatRoomInfo = new ChatRoomInfo(chatRoomId, name, record.partition());
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


  @Override
  public void run()
  {
    running = true;

    consumer
        .endOffsets(consumer.assignment())
        .entrySet()
        .stream()
        .forEach(entry -> this.currentOffset[entry.getKey().partition()] = entry.getValue());
    IntStream
        .range(0, numShards)
        .forEach(partition -> this.nextOffset[partition] = 0l);

    while (running)
    {
      try
      {
        ConsumerRecords<String, AbstractMessageTo> records = consumer.poll(Duration.ofMinutes(1));
        log.debug("Fetched {} messages", records.count());
        handleMessages(records);
      }
      catch (WakeupException e)
      {
        log.info("Received WakeupException, exiting!");
        running = false;
      }
    }

    log.info("Exiting normally");
  }

  private void handleMessages(ConsumerRecords<String, AbstractMessageTo> records)
  {
    for (ConsumerRecord<String, AbstractMessageTo> record : records)
    {
      switch (record.value().getType())
      {
        case EVENT_CHATROOM_CREATED:
          EventChatRoomCreated eventChatRoomCreated =
              (EventChatRoomCreated) record.value();
          createChatRoom(eventChatRoomCreated.toChatRoomInfo());
          break;

        default:
          log.debug(
              "Ignoring message for key={} with offset={}: {}",
              record.key(),
              record.offset(),
              record.value());
      }

      nextOffset[record.partition()] = record.offset() + 1;
    }
  }

  private void createChatRoom(ChatRoomInfo chatRoomInfo)
  {
    UUID chatRoomId = chatRoomInfo.getId();
    Integer partition = chatRoomInfo.getShard();

    if (this.chatRoomInfo.containsKey(chatRoomId))
    {
      log.warn(
          "Ignoring existing chat-room for {}: {}",
          partition,
          chatRoomId);
    }
    else
    {
      log.info(
          "Adding new chat-room for partition {}: {}",
          partition,
          chatRoomId);

      this.chatRoomInfo.put(chatRoomId, chatRoomInfo);
    }
  }

  Flux<ChatRoomInfo> getChatRoomInfo()
  {
    return Flux.fromIterable(chatRoomInfo.values());
  }

  Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    return Mono.fromSupplier(() -> chatRoomInfo.get(id));
  }
}
