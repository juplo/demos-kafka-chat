package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHomeService;
import de.juplo.kafka.chat.backend.implementation.StorageStrategy;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;


@SpringBootApplication
public class ChatBackendApplication implements WebFluxConfigurer
{
	@Autowired
	ChatBackendProperties properties;
	@Autowired
	ChatHomeService chatHomeService;
	@Autowired
	StorageStrategy storageStrategy;


	@Override
	public void addCorsMappings(CorsRegistry registry)
	{
		registry
				.addMapping("/**")
				.allowedOrigins(properties.getAllowedOrigins());
	}

	@PreDestroy
	public void onExit()
	{
		storageStrategy.write(chatHomeService);
	}

	public static void main(String[] args)
	{
		SpringApplication.run(ChatBackendApplication.class, args);
	}
}
