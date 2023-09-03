package de.juplo.kafka.chat.backend.persistence.storage.files;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.inmemory.ShardingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;


@RequiredArgsConstructor
@Slf4j
public class FilesStorageStrategy implements StorageStrategy
{
  public static final String CHATROOMS_FILENAME = "chatrooms.json";


  private final Path storagePath;
  private final ShardingStrategy shardingStrategy;
  private final ObjectMapper mapper;


  @Override
  public void writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux)
  {
    Path path = chatroomsPath();
    log.info("Writing chatrooms to {}", path);
    try
    {
      Files.createDirectories(storagePath);

      JsonGenerator generator =
          mapper
              .getFactory()
              .createGenerator(Files.newBufferedWriter(path, CREATE, TRUNCATE_EXISTING));

      chatRoomInfoFlux
          .log()
          .doFirst(() ->
          {
            try
            {
              generator.useDefaultPrettyPrinter();
              generator.writeStartArray();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .doOnTerminate(() ->
          {
            try
            {
              generator.writeEndArray();
              generator.close();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .subscribe(chatRoomInfo ->
          {
            try
            {
              ChatRoomInfoTo chatRoomInfoTo = ChatRoomInfoTo.from(chatRoomInfo);
              generator.writeObject(chatRoomInfoTo);
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          });
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Flux<ChatRoomInfo> readChatRoomInfo()
  {
    JavaType type = mapper.getTypeFactory().constructType(ChatRoomInfoTo.class);
    return Flux
        .from(new JsonFilePublisher<ChatRoomInfoTo>(chatroomsPath(), mapper, type))
        .log()
        .map(chatRoomInfoTo ->
        {
          UUID chatRoomId = chatRoomInfoTo.getId();
          int shard = shardingStrategy.selectShard(chatRoomId);

          log.info(
              "{} - old shard: {}, new shard:  {}",
              chatRoomId,
              chatRoomInfoTo.getShard(),
              shard);

          return new ChatRoomInfo(
              chatRoomId,
              chatRoomInfoTo.getName(),
              shard);
        });
  }

  @Override
  public void writeChatRoomData(
      UUID chatRoomId,
      Flux<Message> messageFlux)
  {
    Path path = chatroomPath(chatRoomId);
    log.info("Writing messages for {} to {}", chatRoomId, path);
    try
    {
      Files.createDirectories(storagePath);

      JsonGenerator generator =
          mapper
              .getFactory()
              .createGenerator(Files.newBufferedWriter(path, CREATE, TRUNCATE_EXISTING));

      messageFlux
          .log()
          .doFirst(() ->
          {
            try
            {
              generator.useDefaultPrettyPrinter();
              generator.writeStartArray();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .doOnTerminate(() ->
          {
            try
            {
              generator.writeEndArray();
              generator.close();
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          })
          .subscribe(message ->
          {
            try
            {
              MessageTo messageTo = MessageTo.from(message);
              generator.writeObject(messageTo);
            }
            catch (IOException e)
            {
              throw new RuntimeException(e);
            }
          });
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Flux<Message> readChatRoomData(UUID chatRoomId)
  {
    JavaType type = mapper.getTypeFactory().constructType(MessageTo.class);
    return Flux
        .from(new JsonFilePublisher<MessageTo>(chatroomPath(chatRoomId), mapper, type))
        .log()
        .map(MessageTo::toMessage);
  }

  Path chatroomsPath()
  {
    return storagePath.resolve(Path.of(CHATROOMS_FILENAME));
  }

  Path chatroomPath(UUID id)
  {
    return storagePath.resolve(Path.of(id.toString() + ".json"));
  }
}
