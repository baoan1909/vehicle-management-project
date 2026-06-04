package com.ban.vehicle_management.entrypoint.dto.people.customer.response;

import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response.CustomerVehicleAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.UserProfileAdminResponse;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerAdminProfileResponse {

    private UserProfileAdminResponse userProfile;
    private CustomerAdminResponse customer;
    private List<CustomerVehicleAdminResponse> customerVehicles;
}
