package com.gestion.service;

import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.MargeDTO.LigneMargeDTO;
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
import java.util.*;

@Service
@Transactional(readOnly = true)
public class MargeService {

    private final LigneVenteRepository ligneVenteRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final VenteRepository venteRepository;
    private final AvoirRepository avoirRepository;

    public MargeService(LigneVenteRepository ligneVenteRepository,
                        LigneFactureRepository ligneFactureRepository,
                        VenteRepository venteRepository,
                        AvoirRepository avoirRepository) {
        this.ligneVenteRepository = ligneVenteRepository;
        this.ligneFactureRepository = ligneFactureRepository;
        this.venteRepository = venteRepository;
        this.avoirRepository = avoirRepository;
    }

    /**
     * Calcule la rentabilité et les marges globales pour une période donnée
     * en combinant les ventes au comptoir (POS) et les factures de vente (B2B).
     */
    public MargeDTO calculerMargeGlobale(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debutDateTime = dateDebut.atStartOfDay();
        LocalDateTime finDateTime = dateFin.atTime(LocalTime.MAX);

        MargeDTO dto = new MargeDTO();
        dto.setDateDebut(dateDebut);
        dto.setDateFin(dateFin);

        BigDecimal caHT = BigDecimal.ZERO;
        BigDecimal coutHT = BigDecimal.ZERO;
        BigDecimal remisesLignes = BigDecimal.ZERO;

        // 1. Totaux depuis les ventes POS validées
        List<Object[]> totauxVente = ligneVenteRepository.calculerTotauxMargeGlobale(debutDateTime, finDateTime);
        if (totauxVente != null && !totauxVente.isEmpty()) {
            Object[] row = totauxVente.get(0);
            if (row != null && row.length >= 3) {
                caHT = caHT.add(toBigDecimal(row[0]));
                coutHT = coutHT.add(toBigDecimal(row[1]));
                remisesLignes = remisesLignes.add(toBigDecimal(row[2]));
            }
        }

        // 2. Totaux depuis les Factures de vente validées (non annulées, hors vente POS)
        List<Object[]> totauxFacture = ligneFactureRepository.calculerTotauxMargeGlobale(dateDebut, dateFin);
        if (totauxFacture != null && !totauxFacture.isEmpty()) {
            Object[] row = totauxFacture.get(0);
            if (row != null && row.length >= 3) {
                caHT = caHT.add(toBigDecimal(row[0]));
                coutHT = coutHT.add(toBigDecimal(row[1]));
                remisesLignes = remisesLignes.add(toBigDecimal(row[2]));
            }
        }

        // 3. Avoirs clients (retours de marchandises)
        BigDecimal retours = avoirRepository.sumMontantByPeriodeAndType(TypeAvoir.CLIENT, dateDebut, dateFin);
        if (retours == null) retours = BigDecimal.ZERO;

        BigDecimal margeBrute = caHT.subtract(coutHT);
        BigDecimal margeNette = margeBrute.subtract(remisesLignes).subtract(retours);

        BigDecimal tauxMarge = BigDecimal.ZERO;
        if (caHT.compareTo(BigDecimal.ZERO) > 0) {
            tauxMarge = margeNette.multiply(new BigDecimal("100")).divide(caHT, 2, RoundingMode.HALF_UP);
        }

        dto.setChiffreAffairesHT(caHT);
        dto.setCoutMarchandisesHT(coutHT);
        dto.setTotalRemises(remisesLignes);
        dto.setTotalRetoursAvoirs(retours);
        dto.setMargeBrute(margeBrute);
        dto.setMargeNetteCommerciale(margeNette);
        dto.setTauxMarge(tauxMarge);

        // 4. Détails
        dto.setMargesParProduit(calculerMargeParProduit(dateDebut, dateFin));
        dto.setMargesParCategorie(calculerMargeParCategorie(dateDebut, dateFin));
        dto.setMargesParClient(calculerMargeParClient(dateDebut, dateFin));

        return dto;
    }

    public List<LigneMargeDTO> calculerMargeParProduit(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);

        Map<Long, LigneMargeDTO> map = new LinkedHashMap<>();

        // Depuis POS
        List<Object[]> rowsVente = ligneVenteRepository.calculerMargeParProduit(debut, fin);
        accumulerLignesMarge(map, rowsVente);

        // Depuis Factures
        List<Object[]> rowsFacture = ligneFactureRepository.calculerMargeParProduit(dateDebut, dateFin);
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

        Map<Long, LigneMargeDTO> map = new LinkedHashMap<>();

        // Depuis POS
        List<Object[]> rowsVente = ligneVenteRepository.calculerMargeParCategorie(debut, fin);
        accumulerLignesCategorie(map, rowsVente);

        // Depuis Factures
        List<Object[]> rowsFacture = ligneFactureRepository.calculerMargeParCategorie(dateDebut, dateFin);
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

        Map<Long, LigneMargeDTO> map = new LinkedHashMap<>();

        // Depuis POS
        List<Object[]> rowsVente = ligneVenteRepository.calculerMargeParClient(debut, fin);
        accumulerLignesClient(map, rowsVente);

        // Depuis Factures
        List<Object[]> rowsFacture = ligneFactureRepository.calculerMargeParClient(dateDebut, dateFin);
        accumulerLignesClient(map, rowsFacture);

        List<LigneMargeDTO> result = new ArrayList<>(map.values());
        result.sort((a, b) -> (b.getMarge() != null && a.getMarge() != null)
                ? b.getMarge().compareTo(a.getMarge()) : 0);
        return result;
    }

    public List<LigneMargeDTO> calculerMargeParProduit(LocalDateTime debut, LocalDateTime fin) {
        return calculerMargeParProduit(debut.toLocalDate(), fin.toLocalDate());
    }

    public List<LigneMargeDTO> calculerMargeParCategorie(LocalDateTime debut, LocalDateTime fin) {
        return calculerMargeParCategorie(debut.toLocalDate(), fin.toLocalDate());
    }

    public List<LigneMargeDTO> calculerMargeParClient(LocalDateTime debut, LocalDateTime fin) {
        return calculerMargeParClient(debut.toLocalDate(), fin.toLocalDate());
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
                BigDecimal nMarge = nCa.subtract(nCout).subtract(nRemise);
                BigDecimal nTaux = BigDecimal.ZERO;
                if (nCa.compareTo(BigDecimal.ZERO) > 0) {
                    nTaux = nMarge.multiply(new BigDecimal("100")).divide(nCa, 2, RoundingMode.HALF_UP);
                }
                existant.setQuantiteVendue(nQte);
                existant.setChiffreAffairesHT(nCa);
                existant.setCoutAchatHT(nCout);
                existant.setRemise(nRemise);
                existant.setMarge(nMarge);
                existant.setTauxMarge(nTaux);
            } else {
                BigDecimal marge = ca.subtract(cout).subtract(remise);
                BigDecimal taux = BigDecimal.ZERO;
                if (ca.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(ca, 2, RoundingMode.HALF_UP);
                }
                map.put(id, new LigneMargeDTO(id, ref, nom, qte, ca, cout, remise, marge, taux));
            }
        }
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
                BigDecimal nMarge = nCa.subtract(nCout).subtract(nRemise);
                BigDecimal nTaux = BigDecimal.ZERO;
                if (nCa.compareTo(BigDecimal.ZERO) > 0) {
                    nTaux = nMarge.multiply(new BigDecimal("100")).divide(nCa, 2, RoundingMode.HALF_UP);
                }
                existant.setChiffreAffairesHT(nCa);
                existant.setCoutAchatHT(nCout);
                existant.setRemise(nRemise);
                existant.setMarge(nMarge);
                existant.setTauxMarge(nTaux);
            } else {
                BigDecimal marge = ca.subtract(cout).subtract(remise);
                BigDecimal taux = BigDecimal.ZERO;
                if (ca.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(ca, 2, RoundingMode.HALF_UP);
                }
                map.put(id, new LigneMargeDTO(id, "", nom, BigDecimal.ZERO, ca, cout, remise, marge, taux));
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
                BigDecimal nMarge = nCa.subtract(nCout).subtract(nRemise);
                BigDecimal nTaux = BigDecimal.ZERO;
                if (nCa.compareTo(BigDecimal.ZERO) > 0) {
                    nTaux = nMarge.multiply(new BigDecimal("100")).divide(nCa, 2, RoundingMode.HALF_UP);
                }
                existant.setChiffreAffairesHT(nCa);
                existant.setCoutAchatHT(nCout);
                existant.setRemise(nRemise);
                existant.setMarge(nMarge);
                existant.setTauxMarge(nTaux);
            } else {
                BigDecimal marge = ca.subtract(cout).subtract(remise);
                BigDecimal taux = BigDecimal.ZERO;
                if (ca.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(ca, 2, RoundingMode.HALF_UP);
                }
                map.put(id, new LigneMargeDTO(id, "", nom, BigDecimal.ZERO, ca, cout, remise, marge, taux));
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
