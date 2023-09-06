package de.juplo.kafka.chat.backend.implementation.kafka.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class CommandCreateChatRoomToTest
{
  final String json = """
  {
    "name": "Foo-Room!"
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
    CommandCreateChatRoomTo message = mapper.readValue(json, CommandCreateChatRoomTo.class);
    assertThat(message.getName()).isEqualTo("Foo-Room!");
  }
}
