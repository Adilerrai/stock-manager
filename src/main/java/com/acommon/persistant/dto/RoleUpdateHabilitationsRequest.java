package com.acommon.persistant.dto;

import java.util.List;

public class RoleUpdateHabilitationsRequest {
    private List<String> habilitations;

    public List<String> getHabilitations() { return habilitations; }
    public void setHabilitations(List<String> habilitations) { this.habilitations = habilitations; }
}
