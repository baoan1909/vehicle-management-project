package com.ban.vehicle_management.application.operations.guestsupport.service;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestSupportRequestPortOut;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.operations.guestsupport.model.GuestSupportRequest;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.CreateGuestSupportRequest;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestSupportService {
    private final GuestSupportRequestPortOut requestPortOut;
    private final SupportTicketCategoryPortIn categoryPortIn;
    private final AuditLogPortOut auditLogPortOut;
    private final GuestSupportTokenService tokenService;
    private final GuestSupportProperties properties;
    private final CaptchaProofService captchaProofService;
    private final SecureRandom secureRandom = new SecureRandom();

    public GuestSupportService(
            GuestSupportRequestPortOut requestPortOut,
            SupportTicketCategoryPortIn categoryPortIn,
            AuditLogPortOut auditLogPortOut,
            GuestSupportTokenService tokenService,
            GuestSupportProperties properties,
            CaptchaProofService captchaProofService
    ) {
        this.requestPortOut = requestPortOut;
        this.categoryPortIn = categoryPortIn;
        this.auditLogPortOut = auditLogPortOut;
        this.tokenService = tokenService;
        this.properties = properties;
        this.captchaProofService = captchaProofService;
    }

    @Transactional
    public CreatedRequest create(CreateGuestSupportRequest input, String idempotencyKey, String remoteAddress) {
        String normalizedKey = normalizeRequired(idempotencyKey, "Idempotency-Key", 100);
        String email = normalizeEmail(input.email());
        String phone = normalizePhone(input.phone());
        if (email == null && phone == null) {
            throw new BadRequestException("Vui lòng cung cấp email hoặc số điện thoại hợp lệ");
        }
        String contactHash = tokenService.hash((email == null ? "" : email) + "|" + (phone == null ? "" : phone));
        String idempotencyHash = tokenService.hash(normalizedKey);
        String token = tokenService.capabilityToken(contactHash, idempotencyHash);
        requestPortOut.lockContact(contactHash);
        GuestSupportRequest replay = requestPortOut
                .findByContactHashAndIdempotencyHash(contactHash, idempotencyHash).orElse(null);
        if (replay != null) {
            ensureSameRequest(replay, input);
            return new CreatedRequest(replay, token, true);
        }

        captchaProofService.requireProof(
                CaptchaPurpose.INTAKE,
                input.captchaProof(),
                input.sessionId(),
                remoteAddress);

        SupportTicketCategory category = categoryPortIn.getCategoryById(input.categoryId());
        if (category.getStatus() != SupportTicketCategoryStatus.ACTIVE) {
            throw new BadRequestException("Danh mục hỗ trợ không còn hoạt động");
        }

        String ipHash = tokenService.hash(remoteAddress == null ? "unknown" : remoteAddress);
        String sessionHash = isBlank(input.sessionId()) ? null : tokenService.hash(input.sessionId().strip());
        Instant now = Instant.now();
        Instant since = now.minus(properties.getRateWindow());
        if (requestPortOut.countByIpHashSince(ipHash, since) >= properties.getMaxRequestsPerIp()
                || requestPortOut.countByContactHashSince(contactHash, since) >= properties.getMaxRequestsPerContact()
                || (sessionHash != null && requestPortOut.countBySessionHashSince(sessionHash, since)
                        >= properties.getMaxRequestsPerSession())) {
            throw new TooManyRequestsException("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau");
        }

        GuestSupportRequest request = new GuestSupportRequest();
        request.setRequestId(UUID.randomUUID());
        request.setPublicReferenceCode(newReferenceCode());
        request.setCategoryId(input.categoryId());
        request.setTitle(input.title().strip());
        request.setContent(input.content().strip());
        request.setEmail(email);
        request.setPhone(phone);
        request.setContactHash(contactHash);
        request.setIpHash(ipHash);
        request.setSessionHash(sessionHash);
        request.setIdempotencyHash(idempotencyHash);
        request.setCapabilityTokenHash(tokenService.hash(token));
        request.setCapabilityExpiresAt(now.plus(properties.getCapabilityTtl()));
        request.setRetentionExpiresAt(now.plus(properties.getRetention()));
        request.setStatus(SupportTicketStatus.OPEN);
        GuestSupportRequest saved = requestPortOut.save(request);
        audit("GUEST_SUPPORT_REQUEST_CREATED", saved);
        return new CreatedRequest(saved, token, false);
    }

    @Transactional
    public GuestSupportRequest access(String referenceCode, String capabilityToken) {
        GuestSupportRequest request = requestPortOut.findByPublicReferenceCode(normalizeReference(referenceCode))
                .orElseThrow(this::genericNotFound);
        if (request.getCapabilityExpiresAt().isBefore(Instant.now())
                || !tokenService.matches(capabilityToken, request.getCapabilityTokenHash())) {
            throw genericNotFound();
        }
        audit("GUEST_SUPPORT_REQUEST_ACCESSED", request);
        return request;
    }

    @Transactional
    public long purgeExpired() {
        return requestPortOut.deleteExpiredBefore(Instant.now());
    }

    public String maskedContact(GuestSupportRequest request) {
        if (request.getEmail() != null) {
            int at = request.getEmail().indexOf('@');
            String local = request.getEmail().substring(0, at);
            return local.substring(0, Math.min(2, local.length())) + "***" + request.getEmail().substring(at);
        }
        String phone = request.getPhone();
        return "***" + phone.substring(Math.max(0, phone.length() - 4));
    }

    private void ensureSameRequest(GuestSupportRequest existing, CreateGuestSupportRequest input) {
        if (!existing.getCategoryId().equals(input.categoryId())
                || !existing.getTitle().equals(input.title().strip())
                || !existing.getContent().equals(input.content().strip())) {
            throw new ConflictException("Idempotency-Key đã được dùng cho một yêu cầu khác");
        }
    }

    private void audit(String action, GuestSupportRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(null);
        auditLog.setAction(action);
        auditLog.setTargetSchema("operations");
        auditLog.setTargetTable("guest_support_requests");
        auditLog.setTargetId(request.getRequestId());
        auditLog.setNewData(Map.of("referenceCode", request.getPublicReferenceCode(), "status", request.getStatus().name()));
        auditLogPortOut.save(auditLog);
    }

    private String newReferenceCode() {
        byte[] random = new byte[12];
        secureRandom.nextBytes(random);
        return "GS-" + HexFormat.of().withUpperCase().formatHex(random);
    }

    private String normalizeReference(String value) {
        return normalizeRequired(value, "referenceCode", 32).toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        if (isBlank(value)) return null;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BadRequestException("Email không hợp lệ");
        }
        return normalized;
    }

    private String normalizePhone(String value) {
        if (isBlank(value)) return null;
        String normalized = value.replaceAll("[ .-]", "");
        if (!normalized.matches("^\\+?[0-9]{8,15}$")) {
            throw new BadRequestException("Số điện thoại không hợp lệ");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String field, int maxLength) {
        if (isBlank(value)) throw new BadRequestException(field + " là bắt buộc");
        String normalized = value.strip();
        if (normalized.length() > maxLength) throw new BadRequestException(field + " vượt quá " + maxLength + " ký tự");
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private NotFoundException genericNotFound() {
        return new NotFoundException("Không tìm thấy yêu cầu hỗ trợ hoặc quyền truy cập đã hết hạn");
    }

    public record CreatedRequest(GuestSupportRequest request, String capabilityToken, boolean replayed) {
    }
}
