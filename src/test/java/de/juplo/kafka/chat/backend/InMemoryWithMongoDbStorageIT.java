package de.juplo.kafka.chat.backend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"chat.backend.storage=mongodb",
				"spring.data.mongodb.host=localhost",
				"spring.data.mongodb.database=test" })
@Testcontainers
@Slf4j
class InMemoryWithMongoDbStorageIT
{
	@LocalServerPort
	private int port;
	@Autowired
	private WebTestClient webTestClient;

	@Test
	@DisplayName("The app starts, the data is restored and accessible")
	void test()
	{
		Awaitility
				.await()
				.atMost(Duration.ofSeconds(15))
				.untilAsserted(() ->
				{
					webTestClient
							.get()
							.uri("http://localhost:{port}/actuator/health", port)
							.exchange()
							.expectStatus().isOk()
							.expectBody().jsonPath("$.status").isEqualTo("UP");
					webTestClient
							.get()
							.uri("http://localhost:{port}/4ace69b7-a79f-481b-ad0d-d756d60b66ec", port)
							.accept(MediaType.APPLICATION_JSON)
							.exchange()
							.expectStatus().isOk()
							.expectBody().jsonPath("$.name").isEqualTo("FOO");
					webTestClient
							.get()
							.uri("http://localhost:{port}/4ace69b7-a79f-481b-ad0d-d756d60b66ec/ute/1", port)
							.accept(MediaType.APPLICATION_JSON)
							.exchange()
							.expectStatus().isOk()
							.expectBody().jsonPath("$.text").isEqualTo("Ich bin Ute...");
					webTestClient
							.get()
							.uri("http://localhost:{port}/4ace69b7-a79f-481b-ad0d-d756d60b66ec/peter/1", port)
							.accept(MediaType.APPLICATION_JSON)
							.exchange()
							.expectStatus().isOk()
							.expectBody().jsonPath("$.text").isEqualTo("Hallo, ich heiße Peter!");
				});
	}

	private static final int MONGODB_PORT = 27017;

	@Container
	private static final GenericContainer CONTAINER =
			new GenericContainer("mongo:6")
					.withClasspathResourceMapping(
							"data/mongodb",
							"/docker-entrypoint-initdb.d",
							BindMode.READ_ONLY)
					.withExposedPorts(MONGODB_PORT);

	@DynamicPropertySource
	static void addMongoPortProperty(DynamicPropertyRegistry registry)
	{
		registry.add("spring.data.mongodb.port", () -> CONTAINER.getMappedPort(27017));
	}

	@BeforeEach
	void setUpLogging()
	{
		Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
		CONTAINER.followOutput(logConsumer);
	}
}
