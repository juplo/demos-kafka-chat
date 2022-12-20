package de.juplo.kafka.chatroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;


@SpringBootApplication
public class ChatroomApplication
{
	@Bean
	public Clock clock()
	{
		return Clock.systemDefaultZone();
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ChatroomApplication.class, args);
	}
}
