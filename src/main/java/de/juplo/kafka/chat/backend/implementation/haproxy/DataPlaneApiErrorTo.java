package de.juplo.kafka.chat.backend.implementation.haproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public record DataPlaneApiErrorTo(int code, String message)
{
}
