package de.juplo.kafka.chat.backend;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "chat.backend.storage-directory=target/test-classes/data/files")
class InMemoryWithFilesConfigurationIT extends AbstractConfigurationIT
{
}
