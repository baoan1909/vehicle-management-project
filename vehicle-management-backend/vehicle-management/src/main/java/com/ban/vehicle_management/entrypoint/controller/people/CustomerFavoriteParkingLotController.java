package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.customerfavoriteparkinglot.mapper.CustomerFavoriteParkingLotApiMapper;
import com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.in.CustomerFavoriteParkingLotPortIn;
import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.request.UpdateCustomerFavoriteParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.response.CustomerFavoriteParkingLotListResponse;
import com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.response.CustomerFavoriteParkingLotResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people/customers/me/favorite-parking-lots")
public class CustomerFavoriteParkingLotController {

    private final CustomerFavoriteParkingLotPortIn favoriteParkingLotPortIn;
    private final CustomerFavoriteParkingLotApiMapper favoriteParkingLotApiMapper;

    public CustomerFavoriteParkingLotController(
            CustomerFavoriteParkingLotPortIn favoriteParkingLotPortIn,
            CustomerFavoriteParkingLotApiMapper favoriteParkingLotApiMapper
    ) {
        this.favoriteParkingLotPortIn = favoriteParkingLotPortIn;
        this.favoriteParkingLotApiMapper = favoriteParkingLotApiMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerFavoriteParkingLotListResponse>> getMyFavoriteParkingLots() {
        Set<UUID> parkingLotIds = favoriteParkingLotPortIn.getMyFavoriteParkingLotIds();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched favorite parking lots successfully",
                favoriteParkingLotApiMapper.toListResponse(parkingLotIds)
        ));
    }

    @PutMapping("/{parkingLotId}")
    public ResponseEntity<ApiResponse<CustomerFavoriteParkingLotResponse>> setMyFavoriteParkingLot(
            @PathVariable UUID parkingLotId,
            @Valid @RequestBody UpdateCustomerFavoriteParkingLotRequest request
    ) {
        CustomerFavoriteParkingLot favoriteParkingLot = favoriteParkingLotPortIn
                .setMyFavoriteParkingLot(parkingLotId, request.favorite());
        return ResponseEntity.ok(ApiResponse.ok(
                request.favorite() ? "Parking lot added to favorites" : "Parking lot removed from favorites",
                favoriteParkingLotApiMapper.toResponse(favoriteParkingLot)
        ));
    }
}
