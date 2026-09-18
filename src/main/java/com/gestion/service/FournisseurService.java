package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.FournisseurDTO;
import com.gestion.persistent.dto.FournisseurSearchCriteria;
import com.gestion.persistent.model.Fournisseur;
import com.gestion.repository.FournisseurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;

    public FournisseurService(FournisseurRepository fournisseurRepository) {
        this.fournisseurRepository = fournisseurRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new org.springframework.security.access.AccessDeniedException("Impossible d'identifier l'entreprise (tenant) courante");
        }
        return tenant;
    }

    @Transactional
    public Fournisseur createFournisseur(FournisseurDTO fournisseurDTO) {
        Long tenantId = getTenantId();
        if (fournisseurRepository.existsByRaisonSocialeAndPointDeVenteId(fournisseurDTO.getRaisonSociale(), tenantId)) {
            throw new IllegalArgumentException("Un fournisseur avec ce nom existe déjà");
        }

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        fournisseur.setAdresse(fournisseurDTO.getAdresse());
        fournisseur.setTelephone(fournisseurDTO.getTelephone());
        fournisseur.setEmail(fournisseurDTO.getEmail());
        fournisseur.setContact(fournisseurDTO.getContact());
        fournisseur.setIce(fournisseurDTO.getIce());
        fournisseur.setNumeroRegistreCommerce(fournisseurDTO.getNumeroRegistreCommerce());
        fournisseur.setNumeroIdentificationFiscale(fournisseurDTO.getNumeroIdentificationFiscale());
        fournisseur.setPatente(fournisseurDTO.getPatente());
        fournisseur.setRibBancaire(fournisseurDTO.getRibBancaire());
        fournisseur.setBanqueNom(fournisseurDTO.getBanqueNom());
        fournisseur.setDelaiPaiementJours(fournisseurDTO.getDelaiPaiementJours() != null ? fournisseurDTO.getDelaiPaiementJours() : 30);
        fournisseur.setVille(fournisseurDTO.getVille());
        fournisseur.setCodePostal(fournisseurDTO.getCodePostal());
        fournisseur.setPays(fournisseurDTO.getPays() != null ? fournisseurDTO.getPays() : "Maroc");
        fournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
        fournisseur.setActif(true);
        fournisseur.setPointDeVenteId(tenantId);

        return fournisseurRepository.save(fournisseur);
    }

    public List<Fournisseur> getAllFournisseursActifs() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return List.of();
        }
        return fournisseurRepository.findActiveByPointDeVenteIdOrderByNom(tenant);
    }

    public Fournisseur getFournisseurById(Long fournisseurId) {
        Long tenantId = getTenantId();
        return fournisseurRepository.findByIdAndPointDeVenteId(fournisseurId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", fournisseurId));
    }

    @Transactional
    public Fournisseur updateFournisseur(FournisseurDTO fournisseurDTO) {
        Fournisseur fournisseur = getFournisseurById(fournisseurDTO.getId());
        
        // Mise à jour des champs
        if (fournisseurDTO.getRaisonSociale() != null) {
            fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        }
        if (fournisseurDTO.getAdresse() != null) {
            fournisseur.setAdresse(fournisseurDTO.getAdresse());
        }
        if (fournisseurDTO.getTelephone() != null) {
            fournisseur.setTelephone(fournisseurDTO.getTelephone());
        }
        if (fournisseurDTO.getEmail() != null) {
            fournisseur.setEmail(fournisseurDTO.getEmail());
        }
        if (fournisseurDTO.getContact() != null) {
            fournisseur.setContact(fournisseurDTO.getContact());
        }
        if (fournisseurDTO.getIce() != null) {
            fournisseur.setIce(fournisseurDTO.getIce());
        }
        if (fournisseurDTO.getNumeroRegistreCommerce() != null) {
            fournisseur.setNumeroRegistreCommerce(fournisseurDTO.getNumeroRegistreCommerce());
        }
        if (fournisseurDTO.getNumeroIdentificationFiscale() != null) {
            fournisseur.setNumeroIdentificationFiscale(fournisseurDTO.getNumeroIdentificationFiscale());
        }
        if (fournisseurDTO.getPatente() != null) {
            fournisseur.setPatente(fournisseurDTO.getPatente());
        }
        if (fournisseurDTO.getRibBancaire() != null) {
            fournisseur.setRibBancaire(fournisseurDTO.getRibBancaire());
        }
        if (fournisseurDTO.getBanqueNom() != null) {
            fournisseur.setBanqueNom(fournisseurDTO.getBanqueNom());
        }
        if (fournisseurDTO.getDelaiPaiementJours() != null) {
            fournisseur.setDelaiPaiementJours(fournisseurDTO.getDelaiPaiementJours());
        }
        if (fournisseurDTO.getVille() != null) {
            fournisseur.setVille(fournisseurDTO.getVille());
        }
        if (fournisseurDTO.getCodePostal() != null) {
            fournisseur.setCodePostal(fournisseurDTO.getCodePostal());
        }
        if (fournisseurDTO.getPays() != null) {
            fournisseur.setPays(fournisseurDTO.getPays());
        }
        if (fournisseurDTO.getConditionsPaiement() != null) {
            fournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
        }
        if (fournisseurDTO.getActif() != null) {
            fournisseur.setActif(fournisseurDTO.getActif());
        }

        return fournisseurRepository.save(fournisseur);
    }

    @Transactional
    public void deleteFournisseur(Long fournisseurId) {
        Fournisseur fournisseur = getFournisseurById(fournisseurId);
        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }

    public Page<Fournisseur> searchFournisseurs(FournisseurSearchCriteria criteria, Pageable pageable) {

        return fournisseurRepository.findByCriteria(criteria, pageable);
    }
}

