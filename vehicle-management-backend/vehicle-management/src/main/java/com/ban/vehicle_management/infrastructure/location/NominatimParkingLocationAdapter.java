package com.ban.vehicle_management.infrastructure.location;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationPortOut;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NominatimParkingLocationAdapter implements ParkingLocationPortOut {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://nominatim.openstreetmap.org")
            .defaultHeader(HttpHeaders.USER_AGENT, "CoParking/1.0 (parking-location-search)")
            .build();

    @Override
    public List<ParkingLocationSearchResult> search(String query) {
        return request("/search", builder -> builder
                .queryParam("q", query)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 5)
                .queryParam("countrycodes", "vn"));
    }

    @Override
    public List<ParkingLocationSearchResult> reverse(BigDecimal latitude, BigDecimal longitude) {
        return request("/reverse", builder -> builder
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("format", "jsonv2"));
    }

    private List<ParkingLocationSearchResult> request(
            String path,
            java.util.function.Function<org.springframework.web.util.UriBuilder, org.springframework.web.util.UriBuilder> uriCustomizer
    ) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriCustomizer.apply(uriBuilder.path(path)).build())
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                return List.of();
            }
            if ("/reverse".equals(path)) {
                NominatimLocation result = OBJECT_MAPPER.readValue(response, NominatimLocation.class);
                return result.toResult() == null ? List.of() : List.of(result.toResult());
            }
            return Arrays.stream(OBJECT_MAPPER.readValue(response, NominatimLocation[].class))
                    .map(NominatimLocation::toResult)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (RestClientException | JsonProcessingException exception) {
            throw new ConflictException("Parking location search service is unavailable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimLocation(String display_name, String lat, String lon) {
        ParkingLocationSearchResult toResult() {
            try {
                if (display_name == null || lat == null || lon == null) return null;
                return new ParkingLocationSearchResult(display_name, new BigDecimal(lat), new BigDecimal(lon));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }
}
