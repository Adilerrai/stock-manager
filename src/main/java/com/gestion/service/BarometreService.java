package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.BarometreEntrepriseDTO;
import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.PilierSanteDTO;
import com.gestion.persistent.dto.SyntheseTresorerieDTO;
import com.gestion.persistent.enums.StatutSanteEntreprise;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BarometreService {

    private final FactureRepository factureRepository;
    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;
    private final StockQualiteRepository stockQualiteRepository;
    private final DepenseRepository depenseRepository;
    private final TresorerieAvanceeService tresorerieService;
    private final MargeService margeService;

    public BarometreService(FactureRepository factureRepository,
                            VenteRepository venteRepository,
                            ProduitRepository produitRepository,
                            StockQualiteRepository stockQualiteRepository,
                            DepenseRepository depenseRepository,
                            TresorerieAvanceeService tresorerieService,
                            MargeService margeService) {
        this.factureRepository = factureRepository;
        this.venteRepository = venteRepository;
        this.produitRepository = produitRepository;
        this.stockQualiteRepository = stockQualiteRepository;
        this.depenseRepository = depenseRepository;
        this.tresorerieService = tresorerieService;
        this.margeService = margeService;
    }

    /**
     * Calcule le Baromètre de santé globale et les 5 piliers de l'entreprise connectée
     */
    public BarometreEntrepriseDTO calculerBarometre() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;

        BarometreEntrepriseDTO barometre = new BarometreEntrepriseDTO();
        barometre.setPointDeVenteId(tenantId);
        barometre.setDateCalcul(LocalDateTime.now());

        LocalDate today = LocalDate.now();
        LocalDate debutMois = today.withDayOfMonth(1);
        LocalDateTime debutMoisDT = debutMois.atStartOfDay();
        LocalDateTime finAujourdhui = today.atTime(LocalTime.MAX);

        List<String> pointsForts = new ArrayList<>();
        List<String> pointsVigilance = new ArrayList<>();
        List<String> recommandations = new ArrayList<>();

        // -------------------------------------------------------------
        // 1. PILIER TRÉSORERIE & LIQUIDITÉS (Poids 25%)
        // -------------------------------------------------------------
        SyntheseTresorerieDTO treso = tresorerieService.getSynthese();
        BigDecimal liquidites = treso != null && treso.getTresorerieDisponibleGlobale() != null
                ? treso.getTresorerieDisponibleGlobale() : BigDecimal.ZERO;

        BigDecimal depensesMois = depenseRepository.sumMontantByPeriode(debutMois, today);
        if (depensesMois == null || depensesMois.compareTo(BigDecimal.ZERO) == 0) depensesMois = new BigDecimal("100000"); // estimation base

        int scoreTreso = 70;
        String commTreso;
        String valTreso = String.format("%,.0f DA dispo", liquidites);
        String statutTreso = "BON";
        String coulTreso = "#3b82f6";

        if (liquidites.compareTo(depensesMois.multiply(new BigDecimal("3"))) >= 0) {
            scoreTreso = 95;
            commTreso = "Excellente couverture : plus de 3 mois de dépenses sécurisés en caisse et banque.";
            statutTreso = "EXCELLENT";
            coulTreso = "#10b981";
            pointsForts.add("Trésorerie robuste capable d'absorber les imprévus sans découvert.");
        } else if (liquidites.compareTo(depensesMois) >= 0) {
            scoreTreso = 75;
            commTreso = "Liquidités suffisantes couvrant les charges courantes du mois.";
            statutTreso = "BON";
            coulTreso = "#3b82f6";
            pointsForts.add("Couverture des dépenses d'exploitation assurée.");
        } else {
            scoreTreso = 40;
            commTreso = "Tension de trésorerie : liquidités inférieures aux charges d'exploitation mensuelles.";
            statutTreso = "ALARME";
            coulTreso = "#ef4444";
            pointsVigilance.add("Liquidités faibles : accélérer les encaissements pour éviter le gel des paiements.");
            recommandations.add("Limiter les dépenses non urgentes et effectuer un versement de sécurité.");
        }
        barometre.setTresorerie(new PilierSanteDTO("Trésorerie & Liquidités", scoreTreso, 25, valTreso, commTreso, statutTreso, coulTreso));

        // -------------------------------------------------------------
        // 2. PILIER RECOUVREMENT & CRÉANCES CLIENTS (Poids 25%)
        // -------------------------------------------------------------
        BigDecimal totalCreances = factureRepository.sumTotalCreancesByPointDeVenteId(tenantId);
        BigDecimal creancesEchues = factureRepository.sumCreancesEchuesByPointDeVenteId(today, tenantId);
        if (totalCreances == null) totalCreances = BigDecimal.ZERO;
        if (creancesEchues == null) creancesEchues = BigDecimal.ZERO;

        int scoreRecouvrement = 80;
        String commRecouv;
        String statutRecouv = "BON";
        String coulRecouv = "#3b82f6";
        String valRecouv;

        if (totalCreances.compareTo(BigDecimal.ZERO) == 0) {
            scoreRecouvrement = 100;
            valRecouv = "0 DA d'impayés";
            commRecouv = "Aucune créance impayée. Rentrées financières immédiates.";
            statutRecouv = "EXCELLENT";
            coulRecouv = "#10b981";
            pointsForts.add("Politique d'encaissement exemplaire : aucun retard de paiement.");
        } else {
            BigDecimal ratioEchues = creancesEchues.divide(totalCreances, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            valRecouv = String.format("%.1f%% échues", ratioEchues);

            if (ratioEchues.compareTo(new BigDecimal("15")) <= 0) {
                scoreRecouvrement = 90;
                commRecouv = "Très bon taux de recouvrement : moins de 15% des créances sont en retard.";
                statutRecouv = "EXCELLENT";
                coulRecouv = "#10b981";
                pointsForts.add("Les clients règlent généralement dans les délais convenus.");
            } else if (ratioEchues.compareTo(new BigDecimal("35")) <= 0) {
                scoreRecouvrement = 65;
                commRecouv = "Retards modérés : surveiller les factures dépassant 30 jours.";
                statutRecouv = "MOYEN";
                coulRecouv = "#f59e0b";
                pointsVigilance.add("Une part notable des créances a dépassé son échéance.");
                recommandations.add("Lancer une campagne de relance téléphonique sur les créances échues.");
            } else {
                scoreRecouvrement = 35;
                commRecouv = "Alerte impayés : plus de 35% de vos créances sont échues non réglées.";
                statutRecouv = "ALARME";
                coulRecouv = "#ef4444";
                pointsVigilance.add("Risque d'impayés critique pesant lourdement sur la rentabilité.");
                recommandations.add("Bloquer temporairement les livraisons à crédit pour les clients en retard.");
            }
        }
        barometre.setRecouvrement(new PilierSanteDTO("Recouvrement & Crédits", scoreRecouvrement, 25, valRecouv, commRecouv, statutRecouv, coulRecouv));

        // -------------------------------------------------------------
        // 3. PILIER RENTABILITÉ & MARGE COMMERCIALE (Poids 20%)
        // -------------------------------------------------------------
        int scoreMarge = 75;
        String valMarge = "20% marge";
        String commMarge;
        String statutMarge = "BON";
        String coulMarge = "#3b82f6";

        try {
            MargeDTO m = margeService.calculerMargeGlobale(debutMois, today);
            BigDecimal taux = m != null && m.getTauxMarge() != null ? m.getTauxMarge() : new BigDecimal("20.0");
            valMarge = String.format("%.1f%%", taux);

            if (taux.compareTo(new BigDecimal("25")) >= 0) {
                scoreMarge = 95;
                commMarge = "Rentabilité excellente : marges confortables supérieures à 25%.";
                statutMarge = "EXCELLENT";
                coulMarge = "#10b981";
                pointsForts.add("Marges commerciales fortes garantissant une bonne rentabilité nette.");
            } else if (taux.compareTo(new BigDecimal("15")) >= 0) {
                scoreMarge = 75;
                commMarge = "Rentabilité saine et conforme aux standards du secteur.";
                statutMarge = "BON";
                coulMarge = "#3b82f6";
            } else {
                scoreMarge = 45;
                commMarge = "Marge érodée (inférieure à 15%) : vérifier remises et hausses d'achat.";
                statutMarge = "ALARME";
                coulMarge = "#ef4444";
                pointsVigilance.add("Marges faibles : les remises accordées diminuent trop les gains.");
                recommandations.add("Plafonner les remises commerciales et renégocier les prix d'achat.");
            }
        } catch (Exception e) {
            commMarge = "Marge estimée en cours de calcul.";
        }
        barometre.setRentabilite(new PilierSanteDTO("Rentabilité Commerciale", scoreMarge, 20, valMarge, commMarge, statutMarge, coulMarge));

        // -------------------------------------------------------------
        // 4. PILIER PERFORMANCE COMMERCIALE (Poids 15%)
        // -------------------------------------------------------------
        BigDecimal caMois = venteRepository.calculerChiffreAffairesByPointDeVenteId(debutMoisDT, finAujourdhui, tenantId);
        if (caMois == null) caMois = BigDecimal.ZERO;

        int scoreCom = 80;
        String valCom = String.format("%,.0f DA", caMois);
        String commCom = "Activité commerciale active sur le mois en cours.";
        String statutCom = "BON";
        String coulCom = "#3b82f6";

        if (caMois.compareTo(new BigDecimal("500000")) >= 0) {
            scoreCom = 90;
            statutCom = "EXCELLENT";
            coulCom = "#10b981";
            pointsForts.add("Volume d'affaires soutenu généré sur la période.");
        } else if (caMois.compareTo(BigDecimal.ZERO) == 0) {
            scoreCom = 50;
            statutCom = "MOYEN";
            coulCom = "#f59e0b";
            recommandations.add("Dynamiser les ventes comptoir et relancer les devis en attente.");
        }
        barometre.setCommercial(new PilierSanteDTO("Activité Commerciale", scoreCom, 15, valCom, commCom, statutCom, coulCom));

        // -------------------------------------------------------------
        // 5. PILIER SANTÉ DU STOCK & DISPONIBILITÉ (Poids 15%)
        // -------------------------------------------------------------
        Long ruptures = stockQualiteRepository.countEnRupture();
        if (ruptures == null) ruptures = 0L;
        long totalProduits = produitRepository.count();

        int scoreStock = 85;
        String valStock = ruptures + " ruptures";
        String commStock;
        String statutStock = "BON";
        String coulStock = "#3b82f6";

        if (ruptures == 0) {
            scoreStock = 100;
            commStock = "Zéro rupture : disponibilité totale pour satisfaire les clients.";
            statutStock = "EXCELLENT";
            coulStock = "#10b981";
            pointsForts.add("Disponibilité optimale des produits en magasin et dépôt.");
        } else if (ruptures <= 2) {
            scoreStock = 80;
            commStock = "Stock bien maîtrisé avec seulement quelques références sous tension.";
            statutStock = "BON";
            coulStock = "#3b82f6";
            recommandations.add("Réapprovisionner les articles proches de l'épuisement.");
        } else {
            scoreStock = 45;
            commStock = "Ruptures multiples impactant directement les opportunités de vente.";
            statutStock = "ALARME";
            coulStock = "#ef4444";
            pointsVigilance.add("Nombre élevé de ruptures de stock provoquant des ventes perdues.");
            recommandations.add("Passer des commandes de réapprovisionnement urgentes auprès des fournisseurs.");
        }
        barometre.setStock(new PilierSanteDTO("Disponibilité des Stocks", scoreStock, 15, valStock, commStock, statutStock, coulStock));

        // -------------------------------------------------------------
        // SCORE GLOBAL PONDÉRÉ (SUR 100)
        // -------------------------------------------------------------
        int scoreFinal = (int) Math.round(
                (scoreTreso * 0.25) +
                (scoreRecouvrement * 0.25) +
                (scoreMarge * 0.20) +
                (scoreCom * 0.15) +
                (scoreStock * 0.15)
        );
        barometre.setScoreGlobal(scoreFinal);

        if (scoreFinal >= 80) {
            barometre.setStatutGlobal(StatutSanteEntreprise.EXCELLENTE);
        } else if (scoreFinal >= 65) {
            barometre.setStatutGlobal(StatutSanteEntreprise.BONNE);
        } else if (scoreFinal >= 50) {
            barometre.setStatutGlobal(StatutSanteEntreprise.VIGILANCE);
        } else {
            barometre.setStatutGlobal(StatutSanteEntreprise.CRITIQUE);
        }

        barometre.setPointsForts(pointsForts);
        barometre.setPointsVigilance(pointsVigilance);
        barometre.setRecommandationsDirecteur(recommandations);

        return barometre;
    }
}
