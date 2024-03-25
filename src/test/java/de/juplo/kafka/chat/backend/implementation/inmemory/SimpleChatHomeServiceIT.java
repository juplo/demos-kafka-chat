package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.domain.ChatHomeServiceTest;
import org.springframework.test.context.TestPropertySource;


@TestPropertySource(properties = {
    "chat.backend.inmemory.sharding-strategy=none",
    "chat.backend.inmemory.storage-strategy=files",
    "chat.backend.inmemory.storage-directory=target/test-classes/data/files" })
public class SimpleChatHomeServiceTest extends ChatHomeServiceTest
{
}
