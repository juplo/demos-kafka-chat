package de.juplo.kafka.chat.backend.implementation.kafka.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class EventChatMessageReceivedToTest
{
  final String json = """
  {
    "id": 1,
    "text": "Hallo, ich heiße Peter!",
    "user": "Peter"
  }""";

  ObjectMapper mapper;

  @BeforeEach
  public void setUp()
  {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  public void testDeserialization() throws Exception
  {
    EventChatMessageReceivedTo message = mapper.readValue(json, EventChatMessageReceivedTo.class);
    assertThat(message.getId()).isEqualTo(1l);
    assertThat(message.getText()).isEqualTo("Hallo, ich heiße Peter!");
    assertThat(message.getUser()).isEqualTo("Peter");
  }
}
