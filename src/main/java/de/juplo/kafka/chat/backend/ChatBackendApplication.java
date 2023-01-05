package de.juplo.kafka.chat.backend;

import de.juplo.kafka.chat.backend.domain.ChatHome;
import de.juplo.kafka.chat.backend.persistence.StorageStrategy;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;


@SpringBootApplication
public class ChatBackendApplication
{
	@Autowired
	ChatHome chatHome;
	@Autowired
	StorageStrategy storageStrategy;

	@PreDestroy
	public void onExit()
	{
		storageStrategy.writeChatrooms(Flux.fromIterable(chatHome.list()));
	}

	public static void main(String[] args)
	{
		SpringApplication.run(ChatBackendApplication.class, args);
	}
}
