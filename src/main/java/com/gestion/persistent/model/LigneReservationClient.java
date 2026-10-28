package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_reservation_client")
public class LigneReservationClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_client_id", nullable = false)
    private ReservationClient reservationClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite_demandee", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteDemandee = BigDecimal.ONE;

    @Column(name = "remarque", length = 500)
    private String remarque;

    public LigneReservationClient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReservationClient getReservationClient() { return reservationClient; }
    public void setReservationClient(ReservationClient reservationClient) { this.reservationClient = reservationClient; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
    public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }

    public String getRemarque() { return remarque; }
    public void setRemarque(String remarque) { this.remarque = remarque; }
}
