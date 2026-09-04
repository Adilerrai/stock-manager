package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.AlerteNotificationDTO;
import com.gestion.persistent.dto.NotificationSummaryDTO;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.TypeNotificationAlerte;
import com.gestion.persistent.model.ChequeEffet;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.Facture;
import com.gestion.persistent.model.StockQualite;
import com.gestion.repository.ChequeEffetRepository;
import com.gestion.repository.CommandeClientRepository;
import com.gestion.repository.FactureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationHubService {

    private final StockService stockService;
    private final FactureRepository factureRepository;
    private final ChequeEffetRepository chequeEffetRepository;
    private final CommandeClientRepository commandeClientRepository;

    public NotificationHubService(StockService stockService,
                                  FactureRepository factureRepository,
                                  ChequeEffetRepository chequeEffetRepository,
                                  CommandeClientRepository commandeClientRepository) {
        this.stockService = stockService;
        this.factureRepository = factureRepository;
        this.chequeEffetRepository = chequeEffetRepository;
        this.commandeClientRepository = commandeClientRepository;
    }

    private Long getCurrentTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    public NotificationSummaryDTO getNotificationSummary() {
        Long tenantId = getCurrentTenant();
        NotificationSummaryDTO summary = new NotificationSummaryDTO();
        List<AlerteNotificationDTO> allAlertes = new ArrayList<>();

        // 1. Alertes Stock Faible / Rupture
        try {
            List<StockQualite> stocksAlerte = stockService.getStocksQualiteEnAlerte();
            for (StockQualite sq : stocksAlerte) {
                String nomProd = (sq.getProduit() != null) ? sq.getProduit().getNom() : "Produit #" + sq.getId();
                String refProd = (sq.getProduit() != null) ? sq.getProduit().getReference() : "";
                AlerteNotificationDTO alerte = new AlerteNotificationDTO(
                        "STOCK-" + sq.getId(),
                        TypeNotificationAlerte.STOCK_FAIBLE,
                        "Stock faible : " + nomProd,
                        "Quantité restante : " + sq.getQuantiteDisponible() + " (Seuil d'alerte : " + sq.getSeuilAlerte() + ")",
                        "DANGER",
                        LocalDateTime.now(),
                        "/stock/stocks",
                        refProd,
                        null
                );
                allAlertes.add(alerte);
            }
            summary.setNbStockFaible(stocksAlerte.size());
        } catch (Exception e) {
            // Tolérance
        }

        // 2. Alertes Factures Impayées & Échues
        try {
            List<Facture> facturesEchues = factureRepository.findFacturesEchues(LocalDate.now());
            for (Facture f : facturesEchues) {
                if (f.getAnnulee() == null || !f.getAnnulee()) {
                    String clientNom = (f.getClient() != null) ? f.getClient().getNom() : "Client inconnu";
                    LocalDateTime dateF = (f.getDateFacture() != null) ? f.getDateFacture().atStartOfDay() : LocalDateTime.now();
                    AlerteNotificationDTO alerte = new AlerteNotificationDTO(
                            "FAC-" + f.getId(),
                            TypeNotificationAlerte.FACTURE_IMPAYEE,
                            "Facture échue impayée : " + f.getNumeroFacture(),
                            "Client " + clientNom + " - Reste à payer : " + f.getMontantRestant() + " DA (Échéance : " + f.getDateEcheance() + ")",
                            "WARNING",
                            dateF,
                            "/ventes/factures",
                            f.getNumeroFacture(),
                            f.getMontantRestant()
                    );
                    allAlertes.add(alerte);
                }
            }
            summary.setNbFacturesImpayees(facturesEchues.size());
        } catch (Exception e) {
            // Tolérance
        }

        // 3. Alertes Échéances de Chèques / Traites proches
        try {
            List<ChequeEffet> chequesEcheance = chequeEffetRepository.findEcheancesProches(tenantId, LocalDate.now().plusDays(7));
            for (ChequeEffet c : chequesEcheance) {
                String typeStr = (c.getTypeEffet() != null) ? c.getTypeEffet().getLibelle() : "Effet";
                AlerteNotificationDTO alerte = new AlerteNotificationDTO(
                        "CHQ-" + c.getId(),
                        TypeNotificationAlerte.ECHEANCE_EFFET,
                        "Échéance proche : " + typeStr + " N° " + c.getNumeroPiece(),
                        "Tireur : " + (c.getTireur() != null ? c.getTireur() : "N/A") + " - Montant : " + c.getMontant() + " DA (Date d'échéance : " + c.getDateEcheance() + ")",
                        "WARNING",
                        c.getDateCreation(),
                        "/finance/cheques",
                        c.getNumeroPiece(),
                        c.getMontant()
                );
                allAlertes.add(alerte);
            }
            summary.setNbEcheancesProches(chequesEcheance.size());
        } catch (Exception e) {
            // Tolérance
        }

        // 4. Alertes Commandes en attente de préparation
        try {
            List<CommandeClient> commandes = commandeClientRepository.findByStatut(StatutCommandeClient.CONFIRMEE);
            for (CommandeClient cc : commandes) {
                String clientNom = (cc.getClient() != null) ? cc.getClient().getNom() : (cc.getClientNom() != null ? cc.getClientNom() : "Client");
                AlerteNotificationDTO alerte = new AlerteNotificationDTO(
                        "CMD-" + cc.getId(),
                        TypeNotificationAlerte.COMMANDE_EN_ATTENTE,
                        "Commande confirmée à traiter : " + cc.getNumeroCommande(),
                        "Client " + clientNom + " - Date commande : " + cc.getDateCommande(),
                        "INFO",
                        cc.getDateCommande(),
                        "/ventes/commandes",
                        cc.getNumeroCommande(),
                        cc.getMontantTTC()
                );
                allAlertes.add(alerte);
            }
            summary.setNbCommandesEnAttente(commandes.size());
        } catch (Exception e) {
            // Tolérance
        }

        summary.setTotalAlertes(allAlertes.size());
        summary.setAlertes(allAlertes);
        return summary;
    }
}
