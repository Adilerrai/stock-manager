package com.gestion.ai.dto;

import java.util.Map;

public class AiChatRequestDTO {

    private String message;
    private String conversationId;
    private Map<String, Object> context;

    public AiChatRequestDTO() {
    }

    public AiChatRequestDTO(String message, String conversationId) {
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
