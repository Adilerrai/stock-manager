package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CategorieDTO;
import com.gestion.persistent.dto.CategorieNodeDTO;
import com.gestion.persistent.model.Categorie;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.CategorieRepository;
import com.gestion.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;

    public CategorieService(CategorieRepository categorieRepository, ProduitRepository produitRepository) {
        this.categorieRepository = categorieRepository;
        this.produitRepository = produitRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorieDTO> getAllCategories() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Categorie> categories;
        if (tenantId != null) {
            categories = categorieRepository.findByPointDeVenteIdOrderByNomAsc(tenantId);
        } else {
            categories = categorieRepository.findByActifTrueOrderByNomAsc();
        }
        return categories.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Categorie getCategorieById(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public CategorieDTO getCategorieDtoById(Long id) {
        return toDto(getCategorieById(id));
    }

    /**
     * Retourne les catégories racines (niveau 0, sans parent)
     */
    @Transactional(readOnly = true)
    public List<CategorieDTO> getCategoriesRacines() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Categorie> racines;
        if (tenantId != null) {
            racines = categorieRepository.findByParentIsNullAndPointDeVenteIdOrderByNomAsc(tenantId);
        } else {
            racines = categorieRepository.findByParentIsNullOrderByNomAsc();
        }
        return racines.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Retourne les sous-catégories directes d'une catégorie
     */
    @Transactional(readOnly = true)
    public List<CategorieDTO> getSousCategories(Long parentId) {
        return categorieRepository.findByParentIdOrderByNomAsc(parentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Construit l'arbre complet récursif des catégories et sous-catégories
     */
    @Transactional(readOnly = true)
    public List<CategorieNodeDTO> getArbreCategories() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Categorie> racines;
        if (tenantId != null) {
            racines = categorieRepository.findByParentIsNullAndPointDeVenteIdOrderByNomAsc(tenantId);
        } else {
            racines = categorieRepository.findByParentIsNullOrderByNomAsc();
        }

        return racines.stream()
                .map(this::construireNoeudRecursif)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les IDs de la catégorie et de toutes ses sous-catégories récursivement
     */
    @Transactional(readOnly = true)
    public List<Long> getIdsRecursifs(Long categorieId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categorieId);
        collecterIdsDescendants(categorieId, ids);
        return ids;
    }

    private void collecterIdsDescendants(Long parentId, List<Long> ids) {
        List<Categorie> enfants = categorieRepository.findByParentIdOrderByNomAsc(parentId);
        for (Categorie enfant : enfants) {
            ids.add(enfant.getId());
            collecterIdsDescendants(enfant.getId(), ids);
        }
    }

    /**
     * Retourne tous les produits appartenant à cette catégorie ou à l'une de ses sous-catégories
     */
    @Transactional(readOnly = true)
    public List<Produit> getProduitsParCategorieRecursive(Long categorieId) {
        List<Long> tousLesIds = getIdsRecursifs(categorieId);
        return produitRepository.findByCategorieIdIn(tousLesIds);
    }

    /**
     * Création d'une catégorie (avec ou sans parent)
     */
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

    /**
     * Crée une sous-catégorie sous un parent existant
     */
    public Categorie creerSousCategorie(Long parentId, Categorie sousCategorie) {
        Categorie parent = getCategorieById(parentId);
        sousCategorie.setParent(parent);
        return creerCategorie(sousCategorie);
    }

    public Categorie modifierCategorie(Long id, Categorie updated) {
        Categorie existing = getCategorieById(id);
        if (updated.getNom() != null) existing.setNom(updated.getNom());
        if (updated.getCode() != null) existing.setCode(updated.getCode());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getCouleur() != null) existing.setCouleur(updated.getCouleur());
        if (updated.getIcone() != null) existing.setIcone(updated.getIcone());
        if (updated.getActif() != null) existing.setActif(updated.getActif());

        // Modification éventuelle du parent (en évitant la boucle cyclique)
        if (updated.getParent() != null) {
            if (!updated.getParent().getId().equals(existing.getId())) {
                existing.setParent(updated.getParent());
            }
        }
        return categorieRepository.save(existing);
    }

    public void supprimerCategorie(Long id) {
        categorieRepository.deleteById(id);
    }

    private CategorieNodeDTO construireNoeudRecursif(Categorie c) {
        CategorieNodeDTO node = new CategorieNodeDTO();
        node.setId(c.getId());
        node.setNom(c.getNom());
        node.setCode(c.getCode());
        node.setDescription(c.getDescription());
        node.setCouleur(c.getCouleur());
        node.setIcone(c.getIcone());
        node.setNiveau(c.getNiveau());
        node.setCheminComplet(c.getCheminComplet());

        if (c.getParent() != null) {
            node.setParentId(c.getParent().getId());
            node.setParentNom(c.getParent().getNom());
        }

        // Compter les produits directement attachés
        long nbProd = produitRepository.countByCategorieId(c.getId());
        node.setNombreProduits(nbProd);

        // Récursion sur les sous-catégories
        List<Categorie> sousCats = categorieRepository.findByParentIdOrderByNomAsc(c.getId());
        for (Categorie sc : sousCats) {
            node.addChild(construireNoeudRecursif(sc));
        }

        return node;
    }

    private CategorieDTO toDto(Categorie c) {
        CategorieDTO dto = new CategorieDTO();
        dto.setId(c.getId());
        dto.setNom(c.getNom());
        dto.setCode(c.getCode());
        dto.setDescription(c.getDescription());
        dto.setCouleur(c.getCouleur());
        dto.setIcone(c.getIcone());
        dto.setActif(c.getActif());
        dto.setPointDeVenteId(c.getPointDeVenteId());
        dto.setDateCreation(c.getDateCreation());
        dto.setNiveau(c.getNiveau());
        dto.setCheminComplet(c.getCheminComplet());

        if (c.getParent() != null) {
            dto.setParentId(c.getParent().getId());
            dto.setParentNom(c.getParent().getNom());
        }
        return dto;
    }
}
