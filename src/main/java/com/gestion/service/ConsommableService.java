package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ConsommableDTO;
import com.gestion.persistent.dto.ConsommableSearchCriteria;
import com.gestion.persistent.dto.SortieConsommableRequest;
import com.gestion.persistent.enums.CategorieConsommable;
import com.gestion.persistent.enums.UniteConsommable;
import com.gestion.persistent.model.Consommable;
import com.gestion.repository.ConsommableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConsommableService {

    private final ConsommableRepository consommableRepository;

    public ConsommableService(ConsommableRepository consommableRepository) {
        this.consommableRepository = consommableRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    public ConsommableDTO creerConsommable(ConsommableDTO dto) {
        if (dto.getDesignation() == null || dto.getDesignation().trim().isEmpty()) {
            throw new IllegalArgumentException("La désignation du consommable est obligatoire.");
        }

        Long tenantId = getTenantId();
        Consommable c = new Consommable();
        c.setPointDeVenteId(tenantId);

        String ref = dto.getReference();
        if (ref == null || ref.trim().isEmpty()) {
            ref = genererReference(tenantId);
        } else {
            ref = ref.trim().toUpperCase();
            if (consommableRepository.existsByPointDeVenteIdAndReference(tenantId, ref)) {
                throw new IllegalArgumentException("Un consommable avec la référence " + ref + " existe déjà.");
            }
        }
        c.setReference(ref);
        c.setCodeBarre(dto.getCodeBarre());
        c.setDesignation(dto.getDesignation().trim());
        c.setDescription(dto.getDescription());
        c.setCategorie(dto.getCategorie() != null ? dto.getCategorie() : CategorieConsommable.FOURNITURE_BUREAU);
        c.setUniteMesure(dto.getUniteMesure() != null ? dto.getUniteMesure() : UniteConsommable.PIECE);
        c.setStockActuel(dto.getStockActuel() != null ? dto.getStockActuel() : BigDecimal.ZERO);
        c.setStockMinimum(dto.getStockMinimum() != null ? dto.getStockMinimum() : BigDecimal.ZERO);
        c.setDernierPrixAchat(dto.getDernierPrixAchat() != null ? dto.getDernierPrixAchat() : BigDecimal.ZERO);
        c.setEmplacement(dto.getEmplacement());
        c.setActif(dto.getActif() != null ? dto.getActif() : true);
        c.setNotes(dto.getNotes());
        c.setDateCreation(LocalDateTime.now());
        c.setDerniereMiseAJour(LocalDateTime.now());

        Consommable saved = consommableRepository.save(c);
        return toDto(saved);
    }

    public ConsommableDTO modifierConsommable(Long id, ConsommableDTO dto) {
        Long tenantId = getTenantId();
        Consommable c = consommableRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consommable introuvable avec l'ID: " + id));

        if (dto.getDesignation() != null && !dto.getDesignation().trim().isEmpty()) {
            c.setDesignation(dto.getDesignation().trim());
        }
        if (dto.getReference() != null && !dto.getReference().trim().isEmpty()) {
            String newRef = dto.getReference().trim().toUpperCase();
            if (!newRef.equalsIgnoreCase(c.getReference()) && consommableRepository.existsByPointDeVenteIdAndReference(tenantId, newRef)) {
                throw new IllegalArgumentException("Un consommable avec la référence " + newRef + " existe déjà.");
            }
            c.setReference(newRef);
        }
        if (dto.getCodeBarre() != null) c.setCodeBarre(dto.getCodeBarre());
        if (dto.getDescription() != null) c.setDescription(dto.getDescription());
        if (dto.getCategorie() != null) c.setCategorie(dto.getCategorie());
        if (dto.getUniteMesure() != null) c.setUniteMesure(dto.getUniteMesure());
        if (dto.getStockActuel() != null) c.setStockActuel(dto.getStockActuel());
        if (dto.getStockMinimum() != null) c.setStockMinimum(dto.getStockMinimum());
        if (dto.getDernierPrixAchat() != null) c.setDernierPrixAchat(dto.getDernierPrixAchat());
        if (dto.getEmplacement() != null) c.setEmplacement(dto.getEmplacement());
        if (dto.getActif() != null) c.setActif(dto.getActif());
        if (dto.getNotes() != null) c.setNotes(dto.getNotes());
        c.setDerniereMiseAJour(LocalDateTime.now());

        Consommable saved = consommableRepository.save(c);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsommableDTO> getAllConsommables(boolean actifSeulement) {
        Long tenantId = getTenantId();
        List<Consommable> list = actifSeulement
                ? consommableRepository.findByPointDeVenteIdAndActifTrueOrderByDesignationAsc(tenantId)
                : consommableRepository.findByPointDeVenteIdOrderByDesignationAsc(tenantId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsommableDTO getConsommableById(Long id) {
        Long tenantId = getTenantId();
        Consommable c = consommableRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consommable introuvable avec l'ID: " + id));
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<ConsommableDTO> getAlertesStock() {
        Long tenantId = getTenantId();
        return consommableRepository.findAlertesStock(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void supprimerConsommable(Long id) {
        Long tenantId = getTenantId();
        Consommable c = consommableRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consommable introuvable avec l'ID: " + id));
        // Désactivation douce
        c.setActif(false);
        consommableRepository.save(c);
    }

    public ConsommableDTO ajusterStock(Long id, BigDecimal nouvelleQuantite, String motif) {
        Long tenantId = getTenantId();
        Consommable c = consommableRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consommable introuvable avec l'ID: " + id));

        c.setStockActuel(nouvelleQuantite != null ? nouvelleQuantite : BigDecimal.ZERO);
        if (motif != null && !motif.isBlank()) {
            String noteActuelle = c.getNotes() != null ? c.getNotes() : "";
            c.setNotes((noteActuelle + "\n[Ajustement Stock " + LocalDateTime.now() + "]: " + motif).trim());
        }
        c.setDerniereMiseAJour(LocalDateTime.now());
        return toDto(consommableRepository.save(c));
    }

    /**
     * Enregistre une sortie / consommation interne de consommable.
     * Décrémente le stock et consigne une trace horodatée dans l'historique des notes.
     */
    @Transactional
    public ConsommableDTO enregistrerSortie(Long id, SortieConsommableRequest request) {
        Long tenantId = getTenantId();
        Consommable c = consommableRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consommable introuvable avec l'ID: " + id));

        BigDecimal qteSortie = (request != null && request.getQuantite() != null) 
                ? request.getQuantite() 
                : BigDecimal.ONE;

        if (qteSortie.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La quantité sortie doit être strictement positive.");
        }

        BigDecimal stockActuel = c.getStockActuel() != null ? c.getStockActuel() : BigDecimal.ZERO;
        
        // Décrémentation avec plancher à 0 (évite les stocks incohérents)
        BigDecimal nouveauStock = stockActuel.subtract(qteSortie);
        if (nouveauStock.compareTo(BigDecimal.ZERO) < 0) {
            nouveauStock = BigDecimal.ZERO;
        }
        c.setStockActuel(nouveauStock);

        // Date de sortie
        String dateStr = (request != null && request.getDateSortie() != null)
                ? request.getDateSortie().toString()
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // Journalisation / Traçabilité dans les notes
        StringBuilder trace = new StringBuilder("\n[Sortie/Utilisation le ")
                .append(dateStr)
                .append("]: -").append(qteSortie);
        
        if (c.getUniteMesure() != null) {
            trace.append(" ").append(c.getUniteMesure());
        }
        if (request != null && request.getBeneficiaire() != null && !request.getBeneficiaire().isBlank()) {
            trace.append(" | Demandeur/Service: ").append(request.getBeneficiaire().trim());
        }
        if (request != null && request.getMotif() != null && !request.getMotif().isBlank()) {
            trace.append(" | Motif: ").append(request.getMotif().trim());
        }

        String noteActuelle = c.getNotes() != null ? c.getNotes() : "";
        c.setNotes((noteActuelle + trace.toString()).trim());
        c.setDerniereMiseAJour(LocalDateTime.now());

        return toDto(consommableRepository.save(c));
    }

    @Transactional(readOnly = true)
    public Page<ConsommableDTO> searchConsommables(ConsommableSearchCriteria criteria, Pageable pageable) {
        Long tenantId = getTenantId();
        List<Consommable> list = consommableRepository.findByPointDeVenteIdOrderByDesignationAsc(tenantId);

        List<ConsommableDTO> filtered = list.stream()
                .filter(c -> {
                    if (criteria == null) return true;
                    if (criteria.getActif() != null && !criteria.getActif().equals(c.getActif())) {
                        return false;
                    }
                    if (criteria.getCategorie() != null && !criteria.getCategorie().equals(c.getCategorie())) {
                        return false;
                    }
                    if (Boolean.TRUE.equals(criteria.getAlerteStockSeulement()) && !c.isEnAlerteStock()) {
                        return false;
                    }
                    if (criteria.getReference() != null && !criteria.getReference().trim().isEmpty()) {
                        if (c.getReference() == null || !c.getReference().toLowerCase().contains(criteria.getReference().toLowerCase().trim())) {
                            return false;
                        }
                    }
                    if (criteria.getDesignation() != null && !criteria.getDesignation().trim().isEmpty()) {
                        if (c.getDesignation() == null || !c.getDesignation().toLowerCase().contains(criteria.getDesignation().toLowerCase().trim())) {
                            return false;
                        }
                    }
                    if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
                        String q = criteria.getQuery().toLowerCase().trim();
                        boolean match = (c.getReference() != null && c.getReference().toLowerCase().contains(q))
                                || (c.getDesignation() != null && c.getDesignation().toLowerCase().contains(q))
                                || (c.getCodeBarre() != null && c.getCodeBarre().toLowerCase().contains(q))
                                || (c.getEmplacement() != null && c.getEmplacement().toLowerCase().contains(q));
                        if (!match) return false;
                    }
                    return true;
                })
                .map(this::toDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<ConsommableDTO> pageContent = (start <= end && start < filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    private ConsommableDTO toDto(Consommable c) {
        ConsommableDTO dto = new ConsommableDTO();
        dto.setId(c.getId());
        dto.setPointDeVenteId(c.getPointDeVenteId());
        dto.setReference(c.getReference());
        dto.setCodeBarre(c.getCodeBarre());
        dto.setDesignation(c.getDesignation());
        dto.setDescription(c.getDescription());
        dto.setCategorie(c.getCategorie());
        dto.setUniteMesure(c.getUniteMesure());
        dto.setStockActuel(c.getStockActuel());
        dto.setStockMinimum(c.getStockMinimum());
        dto.setDernierPrixAchat(c.getDernierPrixAchat());
        dto.setEmplacement(c.getEmplacement());
        dto.setActif(c.getActif());
        dto.setNotes(c.getNotes());
        dto.setEnAlerteStock(c.isEnAlerteStock());
        dto.setDateCreation(c.getDateCreation());
        dto.setDerniereMiseAJour(c.getDerniereMiseAJour());
        return dto;
    }

    private String genererReference(Long tenantId) {
        long count = consommableRepository.countByPointDeVenteId(tenantId) + 1;
        return String.format("CON-%04d", count);
    }
}
