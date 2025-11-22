package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.ceramique.persistent.dto.FournisseurDTO;
import com.ceramique.persistent.dto.FournisseurSearchCriteria;
import com.ceramique.persistent.model.Fournisseur;
import com.ceramique.repository.FournisseurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public FournisseurService(FournisseurRepository fournisseurRepository, 
                             PointDeVenteRepository pointDeVenteRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un nouveau fournisseur")
    public Fournisseur createFournisseur(FournisseurDTO fournisseurDTO) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        if (fournisseurRepository.existsByRaisonSocialeAndPointDeVente_Id(fournisseurDTO.getRaisonSociale(), pointDeVente.getId())) {
            throw new IllegalArgumentException("Un fournisseur avec ce nom existe déjà");
        }

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        fournisseur.setAdresse(fournisseurDTO.getAdresse());
        fournisseur.setTelephone(fournisseurDTO.getTelephone());
        fournisseur.setEmail(fournisseurDTO.getEmail());
        fournisseur.setContact(fournisseurDTO.getContact());
        fournisseur.setPointDeVente(pointDeVente);
        fournisseur.setActif(true);

        return fournisseurRepository.save(fournisseur);
    }

    @MultitenantSearchMethod(description = "Récupération de tous les fournisseurs actifs")
    public List<Fournisseur> getAllFournisseursActifs() {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return fournisseurRepository.findActiveByPointDeVenteOrderByNom(pointDeVente.getId());
    }

    @MultitenantSearchMethod(description = "Récupération d'un fournisseur par ID")
    public Fournisseur getFournisseurById(Long fournisseurId) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return fournisseurRepository.findByIdAndPointDeVente_Id(fournisseurId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", fournisseurId));
    }

    @Transactional
    @MultitenantSearchMethod(description = "Mise à jour d'un fournisseur")
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
        if (fournisseurDTO.getActif() != null) {
            fournisseur.setActif(fournisseurDTO.getActif());
        }

        return fournisseurRepository.save(fournisseur);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Suppression d'un fournisseur")
    public void deleteFournisseur(Long fournisseurId) {
        Fournisseur fournisseur = getFournisseurById(fournisseurId);
        fournisseur.setActif(false);
        fournisseurRepository.save(fournisseur);
    }

    @MultitenantSearchMethod(description = "Recherche de fournisseurs par critères avec pagination")
    public Page<Fournisseur> searchFournisseurs(FournisseurSearchCriteria criteria, Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        // Le filtre du point de vente sera ajouté dans le repository
        return fournisseurRepository.findByCriteria(criteria, pageable);
    }
}
