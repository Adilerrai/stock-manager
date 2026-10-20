package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeActiviteCrm;
import java.time.LocalDateTime;

public class CrmActiviteDTO {
    private Long id;
    private Long pointDeVenteId;
    private TypeActiviteCrm typeActivite;
    private String titre;
    private String description;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long contactId;
    private String contactNom;
    private Long opportuniteId;
    private String opportuniteTitre;
    private Long visiteId;
    private Long userId;
    private String userNom;
    private LocalDateTime dateActivite;
    private String metadataJson;

    public CrmActiviteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public TypeActiviteCrm getTypeActivite() { return typeActivite; }
    public void setTypeActivite(TypeActiviteCrm typeActivite) { this.typeActivite = typeActivite; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getContactNom() { return contactNom; }
    public void setContactNom(String contactNom) { this.contactNom = contactNom; }

    public Long getOpportuniteId() { return opportuniteId; }
    public void setOpportuniteId(Long opportuniteId) { this.opportuniteId = opportuniteId; }

    public String getOpportuniteTitre() { return opportuniteTitre; }
    public void setOpportuniteTitre(String opportuniteTitre) { this.opportuniteTitre = opportuniteTitre; }

    public Long getVisiteId() { return visiteId; }
    public void setVisiteId(Long visiteId) { this.visiteId = visiteId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserNom() { return userNom; }
    public void setUserNom(String userNom) { this.userNom = userNom; }

    public LocalDateTime getDateActivite() { return dateActivite; }
    public void setDateActivite(LocalDateTime dateActivite) { this.dateActivite = dateActivite; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
