package com.gestion.service;

import com.gestion.persistent.dto.DashboardDTO;
import com.gestion.persistent.dto.DashboardDTO.TopClientDTO;
import com.gestion.persistent.dto.DashboardDTO.TopProduitDTO;
import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.StatutSessionCaisse;
import com.gestion.persistent.enums.StatutVente;
import com.gestion.persistent.model.SessionCaisse;
import com.gestion.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final FactureRepository factureRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final PaiementRepository paiementRepository;
    private final StockQualiteRepository stockQualiteRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final DepenseRepository depenseRepository;
    private final MargeService margeService;
    private final AnomalieService anomalieService;
    private final BarometreService barometreService;

    public DashboardService(VenteRepository venteRepository,
                            LigneVenteRepository ligneVenteRepository,
                            FactureRepository factureRepository,
                            FactureAchatRepository factureAchatRepository,
                            PaiementRepository paiementRepository,
                            StockQualiteRepository stockQualiteRepository,
                            SessionCaisseRepository sessionCaisseRepository,
                            DepenseRepository depenseRepository,
                            MargeService margeService,
                            AnomalieService anomalieService,
                            BarometreService barometreService) {
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.paiementRepository = paiementRepository;
        this.stockQualiteRepository = stockQualiteRepository;
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.depenseRepository = depenseRepository;
        this.margeService = margeService;
        this.anomalieService = anomalieService;
        this.barometreService = barometreService;
    }

    /**
     * Génère la vue complète du tableau de bord décisionnel
     */
    public DashboardDTO getDashboardMetrics() {
        DashboardDTO dashboard = new DashboardDTO();

        LocalDate today = LocalDate.now();
        LocalDate hier = today.minusDays(1);
        LocalDate debutMois = today.withDayOfMonth(1);

        LocalDateTime debutAujourdhui = today.atStartOfDay();
        LocalDateTime finAujourdhui = today.atTime(LocalTime.MAX);
        LocalDateTime debutHier = hier.atStartOfDay();
        LocalDateTime finHier = hier.atTime(LocalTime.MAX);
        LocalDateTime debutMoisDT = debutMois.atStartOfDay();

        // 1. Chiffre d'affaires
        BigDecimal caAujourdhui = venteRepository.calculerChiffreAffaires(debutAujourdhui, finAujourdhui);
        dashboard.setCaAujourdhui(caAujourdhui != null ? caAujourdhui : BigDecimal.ZERO);

        BigDecimal caHier = venteRepository.calculerChiffreAffaires(debutHier, finHier);
        dashboard.setCaHier(caHier != null ? caHier : BigDecimal.ZERO);

        BigDecimal caMois = venteRepository.calculerChiffreAffaires(debutMoisDT, finAujourdhui);
        dashboard.setCaMoisEnCours(caMois != null ? caMois : BigDecimal.ZERO);

        Long nbVentesAuj = venteRepository.countVentesByStatutAndPeriode(StatutVente.VALIDEE, debutAujourdhui, finAujourdhui);
        dashboard.setNombreVentesAujourdhui(nbVentesAuj != null ? nbVentesAuj : 0L);

        Long nbVentesHier = venteRepository.countVentesByStatutAndPeriode(StatutVente.VALIDEE, debutHier, finHier);
        dashboard.setNombreVentesHier(nbVentesHier != null ? nbVentesHier : 0L);

        Long nbVentesMois = venteRepository.countVentesByStatutAndPeriode(StatutVente.VALIDEE, debutMoisDT, finAujourdhui);
        dashboard.setNombreVentesMoisEnCours(nbVentesMois != null ? nbVentesMois : 0L);

        // 2. Créances Clients
        BigDecimal totalCreances = factureRepository.sumTotalCreances();
        dashboard.setTotalCreancesClients(totalCreances != null ? totalCreances : BigDecimal.ZERO);

        BigDecimal creancesEchues = factureRepository.sumCreancesEchues(today);
        dashboard.setTotalCreancesEchues(creancesEchues != null ? creancesEchues : BigDecimal.ZERO);

        BigDecimal creancesNonEchues = dashboard.getTotalCreancesClients().subtract(dashboard.getTotalCreancesEchues());
        dashboard.setTotalCreancesNonEchues(creancesNonEchues.compareTo(BigDecimal.ZERO) >= 0 ? creancesNonEchues : BigDecimal.ZERO);

        Long nbFacturesImpayees = factureRepository.countFacturesImpayees();
        dashboard.setNombreFacturesImpayees(nbFacturesImpayees != null ? nbFacturesImpayees : 0L);

        // 3. Encaissements & Trésorerie du jour
        BigDecimal totalEncaissements = paiementRepository.sumMontantByPeriode(debutAujourdhui, finAujourdhui);
        dashboard.setTotalEncaissementsAujourdhui(totalEncaissements != null ? totalEncaissements : BigDecimal.ZERO);

        Map<String, BigDecimal> encaissementsMap = new HashMap<>();
        for (ModePaiement mode : ModePaiement.values()) {
            BigDecimal sum = paiementRepository.sumMontantByModePaiement(mode, debutAujourdhui, finAujourdhui);
            encaissementsMap.put(mode.name(), sum != null ? sum : BigDecimal.ZERO);
        }
        dashboard.setEncaissementsParMode(encaissementsMap);

        // Caisse active
        List<SessionCaisse> sessionsOuvertes = sessionCaisseRepository.findByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE);
        BigDecimal soldeCaisse = BigDecimal.ZERO;
        for (SessionCaisse sc : sessionsOuvertes) {
            BigDecimal fond = sc.getFondDeCaisseInitial() != null ? sc.getFondDeCaisseInitial() : BigDecimal.ZERO;
            BigDecimal esp = sc.getTotalEspeces() != null ? sc.getTotalEspeces() : BigDecimal.ZERO;
            soldeCaisse = soldeCaisse.add(fond).add(esp);
        }
        dashboard.setSoldeCaisseActuel(soldeCaisse);

        // Dettes fournisseurs
        BigDecimal dettesFrs = factureAchatRepository.sumTotalDettesFournisseurs();
        dashboard.setTotalDettesFournisseurs(dettesFrs != null ? dettesFrs : BigDecimal.ZERO);

        // 4. Stocks & Alertes
        Long ruptures = stockQualiteRepository.countEnRupture();
        dashboard.setNombreProduitsEnRupture(ruptures != null ? ruptures : 0L);

        Long stockBas = stockQualiteRepository.countStockBas();
        dashboard.setNombreProduitsStockBas(stockBas != null ? stockBas : 0L);

        // 5. Marges & Résultat Net du mois en cours
        try {
            MargeDTO marge = margeService.calculerMargeGlobale(debutMois, today);
            dashboard.setMargeMoisEnCours(marge.getMargeNetteCommerciale());
            dashboard.setTauxMargeMoisEnCours(marge.getTauxMarge());

            BigDecimal depensesMois = depenseRepository.sumMontantByPeriode(debutMois, today);
            if (depensesMois == null) depensesMois = BigDecimal.ZERO;
            dashboard.setTotalDepensesMoisEnCours(depensesMois);
            dashboard.setResultatNetMoisEnCours(marge.getMargeNetteCommerciale().subtract(depensesMois));
        } catch (Exception e) {
            dashboard.setMargeMoisEnCours(BigDecimal.ZERO);
            dashboard.setTauxMargeMoisEnCours(BigDecimal.ZERO);
            dashboard.setTotalDepensesMoisEnCours(BigDecimal.ZERO);
            dashboard.setResultatNetMoisEnCours(BigDecimal.ZERO);
        }

        // 6. Top Clients (Top 5)
        List<Object[]> topClientsRaw = venteRepository.findTopClients(PageRequest.of(0, 5));
        List<TopClientDTO> topClients = new ArrayList<>();
        if (topClientsRaw != null) {
            for (Object[] row : topClientsRaw) {
                Long cId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String nom = row[1] != null ? row[1].toString() : "";
                String nomComplet = row[2] != null ? row[2].toString() : nom;
                BigDecimal total = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
                Long count = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                topClients.add(new TopClientDTO(cId, nom, nomComplet, total, count));
            }
        }
        dashboard.setTopClients(topClients);

        // 7. Top Produits (Top 5)
        List<Object[]> topProduitsRaw = ligneVenteRepository.findTopProduits(PageRequest.of(0, 5));
        List<TopProduitDTO> topProduits = new ArrayList<>();
        if (topProduitsRaw != null) {
            for (Object[] row : topProduitsRaw) {
                Long pId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String ref = row[1] != null ? row[1].toString() : "";
                String desig = row[2] != null ? row[2].toString() : "";
                BigDecimal qte = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
                BigDecimal total = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
                topProduits.add(new TopProduitDTO(pId, ref, desig, qte, total));
            }
        }
        dashboard.setTopProduits(topProduits);

        // 8. Anomalies récentes (top 10 alertes)
        try {
            dashboard.setAlertesRecentes(anomalieService.detecterToutesLesAnomalies().stream()
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            dashboard.setAlertesRecentes(new ArrayList<>());
        }

        // 9. Baromètre de Santé de l'Entreprise
        try {
            dashboard.setBarometre(barometreService.calculerBarometre());
        } catch (Exception e) {
            dashboard.setBarometre(null);
        }

        return dashboard;
    }
}
