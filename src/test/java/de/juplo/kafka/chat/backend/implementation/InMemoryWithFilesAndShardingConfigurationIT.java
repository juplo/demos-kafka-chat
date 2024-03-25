package de.juplo.kafka.chat.backend.implementation;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "chat.backend.inmemory.storage-strategy=files",
        "chat.backend.inmemory.storage-directory=target/test-classes/data/files",
        "chat.backend.inmemory.sharding-strategy=kafkalike",
        "chat.backend.inmemory.num-shards=10",
        "chat.backend.inmemory.owned-shards=2" })
class InMemoryWithFilesAndShardingConfigurationIT extends AbstractConfigurationWithShardingIT
{
}
