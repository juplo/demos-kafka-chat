package de.juplo.kafka.chat.backend.implementation;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "chat.backend.inmemory.storage-strategy=none",
        "chat.backend.inmemory.sharding-strategy=kafkalike",
        "chat.backend.inmemory.num-shards=10",
        "chat.backend.inmemory.owned-shards=2" })
class InMemoryWithNoStorageAndShardingConfigurationIT extends AbstractConfigurationWithShardingIT
{
  @Override
  @Disabled("Chat-Rooms cannot be restored, if storage is disabled")
  void testRestoredChatRoomsCanBeListed() {}

  @Override
  @Disabled("Chat-Rooms cannot be restored, if storage is disabled")
  void testRestoredChatRoomHasExpectedDetails() {}

  @Override
  @Disabled("Chat-Rooms cannot be restored, if storage is disabled")
  void testRestoredMessageForUteHasExpectedText() {}

  @Override
  @Disabled("Chat-Rooms cannot be restored, if storage is disabled")
  void testRestoredMessageForPeterHasExpectedText() {}

  @Override
  @Disabled("Chat-Rooms cannot be restored, if storage is disabled")
  void testListenToRestoredChatRoomYieldsOnlyNewlyAddedMessages() {}
}
