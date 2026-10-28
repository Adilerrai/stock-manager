package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class EspaceClientDashboardDTO {
    private Long clientId;
    private String codeClient;
    private String clientNomComplet;
    private String clientIce;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;
    private Long tenantId;
    private String nomTenant;

    private long totalReservations;
    private long reservationsEnAttente;
    private long devisEnAttenteValidation;
    private long devisAcceptes;
    private long commandesEnCours;
    private long totalCommandes;
    private long blEnAttente;
    private long blExpedies;
    private long blLivres;

    private List<ReservationClientDTO> dernieresReservations = new ArrayList<>();
    private List<DevisDTO> devisEnAttente = new ArrayList<>();
    private List<CommandeClientDTO> commandesRecentes = new ArrayList<>();
    private List<BonLivraisonClientDTO> derniersBonsLivraison = new ArrayList<>();

    public EspaceClientDashboardDTO() {}

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getCodeClient() { return codeClient; }
    public void setCodeClient(String codeClient) { this.codeClient = codeClient; }

    public String getClientNomComplet() { return clientNomComplet; }
    public void setClientNomComplet(String clientNomComplet) { this.clientNomComplet = clientNomComplet; }

    public String getClientIce() { return clientIce; }
    public void setClientIce(String clientIce) { this.clientIce = clientIce; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public String getClientAdresse() { return clientAdresse; }
    public void setClientAdresse(String clientAdresse) { this.clientAdresse = clientAdresse; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getNomTenant() { return nomTenant; }
    public void setNomTenant(String nomTenant) { this.nomTenant = nomTenant; }

    public long getTotalReservations() { return totalReservations; }
    public void setTotalReservations(long totalReservations) { this.totalReservations = totalReservations; }

    public long getReservationsEnAttente() { return reservationsEnAttente; }
    public void setReservationsEnAttente(long reservationsEnAttente) { this.reservationsEnAttente = reservationsEnAttente; }

    public long getDevisEnAttenteValidation() { return devisEnAttenteValidation; }
    public void setDevisEnAttenteValidation(long devisEnAttenteValidation) { this.devisEnAttenteValidation = devisEnAttenteValidation; }

    public long getDevisAcceptes() { return devisAcceptes; }
    public void setDevisAcceptes(long devisAcceptes) { this.devisAcceptes = devisAcceptes; }

    public long getCommandesEnCours() { return commandesEnCours; }
    public void setCommandesEnCours(long commandesEnCours) { this.commandesEnCours = commandesEnCours; }

    public long getTotalCommandes() { return totalCommandes; }
    public void setTotalCommandes(long totalCommandes) { this.totalCommandes = totalCommandes; }

    public long getBlEnAttente() { return blEnAttente; }
    public void setBlEnAttente(long blEnAttente) { this.blEnAttente = blEnAttente; }

    public long getBlExpedies() { return blExpedies; }
    public void setBlExpedies(long blExpedies) { this.blExpedies = blExpedies; }

    public long getBlLivres() { return blLivres; }
    public void setBlLivres(long blLivres) { this.blLivres = blLivres; }

    public List<ReservationClientDTO> getDernieresReservations() { return dernieresReservations; }
    public void setDernieresReservations(List<ReservationClientDTO> dernieresReservations) { this.dernieresReservations = dernieresReservations; }

    public List<DevisDTO> getDevisEnAttente() { return devisEnAttente; }
    public void setDevisEnAttente(List<DevisDTO> devisEnAttente) { this.devisEnAttente = devisEnAttente; }

    public List<CommandeClientDTO> getCommandesRecentes() { return commandesRecentes; }
    public void setCommandesRecentes(List<CommandeClientDTO> commandesRecentes) { this.commandesRecentes = commandesRecentes; }

    public List<BonLivraisonClientDTO> getDerniersBonsLivraison() { return derniersBonsLivraison; }
    public void setDerniersBonsLivraison(List<BonLivraisonClientDTO> derniersBonsLivraison) { this.derniersBonsLivraison = derniersBonsLivraison; }
}
