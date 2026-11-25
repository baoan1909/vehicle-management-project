package com.ban.vehicle_management.application.ai.service;

import org.springframework.stereotype.Component;

/**
 * Provider-neutral document/query formatting for question-answering retrieval.
 * Prompt version is part of the index snapshot so changing the format requires a new index.
 */
@Component
public class EmbeddingPromptFormatter {

    public static final String QWEN_QA_V1 = "qwen-qa-mrl768-v1";

    public static final String RAG_QA_V1 = "rag-qa-v1";

    public String formatDocument(String promptVersion, String title, String content) {
        requireSupported(promptVersion);
        String titleText = title == null ? "" : title.trim();
        String contentText = content == null ? "" : content.trim();
        return "title: " + titleText + " | text: " + contentText;
    }

    public String formatQuery(String promptVersion, String query) {
        requireSupported(promptVersion);
        String queryText = query == null ? "" : query.trim();
        if (QWEN_QA_V1.equals(promptVersion)) {
            return "Instruct: Retrieve CoParking documentation that answers the user question.\nQuery: " + queryText;
        }
        return "task: question answering | query: " + queryText;
    }

    public String version(com.ban.vehicle_management.shared.enumeration.ai.AiProvider provider) {
        return provider == com.ban.vehicle_management.shared.enumeration.ai.AiProvider.LM_STUDIO ? QWEN_QA_V1 : RAG_QA_V1;
    }
    public String version() {
        return RAG_QA_V1;
    }

    public boolean supports(String promptVersion) {
        return RAG_QA_V1.equals(promptVersion) || QWEN_QA_V1.equals(promptVersion);
    }

    private void requireSupported(String promptVersion) {
        if (!supports(promptVersion)) {
            throw new IllegalArgumentException("Phiên bản định dạng embedding không được hỗ trợ: " + promptVersion);
        }
    }
}
