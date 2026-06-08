package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.supportticketcategory.mapper.SupportTicketCategoryApiMapper;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.CreateSupportTicketCategoryRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.SupportTicketCategoryFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.UpdateSupportTicketCategoryRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.response.SupportTicketCategoryAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/support-ticket-categories")
public class SupportTicketCategoryController {

    private final SupportTicketCategoryPortIn categoryPortIn;
    private final SupportTicketCategoryApiMapper categoryApiMapper;

    public SupportTicketCategoryController(
            SupportTicketCategoryPortIn categoryPortIn,
            SupportTicketCategoryApiMapper categoryApiMapper
    ) {
        this.categoryPortIn = categoryPortIn;
        this.categoryApiMapper = categoryApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketCategoryAdminResponse>> createCategory(
            @RequestBody CreateSupportTicketCategoryRequest request
    ) {
        SupportTicketCategory createdCategory = categoryPortIn.createCategory(categoryApiMapper.toDomain(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Support ticket category created successfully",
                categoryApiMapper.toAdminResponse(createdCategory)
        ));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<SupportTicketCategoryAdminResponse>> getCategoryById(
            @PathVariable UUID categoryId
    ) {
        SupportTicketCategory category = categoryPortIn.getCategoryById(categoryId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched support ticket category successfully",
                categoryApiMapper.toAdminResponse(category)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketCategoryAdminResponse>>> getCategories(
            @ModelAttribute SupportTicketCategoryFilterRequest request
    ) {
        List<SupportTicketCategory> categories = categoryPortIn.getCategories(
                request.status(),
                request.priority(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched support ticket categories successfully",
                categoryApiMapper.toAdminResponses(categories)
        ));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<SupportTicketCategoryAdminResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @RequestBody UpdateSupportTicketCategoryRequest request
    ) {
        SupportTicketCategory updatedCategory = categoryPortIn.updateCategory(
                categoryId,
                categoryApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket category updated successfully",
                categoryApiMapper.toAdminResponse(updatedCategory)
        ));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID categoryId) {
        categoryPortIn.deleteCategory(categoryId);

        return ResponseEntity.ok(ApiResponse.ok("Support ticket category deactivated successfully"));
    }

    @PatchMapping("/{categoryId}/activate")
    public ResponseEntity<ApiResponse<SupportTicketCategoryAdminResponse>> activateCategory(
            @PathVariable UUID categoryId
    ) {
        SupportTicketCategory activatedCategory = categoryPortIn.activateCategory(categoryId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket category activated successfully",
                categoryApiMapper.toAdminResponse(activatedCategory)
        ));
    }
}