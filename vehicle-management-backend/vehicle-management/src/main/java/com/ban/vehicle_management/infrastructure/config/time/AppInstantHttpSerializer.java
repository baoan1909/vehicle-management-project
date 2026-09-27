package com.ban.vehicle_management.infrastructure.config.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Jackson 3 serializer used by Spring MVC's HTTP message converter.
 */
public final class AppInstantHttpSerializer extends ValueSerializer<Instant> {

    private final ZoneId zoneId;

    public AppInstantHttpSerializer(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
    }

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializationContext context)
            throws JacksonException {
        generator.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.atZone(zoneId)));
    }
}
