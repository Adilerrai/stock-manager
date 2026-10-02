package com.gestion.ai.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AiChatResponseDTO {

    private String reply;
    private String conversationId;
    private List<AiToolExecutionDTO> executedTools = new ArrayList<>();
    private Long tenantId;
    private String entrepriseNom;
    private boolean holding;
    private boolean success = true;
    private String timestamp = LocalDateTime.now().toString();

    public AiChatResponseDTO() {
    }

    public AiChatResponseDTO(String reply, String conversationId) {
        this.reply = reply;
        this.conversationId = conversationId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<AiToolExecutionDTO> getExecutedTools() {
        return executedTools;
    }

    public void setExecutedTools(List<AiToolExecutionDTO> executedTools) {
        this.executedTools = executedTools;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEntrepriseNom() {
        return entrepriseNom;
    }

    public void setEntrepriseNom(String entrepriseNom) {
        this.entrepriseNom = entrepriseNom;
    }

    public boolean isHolding() {
        return holding;
    }

    public void setHolding(boolean holding) {
        this.holding = holding;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
