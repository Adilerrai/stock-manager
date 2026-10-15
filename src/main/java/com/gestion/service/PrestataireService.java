package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.PrestataireDTO;
import com.gestion.persistent.dto.PrestataireSearchCriteria;
import com.gestion.persistent.model.Depense;
import com.gestion.persistent.model.Prestataire;
import com.gestion.repository.DepenseRepository;
import com.gestion.repository.PrestataireRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PrestataireService {

    private final PrestataireRepository prestataireRepository;
    private final DepenseRepository depenseRepository;

    public PrestataireService(PrestataireRepository prestataireRepository,
                              DepenseRepository depenseRepository) {
        this.prestataireRepository = prestataireRepository;
        this.depenseRepository = depenseRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    public PrestataireDTO creerPrestataire(PrestataireDTO dto) {
        if (dto.getRaisonSociale() == null || dto.getRaisonSociale().trim().isEmpty()) {
            throw new IllegalArgumentException("La raison sociale du prestataire est obligatoire.");
        }

        Long tenantId = getTenantId();
        Prestataire p = new Prestataire();
        p.setPointDeVenteId(tenantId);
        p.setRaisonSociale(dto.getRaisonSociale().trim());
        p.setContactNom(dto.getContactNom());
        p.setIce(dto.getIce());
        p.setTelephone(dto.getTelephone());
        p.setEmail(dto.getEmail());
        p.setAdresse(dto.getAdresse());
        p.setVille(dto.getVille());
        p.setCodePostal(dto.getCodePostal());
        p.setPays(dto.getPays() != null ? dto.getPays() : "Maroc");
        p.setTypePrestation(dto.getTypePrestation() != null ? dto.getTypePrestation() : com.gestion.persistent.enums.TypePrestation.AUTRE);
        p.setRegistreCommerce(dto.getRegistreCommerce());
        p.setIdentifiantFiscal(dto.getIdentifiantFiscal());
        p.setPatente(dto.getPatente());
        p.setRibBancaire(dto.getRibBancaire());
        p.setBanqueNom(dto.getBanqueNom());
        p.setDelaiPaiementJours(dto.getDelaiPaiementJours() != null ? dto.getDelaiPaiementJours() : 30);
        p.setActif(dto.getActif() != null ? dto.getActif() : true);
        p.setNotes(dto.getNotes());
        p.setDateCreation(LocalDateTime.now());

        Prestataire saved = prestataireRepository.save(p);
        return toDto(saved, false);
    }

    public PrestataireDTO modifierPrestataire(Long id, PrestataireDTO dto) {
        Long tenantId = getTenantId();
        Prestataire p = prestataireRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prestataire introuvable avec l'ID: " + id));

        if (dto.getRaisonSociale() != null && !dto.getRaisonSociale().trim().isEmpty()) {
            p.setRaisonSociale(dto.getRaisonSociale().trim());
        }
        if (dto.getContactNom() != null) p.setContactNom(dto.getContactNom());
        if (dto.getIce() != null) p.setIce(dto.getIce());
        if (dto.getTelephone() != null) p.setTelephone(dto.getTelephone());
        if (dto.getEmail() != null) p.setEmail(dto.getEmail());
        if (dto.getAdresse() != null) p.setAdresse(dto.getAdresse());
        if (dto.getVille() != null) p.setVille(dto.getVille());
        if (dto.getCodePostal() != null) p.setCodePostal(dto.getCodePostal());
        if (dto.getPays() != null) p.setPays(dto.getPays());
        if (dto.getTypePrestation() != null) p.setTypePrestation(dto.getTypePrestation());
        if (dto.getRegistreCommerce() != null) p.setRegistreCommerce(dto.getRegistreCommerce());
        if (dto.getIdentifiantFiscal() != null) p.setIdentifiantFiscal(dto.getIdentifiantFiscal());
        if (dto.getPatente() != null) p.setPatente(dto.getPatente());
        if (dto.getRibBancaire() != null) p.setRibBancaire(dto.getRibBancaire());
        if (dto.getBanqueNom() != null) p.setBanqueNom(dto.getBanqueNom());
        if (dto.getDelaiPaiementJours() != null) p.setDelaiPaiementJours(dto.getDelaiPaiementJours());
        if (dto.getActif() != null) p.setActif(dto.getActif());
        if (dto.getNotes() != null) p.setNotes(dto.getNotes());

        Prestataire saved = prestataireRepository.save(p);
        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public List<PrestataireDTO> getAllPrestataires(boolean actifSeulement) {
        Long tenantId = getTenantId();
        List<Prestataire> list = actifSeulement
                ? prestataireRepository.findByPointDeVenteIdAndActifTrueOrderByRaisonSocialeAsc(tenantId)
                : prestataireRepository.findByPointDeVenteIdOrderByRaisonSocialeAsc(tenantId);
        return list.stream().map(p -> toDto(p, true)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrestataireDTO getPrestataireById(Long id) {
        Long tenantId = getTenantId();
        Prestataire p = prestataireRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prestataire introuvable avec l'ID: " + id));
        return toDto(p, true);
    }

    public void supprimerPrestataire(Long id) {
        Long tenantId = getTenantId();
        Prestataire p = prestataireRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prestataire introuvable avec l'ID: " + id));
        // Soft delete pour préserver l'historique comptable et des dépenses
        p.setActif(false);
        prestataireRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Page<PrestataireDTO> searchPrestataires(PrestataireSearchCriteria criteria, Pageable pageable) {
        Long tenantId = getTenantId();
        List<Prestataire> list = prestataireRepository.findByPointDeVenteIdOrderByRaisonSocialeAsc(tenantId);

        List<PrestataireDTO> filtered = list.stream()
                .filter(p -> {
                    if (criteria == null) return true;
                    if (criteria.getActif() != null && !criteria.getActif().equals(p.getActif())) {
                        return false;
                    }
                    if (criteria.getTypePrestation() != null && !criteria.getTypePrestation().equals(p.getTypePrestation())) {
                        return false;
                    }
                    if (criteria.getVille() != null && !criteria.getVille().trim().isEmpty()) {
                        if (p.getVille() == null || !p.getVille().toLowerCase().contains(criteria.getVille().toLowerCase().trim())) {
                            return false;
                        }
                    }
                    if (criteria.getIce() != null && !criteria.getIce().trim().isEmpty()) {
                        if (p.getIce() == null || !p.getIce().toLowerCase().contains(criteria.getIce().toLowerCase().trim())) {
                            return false;
                        }
                    }
                    if (criteria.getTelephone() != null && !criteria.getTelephone().trim().isEmpty()) {
                        if (p.getTelephone() == null || !p.getTelephone().toLowerCase().contains(criteria.getTelephone().toLowerCase().trim())) {
                            return false;
                        }
                    }
                    if (criteria.getRaisonSociale() != null && !criteria.getRaisonSociale().trim().isEmpty()) {
                        if (p.getRaisonSociale() == null || !p.getRaisonSociale().toLowerCase().contains(criteria.getRaisonSociale().toLowerCase().trim())) {
                            return false;
                        }
                    }
                    if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
                        String q = criteria.getQuery().toLowerCase().trim();
                        boolean match = (p.getRaisonSociale() != null && p.getRaisonSociale().toLowerCase().contains(q))
                                || (p.getContactNom() != null && p.getContactNom().toLowerCase().contains(q))
                                || (p.getIce() != null && p.getIce().toLowerCase().contains(q))
                                || (p.getTelephone() != null && p.getTelephone().toLowerCase().contains(q))
                                || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q))
                                || (p.getVille() != null && p.getVille().toLowerCase().contains(q));
                        if (!match) return false;
                    }
                    return true;
                })
                .map(p -> toDto(p, true))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<PrestataireDTO> pageContent = (start <= end && start < filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    private PrestataireDTO toDto(Prestataire p, boolean withStats) {
        PrestataireDTO dto = new PrestataireDTO();
        dto.setId(p.getId());
        dto.setPointDeVenteId(p.getPointDeVenteId());
        dto.setRaisonSociale(p.getRaisonSociale());
        dto.setContactNom(p.getContactNom());
        dto.setIce(p.getIce());
        dto.setTelephone(p.getTelephone());
        dto.setEmail(p.getEmail());
        dto.setAdresse(p.getAdresse());
        dto.setVille(p.getVille());
        dto.setCodePostal(p.getCodePostal());
        dto.setPays(p.getPays());
        dto.setTypePrestation(p.getTypePrestation());
        dto.setRegistreCommerce(p.getRegistreCommerce());
        dto.setIdentifiantFiscal(p.getIdentifiantFiscal());
        dto.setPatente(p.getPatente());
        dto.setRibBancaire(p.getRibBancaire());
        dto.setBanqueNom(p.getBanqueNom());
        dto.setDelaiPaiementJours(p.getDelaiPaiementJours());
        dto.setActif(p.getActif());
        dto.setNotes(p.getNotes());
        dto.setDateCreation(p.getDateCreation());

        if (withStats && p.getId() != null) {
            List<Depense> depenses = depenseRepository.findByPointDeVenteId(p.getPointDeVenteId());
            List<Depense> depensesDuPrestataire = depenses.stream()
                    .filter(d -> d.getPrestataire() != null && p.getId().equals(d.getPrestataire().getId()))
                    .collect(Collectors.toList());
            dto.setTotalDepenses((long) depensesDuPrestataire.size());
            BigDecimal total = depensesDuPrestataire.stream()
                    .map(d -> d.getMontant() != null ? d.getMontant() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalMontantDepenses(total);
        } else {
            dto.setTotalDepenses(0L);
            dto.setTotalMontantDepenses(BigDecimal.ZERO);
        }

        return dto;
    }
}
