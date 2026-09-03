package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.DepenseDTO;
import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.ResultatEntrepriseDTO;
import com.gestion.persistent.enums.CategorieDepense;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.model.Depense;
import com.gestion.repository.DepenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final UserRepository userRepository;
    private final MargeService margeService;

    public DepenseService(DepenseRepository depenseRepository,
                          UserRepository userRepository,
                          MargeService margeService) {
        this.depenseRepository = depenseRepository;
        this.userRepository = userRepository;
        this.margeService = margeService;
    }

    public DepenseDTO creerDepense(DepenseDTO dto, Long userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Depense depense = new Depense();
        depense.setReference(genererReference());
        depense.setDesignation(dto.getDesignation());
        depense.setMontant(dto.getMontant() != null ? dto.getMontant() : BigDecimal.ZERO);
        depense.setDateDepense(dto.getDateDepense() != null ? dto.getDateDepense() : LocalDate.now());
        depense.setCategorie(dto.getCategorie() != null ? dto.getCategorie() : CategorieDepense.AUTRES_CHARGES);
        depense.setModePaiement(dto.getModePaiement() != null ? dto.getModePaiement() : ModePaiement.ESPECES);
        depense.setBeneficiaire(dto.getBeneficiaire());
        depense.setNumeroFactureJustificatif(dto.getNumeroFactureJustificatif());
        depense.setNotes(dto.getNotes());
        depense.setCreePar(user);

        Long tenantId = TenantContext.getCurrentTenant();
        depense.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        depense.setDateCreation(LocalDateTime.now());

        Depense saved = depenseRepository.save(depense);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DepenseDTO> getAllDepenses() {
        return depenseRepository.findAll().stream()
                .sorted(Comparator.comparing(Depense::getDateDepense).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepenseDTO> getDepensesByPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        return depenseRepository.findByPeriode(debut, fin).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepenseDTO getDepenseById(Long id) {
        Depense d = depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));
        return toDto(d);
    }

    public void supprimerDepense(Long id) {
        depenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getStatistiquesParCategorie(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        List<Object[]> rows = depenseRepository.findTotauxParCategorie(debut, fin);
        Map<String, BigDecimal> map = new LinkedHashMap<>();

        if (rows != null) {
            for (Object[] r : rows) {
                if (r[0] != null) {
                    CategorieDepense cat = (CategorieDepense) r[0];
                    BigDecimal montant = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                    map.put(cat.getLibelle(), montant);
                }
            }
        }
        return map;
    }

    /**
     * Calcule le résultat d'exploitation net réel de l'entreprise sur la période :
     * Résultat Net = Marge Commerciale Nette - Total Dépenses
     */
    @Transactional(readOnly = true)
    public ResultatEntrepriseDTO calculerResultatNet(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        MargeDTO marge = margeService.calculerMargeGlobale(debut, fin);
        BigDecimal totalDepenses = depenseRepository.sumMontantByPeriode(debut, fin);
        if (totalDepenses == null) totalDepenses = BigDecimal.ZERO;

        BigDecimal resultatNet = marge.getMargeNetteCommerciale().subtract(totalDepenses);

        BigDecimal rentabiliteNette = BigDecimal.ZERO;
        if (marge.getChiffreAffairesHT().compareTo(BigDecimal.ZERO) > 0) {
            rentabiliteNette = resultatNet.multiply(new BigDecimal("100")).divide(marge.getChiffreAffairesHT(), 2, RoundingMode.HALF_UP);
        }

        ResultatEntrepriseDTO resultat = new ResultatEntrepriseDTO();
        resultat.setDateDebut(debut);
        resultat.setDateFin(fin);
        resultat.setChiffreAffairesHT(marge.getChiffreAffairesHT());
        resultat.setCoutMarchandisesHT(marge.getCoutMarchandisesHT());
        resultat.setTotalRemises(marge.getTotalRemises());
        resultat.setTotalRetours(marge.getTotalRetoursAvoirs());
        resultat.setMargeCommerciale(marge.getMargeNetteCommerciale());
        resultat.setTauxMarge(marge.getTauxMarge());
        resultat.setTotalDepenses(totalDepenses);
        resultat.setDepensesParCategorie(getStatistiquesParCategorie(debut, fin));
        resultat.setResultatNetEstime(resultatNet);
        resultat.setRentabiliteNette(rentabiliteNette);

        return resultat;
    }

    private DepenseDTO toDto(Depense d) {
        DepenseDTO dto = new DepenseDTO();
        dto.setId(d.getId());
        dto.setReference(d.getReference());
        dto.setDesignation(d.getDesignation());
        dto.setMontant(d.getMontant());
        dto.setDateDepense(d.getDateDepense());
        dto.setCategorie(d.getCategorie());
        dto.setModePaiement(d.getModePaiement());
        dto.setBeneficiaire(d.getBeneficiaire());
        dto.setNumeroFactureJustificatif(d.getNumeroFactureJustificatif());
        dto.setNotes(d.getNotes());
        if (d.getCreePar() != null) {
            dto.setCreeParUserId(d.getCreePar().getId());
            dto.setCreeParNom(d.getCreePar().getNomComplet() != null ? d.getCreePar().getNomComplet() : d.getCreePar().getUsername());
        }
        dto.setPointDeVenteId(d.getPointDeVenteId());
        dto.setDateCreation(d.getDateCreation());
        return dto;
    }

    private String genererReference() {
        String prefixe = "DEP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = depenseRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}
