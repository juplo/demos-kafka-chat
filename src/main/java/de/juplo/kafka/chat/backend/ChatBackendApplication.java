package de.juplo.kafka.chat.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;


@SpringBootApplication
public class ChatBackendApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(ChatBackendApplication.class, args);
	}
}
