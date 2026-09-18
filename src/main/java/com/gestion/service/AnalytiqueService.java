package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.AxeAnalytiqueDTO;
import com.gestion.persistent.dto.CentreAnalytiqueDTO;
import com.gestion.persistent.dto.CpcAnalytiqueDTO;
import com.gestion.persistent.dto.VentilationAnalytiqueDTO;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalytiqueService {

    private final AxeAnalytiqueRepository axeRepository;
    private final CentreAnalytiqueRepository centreRepository;
    private final VentilationAnalytiqueRepository ventilationRepository;
    private final LigneEcritureRepository ligneRepository;
    private final AuditService auditService;

    public AnalytiqueService(AxeAnalytiqueRepository axeRepository,
                             CentreAnalytiqueRepository centreRepository,
                             VentilationAnalytiqueRepository ventilationRepository,
                             LigneEcritureRepository ligneRepository,
                             AuditService auditService) {
        this.axeRepository = axeRepository;
        this.centreRepository = centreRepository;
        this.ventilationRepository = ventilationRepository;
        this.ligneRepository = ligneRepository;
        this.auditService = auditService;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // AXES ANALYTIQUES
    // =========================================================================

    public AxeAnalytiqueDTO creerAxe(AxeAnalytiqueDTO dto) {
        Long tenantId = getTenantId();
        String code = (dto.getCode() != null && !dto.getCode().trim().isEmpty())
                ? dto.getCode().trim().toUpperCase()
                : "AXE-" + (axeRepository.count() + 1);

        if (axeRepository.existsByCodeAndPointDeVenteId(code, tenantId)) {
            throw new IllegalArgumentException("Un axe analytique avec le code " + code + " existe déjà.");
        }

        AxeAnalytique axe = new AxeAnalytique();
        axe.setCode(code);
        axe.setLibelle(dto.getLibelle() != null ? dto.getLibelle() : "Axe " + code);
        axe.setDescription(dto.getDescription());
        axe.setActif(true);
        axe.setPointDeVenteId(tenantId);
        axe.setDateCreation(LocalDateTime.now());

        AxeAnalytique saved = axeRepository.save(axe);
        auditService.logCreation("AXE_ANALYTIQUE", saved.getId(), "Création axe analytique " + saved.getCode());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AxeAnalytiqueDTO> listerAxes() {
        Long tenantId = getTenantId();
        return axeRepository.findByPointDeVenteIdOrderByLibelleAsc(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public AxeAnalytiqueDTO modifierAxe(Long id, AxeAnalytiqueDTO dto) {
        Long tenantId = getTenantId();
        AxeAnalytique axe = axeRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Axe introuvable ID: " + id));

        if (dto.getLibelle() != null) axe.setLibelle(dto.getLibelle());
        if (dto.getDescription() != null) axe.setDescription(dto.getDescription());
        if (dto.getActif() != null) axe.setActif(dto.getActif());

        AxeAnalytique saved = axeRepository.save(axe);
        return toDto(saved);
    }

    public void supprimerAxe(Long id) {
        Long tenantId = getTenantId();
        AxeAnalytique axe = axeRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Axe introuvable ID: " + id));
        axe.setActif(false);
        axeRepository.save(axe);
        auditService.logSuppression("AXE_ANALYTIQUE", id, "Désactivation axe " + axe.getCode());
    }

    // =========================================================================
    // CENTRES / SECTIONS ANALYTIQUES (COÛT & PROFIT)
    // =========================================================================

    public CentreAnalytiqueDTO creerCentre(CentreAnalytiqueDTO dto) {
        Long tenantId = getTenantId();
        AxeAnalytique axe = axeRepository.findByIdAndPointDeVenteId(dto.getAxeId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Axe analytique introuvable ID: " + dto.getAxeId()));

        String code = (dto.getCode() != null && !dto.getCode().trim().isEmpty())
                ? dto.getCode().trim().toUpperCase()
                : "CTR-" + (centreRepository.count() + 1);

        if (centreRepository.existsByCodeAndPointDeVenteId(code, tenantId)) {
            throw new IllegalArgumentException("Un centre analytique avec le code " + code + " existe déjà.");
        }

        CentreAnalytique centre = new CentreAnalytique();
        centre.setAxe(axe);
        centre.setCode(code);
        centre.setLibelle(dto.getLibelle() != null ? dto.getLibelle() : "Centre " + code);
        centre.setType(dto.getType() != null ? dto.getType() : "COUT");
        centre.setResponsable(dto.getResponsable());
        centre.setActif(true);
        centre.setPointDeVenteId(tenantId);
        centre.setDateCreation(LocalDateTime.now());

        CentreAnalytique saved = centreRepository.save(centre);
        auditService.logCreation("CENTRE_ANALYTIQUE", saved.getId(), "Création centre " + saved.getCode() + " (" + saved.getLibelle() + ")");
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CentreAnalytiqueDTO> listerCentres(Long axeId) {
        Long tenantId = getTenantId();
        List<CentreAnalytique> list = (axeId != null)
                ? centreRepository.findByAxeIdAndPointDeVenteIdOrderByLibelleAsc(axeId, tenantId)
                : centreRepository.findByPointDeVenteIdOrderByLibelleAsc(tenantId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public CentreAnalytiqueDTO modifierCentre(Long id, CentreAnalytiqueDTO dto) {
        Long tenantId = getTenantId();
        CentreAnalytique centre = centreRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Centre introuvable ID: " + id));

        if (dto.getLibelle() != null) centre.setLibelle(dto.getLibelle());
        if (dto.getType() != null) centre.setType(dto.getType());
        if (dto.getResponsable() != null) centre.setResponsable(dto.getResponsable());
        if (dto.getActif() != null) centre.setActif(dto.getActif());

        CentreAnalytique saved = centreRepository.save(centre);
        return toDto(saved);
    }

    public void supprimerCentre(Long id) {
        Long tenantId = getTenantId();
        CentreAnalytique centre = centreRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Centre introuvable ID: " + id));
        centre.setActif(false);
        centreRepository.save(centre);
    }

    // =========================================================================
    // VENTILATION ANALYTIQUE DES ÉCRITURES
    // =========================================================================

    public List<VentilationAnalytiqueDTO> ventilerLigneEcriture(Long ligneEcritureId, List<VentilationAnalytiqueDTO> ventilations) {
        Long tenantId = getTenantId();
        LigneEcriture ligne = ligneRepository.findById(ligneEcritureId)
                .orElseThrow(() -> new IllegalArgumentException("Ligne d'écriture introuvable ID: " + ligneEcritureId));

        if (ventilations == null || ventilations.isEmpty()) {
            ventilationRepository.deleteByLigneEcritureId(ligneEcritureId);
            return List.of();
        }

        BigDecimal montantTotalLigne = (ligne.getDebit() != null && ligne.getDebit().compareTo(BigDecimal.ZERO) > 0)
                ? ligne.getDebit() : (ligne.getCredit() != null ? ligne.getCredit() : BigDecimal.ZERO);

        // Validation de la somme des pourcentages (doit faire 100%)
        BigDecimal sommePourcentages = ventilations.stream()
                .map(v -> v.getPourcentage() != null ? v.getPourcentage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sommePourcentages.subtract(new BigDecimal("100.00")).abs().compareTo(new BigDecimal("0.10")) > 0) {
            throw new IllegalArgumentException(String.format(
                    "La somme des pourcentages de ventilation analytique doit être égale à 100%% (actuel: %s%%)", sommePourcentages));
        }

        // Supprimer les ventilations existantes de cette ligne
        ventilationRepository.deleteByLigneEcritureId(ligneEcritureId);

        List<VentilationAnalytique> entities = new ArrayList<>();
        for (VentilationAnalytiqueDTO vDto : ventilations) {
            CentreAnalytique centre = centreRepository.findByIdAndPointDeVenteId(vDto.getCentreAnalytiqueId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Centre analytique introuvable ID: " + vDto.getCentreAnalytiqueId()));

            BigDecimal pourcent = vDto.getPourcentage();
            BigDecimal mnt = montantTotalLigne.multiply(pourcent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            VentilationAnalytique vent = new VentilationAnalytique();
            vent.setLigneEcritureId(ligneEcritureId);
            vent.setCentreAnalytique(centre);
            vent.setPourcentage(pourcent);
            vent.setMontant(mnt);
            vent.setPointDeVenteId(tenantId);
            vent.setDateCreation(LocalDateTime.now());
            entities.add(vent);
        }

        List<VentilationAnalytique> saved = ventilationRepository.saveAll(entities);
        auditService.logCreation("VENTILATION_ANALYTIQUE", ligneEcritureId,
                "Ventilation analytique de la ligne " + ligneEcritureId + " sur " + saved.size() + " centres");

        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentilationAnalytiqueDTO> getVentilationsLigne(Long ligneEcritureId) {
        return ventilationRepository.findByLigneEcritureId(ligneEcritureId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // COMPTE DE PRODUITS ET CHARGES ANALYTIQUE (CPC PAR CENTRE / PROJET)
    // =========================================================================

    @Transactional(readOnly = true)
    public CpcAnalytiqueDTO getCpcAnalytique(Long centreId, LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        CentreAnalytique centre = centreRepository.findByIdAndPointDeVenteId(centreId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Centre analytique introuvable ID: " + centreId));

        LocalDate dDebut = (debut != null) ? debut : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate dFin = (fin != null) ? fin : LocalDate.now();

        CpcAnalytiqueDTO cpc = new CpcAnalytiqueDTO();
        cpc.setCentreId(centre.getId());
        cpc.setCentreCode(centre.getCode());
        cpc.setCentreLibelle(centre.getLibelle());
        cpc.setAxeLibelle(centre.getAxe() != null ? centre.getAxe().getLibelle() : "");
        cpc.setTypeCentre(centre.getType());
        cpc.setDateDebut(dDebut);
        cpc.setDateFin(dFin);

        List<VentilationAnalytique> ventilations = ventilationRepository.findByCentreAndPeriode(centreId, tenantId, dDebut, dFin);

        Map<String, CpcAnalytiqueDTO.PosteAnalytiqueDTO> chargesMap = new HashMap<>();
        Map<String, CpcAnalytiqueDTO.PosteAnalytiqueDTO> produitsMap = new HashMap<>();

        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalProduits = BigDecimal.ZERO;

        for (VentilationAnalytique v : ventilations) {
            LigneEcriture ligne = ligneRepository.findById(v.getLigneEcritureId()).orElse(null);
            if (ligne != null && ligne.getCompte() != null) {
                String num = ligne.getCompte().getNumeroCompte();
                String lib = ligne.getCompte().getLibelle();
                BigDecimal partMontant = v.getMontant();

                // Classe 6 : Charges
                if (num.startsWith("6")) {
                    totalCharges = totalCharges.add(partMontant);
                    chargesMap.computeIfAbsent(num, k -> new CpcAnalytiqueDTO.PosteAnalytiqueDTO(num, lib, BigDecimal.ZERO))
                            .setMontant(chargesMap.get(num).getMontant().add(partMontant));
                }
                // Classe 7 : Produits
                else if (num.startsWith("7")) {
                    totalProduits = totalProduits.add(partMontant);
                    produitsMap.computeIfAbsent(num, k -> new CpcAnalytiqueDTO.PosteAnalytiqueDTO(num, lib, BigDecimal.ZERO))
                            .setMontant(produitsMap.get(num).getMontant().add(partMontant));
                }
            }
        }

        cpc.setTotalCharges(totalCharges);
        cpc.setTotalProduits(totalProduits);
        cpc.setResultatNetAnalytique(totalProduits.subtract(totalCharges));

        if (totalProduits.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marge = cpc.getResultatNetAnalytique().multiply(new BigDecimal("100"))
                    .divide(totalProduits, 2, RoundingMode.HALF_UP);
            cpc.setTauxMarge(marge);
        } else {
            cpc.setTauxMarge(BigDecimal.ZERO);
        }

        cpc.setDetailsCharges(new ArrayList<>(chargesMap.values()));
        cpc.setDetailsProduits(new ArrayList<>(produitsMap.values()));

        return cpc;
    }

    @Transactional(readOnly = true)
    public List<CpcAnalytiqueDTO> getRentabiliteAxe(Long axeId, LocalDate debut, LocalDate fin) {
        Long tenantId = getTenantId();
        List<CentreAnalytique> centres = centreRepository.findByAxeIdAndPointDeVenteIdAndActifTrueOrderByLibelleAsc(axeId, tenantId);
        List<CpcAnalytiqueDTO> resultats = new ArrayList<>();
        for (CentreAnalytique c : centres) {
            resultats.add(getCpcAnalytique(c.getId(), debut, fin));
        }
        return resultats;
    }

    // =========================================================================
    // MAPPINGS
    // =========================================================================

    private AxeAnalytiqueDTO toDto(AxeAnalytique a) {
        AxeAnalytiqueDTO dto = new AxeAnalytiqueDTO();
        dto.setId(a.getId());
        dto.setCode(a.getCode());
        dto.setLibelle(a.getLibelle());
        dto.setDescription(a.getDescription());
        dto.setActif(a.getActif());
        dto.setDateCreation(a.getDateCreation());
        dto.setNombreCentres(centreRepository.findByAxeIdAndPointDeVenteIdOrderByLibelleAsc(a.getId(), a.getPointDeVenteId()).size());
        return dto;
    }

    private CentreAnalytiqueDTO toDto(CentreAnalytique c) {
        CentreAnalytiqueDTO dto = new CentreAnalytiqueDTO();
        dto.setId(c.getId());
        dto.setAxeId(c.getAxe() != null ? c.getAxe().getId() : null);
        dto.setAxeLibelle(c.getAxe() != null ? c.getAxe().getLibelle() : "");
        dto.setCode(c.getCode());
        dto.setLibelle(c.getLibelle());
        dto.setType(c.getType());
        dto.setResponsable(c.getResponsable());
        dto.setActif(c.getActif());
        dto.setDateCreation(c.getDateCreation());
        return dto;
    }

    private VentilationAnalytiqueDTO toDto(VentilationAnalytique v) {
        VentilationAnalytiqueDTO dto = new VentilationAnalytiqueDTO();
        dto.setId(v.getId());
        dto.setLigneEcritureId(v.getLigneEcritureId());
        dto.setCentreAnalytiqueId(v.getCentreAnalytique() != null ? v.getCentreAnalytique().getId() : null);
        dto.setCentreCode(v.getCentreAnalytique() != null ? v.getCentreAnalytique().getCode() : "");
        dto.setCentreLibelle(v.getCentreAnalytique() != null ? v.getCentreAnalytique().getLibelle() : "");
        dto.setAxeLibelle(v.getCentreAnalytique() != null && v.getCentreAnalytique().getAxe() != null ? v.getCentreAnalytique().getAxe().getLibelle() : "");
        dto.setPourcentage(v.getPourcentage());
        dto.setMontant(v.getMontant());
        return dto;
    }
}
