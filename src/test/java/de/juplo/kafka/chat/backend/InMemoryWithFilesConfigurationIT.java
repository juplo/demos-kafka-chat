package de.juplo.kafka.chat.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;


@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "chat.backend.storage-directory=target/test-classes/data/files")
class InMemoryWithFilesConfigurationIT
{
	@LocalServerPort
	private int port;
	@Autowired
	private WebTestClient webTestClient;

	@Test
	void contextLoads()
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
							.uri("http://localhost:{port}/618e89ae-fdc0-4c65-8055-f49908295e8f", port)
							.accept(MediaType.APPLICATION_JSON)
							.exchange()
							.expectStatus().isOk()
							.expectBody().jsonPath("$.name").isEqualTo("Peter's Chat-Room");
					webTestClient
							.get()
							.uri("http://localhost:{port}/618e89ae-fdc0-4c65-8055-f49908295e8f/ute/1478", port)
							.accept(MediaType.APPLICATION_JSON)
							.exchange()
							.expectStatus().isOk()
							.expectBody().jsonPath("$.text").isEqualTo("Nachricht Nr. 1478");
				});
	}
}
