package de.juplo.kafka.chat.backend;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"chat.backend.inmemory.sharding-strategy=none",
				"chat.backend.inmemory.num-shards=1",
				"chat.backend.inmemory.owned-shards=0",
				"chat.backend.inmemory.storage-directory=target/test-classes/data/files" })
class InMemoryWithFilesConfigurationIT extends AbstractConfigurationIT
{
}
