package de.juplo.kafka.chat.backend.persistence.storage.files;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.juplo.kafka.chat.backend.api.ChatRoomTo;
import de.juplo.kafka.chat.backend.api.MessageTo;
import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.domain.Message;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;


@RequiredArgsConstructor
@Slf4j
public class FilesStorageStrategy implements StorageStrategy
{
  public static final String CHATROOMS_FILENAME = "chatrooms.json";


  private final Path storagePath;
  private final Clock clock;
  private final int bufferSize;
  private final ChatRoomServiceFactory factory;
  private final ObjectMapper mapper;


  @Override
  public void write(Flux<ChatRoom> chatroomFlux)
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

      chatroomFlux
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
          .subscribe(chatroom ->
          {
            try
            {
              ChatRoomTo chatroomTo = ChatRoomTo.from(chatroom);
              generator.writeObject(chatroomTo);
              writeMessages(chatroomTo, chatroom.getMessages());
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
  public Flux<ChatRoom> read()
  {
    JavaType type = mapper.getTypeFactory().constructType(ChatRoomTo.class);
    return Flux
        .from(new JsonFilePublisher<ChatRoomTo>(chatroomsPath(), mapper, type))
        .log()
        .map(chatRoomTo -> new ChatRoom(
              chatRoomTo.getId(),
              chatRoomTo.getName(),
              clock,
              factory.create(readMessages(chatRoomTo)),
              bufferSize));
  }

  public void writeMessages(ChatRoomTo chatroomTo, Flux<Message> messageFlux)
  {
    Path path = chatroomPath(chatroomTo);
    log.info("Writing messages for {} to {}", chatroomTo, path);
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

  public Flux<Message> readMessages(ChatRoomTo chatroomTo)
  {
    JavaType type = mapper.getTypeFactory().constructType(MessageTo.class);
    return Flux
        .from(new JsonFilePublisher<MessageTo>(chatroomPath(chatroomTo), mapper, type))
        .log()
        .map(MessageTo::toMessage);
  }

  Path chatroomsPath()
  {
    return storagePath.resolve(Path.of(CHATROOMS_FILENAME));
  }

  Path chatroomPath(ChatRoomTo chatroomTo)
  {
    return storagePath.resolve(Path.of(chatroomTo.getId().toString() + ".json"));
  }
}
