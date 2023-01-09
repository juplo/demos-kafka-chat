package de.juplo.kafka.chat.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "chat.backend.storage-directory=target/test-classes/data/")
class ChatBackendApplicationTests
{
	@Test
	void contextLoads()
	{
	}
}
