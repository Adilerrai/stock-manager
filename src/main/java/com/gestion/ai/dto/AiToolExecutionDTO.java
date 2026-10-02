package com.gestion.ai.dto;

import java.util.Map;

public class AiToolExecutionDTO {

    private String toolName;
    private String description;
    private Map<String, Object> arguments;
    private Object result;

    public AiToolExecutionDTO() {
    }

    public AiToolExecutionDTO(String toolName, String description, Map<String, Object> arguments, Object result) {
        this.toolName = toolName;
        this.description = description;
        this.arguments = arguments;
        this.result = result;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
