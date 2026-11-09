package com.ban.vehicle_management.application.accesscontrol.card.model.result;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;

public record CardAdminView(
        Card card,
        Subscription subscription,
        TicketType ticketType,
        CustomerVehicle customerVehicle,
        VehicleType registeredVehicleType,
        Customer customer
) {
}
