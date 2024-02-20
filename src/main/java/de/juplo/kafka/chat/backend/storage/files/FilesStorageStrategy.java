package de.juplo.kafka.chat.backend.storage.files;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatRoomInfoTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import de.juplo.kafka.chat.backend.domain.ChatRoomInfo;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import de.juplo.kafka.chat.backend.implementation.ShardingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

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
  private final String loggingCategory = FilesStorageStrategy.class.getSimpleName();
  private final Level loggingLevel;
  private final boolean showOperatorLine;


  @Override
  public Flux<ChatRoomInfo> writeChatRoomInfo(Flux<ChatRoomInfo> chatRoomInfoFlux)
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

      return chatRoomInfoFlux
          .log(
              loggingCategory,
              loggingLevel,
              showOperatorLine)
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
          .map(chatRoomInfo ->
          {
            try
            {
              ChatRoomInfoTo chatRoomInfoTo = ChatRoomInfoTo.from(chatRoomInfo);
              generator.writeObject(chatRoomInfoTo);
              return chatRoomInfo;
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
        .log(
            loggingCategory,
            loggingLevel,
            showOperatorLine)
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
  public Flux<Message> writeChatRoomData(
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

      return messageFlux
          .log(
              loggingCategory,
              loggingLevel,
              showOperatorLine)
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
          .map(message ->
          {
            try
            {
              MessageTo messageTo = MessageTo.from(message);
              generator.writeObject(messageTo);
              return message;
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
        .log(
            loggingCategory,
            loggingLevel,
            showOperatorLine)
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
