package com.ban.vehicle_management.infrastructure.config.time;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public final class AppInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (JsonToken.VALUE_STRING.equals(token)) {
            String rawValue = parser.getText();
            if (rawValue == null || rawValue.isBlank()) {
                return null;
            }

            String value = rawValue.trim();
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignored) {
                try {
                    return Instant.parse(value);
                } catch (DateTimeParseException exception) {
                    throw InvalidFormatException.from(
                            parser,
                            "Timestamp must be ISO-8601 and include an offset or Z",
                            value,
                            Instant.class
                    );
                }
            }
        }

        if (JsonToken.VALUE_NUMBER_INT.equals(token)) {
            return Instant.ofEpochSecond(parser.getLongValue());
        }

        return (Instant) context.handleUnexpectedToken(Instant.class, parser);
    }
}
