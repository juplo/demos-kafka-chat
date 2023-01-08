package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
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
	ChatHome chatHome;
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
		storageStrategy.writeChatrooms(chatHome.list());
	}

	public static void main(String[] args)
	{
		SpringApplication.run(ChatBackendApplication.class, args);
	}
}
