package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeSourceRequest(
        @NotBlank(message = "Tên nguồn kiến thức không được để trống")
        @Size(max = 200, message = "Tên nguồn kiến thức tối đa 200 ký tự")
        String title,
        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        String description,
        KnowledgeAccessScope accessScope
) {
}