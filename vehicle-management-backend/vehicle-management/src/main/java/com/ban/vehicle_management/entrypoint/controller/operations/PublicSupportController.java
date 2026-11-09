package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.guestsupport.service.GuestPublicAssistantService;
import com.ban.vehicle_management.application.operations.guestsupport.service.GuestSupportService;
import com.ban.vehicle_management.application.operations.guestsupport.service.CaptchaProofService;
import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
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
    private final GuestSupportProperties guestSupportProperties;
    private final GuestSupportApiMapper apiMapper;

    public PublicSupportController(
            GuestSupportService guestSupportService,
            GuestPublicAssistantService assistantService,
            SupportTicketCategoryPortIn categoryPortIn,
            CaptchaProofService captchaProofService,
            GuestSupportProperties guestSupportProperties,
            GuestSupportApiMapper apiMapper
    ) {
        this.guestSupportService = guestSupportService;
        this.assistantService = assistantService;
        this.categoryPortIn = categoryPortIn;
        this.captchaProofService = captchaProofService;
        this.guestSupportProperties = guestSupportProperties;
        this.apiMapper = apiMapper;
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
                request, idempotencyKey, servletRequest.getRemoteAddr());
        HttpStatus status = created.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.ok(
                created.replayed() ? "Guest support request already exists" : "Guest support request created successfully",
                apiMapper.toResponse(created.request(), guestSupportService.maskedContact(created.request()),
                        created.capabilityToken())));
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
                apiMapper.toStatusResponse(
                        assistantService.isEnabled(),
                        guestSupportProperties.isCaptchaEnabled(),
                        guestSupportProperties.isCaptchaEnabled() ? guestSupportProperties.getCaptchaProvider() : null,
                        guestSupportProperties.isCaptchaEnabled() ? guestSupportProperties.getCaptchaSiteKey() : null,
                        captchaProofService.isRequired(com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose.INTAKE),
                        captchaProofService.isRequired(com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose.ASSISTANT))));
    }

    @PostMapping("/captcha/verify")
    public ResponseEntity<ApiResponse<CaptchaProofResponse>> verifyCaptcha(
            @Valid @RequestBody VerifyCaptchaRequest request,
            HttpServletRequest servletRequest
    ) {
        CaptchaProof proof = captchaProofService.verifyAndIssue(
                request.purpose(), request.challengeToken(), request.sessionId(), servletRequest.getRemoteAddr());
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
                        request.message(), request.sessionId(), request.captchaProof(), servletRequest.getRemoteAddr())));
    }

}
