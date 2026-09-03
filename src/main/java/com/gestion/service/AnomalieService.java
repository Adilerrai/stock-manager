package com.gestion.service;

import com.gestion.persistent.dto.AnomalieDTO;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnomalieService {

    private final ClientRepository clientRepository;
    private final FactureRepository factureRepository;
    private final StockQualiteRepository stockQualiteRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final PaiementRepository paiementRepository;
    private final ProduitRepository produitRepository;

    public AnomalieService(ClientRepository clientRepository,
                           FactureRepository factureRepository,
                           StockQualiteRepository stockQualiteRepository,
                           SessionCaisseRepository sessionCaisseRepository,
                           PaiementRepository paiementRepository,
                           ProduitRepository produitRepository) {
        this.clientRepository = clientRepository;
        this.factureRepository = factureRepository;
        this.stockQualiteRepository = stockQualiteRepository;
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.paiementRepository = paiementRepository;
        this.produitRepository = produitRepository;
    }

    /**
     * Détecte toutes les anomalies du système
     */
    public List<AnomalieDTO> detecterToutesLesAnomalies() {
        List<AnomalieDTO> result = new ArrayList<>();
        result.addAll(detecterAnomaliesCredits());
        result.addAll(detecterAnomaliesFactures());
        result.addAll(detecterAnomaliesStock());
        result.addAll(detecterAnomaliesCaisse());
        result.addAll(detecterAnomaliesPaiements());
        return result;
    }

    /**
     * Anomalies de crédit client (dépassement plafond)
     */
    public List<AnomalieDTO> detecterAnomaliesCredits() {
        List<AnomalieDTO> anomalies = new ArrayList<>();
        List<Client> clients = clientRepository.findAll();

        for (Client c : clients) {
            BigDecimal autorise = c.getCreditAutorise() != null ? c.getCreditAutorise() : BigDecimal.ZERO;
            BigDecimal utilise = c.getCreditUtilise() != null ? c.getCreditUtilise() : BigDecimal.ZERO;

            if (autorise.compareTo(BigDecimal.ZERO) > 0 && utilise.compareTo(autorise) > 0) {
                BigDecimal depassement = utilise.subtract(autorise);
                AnomalieDTO a = new AnomalieDTO(
                        "CREDIT_DEPASSE",
                        "CRITIQUE",
                        "Plafond de crédit dépassé",
                        String.format("Le client %s dépasse son plafond de crédit de %s DA (Autorisé: %s DA, Utilisé: %s DA)",
                                c.getNomComplet() != null ? c.getNomComplet() : c.getNom(),
                                depassement, autorise, utilise),
                        "CLIENT",
                        c.getId(),
                        c.getNom()
                );
                Map<String, Object> details = new HashMap<>();
                details.put("creditAutorise", autorise);
                details.put("creditUtilise", utilise);
                details.put("depassement", depassement);
                a.setDetails(details);
                anomalies.add(a);
            }
        }
        return anomalies;
    }

    /**
     * Anomalies de factures (impayées échues > 30j ou échues)
     */
    public List<AnomalieDTO> detecterAnomaliesFactures() {
        List<AnomalieDTO> anomalies = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Facture> facturesEchues = factureRepository.findFacturesEchues(today);

        for (Facture f : facturesEchues) {
            if (f.getDateEcheance() != null) {
                long joursRetard = ChronoUnit.DAYS.between(f.getDateEcheance(), today);
                String severite = joursRetard > 30 ? "CRITIQUE" : "AVERTISSEMENT";
                String clientNom = f.getClient() != null ? (f.getClient().getNomComplet() != null ? f.getClient().getNomComplet() : f.getClient().getNom()) : "Inconnu";

                AnomalieDTO a = new AnomalieDTO(
                        "FACTURE_ECHUE",
                        severite,
                        String.format("Facture en retard de %d jours", joursRetard),
                        String.format("La facture %s du client %s est échue depuis %d jours (Reste dû: %s DA)",
                                f.getNumeroFacture(), clientNom, joursRetard, f.getMontantRestant()),
                        "FACTURE",
                        f.getId(),
                        f.getNumeroFacture()
                );
                Map<String, Object> details = new HashMap<>();
                details.put("joursRetard", joursRetard);
                details.put("montantRestant", f.getMontantRestant());
                details.put("dateEcheance", f.getDateEcheance().toString());
                a.setDetails(details);
                anomalies.add(a);
            }
        }

        // Détection factures annulées mais avec encaissement
        List<Facture> factures = factureRepository.findAll();
        for (Facture f : factures) {
            if (Boolean.TRUE.equals(f.getAnnulee()) && f.getMontantPaye() != null && f.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
                AnomalieDTO a = new AnomalieDTO(
                        "FACTURE_ANNULEE_PAYEE",
                        "CRITIQUE",
                        "Facture annulée avec encaissements enregistrés",
                        String.format("La facture %s est marquée comme annulée mais enregistre un montant payé de %s DA",
                                f.getNumeroFacture(), f.getMontantPaye()),
                        "FACTURE",
                        f.getId(),
                        f.getNumeroFacture()
                );
                anomalies.add(a);
            }
        }

        return anomalies;
    }

    /**
     * Anomalies de stock (négatif, rupture, sous-seuil)
     */
    public List<AnomalieDTO> detecterAnomaliesStock() {
        List<AnomalieDTO> anomalies = new ArrayList<>();
        List<StockQualite> stocks = stockQualiteRepository.findAll();

        for (StockQualite sq : stocks) {
            BigDecimal dispo = sq.getQuantiteDisponible() != null ? sq.getQuantiteDisponible() : BigDecimal.ZERO;
            BigDecimal seuil = sq.getSeuilAlerte() != null ? sq.getSeuilAlerte() : BigDecimal.ZERO;
            String prodNom = sq.getProduit() != null ? (sq.getProduit().getDesignation() != null ? sq.getProduit().getDesignation() : sq.getProduit().getReference()) : "Produit inconnu";
            String qualiteStr = sq.getQualite() != null ? sq.getQualite().name() : "";

            if (dispo.compareTo(BigDecimal.ZERO) < 0) {
                AnomalieDTO a = new AnomalieDTO(
                        "STOCK_NEGATIF",
                        "CRITIQUE",
                        "Stock négatif détecté",
                        String.format("Le produit %s (%s) a une quantité négative : %s", prodNom, qualiteStr, dispo),
                        "PRODUIT",
                        sq.getProduit() != null ? sq.getProduit().getId() : null,
                        sq.getProduit() != null ? sq.getProduit().getReference() : null
                );
                anomalies.add(a);
            } else if (dispo.compareTo(BigDecimal.ZERO) == 0) {
                AnomalieDTO a = new AnomalieDTO(
                        "STOCK_RUPTURE",
                        "CRITIQUE",
                        "Produit en rupture totale",
                        String.format("Le produit %s (%s) est en rupture totale de stock", prodNom, qualiteStr),
                        "PRODUIT",
                        sq.getProduit() != null ? sq.getProduit().getId() : null,
                        sq.getProduit() != null ? sq.getProduit().getReference() : null
                );
                anomalies.add(a);
            } else if (seuil.compareTo(BigDecimal.ZERO) > 0 && dispo.compareTo(seuil) <= 0) {
                AnomalieDTO a = new AnomalieDTO(
                        "STOCK_BAS",
                        "AVERTISSEMENT",
                        "Stock sous le seuil d'alerte",
                        String.format("Le produit %s (%s) a un stock critique : %s restant (seuil alerte : %s)",
                                prodNom, qualiteStr, dispo, seuil),
                        "PRODUIT",
                        sq.getProduit() != null ? sq.getProduit().getId() : null,
                        sq.getProduit() != null ? sq.getProduit().getReference() : null
                );
                anomalies.add(a);
            }
        }
        return anomalies;
    }

    /**
     * Anomalies de caisse (écarts de caisse non nuls)
     */
    public List<AnomalieDTO> detecterAnomaliesCaisse() {
        List<AnomalieDTO> anomalies = new ArrayList<>();
        List<SessionCaisse> sessionsAvecEcart = sessionCaisseRepository.findSessionsAvecEcart();

        for (SessionCaisse s : sessionsAvecEcart) {
            String caissierNom = s.getCaissier() != null ? (s.getCaissier().getNomComplet() != null ? s.getCaissier().getNomComplet() : s.getCaissier().getUsername()) : "Inconnu";
            BigDecimal ecart = s.getEcartCaisse();
            String typeEcart = ecart.compareTo(BigDecimal.ZERO) > 0 ? "Excédent" : "Déficit";

            AnomalieDTO a = new AnomalieDTO(
                    "ECART_CAISSE",
                    "AVERTISSEMENT",
                    String.format("Écart de caisse (%s: %s DA)", typeEcart, ecart.abs()),
                    String.format("La session de caisse %s (%s) présente un écart de %s DA (Théorique: %s, Réel: %s)",
                            s.getReference(), caissierNom, ecart, s.getMontantTheoriqueCloture(), s.getMontantReelCloture()),
                    "CAISSE",
                    s.getId(),
                    s.getReference()
            );
            Map<String, Object> details = new HashMap<>();
            details.put("ecart", ecart);
            details.put("theorique", s.getMontantTheoriqueCloture());
            details.put("reel", s.getMontantReelCloture());
            a.setDetails(details);
            anomalies.add(a);
        }
        return anomalies;
    }

    /**
     * Anomalies de paiements (paiements non affectés ou sans vente/facture)
     */
    public List<AnomalieDTO> detecterAnomaliesPaiements() {
        List<AnomalieDTO> anomalies = new ArrayList<>();
        List<Paiement> paiements = paiementRepository.findAll();

        for (Paiement p : paiements) {
            if (!Boolean.TRUE.equals(p.getAnnule()) && p.getVente() == null && p.getFacture() == null) {
                AnomalieDTO a = new AnomalieDTO(
                        "PAIEMENT_NON_AFFECTE",
                        "AVERTISSEMENT",
                        "Paiement sans pièce justificative",
                        String.format("Le paiement %s de %s DA n'est rattaché ni à une vente ni à une facture",
                                p.getNumeroPaiement(), p.getMontant()),
                        "PAIEMENT",
                        p.getId(),
                        p.getNumeroPaiement()
                );
                anomalies.add(a);
            }
        }
        return anomalies;
    }

    /**
     * Anomalies filtrées par sévérité
     */
    public List<AnomalieDTO> detecterParSeverite(String severite) {
        return detecterToutesLesAnomalies().stream()
                .filter(a -> severite.equalsIgnoreCase(a.getSeverite()))
                .collect(Collectors.toList());
    }
}
