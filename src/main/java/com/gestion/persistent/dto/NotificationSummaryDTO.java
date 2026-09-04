package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class NotificationSummaryDTO {
    private int totalAlertes = 0;
    private int nbStockFaible = 0;
    private int nbFacturesImpayees = 0;
    private int nbEcheancesProches = 0;
    private int nbCommandesEnAttente = 0;
    private List<AlerteNotificationDTO> alertes = new ArrayList<>();

    public NotificationSummaryDTO() {}

    public int getTotalAlertes() { return totalAlertes; }
    public void setTotalAlertes(int totalAlertes) { this.totalAlertes = totalAlertes; }

    public int getNbStockFaible() { return nbStockFaible; }
    public void setNbStockFaible(int nbStockFaible) { this.nbStockFaible = nbStockFaible; }

    public int getNbFacturesImpayees() { return nbFacturesImpayees; }
    public void setNbFacturesImpayees(int nbFacturesImpayees) { this.nbFacturesImpayees = nbFacturesImpayees; }

    public int getNbEcheancesProches() { return nbEcheancesProches; }
    public void setNbEcheancesProches(int nbEcheancesProches) { this.nbEcheancesProches = nbEcheancesProches; }

    public int getNbCommandesEnAttente() { return nbCommandesEnAttente; }
    public void setNbCommandesEnAttente(int nbCommandesEnAttente) { this.nbCommandesEnAttente = nbCommandesEnAttente; }

    public List<AlerteNotificationDTO> getAlertes() { return alertes; }
    public void setAlertes(List<AlerteNotificationDTO> alertes) { this.alertes = alertes; }
}
