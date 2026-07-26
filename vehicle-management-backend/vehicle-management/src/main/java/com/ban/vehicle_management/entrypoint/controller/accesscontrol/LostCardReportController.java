package com.ban.vehicle_management.entrypoint.controller.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.mapper.LostCardReportApiMapper;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardPreviewResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReplacementCardResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportDetailResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportSummaryResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportWorkflowResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.in.LostCardReportPortIn;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request.CancelLostCardReportRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request.CreateLostCardReportRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request.LostCardReportFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request.ResolveLostCardReportRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReplacementCardResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportListItemResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportWorkflowResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access-control/lost-card-reports")
public class LostCardReportController {

    private final LostCardReportPortIn lostCardReportPortIn;
    private final LostCardReportApiMapper lostCardReportApiMapper;

    public LostCardReportController(
            LostCardReportPortIn lostCardReportPortIn,
            LostCardReportApiMapper lostCardReportApiMapper
    ) {
        this.lostCardReportPortIn = lostCardReportPortIn;
        this.lostCardReportApiMapper = lostCardReportApiMapper;
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<LostCardPreviewResponse>> previewByLicensePlate(
            @RequestParam String licensePlate
    ) {
        LostCardPreviewResult result = lostCardReportPortIn.previewByLicensePlate(licensePlate);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched lost card preview successfully",
                lostCardReportApiMapper.toPreviewResponse(result)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LostCardReportWorkflowResponse>> createReport(
            @RequestBody CreateLostCardReportRequest request
    ) {
        LostCardReportWorkflowResult result = lostCardReportPortIn.createReport(
                lostCardReportApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Lost card report created successfully",
                lostCardReportApiMapper.toWorkflowResponse(result)
        ));
    }

    @PatchMapping("/{lostCardReportId}/resolve")
    public ResponseEntity<ApiResponse<LostCardReportWorkflowResponse>> resolveReport(
            @PathVariable UUID lostCardReportId,
            @RequestBody(required = false) ResolveLostCardReportRequest request
    ) {
        UUID newCardId = request == null ? null : request.newCardId();

        LostCardReportWorkflowResult result = lostCardReportPortIn.resolveReport(
                lostCardReportId,
                newCardId
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Lost card report resolved successfully",
                lostCardReportApiMapper.toWorkflowResponse(result)
        ));
    }

    @PatchMapping("/{lostCardReportId}/cancel")
    public ResponseEntity<ApiResponse<LostCardReportWorkflowResponse>> cancelReport(
            @PathVariable UUID lostCardReportId,
            @RequestBody CancelLostCardReportRequest request
    ) {
        LostCardReportWorkflowResult result = lostCardReportPortIn.cancelReport(
                lostCardReportId,
                request.cancelReason()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Lost card report cancelled successfully",
                lostCardReportApiMapper.toWorkflowResponse(result)
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<LostCardReportSummaryResponse>> getSummary(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate
    ) {
        LostCardReportSummaryResult result = lostCardReportPortIn.getSummary(fromDate, toDate);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched lost card report summary successfully",
                lostCardReportApiMapper.toSummaryResponse(result)
        ));
    }

    @GetMapping("/{lostCardReportId}")
    public ResponseEntity<ApiResponse<LostCardReportDetailResponse>> getReportById(
            @PathVariable UUID lostCardReportId
    ) {
        LostCardReportDetailResult result = lostCardReportPortIn.getReportById(lostCardReportId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched lost card report successfully",
                lostCardReportApiMapper.toDetailResponse(result)
        ));
    }

    @GetMapping("/{lostCardReportId}/replacement-cards")
    public ResponseEntity<ApiResponse<List<LostCardReplacementCardResponse>>> getAvailableReplacementCards(
            @PathVariable UUID lostCardReportId
    ) {
        List<LostCardReplacementCardResult> cards =
                lostCardReportPortIn.getAvailableReplacementCards(lostCardReportId);
        List<LostCardReplacementCardResponse> response = cards.stream()
                .map(card -> new LostCardReplacementCardResponse(
                        card.cardId(),
                        card.cardNumber(),
                        card.uid(),
                        card.cardTypeId(),
                        card.status()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched available replacement cards successfully",
                response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LostCardReportListItemResponse>>> getReports(
            @ModelAttribute LostCardReportFilterRequest request
    ) {
        List<LostCardReportListItemResult> reports = lostCardReportPortIn.getReportListItems(
                request.status(),
                request.context(),
                request.customerId(),
                request.cardId(),
                request.parkingSessionId(),
                request.subscriptionId(),
                request.fromDate(),
                request.toDate(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched lost card reports successfully",
                lostCardReportApiMapper.toListItemResponses(reports)
        ));
    }
}
