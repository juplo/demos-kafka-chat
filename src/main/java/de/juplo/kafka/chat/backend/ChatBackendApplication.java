package de.juplo.kafka.chat.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;


@SpringBootApplication
public class ChatBackendApplication
{
	@Bean
	public Clock clock()
	{
		return Clock.systemDefaultZone();
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ChatBackendApplication.class, args);
	}
}
