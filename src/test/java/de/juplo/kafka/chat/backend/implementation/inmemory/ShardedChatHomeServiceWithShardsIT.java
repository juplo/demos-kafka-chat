package de.juplo.kafka.chat.backend.implementation.inmemory;

import de.juplo.kafka.chat.backend.domain.AbstractChatHomeServiceWithShardsIT;
import org.springframework.test.context.TestPropertySource;

import static de.juplo.kafka.chat.backend.domain.AbstractChatHomeServiceWithShardsIT.NUM_SHARDS;
import static de.juplo.kafka.chat.backend.domain.AbstractChatHomeServiceWithShardsIT.OWNED_SHARD;


@TestPropertySource(properties = {
    "chat.backend.inmemory.sharding-strategy=kafkalike",
    "chat.backend.inmemory.num-shards=" + NUM_SHARDS,
    "chat.backend.inmemory.owned-shards=" + OWNED_SHARD,
    "chat.backend.inmemory.storage-strategy=files",
    "chat.backend.inmemory.storage-directory=target/test-classes/data/files" })
public class ShardedChatHomeServiceWithShardsIT extends AbstractChatHomeServiceWithShardsIT
{
}
