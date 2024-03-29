package de.juplo.kafka.chat.backend.implementation.kafka;

import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.AbstractMessageTo;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.info.EventChatRoomCreated;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.info.EventShardAssigned;
import de.juplo.kafka.chat.backend.implementation.kafka.messages.info.EventShardRevoked;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;


@ToString(of = { "topic", "instanceUri" })
@Slf4j
public class InfoChannel implements Channel
{
  private final String topic;
  private final Producer<String, AbstractMessageTo> producer;
  private final Consumer<String, AbstractMessageTo> consumer;
  private final Duration pollingInterval;
  private final int numShards;
  private final String[] shardOwners;
  private final long[] currentOffset;
  private final long[] nextOffset;
  private final Map<UUID, ChatRoomInfo> chatRoomInfo;
  private final String instanceUri;
  private final ChannelMediator channelMediator;

  private boolean running;
  @Getter
  private volatile ChannelState channelState = ChannelState.STARTING;


  public InfoChannel(
    String topic,
    Producer<String, AbstractMessageTo> producer,
    Consumer<String, AbstractMessageTo> infoChannelConsumer,
    Duration pollingInterval,
    int numShards,
    URI instanceUri,
    ChannelMediator channelMediator)
  {
    log.debug(
        "Creating InfoChannel for topic {}",
        topic);
    this.topic = topic;
    this.consumer = infoChannelConsumer;
    this.producer = producer;
    this.chatRoomInfo = new HashMap<>();

    this.pollingInterval = pollingInterval;

    this.numShards = numShards;
    this.shardOwners = new String[numShards];
    this.currentOffset = new long[numShards];
    this.nextOffset = new long[numShards];
    IntStream
        .range(0, numShards)
        .forEach(partition -> this.nextOffset[partition] = -1l);

    this.instanceUri = instanceUri.toASCIIString();

    this.channelMediator = channelMediator;
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
        if (exception == null)
        {
          log.info("Successfully sent created event for chat chat-room: {}", to);
          ChatRoomInfo chatRoomInfo = new ChatRoomInfo(chatRoomId, name, shard);
          sink.success(chatRoomInfo);
        }
        else
        {
          // On send-failure
          log.error(
              "Could not send created event for chat-room (id={}, name={}): {}",
              chatRoomId,
              name,
              exception);
          sink.error(exception);
        }
      }));
    });
  }

  void sendShardAssignedEvent(int shard)
  {
    EventShardAssigned to = EventShardAssigned.of(shard, instanceUri);

    ProducerRecord<String, AbstractMessageTo> record =
        new ProducerRecord<>(
            topic,
            Integer.toString(shard),
            to);

    producer.send(record, ((metadata, exception) ->
    {
      if (metadata != null)
      {
        log.info("Successfully sent shard assigned event for shard: {}", shard);
      }
      else
      {
        // On send-failure
        log.error(
            "Could not send shard assigned event for shard {}: {}",
            shard,
            exception);
        // TODO:
        // Verhalten im Fehlerfall durchdenken!
        // Z.B.: unsubscribe() und darauf folgendes (re-)subscribe() des
        // Consumers veranlassen, so dass die nicht öffentlich Bekannte
        // Zuständigkeit abgegeben und neu zugeordnet wird?
        // Falls der Weg gegangen wird: Achtung wegen Sticke Partitions!
      }
    }));
  }

  void sendShardRevokedEvent(int shard)
  {
    EventShardRevoked to = EventShardRevoked.of(shard, instanceUri);

    ProducerRecord<String, AbstractMessageTo> record =
        new ProducerRecord<>(
            topic,
            Integer.toString(shard),
            to);

    producer.send(record, ((metadata, exception) ->
    {
      if (metadata != null)
      {
        log.info("Successfully sent shard revoked event for shard: {}", shard);
      }
      else
      {
        // On send-failure
        log.error(
            "Could not send shard revoked event for shard {}: {}",
            shard,
            exception);
        // TODO:
        // Verhalten im Fehlerfall durchdenken!
        // Ggf. einfach egal, da die neue zuständige Instanz den
        // nicht gelöschten Eintrag eh überschreibt?
      }
    }));
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
    channelState = ChannelState.LOAD_IN_PROGRESS;

    while (running)
    {
      try
      {
        ConsumerRecords<String, AbstractMessageTo> records = consumer.poll(pollingInterval);
        log.debug("Fetched {} messages", records.count());
        for (ConsumerRecord<String, AbstractMessageTo> record : records)
        {
          handleMessage(record);
          this.nextOffset[record.partition()] = record.offset() + 1;
        }
        updateChannelState();
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

  private void updateChannelState()
  {
    if (channelState == ChannelState.LOAD_IN_PROGRESS)
    {
      boolean loadInProgress = IntStream
          .range(0, numShards)
          .anyMatch(shard -> this.nextOffset[shard] < currentOffset[shard]);
      if (!loadInProgress)
      {
        log.info("Loading of info completed! Resuming normal operations...");
        channelState = ChannelState.READY;
      }
    }
  }

  private void handleMessage(ConsumerRecord<String, AbstractMessageTo> record)
  {
    switch (record.value().getType())
    {
      case EVENT_CHATROOM_CREATED:
        EventChatRoomCreated eventChatRoomCreated =
            (EventChatRoomCreated) record.value();
        createChatRoom(eventChatRoomCreated.toChatRoomInfo());
        break;

      case EVENT_SHARD_ASSIGNED:
        EventShardAssigned eventShardAssigned =
            (EventShardAssigned) record.value();
        log.info(
            "Shard {} was assigned to {}",
            eventShardAssigned.getShard(),
            eventShardAssigned.getUri());
        shardOwners[eventShardAssigned.getShard()] = eventShardAssigned.getUri();
        break;

      case EVENT_SHARD_REVOKED:
        EventShardRevoked eventShardRevoked =
            (EventShardRevoked) record.value();
        log.info(
            "Shard {} was revoked from {}",
            eventShardRevoked.getShard(),
            eventShardRevoked.getUri());
        shardOwners[eventShardRevoked.getShard()] = null;
        break;

      default:
        log.debug(
            "Ignoring message for key={} with offset={}: {}",
            record.key(),
            record.offset(),
            record.value());
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
      this.channelMediator.chatRoomCreated(chatRoomInfo);
    }
  }

  Flux<ChatRoomInfo> getChatRoomInfo()
  {
    return Flux.fromIterable(chatRoomInfo.values());
  }

  Mono<ChatRoomInfo> getChatRoomInfo(UUID id)
  {
    ChannelState capturedState = channelState;
    if (capturedState != ChannelState.READY)
    {
      return Mono.error(new ChannelNotReadyException(capturedState));
    }

    return Mono.fromSupplier(() -> chatRoomInfo.get(id));
  }

  Mono<String[]> getShardOwners()
  {
    return Mono.just(shardOwners);
  }
}
