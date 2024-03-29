package de.juplo.kafka.chat.backend.storage;

import de.juplo.kafka.chat.backend.ChatBackendProperties;
import de.juplo.kafka.chat.backend.domain.*;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import de.juplo.kafka.chat.backend.implementation.inmemory.InMemoryServicesConfiguration;
import de.juplo.kafka.chat.backend.storage.files.FilesStorageConfiguration;
import de.juplo.kafka.chat.backend.storage.mongodb.MongoDbStorageConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static pl.rzrz.assertj.reactor.Assertions.*;


@SpringJUnitConfig(classes = {
    InMemoryServicesConfiguration.class,
    FilesStorageConfiguration.class,
    MongoDbStorageConfiguration.class,
    AbstractStorageStrategyIT.TestConfig.class })
@EnableConfigurationProperties(ChatBackendProperties.class)
@Slf4j
public abstract class AbstractStorageStrategyIT
{
  @Autowired
  ChatHomeService chathome;
  @Autowired
  StorageStrategy storageStrategy;


  abstract void restore();

  void store()
  {
    storageStrategy
        .write(chathome)
        .block();
  }

  @Test
  void testStoreAndRecreate()
  {
    restore();

    assertThat(chathome.getChatRoomInfo().toStream()).hasSize(0);

    UUID chatRoomId = UUID.fromString("5c73531c-6fc4-426c-adcb-afc5c140a0f7");
    ChatRoomInfo info = chathome.createChatRoom(chatRoomId, "FOO").block();
    log.debug("Created chat-room {}", info);
    ChatRoomData chatroom = chathome.getChatRoomData(chatRoomId).block();
    Message m1 = chatroom.addMessage(1l,"peter", "Hallo, ich heiße Peter!").block();
    Message m2 = chatroom.addMessage(1l, "ute", "Ich bin Ute...").block();
    Message m3 = chatroom.addMessage(2l, "peter", "Willst du mit mir gehen?").block();
    Message m4 = chatroom.addMessage(1l, "klaus", "Ja? Nein? Vielleicht??").block();

    assertThat(chathome.getChatRoomInfo().toStream()).containsExactlyElementsOf(List.of(info));
    assertThat(chathome.getChatRoomInfo(chatRoomId)).emitsExactly(info);
    assertThat(chathome
        .getChatRoomData(chatRoomId)
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(m1, m2, m3, m4);

    store();
    restore();

    assertThat(chathome.getChatRoomInfo().toStream()).containsExactlyElementsOf(List.of(info));
    assertThat(chathome.getChatRoomInfo(chatRoomId)).emitsExactly(info);
    assertThat(chathome
        .getChatRoomData(chatRoomId)
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(m1, m2, m3, m4);
  }

  @Test
  void testStoreAndRecreateParallelChatRooms()
  {
    restore();

    assertThat(chathome.getChatRoomInfo().toStream()).hasSize(0);

    UUID chatRoomAId = UUID.fromString("5c73531c-6fc4-426c-adcb-afc5c140a0f7");
    ChatRoomInfo infoA = chathome.createChatRoom(chatRoomAId, "FOO").block();
    log.debug("Created chat-room {}", infoA);
    ChatRoomData chatroomA = chathome.getChatRoomData(chatRoomAId).block();
    Message ma1 = chatroomA.addMessage(1l,"peter", "Hallo, ich heiße Peter!").block();
    Message ma2 = chatroomA.addMessage(1l, "ute", "Ich bin Ute...").block();
    Message ma3 = chatroomA.addMessage(2l, "peter", "Willst du mit mir gehen?").block();
    Message ma4 = chatroomA.addMessage(1l, "klaus", "Ja? Nein? Vielleicht??").block();

    UUID chatRoomBId = UUID.fromString("8763dfdc-4dda-4a74-bea4-4b389177abea");
    ChatRoomInfo infoB = chathome.createChatRoom(chatRoomBId, "BAR").block();
    log.debug("Created chat-room {}", infoB);
    ChatRoomData chatroomB = chathome.getChatRoomData(chatRoomBId).block();
    Message mb1 = chatroomB.addMessage(1l,"peter", "Hallo, ich heiße Uwe!").block();
    Message mb2 = chatroomB.addMessage(1l, "ute", "Ich bin Ute...").block();
    Message mb3 = chatroomB.addMessage(1l, "klaus", "Willst du mit mir gehen?").block();
    Message mb4 = chatroomB.addMessage(2l, "peter", "Hä? Was jetzt?!? Isch glohb isch höb ühn däjah vüh...").block();

    assertThat(chathome.getChatRoomInfo().toStream()).containsExactlyInAnyOrderElementsOf(List.of(infoA, infoB));
    assertThat(chathome.getChatRoomInfo(chatRoomAId)).emitsExactly(infoA);
    assertThat(chathome
        .getChatRoomData(chatRoomAId)
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(ma1, ma2, ma3, ma4);
    assertThat(chathome.getChatRoomData(chatRoomBId)).emitsExactly(chatroomB);
    assertThat(chathome
        .getChatRoomData(chatRoomBId)
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(mb1, mb2, mb3, mb4);

    store();
    restore();

    assertThat(chathome.getChatRoomInfo().toStream()).containsExactlyInAnyOrderElementsOf(List.of(infoA, infoB));
    assertThat(chathome.getChatRoomInfo(chatRoomAId)).emitsExactly(infoA);
    assertThat(chathome
        .getChatRoomData(chatRoomAId)
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(ma1, ma2, ma3, ma4);
    assertThat(chathome.getChatRoomInfo(chatRoomBId)).emitsExactly(infoB);
    assertThat(chathome
        .getChatRoomData(chatRoomBId)
        .flatMapMany(cr -> cr.getMessages())).emitsExactly(mb1, mb2, mb3, mb4);
  }


  static class TestConfig
  {
    @Bean
    Clock clock()
    {
      return Clock.systemDefaultZone();
    }
  }
}
