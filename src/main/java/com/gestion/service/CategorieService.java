package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.model.Categorie;
import com.gestion.repository.CategorieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public CategorieService(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    public List<Categorie> getAllCategories() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return categorieRepository.findByPointDeVenteIdOrderByNomAsc(tenantId);
        }
        return categorieRepository.findByActifTrueOrderByNomAsc();
    }

    public Categorie getCategorieById(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée: " + id));
    }

    public Categorie creerCategorie(Categorie categorie) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            categorie.setPointDeVenteId(tenantId);
        }
        if (categorie.getActif() == null) {
            categorie.setActif(true);
        }
        categorie.setDateCreation(LocalDateTime.now());
        return categorieRepository.save(categorie);
    }

    public Categorie modifierCategorie(Long id, Categorie updated) {
        Categorie existing = getCategorieById(id);
        if (updated.getNom() != null) existing.setNom(updated.getNom());
        if (updated.getCode() != null) existing.setCode(updated.getCode());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getCouleur() != null) existing.setCouleur(updated.getCouleur());
        if (updated.getIcone() != null) existing.setIcone(updated.getIcone());
        if (updated.getActif() != null) existing.setActif(updated.getActif());
        return categorieRepository.save(existing);
    }

    public void supprimerCategorie(Long id) {
        categorieRepository.deleteById(id);
    }
}
