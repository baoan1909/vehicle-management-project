package com.ban.vehicle_management.infrastructure.config.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class AppInstantSerializer extends JsonSerializer<Instant> {

    private final ZoneId zoneId;

    public AppInstantSerializer(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
    }

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.atZone(zoneId)));
    }
}
