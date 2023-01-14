package de.juplo.kafka.chat.backend.persistence.storage.mongodb;

import de.juplo.kafka.chat.backend.domain.ChatRoom;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import de.juplo.kafka.chat.backend.persistence.storage.files.ChatRoomServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
public class MongoDbStorageStrategy implements StorageStrategy
{
  private final ChatRoomRepository repository;
  private final Clock clock;
  private final int bufferSize;
  private final ChatRoomServiceFactory factory;


  @Override
  public void write(Flux<ChatRoom> chatroomFlux)
  {
    chatroomFlux
        .map(ChatRoomTo::from)
        .subscribe(chatroomTo -> repository.save(chatroomTo));
  }

  @Override
  public Flux<ChatRoom> read()
  {
    return Flux
        .fromIterable(repository.findAll())
        .map(chatRoomTo ->
        {
          UUID chatRoomId = UUID.fromString(chatRoomTo.getId());
          return new ChatRoom(
              chatRoomId,
              chatRoomTo.getName(),
              clock,
              factory.create(
                  Flux
                      .fromIterable(chatRoomTo.getMessages())
                      .map(messageTo -> messageTo.toMessage())),
              bufferSize);
        });
  }
}
