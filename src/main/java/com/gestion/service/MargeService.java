package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CockpitRentabiliteDTO;
import com.gestion.persistent.dto.EvolutionMargeDTO;
import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.MargeDTO.LigneMargeDTO;
import com.gestion.persistent.dto.StatistiqueMotifRetourDTO;
import com.gestion.persistent.enums.TypeAvoir;
import com.gestion.repository.AvoirRepository;
import com.gestion.repository.LigneFactureRepository;
import com.gestion.repository.LigneVenteRepository;
import com.gestion.repository.VenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MargeService {

    private final LigneVenteRepository ligneVenteRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final VenteRepository venteRepository;
    private final AvoirRepository avoirRepository;
    private final AvoirService avoirService;

    private static final String[] MOIS_FR = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    public MargeService(LigneVenteRepository ligneVenteRepository,
                        LigneFactureRepository ligneFactureRepository,
                        VenteRepository venteRepository,
                        AvoirRepository avoirRepository,
                        AvoirService avoirService) {
        this.ligneVenteRepository = ligneVenteRepository;
        this.ligneFactureRepository = ligneFactureRepository;
        this.venteRepository = venteRepository;
        this.avoirRepository = avoirRepository;
        this.avoirService = avoirService;
    }

    /**
     * Calcule la rentabilité et les marges globales pour une période donnée
     * selon la cascade stricte TADBEER :
     * CA Net HT = CA Brut HT - Remises - Avoirs
     * Marge Commerciale = CA Net HT - Coût des marchandises (PMP)
     * Taux de Marge % = (Marge Commerciale / CA Net HT) * 100
     */
    public MargeDTO calculerMargeGlobale(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debutDateTime = dateDebut.atStartOfDay();
        LocalDateTime finDateTime = dateFin.atTime(LocalTime.MAX);
        Long tenantId = TenantContext.getCurrentTenant();

        MargeDTO dto = new MargeDTO();
        dto.setDateDebut(dateDebut);
        dto.setDateFin(dateFin);

        BigDecimal caHT = BigDecimal.ZERO;
        BigDecimal coutHT = BigDecimal.ZERO;
        BigDecimal remisesLignes = BigDecimal.ZERO;

        // 1. Totaux depuis les ventes POS validées
        List<Object[]> totauxVente = tenantId != null
                ? ligneVenteRepository.calculerTotauxMargeGlobaleByPointDeVenteId(debutDateTime, finDateTime, tenantId)
                : ligneVenteRepository.calculerTotauxMargeGlobale(debutDateTime, finDateTime);
        if (totauxVente != null && !totauxVente.isEmpty()) {
            Object[] row = totauxVente.get(0);
            if (row != null && row.length >= 3) {
                caHT = caHT.add(toBigDecimal(row[0]));
                coutHT = coutHT.add(toBigDecimal(row[1]));
                remisesLignes = remisesLignes.add(toBigDecimal(row[2]));
            }
        }

        // 2. Totaux depuis les Factures de vente B2B validées
        List<Object[]> totauxFacture = tenantId != null
                ? ligneFactureRepository.calculerTotauxMargeGlobaleByPointDeVenteId(dateDebut, dateFin, tenantId)
                : ligneFactureRepository.calculerTotauxMargeGlobale(dateDebut, dateFin);
        if (totauxFacture != null && !totauxFacture.isEmpty()) {
            Object[] row = totauxFacture.get(0);
            if (row != null && row.length >= 3) {
                caHT = caHT.add(toBigDecimal(row[0]));
                coutHT = coutHT.add(toBigDecimal(row[1]));
                remisesLignes = remisesLignes.add(toBigDecimal(row[2]));
            }
        }

        // 3. Avoirs clients (retours de marchandises)
        BigDecimal retours = tenantId != null
                ? avoirRepository.sumMontantByPeriodeAndTypeAndPointDeVenteId(TypeAvoir.CLIENT, dateDebut, dateFin, tenantId)
                : avoirRepository.sumMontantByPeriodeAndType(TypeAvoir.CLIENT, dateDebut, dateFin);
        if (retours == null) retours = BigDecimal.ZERO;

        // CASCADE STRICTE TADBEER
        BigDecimal caNetHT = caHT.subtract(remisesLignes).subtract(retours);
        BigDecimal margeCommerciale = caNetHT.subtract(coutHT);

        BigDecimal tauxMarge = BigDecimal.ZERO;
        if (caNetHT.compareTo(BigDecimal.ZERO) > 0) {
            tauxMarge = margeCommerciale.multiply(new BigDecimal("100")).divide(caNetHT, 2, RoundingMode.HALF_UP);
        }

        dto.setChiffreAffairesHT(caHT);
        dto.setTotalRemises(remisesLignes);
        dto.setTotalRetoursAvoirs(retours);
        dto.setChiffreAffairesNetHT(caNetHT);
        dto.setCoutMarchandisesHT(coutHT);
        dto.setMargeCommerciale(margeCommerciale);
        dto.setMargeBrute(margeCommerciale);
        dto.setMargeNetteCommerciale(margeCommerciale);
        dto.setTauxMarge(tauxMarge);

        // 4. Détails
        dto.setMargesParProduit(calculerMargeParProduit(dateDebut, dateFin));
        dto.setMargesParCategorie(calculerMargeParCategorie(dateDebut, dateFin));
        dto.setMargesParClient(calculerMargeParClient(dateDebut, dateFin));

        return dto;
    }

    /**
     * Calcule l'évolution mensuelle sur les n derniers mois pour le graphique décisionnel
     */
    public List<EvolutionMargeDTO> calculerEvolutionMensuelle(int nbMois) {
        if (nbMois <= 0) nbMois = 6;
        if (nbMois > 24) nbMois = 24;

        List<EvolutionMargeDTO> list = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        Long tenantId = TenantContext.getCurrentTenant();

        for (int i = nbMois - 1; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            LocalDate debut = ym.atDay(1);
            LocalDate fin = ym.atEndOfMonth();

            LocalDateTime debutDateTime = debut.atStartOfDay();
            LocalDateTime finDateTime = fin.atTime(LocalTime.MAX);

            BigDecimal caHT = BigDecimal.ZERO;
            BigDecimal coutHT = BigDecimal.ZERO;
            BigDecimal remises = BigDecimal.ZERO;

            // POS
            List<Object[]> totauxVente = tenantId != null
                    ? ligneVenteRepository.calculerTotauxMargeGlobaleByPointDeVenteId(debutDateTime, finDateTime, tenantId)
                    : ligneVenteRepository.calculerTotauxMargeGlobale(debutDateTime, finDateTime);
            if (totauxVente != null && !totauxVente.isEmpty() && totauxVente.get(0) != null) {
                Object[] row = totauxVente.get(0);
                if (row.length >= 3) {
                    caHT = caHT.add(toBigDecimal(row[0]));
                    coutHT = coutHT.add(toBigDecimal(row[1]));
                    remises = remises.add(toBigDecimal(row[2]));
                }
            }

            // Factures B2B
            List<Object[]> totauxFacture = tenantId != null
                    ? ligneFactureRepository.calculerTotauxMargeGlobaleByPointDeVenteId(debut, fin, tenantId)
                    : ligneFactureRepository.calculerTotauxMargeGlobale(debut, fin);
            if (totauxFacture != null && !totauxFacture.isEmpty() && totauxFacture.get(0) != null) {
                Object[] row = totauxFacture.get(0);
                if (row.length >= 3) {
                    caHT = caHT.add(toBigDecimal(row[0]));
                    coutHT = coutHT.add(toBigDecimal(row[1]));
                    remises = remises.add(toBigDecimal(row[2]));
                }
            }

            // Avoirs
            BigDecimal avoirs = tenantId != null
                    ? avoirRepository.sumMontantByPeriodeAndTypeAndPointDeVenteId(TypeAvoir.CLIENT, debut, fin, tenantId)
                    : avoirRepository.sumMontantByPeriodeAndType(TypeAvoir.CLIENT, debut, fin);
            if (avoirs == null) avoirs = BigDecimal.ZERO;

            BigDecimal caNetHT = caHT.subtract(remises).subtract(avoirs);
            BigDecimal margeCommerciale = caNetHT.subtract(coutHT);

            BigDecimal taux = BigDecimal.ZERO;
            if (caNetHT.compareTo(BigDecimal.ZERO) > 0) {
                taux = margeCommerciale.multiply(new BigDecimal("100")).divide(caNetHT, 2, RoundingMode.HALF_UP);
            }

            String moisKey = ym.toString(); // "2026-04"
            String labelMois = MOIS_FR[ym.getMonthValue() - 1];

            list.add(new EvolutionMargeDTO(
                    moisKey, labelMois, ym.getYear(), ym.getMonthValue(),
                    caHT, remises, avoirs, caNetHT, coutHT, margeCommerciale, taux
            ));
        }

        return list;
    }

    /**
     * Construit le cockpit rentabilité exécutif tout-en-un pour le gérant
     */
    public CockpitRentabiliteDTO calculerCockpitRentabilite(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        CockpitRentabiliteDTO cockpit = new CockpitRentabiliteDTO();

        // 1. Cascade globale
        MargeDTO cascade = calculerMargeGlobale(dateDebut, dateFin);
        cockpit.setCascadeGlobale(cascade);

        // 2. Évolution mensuelle (6 mois)
        cockpit.setEvolutionMensuelle(calculerEvolutionMensuelle(6));

        // 3. Motifs des avoirs pour le camembert
        List<StatistiqueMotifRetourDTO> motifs = avoirService.getStatistiquesMotifsRetour(dateDebut, dateFin);
        cockpit.setMotifsAvoirs(motifs);

        // 4. Catégories / Familles
        cockpit.setMargesParFamille(cascade.getMargesParCategorie());

        // 5. Analyse Produits & Alertes
        List<LigneMargeDTO> prods = cascade.getMargesParProduit();
        List<LigneMargeDTO> topRentables = prods.stream()
                .filter(p -> p.getMarge() != null && p.getMarge().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getMarge().compareTo(a.getMarge()))
                .limit(5)
                .collect(Collectors.toList());
        cockpit.setTopProduitsRentables(topRentables);

        List<LigneMargeDTO> flopPerte = prods.stream()
                .filter(p -> p.isMargeNegative() || p.isSousPmp())
                .sorted(Comparator.comparing(LigneMargeDTO::getMarge))
                .limit(5)
                .collect(Collectors.toList());
        cockpit.setFlopProduitsPerte(flopPerte);

        // 6. Analyse Clients
        List<LigneMargeDTO> clients = cascade.getMargesParClient();
        List<LigneMargeDTO> topClients = clients.stream()
                .filter(c -> c.getMarge() != null && c.getMarge().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getMarge().compareTo(a.getMarge()))
                .limit(5)
                .collect(Collectors.toList());
        cockpit.setTopClientsRentables(topClients);

        List<LigneMargeDTO> clientsRisque = clients.stream()
                .filter(c -> c.isMargeNegative() || (c.getTauxMarge() != null && c.getTauxMarge().compareTo(new BigDecimal("10")) < 0))
                .sorted(Comparator.comparing(LigneMargeDTO::getMarge))
                .limit(5)
                .collect(Collectors.toList());
        cockpit.setClientsRisque(clientsRisque);

        // 7. Compteurs d'alertes
        CockpitRentabiliteDTO.AlertesRentabiliteDTO alertes = new CockpitRentabiliteDTO.AlertesRentabiliteDTO();
        alertes.setNbProduitsMargeNegative((int) prods.stream().filter(LigneMargeDTO::isMargeNegative).count());
        alertes.setNbProduitsSousPmp((int) prods.stream().filter(LigneMargeDTO::isSousPmp).count());
        alertes.setNbProduitsFaibleMarge((int) prods.stream().filter(LigneMargeDTO::isFaibleMarge).count());
        alertes.setNbClientsAvoirsAnormaux(clientsRisque.size());
        alertes.setPerteEstimeeAvoirs(cascade.getTotalRetoursAvoirs());
        cockpit.setAlertes(alertes);

        return cockpit;
    }

    public List<LigneMargeDTO> calculerMargeParProduit(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        Long tenantId = TenantContext.getCurrentTenant();

        Map<Long, LigneMargeDTO> map = new LinkedHashMap<>();

        // Depuis POS
        List<Object[]> rowsVente = tenantId != null
                ? ligneVenteRepository.calculerMargeParProduitByPointDeVenteId(debut, fin, tenantId)
                : ligneVenteRepository.calculerMargeParProduit(debut, fin);
        accumulerLignesMarge(map, rowsVente);

        // Depuis Factures
        List<Object[]> rowsFacture = tenantId != null
                ? ligneFactureRepository.calculerMargeParProduitByPointDeVenteId(dateDebut, dateFin, tenantId)
                : ligneFactureRepository.calculerMargeParProduit(dateDebut, dateFin);
        accumulerLignesMarge(map, rowsFacture);

        List<LigneMargeDTO> result = new ArrayList<>(map.values());
        result.sort((a, b) -> (b.getMarge() != null && a.getMarge() != null)
                ? b.getMarge().compareTo(a.getMarge()) : 0);
        return result;
    }

    public List<LigneMargeDTO> calculerMargeParCategorie(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        Long tenantId = TenantContext.getCurrentTenant();

        Map<Long, LigneMargeDTO> map = new LinkedHashMap<>();

        // Depuis POS
        List<Object[]> rowsVente = tenantId != null
                ? ligneVenteRepository.calculerMargeParCategorieByPointDeVenteId(debut, fin, tenantId)
                : ligneVenteRepository.calculerMargeParCategorie(debut, fin);
        accumulerLignesCategorie(map, rowsVente);

        // Depuis Factures
        List<Object[]> rowsFacture = tenantId != null
                ? ligneFactureRepository.calculerMargeParCategorieByPointDeVenteId(dateDebut, dateFin, tenantId)
                : ligneFactureRepository.calculerMargeParCategorie(dateDebut, dateFin);
        accumulerLignesCategorie(map, rowsFacture);

        List<LigneMargeDTO> result = new ArrayList<>(map.values());
        result.sort((a, b) -> (b.getMarge() != null && a.getMarge() != null)
                ? b.getMarge().compareTo(a.getMarge()) : 0);
        return result;
    }

    public List<LigneMargeDTO> calculerMargeParClient(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);
        Long tenantId = TenantContext.getCurrentTenant();

        Map<Long, LigneMargeDTO> map = new LinkedHashMap<>();

        // Depuis POS
        List<Object[]> rowsVente = tenantId != null
                ? ligneVenteRepository.calculerMargeParClientByPointDeVenteId(debut, fin, tenantId)
                : ligneVenteRepository.calculerMargeParClient(debut, fin);
        accumulerLignesClient(map, rowsVente);

        // Depuis Factures
        List<Object[]> rowsFacture = tenantId != null
                ? ligneFactureRepository.calculerMargeParClientByPointDeVenteId(dateDebut, dateFin, tenantId)
                : ligneFactureRepository.calculerMargeParClient(dateDebut, dateFin);
        accumulerLignesClient(map, rowsFacture);

        List<LigneMargeDTO> result = new ArrayList<>(map.values());
        result.sort((a, b) -> (b.getMarge() != null && a.getMarge() != null)
                ? b.getMarge().compareTo(a.getMarge()) : 0);
        return result;
    }

    private void accumulerLignesMarge(Map<Long, LigneMargeDTO> map, List<Object[]> rows) {
        if (rows == null) return;
        for (Object[] r : rows) {
            Long id = r[0] != null ? ((Number) r[0]).longValue() : 0L;
            String ref = r[1] != null ? r[1].toString() : "";
            String nom = r[2] != null ? r[2].toString() : "";
            BigDecimal ca = toBigDecimal(r[3]);
            BigDecimal cout = toBigDecimal(r[4]);
            BigDecimal qte = toBigDecimal(r[5]);
            BigDecimal remise = toBigDecimal(r[6]);

            LigneMargeDTO existant = map.get(id);
            if (existant != null) {
                BigDecimal nQte = existant.getQuantiteVendue().add(qte);
                BigDecimal nCa = existant.getChiffreAffairesHT().add(ca);
                BigDecimal nCout = existant.getCoutAchatHT().add(cout);
                BigDecimal nRemise = existant.getRemise().add(remise);
                BigDecimal nCaNet = nCa.subtract(nRemise).subtract(existant.getAvoirsHT());
                BigDecimal nMarge = nCaNet.subtract(nCout);

                BigDecimal nTaux = BigDecimal.ZERO;
                if (nCaNet.compareTo(BigDecimal.ZERO) > 0) {
                    nTaux = nMarge.multiply(new BigDecimal("100")).divide(nCaNet, 2, RoundingMode.HALF_UP);
                }

                existant.setQuantiteVendue(nQte);
                existant.setChiffreAffairesHT(nCa);
                existant.setCoutAchatHT(nCout);
                existant.setRemise(nRemise);
                existant.setCaNetHT(nCaNet);
                existant.setMarge(nMarge);
                existant.setTauxMarge(nTaux);

                majIndicateursProduit(existant);
            } else {
                BigDecimal caNet = ca.subtract(remise);
                BigDecimal marge = caNet.subtract(cout);
                BigDecimal taux = BigDecimal.ZERO;
                if (caNet.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(caNet, 2, RoundingMode.HALF_UP);
                }
                LigneMargeDTO item = new LigneMargeDTO(id, ref, nom, qte, ca, cout, remise, marge, taux);
                item.setCaNetHT(caNet);
                majIndicateursProduit(item);
                map.put(id, item);
            }
        }
    }

    private void majIndicateursProduit(LigneMargeDTO p) {
        BigDecimal qte = p.getQuantiteVendue();
        if (qte != null && qte.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal prixVente = p.getChiffreAffairesHT().divide(qte, 2, RoundingMode.HALF_UP);
            BigDecimal coutUnit = p.getCoutAchatHT().divide(qte, 2, RoundingMode.HALF_UP);
            p.setPrixVenteUnitaireMoyen(prixVente);
            p.setCoutUnitaireMoyen(coutUnit);
            p.setSousPmp(prixVente.compareTo(coutUnit) < 0);
        }
        p.setMargeNegative(p.getMarge() != null && p.getMarge().compareTo(BigDecimal.ZERO) < 0);
        p.setFaibleMarge(!p.isMargeNegative() && p.getTauxMarge() != null && p.getTauxMarge().compareTo(new BigDecimal("10")) < 0);
    }

    private void accumulerLignesCategorie(Map<Long, LigneMargeDTO> map, List<Object[]> rows) {
        if (rows == null) return;
        for (Object[] r : rows) {
            Long id = r[0] != null ? ((Number) r[0]).longValue() : 0L;
            String nom = r[1] != null ? r[1].toString() : "Sans catégorie";
            BigDecimal ca = toBigDecimal(r[2]);
            BigDecimal cout = toBigDecimal(r[3]);
            BigDecimal remise = toBigDecimal(r[4]);

            LigneMargeDTO existant = map.get(id);
            if (existant != null) {
                BigDecimal nCa = existant.getChiffreAffairesHT().add(ca);
                BigDecimal nCout = existant.getCoutAchatHT().add(cout);
                BigDecimal nRemise = existant.getRemise().add(remise);
                BigDecimal nCaNet = nCa.subtract(nRemise);
                BigDecimal nMarge = nCaNet.subtract(nCout);
                BigDecimal nTaux = BigDecimal.ZERO;
                if (nCaNet.compareTo(BigDecimal.ZERO) > 0) {
                    nTaux = nMarge.multiply(new BigDecimal("100")).divide(nCaNet, 2, RoundingMode.HALF_UP);
                }
                existant.setChiffreAffairesHT(nCa);
                existant.setCoutAchatHT(nCout);
                existant.setRemise(nRemise);
                existant.setCaNetHT(nCaNet);
                existant.setMarge(nMarge);
                existant.setTauxMarge(nTaux);
            } else {
                BigDecimal caNet = ca.subtract(remise);
                BigDecimal marge = caNet.subtract(cout);
                BigDecimal taux = BigDecimal.ZERO;
                if (caNet.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(caNet, 2, RoundingMode.HALF_UP);
                }
                LigneMargeDTO item = new LigneMargeDTO(id, "", nom, BigDecimal.ZERO, ca, cout, remise, marge, taux);
                item.setCaNetHT(caNet);
                map.put(id, item);
            }
        }
    }

    private void accumulerLignesClient(Map<Long, LigneMargeDTO> map, List<Object[]> rows) {
        if (rows == null) return;
        for (Object[] r : rows) {
            Long id = r[0] != null ? ((Number) r[0]).longValue() : 0L;
            String nom = r[2] != null ? r[2].toString() : (r[1] != null ? r[1].toString() : "Client");
            BigDecimal ca = toBigDecimal(r[3]);
            BigDecimal cout = toBigDecimal(r[4]);
            BigDecimal remise = toBigDecimal(r[5]);

            LigneMargeDTO existant = map.get(id);
            if (existant != null) {
                BigDecimal nCa = existant.getChiffreAffairesHT().add(ca);
                BigDecimal nCout = existant.getCoutAchatHT().add(cout);
                BigDecimal nRemise = existant.getRemise().add(remise);
                BigDecimal nCaNet = nCa.subtract(nRemise);
                BigDecimal nMarge = nCaNet.subtract(nCout);
                BigDecimal nTaux = BigDecimal.ZERO;
                if (nCaNet.compareTo(BigDecimal.ZERO) > 0) {
                    nTaux = nMarge.multiply(new BigDecimal("100")).divide(nCaNet, 2, RoundingMode.HALF_UP);
                }
                existant.setChiffreAffairesHT(nCa);
                existant.setCoutAchatHT(nCout);
                existant.setRemise(nRemise);
                existant.setCaNetHT(nCaNet);
                existant.setMarge(nMarge);
                existant.setTauxMarge(nTaux);
                existant.setMargeNegative(nMarge.compareTo(BigDecimal.ZERO) < 0);
            } else {
                BigDecimal caNet = ca.subtract(remise);
                BigDecimal marge = caNet.subtract(cout);
                BigDecimal taux = BigDecimal.ZERO;
                if (caNet.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(caNet, 2, RoundingMode.HALF_UP);
                }
                LigneMargeDTO item = new LigneMargeDTO(id, "", nom, BigDecimal.ZERO, ca, cout, remise, marge, taux);
                item.setCaNetHT(caNet);
                item.setMargeNegative(marge.compareTo(BigDecimal.ZERO) < 0);
                map.put(id, item);
            }
        }
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
