package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.data.mongodb.host=localhost",
				"spring.data.mongodb.database=test",
				"chat.backend.inmemory.sharding-strategy=none",
				"chat.backend.inmemory.storage-strategy=mongodb" })
@Testcontainers
@Slf4j
class InMemoryWithMongoDbApplicationStartupIT extends AbstractApplicationStartupIT
{
	private static final int MONGODB_PORT = 27017;

	@Container
	private static final GenericContainer CONTAINER =
			new GenericContainer("mongo:6").withExposedPorts(MONGODB_PORT);

	@DynamicPropertySource
	static void addMongoPortProperty(DynamicPropertyRegistry registry)
	{
		registry.add("spring.data.mongodb.port", () -> CONTAINER.getMappedPort(MONGODB_PORT));
	}

	@BeforeEach
	void setUpLogging()
	{
		Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
		CONTAINER.followOutput(logConsumer);
	}
}
