package com.ban.vehicle_management.infrastructure.config.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PostgresSessionTimeZoneIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationTimeProperties timeProperties;

    @Test
    void shouldApplyApplicationZoneWithoutChangingTheStoredInstant() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try {
                assertEquals(timeProperties.timeZone(), scalar(statement, "SHOW TIME ZONE"));
                assertEquals(
                        "2026-09-26 15:30:00+07",
                        scalar(statement, "SELECT to_char(TIMESTAMPTZ '2026-09-26T08:30:00Z', 'YYYY-MM-DD HH24:MI:SSOF')")
                );

                Instant applicationZoneInstant = queryInstant(statement);
                statement.execute("SET TIME ZONE 'UTC'");
                Instant utcInstant = queryInstant(statement);

                assertEquals(applicationZoneInstant, utcInstant);
                assertEquals(Instant.parse("2026-09-26T08:30:00Z"), utcInstant);
            } finally {
                statement.execute("SET TIME ZONE '" + timeProperties.timeZone() + "'");
            }
        }
    }

    private String scalar(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private Instant queryInstant(Statement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT TIMESTAMPTZ '2026-09-26T08:30:00Z'"
        )) {
            resultSet.next();
            return resultSet.getObject(1, java.time.OffsetDateTime.class).toInstant();
        }
    }
}
