package com.ban.vehicle_management.entrypoint.controller.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.subscription.mapper.SubscriptionApiMapper;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.CreateSubscriptionAdminRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.CreateSubscriptionRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.RejectSubscriptionRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.SubscriptionFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.UpdateSubscriptionRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access-control/subscriptions")
public class SubscriptionController {

    private final SubscriptionPortIn subscriptionPortIn;
    private final SubscriptionApiMapper subscriptionApiMapper;

    public SubscriptionController(
            SubscriptionPortIn subscriptionPortIn,
            SubscriptionApiMapper subscriptionApiMapper
    ) {
        this.subscriptionPortIn = subscriptionPortIn;
        this.subscriptionApiMapper = subscriptionApiMapper;
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> createOwnSubscription(
            @RequestBody CreateSubscriptionRequest request
    ) {
        Subscription createdSubscription = subscriptionPortIn.createOwnSubscription(
                subscriptionApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Subscription created successfully",
                subscriptionApiMapper.toAdminResponse(createdSubscription)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> createSubscriptionForCustomer(
            @RequestBody CreateSubscriptionAdminRequest request
    ) {
        Subscription createdSubscription = subscriptionPortIn.createSubscriptionForCustomer(
                subscriptionApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Subscription created successfully",
                subscriptionApiMapper.toAdminResponse(createdSubscription)
        ));
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> getSubscriptionById(
            @PathVariable UUID subscriptionId
    ) {
        Subscription subscription = subscriptionPortIn.getSubscriptionById(subscriptionId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched subscription successfully",
                subscriptionApiMapper.toAdminResponse(subscription)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionAdminResponse>>> getSubscriptions(
            @ModelAttribute SubscriptionFilterRequest request
    ) {
        List<Subscription> subscriptions = subscriptionPortIn.getSubscriptions(
                request.customerId(),
                request.customerVehicleId(),
                request.cardId(),
                request.ticketTypeId(),
                request.status(),
                request.effectiveFrom(),
                request.effectiveTo(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched subscriptions successfully",
                subscriptionApiMapper.toAdminResponses(subscriptions)
        ));
    }

    @PutMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> updatePendingSubscription(
            @PathVariable UUID subscriptionId,
            @RequestBody UpdateSubscriptionRequest request
    ) {
        Subscription updatedSubscription = subscriptionPortIn.updatePendingSubscription(
                subscriptionId,
                subscriptionApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription updated successfully",
                subscriptionApiMapper.toAdminResponse(updatedSubscription)
        ));
    }

    @PatchMapping("/{subscriptionId}/approve")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> approveSubscription(
            @PathVariable UUID subscriptionId
    ) {
        Subscription approvedSubscription = subscriptionPortIn.approveSubscription(subscriptionId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription approved successfully",
                subscriptionApiMapper.toAdminResponse(approvedSubscription)
        ));
    }

    @PatchMapping("/{subscriptionId}/reject")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> rejectSubscription(
            @PathVariable UUID subscriptionId,
            @RequestBody RejectSubscriptionRequest request
    ) {
        Subscription rejectedSubscription = subscriptionPortIn.rejectSubscription(
                subscriptionId,
                request.reason()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription rejected successfully",
                subscriptionApiMapper.toAdminResponse(rejectedSubscription)
        ));
    }

    @PatchMapping("/{subscriptionId}/assign-card")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> assignReservedCard(
            @PathVariable UUID subscriptionId
    ) {
        Subscription activeSubscription = subscriptionPortIn.assignReservedCard(subscriptionId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription card assigned successfully",
                subscriptionApiMapper.toAdminResponse(activeSubscription)
        ));
    }

    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> cancelSubscription(
            @PathVariable UUID subscriptionId
    ) {
        Subscription cancelledSubscription = subscriptionPortIn.cancelSubscription(subscriptionId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription cancelled successfully",
                subscriptionApiMapper.toAdminResponse(cancelledSubscription)
        ));
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> deleteSubscription(
            @PathVariable UUID subscriptionId
    ) {
        Subscription cancelledSubscription = subscriptionPortIn.cancelSubscription(subscriptionId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription cancelled successfully",
                subscriptionApiMapper.toAdminResponse(cancelledSubscription)
        ));
    }

    @PatchMapping("/{subscriptionId}/expire")
    public ResponseEntity<ApiResponse<SubscriptionAdminResponse>> expireSubscription(
            @PathVariable UUID subscriptionId
    ) {
        Subscription expiredSubscription = subscriptionPortIn.expireSubscription(subscriptionId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Subscription expired successfully",
                subscriptionApiMapper.toAdminResponse(expiredSubscription)
        ));
    }
}