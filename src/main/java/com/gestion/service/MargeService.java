package com.gestion.service;

import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.MargeDTO.LigneMargeDTO;
import com.gestion.persistent.enums.TypeAvoir;
import com.gestion.repository.AvoirRepository;
import com.gestion.repository.LigneVenteRepository;
import com.gestion.repository.VenteRepository;
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
public class MargeService {

    private final LigneVenteRepository ligneVenteRepository;
    private final VenteRepository venteRepository;
    private final AvoirRepository avoirRepository;

    public MargeService(LigneVenteRepository ligneVenteRepository,
                        VenteRepository venteRepository,
                        AvoirRepository avoirRepository) {
        this.ligneVenteRepository = ligneVenteRepository;
        this.venteRepository = venteRepository;
        this.avoirRepository = avoirRepository;
    }

    /**
     * Calcule la rentabilité et les marges globales pour une période donnée
     */
    public MargeDTO calculerMargeGlobale(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        LocalDateTime debutDateTime = dateDebut.atStartOfDay();
        LocalDateTime finDateTime = dateFin.atTime(LocalTime.MAX);

        MargeDTO dto = new MargeDTO();
        dto.setDateDebut(dateDebut);
        dto.setDateFin(dateFin);

        // 1. Totaux depuis les lignes de vente validées
        List<Object[]> totauxList = ligneVenteRepository.calculerTotauxMargeGlobale(debutDateTime, finDateTime);
        BigDecimal caHT = BigDecimal.ZERO;
        BigDecimal coutHT = BigDecimal.ZERO;
        BigDecimal remisesLignes = BigDecimal.ZERO;

        if (totauxList != null && !totauxList.isEmpty()) {
            Object[] totaux = totauxList.get(0);
            if (totaux != null && totaux.length >= 3) {
                caHT = toBigDecimal(totaux[0]);
                coutHT = toBigDecimal(totaux[1]);
                remisesLignes = toBigDecimal(totaux[2]);
            }
        }

        // 2. Avoirs clients (retours de marchandises)
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

        // 3. Détails
        dto.setMargesParProduit(calculerMargeParProduit(debutDateTime, finDateTime));
        dto.setMargesParCategorie(calculerMargeParCategorie(debutDateTime, finDateTime));
        dto.setMargesParClient(calculerMargeParClient(debutDateTime, finDateTime));

        return dto;
    }

    public List<LigneMargeDTO> calculerMargeParProduit(LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> rows = ligneVenteRepository.calculerMargeParProduit(debut, fin);
        List<LigneMargeDTO> list = new ArrayList<>();

        if (rows != null) {
            for (Object[] r : rows) {
                Long id = r[0] != null ? ((Number) r[0]).longValue() : null;
                String ref = r[1] != null ? r[1].toString() : "";
                String nom = r[2] != null ? r[2].toString() : "";
                BigDecimal ca = toBigDecimal(r[3]);
                BigDecimal cout = toBigDecimal(r[4]);
                BigDecimal qte = toBigDecimal(r[5]);
                BigDecimal remise = toBigDecimal(r[6]);
                BigDecimal marge = ca.subtract(cout).subtract(remise);
                BigDecimal taux = BigDecimal.ZERO;
                if (ca.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(ca, 2, RoundingMode.HALF_UP);
                }

                list.add(new LigneMargeDTO(id, ref, nom, qte, ca, cout, remise, marge, taux));
            }
        }
        return list;
    }

    public List<LigneMargeDTO> calculerMargeParCategorie(LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> rows = ligneVenteRepository.calculerMargeParCategorie(debut, fin);
        List<LigneMargeDTO> list = new ArrayList<>();

        if (rows != null) {
            for (Object[] r : rows) {
                Long id = r[0] != null ? ((Number) r[0]).longValue() : null;
                String nom = r[1] != null ? r[1].toString() : "Sans catégorie";
                BigDecimal ca = toBigDecimal(r[2]);
                BigDecimal cout = toBigDecimal(r[3]);
                BigDecimal remise = toBigDecimal(r[4]);
                BigDecimal marge = ca.subtract(cout).subtract(remise);
                BigDecimal taux = BigDecimal.ZERO;
                if (ca.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(ca, 2, RoundingMode.HALF_UP);
                }

                list.add(new LigneMargeDTO(id, "", nom, BigDecimal.ZERO, ca, cout, remise, marge, taux));
            }
        }
        return list;
    }

    public List<LigneMargeDTO> calculerMargeParClient(LocalDateTime debut, LocalDateTime fin) {
        List<Object[]> rows = ligneVenteRepository.calculerMargeParClient(debut, fin);
        List<LigneMargeDTO> list = new ArrayList<>();

        if (rows != null) {
            for (Object[] r : rows) {
                Long id = r[0] != null ? ((Number) r[0]).longValue() : null;
                String nom = r[2] != null ? r[2].toString() : (r[1] != null ? r[1].toString() : "Client");
                BigDecimal ca = toBigDecimal(r[3]);
                BigDecimal cout = toBigDecimal(r[4]);
                BigDecimal remise = toBigDecimal(r[5]);
                BigDecimal marge = ca.subtract(cout).subtract(remise);
                BigDecimal taux = BigDecimal.ZERO;
                if (ca.compareTo(BigDecimal.ZERO) > 0) {
                    taux = marge.multiply(new BigDecimal("100")).divide(ca, 2, RoundingMode.HALF_UP);
                }

                list.add(new LigneMargeDTO(id, "", nom, BigDecimal.ZERO, ca, cout, remise, marge, taux));
            }
        }
        return list;
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
