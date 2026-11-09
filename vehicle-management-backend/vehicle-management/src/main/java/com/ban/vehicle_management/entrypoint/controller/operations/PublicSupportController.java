package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.guestsupport.service.GuestPublicAssistantService;
import com.ban.vehicle_management.application.operations.guestsupport.service.GuestSupportService;
import com.ban.vehicle_management.application.operations.guestsupport.service.CaptchaProofService;
import com.ban.vehicle_management.application.operations.guestsupport.port.in.GuestSupportStatusPortIn;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaProof;
import com.ban.vehicle_management.application.operations.guestsupport.mapper.GuestSupportApiMapper;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.guestsupport.model.GuestSupportRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.AccessGuestSupportRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.CreateGuestSupportRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.GuestAssistantMessageRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.VerifyCaptchaRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.CaptchaProofResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.PublicSupportStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.GuestAssistantResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.GuestSupportRequestResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.PublicSupportCategoryResponse;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.ban.vehicle_management.entrypoint.converter.ClientIpResolver;
import com.ban.vehicle_management.entrypoint.policy.EndpointOutcomePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/support")
public class PublicSupportController {
    private final GuestSupportService guestSupportService;
    private final GuestPublicAssistantService assistantService;
    private final SupportTicketCategoryPortIn categoryPortIn;
    private final CaptchaProofService captchaProofService;
    private final GuestSupportStatusPortIn statusPortIn;
    private final GuestSupportApiMapper apiMapper;
    private final ClientIpResolver clientIpResolver;
    private final EndpointOutcomePolicy endpointOutcomePolicy;

    public PublicSupportController(
            GuestSupportService guestSupportService,
            GuestPublicAssistantService assistantService,
            SupportTicketCategoryPortIn categoryPortIn,
            CaptchaProofService captchaProofService,
            GuestSupportStatusPortIn statusPortIn,
            GuestSupportApiMapper apiMapper,
            ClientIpResolver clientIpResolver,
            EndpointOutcomePolicy endpointOutcomePolicy
    ) {
        this.guestSupportService = guestSupportService;
        this.assistantService = assistantService;
        this.categoryPortIn = categoryPortIn;
        this.captchaProofService = captchaProofService;
        this.statusPortIn = statusPortIn;
        this.apiMapper = apiMapper;
        this.clientIpResolver = clientIpResolver;
        this.endpointOutcomePolicy = endpointOutcomePolicy;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PublicSupportCategoryResponse>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched public support categories successfully",
                apiMapper.toCategoryResponses(
                        categoryPortIn.getCategories(SupportTicketCategoryStatus.ACTIVE, null, null))));
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<GuestSupportRequestResponse>> createRequest(
            @Valid @RequestBody CreateGuestSupportRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        GuestSupportService.CreatedRequest created = guestSupportService.create(
                request, idempotencyKey, clientIpResolver.resolve(servletRequest));
        return endpointOutcomePolicy.createdOrOk(
                !created.replayed(),
                apiMapper.toResponse(created.request(), guestSupportService.maskedContact(created.request()),
                        created.capabilityToken()),
                "Guest support request created successfully",
                "Guest support request already exists");
    }

    @PostMapping("/requests/access")
    public ResponseEntity<ApiResponse<GuestSupportRequestResponse>> accessRequest(
            @Valid @RequestBody AccessGuestSupportRequest request
    ) {
        GuestSupportRequest found = guestSupportService.access(request.referenceCode(), request.capabilityToken());
        return ResponseEntity.ok(ApiResponse.ok("Fetched guest support request successfully",
                apiMapper.toResponse(found, guestSupportService.maskedContact(found), null)));
    }

    @GetMapping("/assistant/status")
    public ResponseEntity<ApiResponse<PublicSupportStatusResponse>> assistantStatus() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched public assistant status successfully",
                apiMapper.toStatusResponse(statusPortIn.getStatus())));
    }

    @PostMapping("/captcha/verify")
    public ResponseEntity<ApiResponse<CaptchaProofResponse>> verifyCaptcha(
            @Valid @RequestBody VerifyCaptchaRequest request,
            HttpServletRequest servletRequest
    ) {
        CaptchaProof proof = captchaProofService.verifyAndIssue(
                request.purpose(), request.challengeToken(), request.sessionId(),
                clientIpResolver.resolve(servletRequest));
        return ResponseEntity.ok(ApiResponse.ok("CAPTCHA verified successfully",
                apiMapper.toResponse(proof)));
    }

    @PostMapping("/assistant/messages")
    public ResponseEntity<ApiResponse<GuestAssistantResponse>> assistantMessage(
            @Valid @RequestBody GuestAssistantMessageRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Public assistant response generated successfully",
                assistantService.answer(
                        request.message(), request.sessionId(), request.captchaProof(),
                        clientIpResolver.resolve(servletRequest))));
    }

}
