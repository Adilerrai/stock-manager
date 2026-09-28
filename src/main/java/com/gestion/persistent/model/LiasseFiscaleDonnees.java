package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "liasse_fiscale_donnees", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"annee", "point_de_vente_id"})
})
public class LiasseFiscaleDonnees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(nullable = false)
    private Integer annee;


    @Column(name = "statut", length = 30)
    private String statut = "BROUILLON"; // BROUILLON, MODIFIE_USER, VALIDE, TELEDECLAREE

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "modifie_par")
    private String modifiePar;

    // Sauvegarde intégrale du JSON de la liasse modifiée / ajustée par l'utilisateur
    @Column(name = "donnees_json", columnDefinition = "TEXT")
    private String donneesJson;

    @Column(name = "reintegrations_fiscales", precision = 15, scale = 2)
    private BigDecimal reintegrationsFiscales = BigDecimal.ZERO;

    @Column(name = "deductions_fiscales", precision = 15, scale = 2)
    private BigDecimal deductionsFiscales = BigDecimal.ZERO;

    @Column(name = "commentaires_metic", columnDefinition = "TEXT")
    private String commentairesMetic;

    public LiasseFiscaleDonnees() {}

    public LiasseFiscaleDonnees(Long pointDeVenteId, Integer annee, String donneesJson) {
        this.pointDeVenteId = pointDeVenteId;
        this.annee = annee;
        this.donneesJson = donneesJson;
        this.dateModification = LocalDateTime.now();
        this.statut = "MODIFIE_USER";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }


    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String modifiePar) { this.modifiePar = modifiePar; }

    public String getDonneesJson() { return donneesJson; }
    public void setDonneesJson(String donneesJson) { this.donneesJson = donneesJson; }

    public BigDecimal getReintegrationsFiscales() { return reintegrationsFiscales; }
    public void setReintegrationsFiscales(BigDecimal reintegrationsFiscales) { this.reintegrationsFiscales = reintegrationsFiscales; }

    public BigDecimal getDeductionsFiscales() { return deductionsFiscales; }
    public void setDeductionsFiscales(BigDecimal deductionsFiscales) { this.deductionsFiscales = deductionsFiscales; }

    public String getCommentairesMetic() { return commentairesMetic; }
    public void setCommentairesMetic(String commentairesMetic) { this.commentairesMetic = commentairesMetic; }
}
