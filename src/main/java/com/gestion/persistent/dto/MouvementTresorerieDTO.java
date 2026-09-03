package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeMouvementTresorerie;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MouvementTresorerieDTO {
    private Long id;
    private String reference;
    private TypeMouvementTresorerie typeMouvement;
    private String typeMouvementLibelle;
    private Long compteSourceId;
    private String compteSourceNom;
    private Long compteDestinationId;
    private String compteDestinationNom;
    private BigDecimal montant;
    private LocalDateTime dateMouvement;
    private String motif;
    private String justificatifReference;
    private Long effectueParUserId;
    private String effectueParNom;
    private BigDecimal soldeApresSource;
    private BigDecimal soldeApresDestination;

    public MouvementTresorerieDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public TypeMouvementTresorerie getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(TypeMouvementTresorerie typeMouvement) {
        this.typeMouvement = typeMouvement;
        if (typeMouvement != null) this.typeMouvementLibelle = typeMouvement.getLibelle();
    }

    public String getTypeMouvementLibelle() { return typeMouvementLibelle; }
    public void setTypeMouvementLibelle(String typeMouvementLibelle) { this.typeMouvementLibelle = typeMouvementLibelle; }

    public Long getCompteSourceId() { return compteSourceId; }
    public void setCompteSourceId(Long compteSourceId) { this.compteSourceId = compteSourceId; }

    public String getCompteSourceNom() { return compteSourceNom; }
    public void setCompteSourceNom(String compteSourceNom) { this.compteSourceNom = compteSourceNom; }

    public Long getCompteDestinationId() { return compteDestinationId; }
    public void setCompteDestinationId(Long compteDestinationId) { this.compteDestinationId = compteDestinationId; }

    public String getCompteDestinationNom() { return compteDestinationNom; }
    public void setCompteDestinationNom(String compteDestinationNom) { this.compteDestinationNom = compteDestinationNom; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getJustificatifReference() { return justificatifReference; }
    public void setJustificatifReference(String justificatifReference) { this.justificatifReference = justificatifReference; }

    public Long getEffectueParUserId() { return effectueParUserId; }
    public void setEffectueParUserId(Long effectueParUserId) { this.effectueParUserId = effectueParUserId; }

    public String getEffectueParNom() { return effectueParNom; }
    public void setEffectueParNom(String effectueParNom) { this.effectueParNom = effectueParNom; }

    public BigDecimal getSoldeApresSource() { return soldeApresSource; }
    public void setSoldeApresSource(BigDecimal soldeApresSource) { this.soldeApresSource = soldeApresSource; }

    public BigDecimal getSoldeApresDestination() { return soldeApresDestination; }
    public void setSoldeApresDestination(BigDecimal soldeApresDestination) { this.soldeApresDestination = soldeApresDestination; }
}
