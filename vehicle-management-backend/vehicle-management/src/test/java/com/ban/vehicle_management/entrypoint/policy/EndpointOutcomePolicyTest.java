package com.ban.vehicle_management.entrypoint.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class EndpointOutcomePolicyTest {

    private final EndpointOutcomePolicy policy = new EndpointOutcomePolicy();

    @Test
    void createdOrOkUsesCreatedContractForNewResult() {
        var response = policy.createdOrOk(true, "body", "created", "existing");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("created", response.getBody().getMessage());
        assertEquals("body", response.getBody().getData());
    }

    @Test
    void createdOrOkUsesOkContractForExistingResult() {
        var response = policy.createdOrOk(false, "body", "created", "existing");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("existing", response.getBody().getMessage());
    }

    @Test
    void acceptedOrOkUsesAcceptedContractForQueuedResult() {
        var response = policy.acceptedOrOk(true, "body", "accepted", "existing");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("accepted", response.getBody().getMessage());
    }
}
