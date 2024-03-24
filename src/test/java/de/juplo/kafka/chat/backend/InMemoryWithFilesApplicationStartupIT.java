package de.juplo.kafka.chat.backend;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"chat.backend.inmemory.sharding-strategy=none",
				"chat.backend.inmemory.storage-strategy=files" })
class InMemoryWithFilesApplicationStartupIT extends AbstractApplicationStartupIT
{
}
