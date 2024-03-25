package de.juplo.kafka.chat.backend;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"chat.backend.inmemory.sharding-strategy=none",
				"chat.backend.inmemory.storage-strategy=none" })
class InMemoryWithNoStorageConfigurationIT extends AbstractConfigurationIT
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
