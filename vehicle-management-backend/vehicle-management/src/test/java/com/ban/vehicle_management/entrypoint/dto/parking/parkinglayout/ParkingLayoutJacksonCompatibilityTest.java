package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout;

import static org.assertj.core.api.Assertions.assertThat;

import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request.LayoutElementRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request.SaveLayoutRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.LayoutElementResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ParkingLayoutJacksonCompatibilityTest {

    private final JsonMapper httpMapper = JsonMapper.builder().build();

    @Test
    void shouldDeserializeLayoutElementWithJackson3() {
        LayoutElementRequest request = httpMapper.readValue("""
                {
                  "elementType": "DRIVE_LANE",
                  "geometry": {"x": 120, "points": [0, 20, 40]},
                  "style": {"variant": "ARROW", "fill": "#ffffff"},
                  "label": "Mũi tên",
                  "displayOrder": 1
                }
                """, LayoutElementRequest.class);

        assertThat(request.geometry()).isInstanceOf(Map.class);
        assertThat(request.style()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) request.style()).get("variant")).isEqualTo("ARROW");
    }

    @Test
    void shouldDeserializeSpaceCodeUsingPublicApiFieldName() {
        SaveLayoutRequest request = httpMapper.readValue("""
                {
                  "expectedDraftRevision": 0,
                  "items": [{
                    "parkingSpaceId": null,
                    "spaceCode": "A-01",
                    "vehicleTypeId": null,
                    "x": 50,
                    "y": 70,
                    "width": 50,
                    "height": 100,
                    "rotation": 0
                  }],
                  "elements": []
                }
                """, SaveLayoutRequest.class);

        assertThat(request.items()).hasSize(1);
        assertThat(request.items().getFirst().spaceCode()).isEqualTo("A-01");
    }

    @Test
    void shouldSerializeLayoutElementWithoutExposingJackson2Tree() {
        LayoutElementResponse response = new LayoutElementResponse();
        response.setElementType("WALL");
        response.setGeometry(Map.of("x", 10, "y", 20));
        response.setStyle(Map.of("fill", "#334155"));

        JsonNode json = httpMapper.readTree(httpMapper.writeValueAsString(response));

        assertThat(json.path("geometry").path("x").asInt()).isEqualTo(10);
        assertThat(json.path("geometry").path("y").asInt()).isEqualTo(20);
        assertThat(json.path("style").path("fill").asString()).isEqualTo("#334155");
    }
}
