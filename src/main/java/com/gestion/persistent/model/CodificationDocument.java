package com.gestion.persistent.model;

import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.enums.TypeReinitialisationCodification;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "codifications_document", uniqueConstraints = {
    @UniqueConstraint(name = "uk_codification_tenant_type", columnNames = {"point_de_vente_id", "type_document"})
})
public class CodificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 50)
    private TypeDocumentCodification typeDocument;

    @Column(name = "prefixe", nullable = false, length = 30)
    private String prefixe;

    @Column(name = "modele_format", nullable = false, length = 60)
    private String modeleFormat = "{PREFIX}-{AAAA}-{NUM}";

    @Column(name = "longueur_sequence", nullable = false)
    private Integer longueurSequence = 3;

    @Column(name = "dernier_numero", nullable = false)
    private Long dernierNumero = 0L;

    @Column(name = "annee_courante")
    private Integer anneeCourante;

    @Column(name = "mois_courant")
    private Integer moisCourant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_reinitialisation", nullable = false, length = 30)
    private TypeReinitialisationCodification typeReinitialisation = TypeReinitialisationCodification.ANNUELLE;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "date_derniere_maj")
    private LocalDateTime dateDerniereMaj = LocalDateTime.now();

    public CodificationDocument() {}

    public CodificationDocument(Long pointDeVenteId, TypeDocumentCodification typeDocument) {
        this.pointDeVenteId = pointDeVenteId;
        this.typeDocument = typeDocument;
        this.prefixe = typeDocument.getPrefixeDefaut();
        this.modeleFormat = typeDocument.getFormatDefaut();
        this.longueurSequence = typeDocument.getLongueurSequenceDefaut();
        this.dernierNumero = 0L;
        this.anneeCourante = LocalDateTime.now().getYear();
        this.moisCourant = LocalDateTime.now().getMonthValue();
        this.typeReinitialisation = TypeReinitialisationCodification.ANNUELLE;
        this.actif = true;
        this.dateDerniereMaj = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.dateDerniereMaj = LocalDateTime.now();
        if (this.pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public TypeDocumentCodification getTypeDocument() { return typeDocument; }
    public void setTypeDocument(TypeDocumentCodification typeDocument) { this.typeDocument = typeDocument; }

    public String getPrefixe() { return prefixe; }
    public void setPrefixe(String prefixe) { this.prefixe = prefixe; }

    public String getModeleFormat() { return modeleFormat; }
    public void setModeleFormat(String modeleFormat) { this.modeleFormat = modeleFormat; }

    public Integer getLongueurSequence() { return longueurSequence; }
    public void setLongueurSequence(Integer longueurSequence) { this.longueurSequence = longueurSequence; }

    public Long getDernierNumero() { return dernierNumero; }
    public void setDernierNumero(Long dernierNumero) { this.dernierNumero = dernierNumero; }

    public Integer getAnneeCourante() { return anneeCourante; }
    public void setAnneeCourante(Integer anneeCourante) { this.anneeCourante = anneeCourante; }

    public Integer getMoisCourant() { return moisCourant; }
    public void setMoisCourant(Integer moisCourant) { this.moisCourant = moisCourant; }

    public TypeReinitialisationCodification getTypeReinitialisation() { return typeReinitialisation; }
    public void setTypeReinitialisation(TypeReinitialisationCodification typeReinitialisation) { this.typeReinitialisation = typeReinitialisation; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateDerniereMaj() { return dateDerniereMaj; }
    public void setDateDerniereMaj(LocalDateTime dateDerniereMaj) { this.dateDerniereMaj = dateDerniereMaj; }
}
