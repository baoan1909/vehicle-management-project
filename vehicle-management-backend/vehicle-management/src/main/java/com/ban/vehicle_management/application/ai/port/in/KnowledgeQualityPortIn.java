package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.application.ai.service.KnowledgeQualityService.KnowledgeQualityDashboard;

public interface KnowledgeQualityPortIn {

    KnowledgeQualityDashboard summarize();
}