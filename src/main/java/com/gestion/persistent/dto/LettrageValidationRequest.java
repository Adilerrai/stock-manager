package com.gestion.persistent.dto;

import java.util.List;

public class LettrageValidationRequest {
    private List<Long> ligneIds;

    public LettrageValidationRequest() {}

    public LettrageValidationRequest(List<Long> ligneIds) {
        this.ligneIds = ligneIds;
    }

    public List<Long> getLigneIds() {
        return ligneIds;
    }

    public void setLigneIds(List<Long> ligneIds) {
        this.ligneIds = ligneIds;
    }
}
