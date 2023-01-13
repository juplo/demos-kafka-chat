package de.juplo.kafka.chat.backend;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "chat.backend.inmemory.storage-directory=target/test-classes/data/files",
        "chat.backend.inmemory.sharding-strategy=kafkalike" })
class InMemoryWithFilesAndShardingConfigurationIT extends AbstractConfigurationIT
{
}
