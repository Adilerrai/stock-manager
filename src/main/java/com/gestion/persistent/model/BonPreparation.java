package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutPreparation;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bons_preparation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BonPreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_preparation", unique = true, nullable = false)
    private String numeroPreparation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_client_id", nullable = false)
    private CommandeClient commandeClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_preparation")
    private LocalDateTime datePreparation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPreparation statut = StatutPreparation.A_PREPARER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magasinier_user_id")
    private User magasinier;

    private String notes;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @OneToMany(mappedBy = "bonPreparation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneBonPreparation> lignes = new ArrayList<>();

    public BonPreparation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPreparation() { return numeroPreparation; }
    public void setNumeroPreparation(String numeroPreparation) { this.numeroPreparation = numeroPreparation; }

    public CommandeClient getCommandeClient() { return commandeClient; }
    public void setCommandeClient(CommandeClient commandeClient) { this.commandeClient = commandeClient; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDatePreparation() { return datePreparation; }
    public void setDatePreparation(LocalDateTime datePreparation) { this.datePreparation = datePreparation; }

    public StatutPreparation getStatut() { return statut; }
    public void setStatut(StatutPreparation statut) { this.statut = statut; }

    public User getMagasinier() { return magasinier; }
    public void setMagasinier(User magasinier) { this.magasinier = magasinier; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneBonPreparation> getLignes() { return lignes; }
    public void setLignes(List<LigneBonPreparation> lignes) { this.lignes = lignes; }

    public void addLigne(LigneBonPreparation ligne) {
        lignes.add(ligne);
        ligne.setBonPreparation(this);
    }
}
