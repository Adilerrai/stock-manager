package com.gestion.service;

import com.gestion.persistent.enums.CleRepartition;
import com.gestion.persistent.model.AllocationCharge;
import com.gestion.persistent.model.ChargeAchat;
import com.gestion.persistent.model.LigneCommande;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class RepartitionChargeService {

    public List<AllocationCharge> repartirCharge(ChargeAchat charge, List<LigneCommande> lignes) {
        if (charge == null || lignes == null || lignes.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal montantTotal = charge.getMontantRetenuMad() != null ? charge.getMontantRetenuMad() : BigDecimal.ZERO;
        if (montantTotal.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        // Cas 1 : Charge directe sur une ligne spécifique
        if (charge.getLigneCommande() != null) {
            AllocationCharge alloc = new AllocationCharge();
            alloc.setCharge(charge);
            alloc.setLigneCommande(charge.getLigneCommande());
            alloc.setBaseRepartition(BigDecimal.ONE);
            alloc.setPourcentage(new BigDecimal("100.000000"));
            alloc.setMontantMad(montantTotal.setScale(2, RoundingMode.HALF_UP));
            return Collections.singletonList(alloc);
        }

        // Cas 2 : Répartition globale sur les lignes
        CleRepartition cle = charge.getCleRepartition() != null ? charge.getCleRepartition() : CleRepartition.VALEUR;
        Map<LigneCommande, BigDecimal> basesParLigne = new LinkedHashMap<>();
        BigDecimal totalBase = BigDecimal.ZERO;

        for (LigneCommande ligne : lignes) {
            BigDecimal base = extraireBase(ligne, cle);
            basesParLigne.put(ligne, base);
            totalBase = totalBase.add(base);
        }

        // Si la base totale est nulle (ex. aucun poids saisi pour une clé POIDS), on bascule sur la valeur, puis la quantité
        if (totalBase.compareTo(BigDecimal.ZERO) <= 0) {
            if (cle != CleRepartition.VALEUR) {
                return repartirParRepli(charge, lignes, CleRepartition.VALEUR);
            }
            return repartirParRepli(charge, lignes, CleRepartition.QUANTITE);
        }

        List<AllocationCharge> allocations = new ArrayList<>();
        BigDecimal cumulAlloue = BigDecimal.ZERO;
        AllocationCharge allocationPlusLourde = null;
        BigDecimal baseMax = BigDecimal.valueOf(-1);

        for (Map.Entry<LigneCommande, BigDecimal> entry : basesParLigne.entrySet()) {
            LigneCommande ligne = entry.getKey();
            BigDecimal base = entry.getValue();

            BigDecimal ratio = base.divide(totalBase, 8, RoundingMode.HALF_UP);
            BigDecimal montantLigne = montantTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pourcentage = ratio.multiply(new BigDecimal("100")).setScale(6, RoundingMode.HALF_UP);

            AllocationCharge alloc = new AllocationCharge();
            alloc.setCharge(charge);
            alloc.setLigneCommande(ligne);
            alloc.setBaseRepartition(base);
            alloc.setPourcentage(pourcentage);
            alloc.setMontantMad(montantLigne);

            allocations.add(alloc);
            cumulAlloue = cumulAlloue.add(montantLigne);

            if (base.compareTo(baseMax) > 0) {
                baseMax = base;
                allocationPlusLourde = alloc;
            }
        }

        // Résorption de l'écart d'arrondi au centime (garantit exactitude de la somme)
        BigDecimal ecart = montantTotal.subtract(cumulAlloue);
        if (ecart.compareTo(BigDecimal.ZERO) != 0 && allocationPlusLourde != null) {
            allocationPlusLourde.setMontantMad(allocationPlusLourde.getMontantMad().add(ecart));
        }

        return allocations;
    }

    private List<AllocationCharge> repartirParRepli(ChargeAchat charge, List<LigneCommande> lignes, CleRepartition cleRepli) {
        ChargeAchat copie = new ChargeAchat();
        copie.setMontantRetenuMad(charge.getMontantRetenuMad());
        copie.setCleRepartition(cleRepli);
        return repartirCharge(copie, lignes);
    }

    private BigDecimal extraireBase(LigneCommande ligne, CleRepartition cle) {
        switch (cle) {
            case QUANTITE:
                return ligne.getQuantiteCommandee() != null ? BigDecimal.valueOf(ligne.getQuantiteCommandee()) : BigDecimal.ONE;
            case POIDS:
                return (ligne.getPoidsTotalKg() != null && ligne.getPoidsTotalKg().compareTo(BigDecimal.ZERO) > 0)
                        ? ligne.getPoidsTotalKg() : BigDecimal.ZERO;
            case VOLUME:
                return (ligne.getVolumeTotalM3() != null && ligne.getVolumeTotalM3().compareTo(BigDecimal.ZERO) > 0)
                        ? ligne.getVolumeTotalM3() : BigDecimal.ZERO;
            case VALEUR:
            default:
                if (ligne.getMontantHT() != null && ligne.getMontantHT().compareTo(BigDecimal.ZERO) > 0) {
                    return ligne.getMontantHT();
                }
                if (ligne.getPrixUnitaire() != null && ligne.getQuantiteCommandee() != null) {
                    return ligne.getPrixUnitaire().multiply(BigDecimal.valueOf(ligne.getQuantiteCommandee()));
                }
                return BigDecimal.ZERO;
        }
    }
}
