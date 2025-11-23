package com.ceramique.service;

import com.acommon.exception.ResourceNotFoundException;
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

    public FournisseurService(FournisseurRepository fournisseurRepository) {
        this.fournisseurRepository = fournisseurRepository;
    }

    @Transactional
    public Fournisseur createFournisseur(FournisseurDTO fournisseurDTO) {

        if (fournisseurRepository.existsByRaisonSociale((fournisseurDTO.getRaisonSociale())) ){
            throw new IllegalArgumentException("Un fournisseur avec ce nom existe déjà");
        }

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
        fournisseur.setAdresse(fournisseurDTO.getAdresse());
        fournisseur.setTelephone(fournisseurDTO.getTelephone());
        fournisseur.setEmail(fournisseurDTO.getEmail());
        fournisseur.setContact(fournisseurDTO.getContact());
        fournisseur.setActif(true);

        return fournisseurRepository.save(fournisseur);
    }

    public List<Fournisseur> getAllFournisseursActifs() {
        return fournisseurRepository.findActiveOrderByNom();
    }

    public Fournisseur getFournisseurById(Long fournisseurId) {
        return fournisseurRepository.findById(fournisseurId)
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
