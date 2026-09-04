package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.NotificationDTO;
import com.gestion.persistent.enums.SeveriteNotification;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.enums.TypeNotification;
import com.gestion.persistent.model.ChequeEffet;
import com.gestion.persistent.model.Facture;
import com.gestion.persistent.model.Notification;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.ChequeEffetRepository;
import com.gestion.repository.FactureRepository;
import com.gestion.repository.NotificationRepository;
import com.gestion.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProduitRepository produitRepository;
    private final FactureRepository factureRepository;
    private final ChequeEffetRepository chequeRepository;
    private final com.gestion.repository.StockQualiteRepository stockQualiteRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               ProduitRepository produitRepository,
                               FactureRepository factureRepository,
                               ChequeEffetRepository chequeRepository,
                               com.gestion.repository.StockQualiteRepository stockQualiteRepository) {
        this.notificationRepository = notificationRepository;
        this.produitRepository = produitRepository;
        this.factureRepository = factureRepository;
        this.chequeRepository = chequeRepository;
        this.stockQualiteRepository = stockQualiteRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(Boolean nonLuesUniquement) {
        Long tenantId = getTenantId();
        List<Notification> liste;
        if (Boolean.TRUE.equals(nonLuesUniquement)) {
            liste = notificationRepository.findByPointDeVenteIdAndLuFalseOrderByDateCreationDesc(tenantId);
        } else {
            liste = notificationRepository.findByPointDeVenteIdOrderByDateCreationDesc(tenantId);
        }
        return liste.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long countNonLues() {
        return notificationRepository.countByPointDeVenteIdAndLuFalse(getTenantId());
    }

    public NotificationDTO creerNotification(String titre, String message, TypeNotification type,
                                             SeveriteNotification severite, String lienAction) {
        Long tenantId = getTenantId();
        Notification n = new Notification(titre, message, type, severite, lienAction, tenantId);
        return toDto(notificationRepository.save(n));
    }

    public NotificationDTO marquerCommeLue(Long id) {
        Notification n = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification introuvable: " + id));
        n.setLu(true);
        n.setDateLecture(LocalDateTime.now());
        return toDto(notificationRepository.save(n));
    }

    public NotificationDTO marquerCommeLu(Long id) {
        return marquerCommeLue(id);
    }

    public void marquerToutesCommeLues() {
        notificationRepository.marquerToutesCommeLues(getTenantId());
    }

    public void supprimerNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    /**
     * Génère automatiquement les alertes du système basées sur l'état courant:
     * - Produits en rupture ou sous le seuil d'alerte
     * - Factures impayées échues
     * - Chèques et effets arrivant à échéance sous 3 jours
     */
    public int genererAlertesAutomatiques() {
        Long tenantId = getTenantId();
        int alertesGenerees = 0;

        // 1. Alerte Stocks Faibles
        try {
            List<com.gestion.persistent.model.StockQualite> stocksAlerte = stockQualiteRepository.findStocksEnAlerte();
            for (com.gestion.persistent.model.StockQualite sq : stocksAlerte) {
                if (sq.getProduit() != null) {
                    String titre = "Stock critique : " + sq.getProduit().getDesignation();
                    if (!notificationRepository.existsByPointDeVenteIdAndTitreAndLuFalse(tenantId, titre)) {
                        String message = String.format("Le stock disponible (%s) a atteint le seuil d'alerte (%s).",
                            sq.getQuantiteDisponible(), sq.getSeuilAlerte());
                        notificationRepository.save(new Notification(
                            titre, message, TypeNotification.STOCK_FAIBLE, SeveriteNotification.WARNING,
                            "/stocks", tenantId
                        ));
                        alertesGenerees++;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Alerte Factures Impayées échues
        try {
            List<Facture> factures = factureRepository.findAll();
            LocalDate today = LocalDate.now();
            for (Facture f : factures) {
                if (f.getStatut() != StatutFacture.PAYEE_TOTALEMENT && f.getStatut() != StatutFacture.ANNULEE) {
                    if (f.getDateEcheance() != null && f.getDateEcheance().isBefore(today)) {
                        String titre = "Facture impayée échue : " + f.getNumeroFacture();
                        if (!notificationRepository.existsByPointDeVenteIdAndTitreAndLuFalse(tenantId, titre)) {
                            String clientNom = f.getClient() != null ? f.getClient().getNom() : "Client";
                            String message = String.format("La facture %s émise pour %s (montant TTC %s) est échue depuis le %s.",
                                f.getNumeroFacture(), clientNom, f.getMontantTTC(), f.getDateEcheance());
                            notificationRepository.save(new Notification(
                                titre, message, TypeNotification.FACTURE_IMPAYEE, SeveriteNotification.DANGER,
                                "/factures", tenantId
                            ));
                            alertesGenerees++;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Alerte Échéances de Chèques & Effets
        try {
            List<ChequeEffet> cheques = chequeRepository.findEcheancesProches(tenantId, LocalDate.now().plusDays(3));
            for (ChequeEffet c : cheques) {
                String titre = "Échéance proche : " + c.getTypeEffet() + " N° " + c.getNumeroPiece();
                if (!notificationRepository.existsByPointDeVenteIdAndTitreAndLuFalse(tenantId, titre)) {
                    String message = String.format("Le %s N° %s d'un montant de %s arrive à échéance le %s.",
                        c.getTypeEffet(), c.getNumeroPiece(), c.getMontant(), c.getDateEcheance());
                    notificationRepository.save(new Notification(
                        titre, message, TypeNotification.ECHEANCE_CHEQUE, SeveriteNotification.INFO,
                        "/finance/cheques", tenantId
                    ));
                    alertesGenerees++;
                }
            }
        } catch (Exception ignored) {}

        return alertesGenerees;
    }

    private NotificationDTO toDto(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setTitre(n.getTitre());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setSeverite(n.getSeverite());
        dto.setLu(n.getLu());
        dto.setLienAction(n.getLienAction());
        dto.setDateCreation(n.getDateCreation());
        return dto;
    }
}
