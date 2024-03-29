package de.juplo.kafka.chat.backend.implementation;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"chat.backend.inmemory.sharding-strategy=none",
				"chat.backend.inmemory.storage-strategy=files",
				"chat.backend.inmemory.storage-directory=target/test-classes/data/files" })
class InMemoryWithFilesConfigurationIT extends AbstractConfigurationIT
{
}
