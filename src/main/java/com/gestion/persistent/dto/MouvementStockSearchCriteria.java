package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeMouvement;
import java.time.LocalDateTime;

public class MouvementStockSearchCriteria {
    private Long produitId;
    private Long depotId;
    private TypeMouvement typeMouvement;
    private String numeroLot;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    public MouvementStockSearchCriteria() {}

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public Long getDepotId() { return depotId; }
    public void setDepotId(Long depotId) { this.depotId = depotId; }

    public TypeMouvement getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(TypeMouvement typeMouvement) { this.typeMouvement = typeMouvement; }

    public String getNumeroLot() { return numeroLot; }
    public void setNumeroLot(String numeroLot) { this.numeroLot = numeroLot; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
}
