package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditStatsResponseDTO {
    private Long pointDeVenteId;
    private String nomPointDeVente;
    private long totalActions;
    private long totalCreations;
    private long totalModifications;
    private long totalSuppressions;
    private long totalValidations;
    private long totalAnnulations;
    private List<CollaborateurAuditStatDTO> collaborateurs = new ArrayList<>();
    private Map<String, Long> repartitionParEntite = new HashMap<>();

    public AuditStatsResponseDTO() {}

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getNomPointDeVente() { return nomPointDeVente; }
    public void setNomPointDeVente(String nomPointDeVente) { this.nomPointDeVente = nomPointDeVente; }

    public long getTotalActions() { return totalActions; }
    public void setTotalActions(long totalActions) { this.totalActions = totalActions; }

    public long getTotalCreations() { return totalCreations; }
    public void setTotalCreations(long totalCreations) { this.totalCreations = totalCreations; }

    public long getTotalModifications() { return totalModifications; }
    public void setTotalModifications(long totalModifications) { this.totalModifications = totalModifications; }

    public long getTotalSuppressions() { return totalSuppressions; }
    public void setTotalSuppressions(long totalSuppressions) { this.totalSuppressions = totalSuppressions; }

    public long getTotalValidations() { return totalValidations; }
    public void setTotalValidations(long totalValidations) { this.totalValidations = totalValidations; }

    public long getTotalAnnulations() { return totalAnnulations; }
    public void setTotalAnnulations(long totalAnnulations) { this.totalAnnulations = totalAnnulations; }

    public List<CollaborateurAuditStatDTO> getCollaborateurs() { return collaborateurs; }
    public void setCollaborateurs(List<CollaborateurAuditStatDTO> collaborateurs) { this.collaborateurs = collaborateurs; }

    public Map<String, Long> getRepartitionParEntite() { return repartitionParEntite; }
    public void setRepartitionParEntite(Map<String, Long> repartitionParEntite) { this.repartitionParEntite = repartitionParEntite; }
}
