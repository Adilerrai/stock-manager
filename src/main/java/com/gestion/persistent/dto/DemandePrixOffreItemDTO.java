package com.gestion.persistent.dto;

public class DemandePrixOffreItemDTO {
    private Long ddpId;
    private Long offreId;

    public DemandePrixOffreItemDTO() {
    }

    public DemandePrixOffreItemDTO(Long ddpId, Long offreId) {
        this.ddpId = ddpId;
        this.offreId = offreId;
    }

    public Long getDdpId() {
        return ddpId;
    }

    public void setDdpId(Long ddpId) {
        this.ddpId = ddpId;
    }

    public Long getOffreId() {
        return offreId;
    }

    public void setOffreId(Long offreId) {
        this.offreId = offreId;
    }
}
