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
        List<CategorieDTO> dtos = racines.stream().map(this::toDto).collect(Collectors.toList());
        enrichirCategoriesCounts(dtos);
        return dtos;
    }

    /**
     * Retourne uniquement les catégories feuilles (terminales, sans sous-catégories)
     */
    @Transactional(readOnly = true)
    public List<CategorieDTO> getCategoriesFeuilles() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Categorie> feuilles = categorieRepository.findCategoriesFeuillesOrderByNomAsc(tenantId);
        List<CategorieDTO> dtos = feuilles.stream().map(this::toDto).collect(Collectors.toList());
        enrichirCategoriesCounts(dtos);
        return dtos;
    }

    /**
     * Retourne les sous-catégories directes d'une catégorie
     */
    @Transactional(readOnly = true)
    public List<CategorieDTO> getSousCategories(Long parentId) {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Categorie> enfants;
        if (tenantId != null) {
            enfants = categorieRepository.findByParentIdAndPointDeVenteIdOrderByNomAsc(parentId, tenantId);
        } else {
            enfants = categorieRepository.findByParentIdOrderByNomAsc(parentId);
        }
        List<CategorieDTO> dtos = enfants.stream().map(this::toDto).collect(Collectors.toList());
        enrichirCategoriesCounts(dtos);
        return dtos;
    }

    /**
     * Chargement paresseux (Lazy-loading) pour l'arbre des catégories :
     * - Si parentId == null : retourne uniquement les catégories top parents (niveau racine).
     * - Si parentId != null : retourne uniquement les sous-catégories directes de ce parent.
     */
    @Transactional(readOnly = true)
    public List<CategorieNodeDTO> getNoeudsDirects(Long parentId) {
        Long tenantId = TenantContext.getCurrentTenant();
        List<Categorie> categories;
        if (parentId == null) {
            if (tenantId != null) {
                categories = categorieRepository.findByParentIsNullAndPointDeVenteIdOrderByNomAsc(tenantId);
            } else {
                categories = categorieRepository.findByParentIsNullOrderByNomAsc();
            }
        } else {
            if (tenantId != null) {
                categories = categorieRepository.findByParentIdAndPointDeVenteIdOrderByNomAsc(parentId, tenantId);
            } else {
                categories = categorieRepository.findByParentIdOrderByNomAsc(parentId);
            }
        }

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = categories.stream().map(Categorie::getId).collect(Collectors.toList());

        // 1. Nombre de sous-catégories directes
        Map<Long, Integer> sousCatCounts = new HashMap<>();
        List<Object[]> scList = categorieRepository.countSousCategoriesGroupedByParentIdIn(ids);
        if (scList != null) {
            for (Object[] row : scList) {
                if (row[0] != null && row[1] != null) {
                    sousCatCounts.put((Long) row[0], ((Number) row[1]).intValue());
                }
            }
        }

        // 2. Nombre de produits rattachés
        Map<Long, Long> prodCounts = new HashMap<>();
        List<Object[]> pList = produitRepository.countGroupedByCategorieIdIn(ids);
        if (pList != null) {
            for (Object[] row : pList) {
                if (row[0] != null && row[1] != null) {
                    prodCounts.put((Long) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        return categories.stream().map(c -> {
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

            int scCount = sousCatCounts.getOrDefault(c.getId(), 0);
            long prodCount = prodCounts.getOrDefault(c.getId(), 0L);

            node.setNombreSousCategories(scCount);
            node.setHasChildren(scCount > 0);
            node.setNombreProduits(prodCount);
            node.setChildren(new ArrayList<>()); // Vide par défaut pour chargement à la demande

            return node;
        }).collect(Collectors.toList());
    }

    private void enrichirCategoriesCounts(List<CategorieDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        List<Long> ids = dtos.stream().map(CategorieDTO::getId).collect(Collectors.toList());

        Map<Long, Integer> sousCatCounts = new HashMap<>();
        List<Object[]> scList = categorieRepository.countSousCategoriesGroupedByParentIdIn(ids);
        if (scList != null) {
            for (Object[] row : scList) {
                if (row[0] != null && row[1] != null) {
                    sousCatCounts.put((Long) row[0], ((Number) row[1]).intValue());
                }
            }
        }

        Map<Long, Long> prodCounts = new HashMap<>();
        List<Object[]> pList = produitRepository.countGroupedByCategorieIdIn(ids);
        if (pList != null) {
            for (Object[] row : pList) {
                if (row[0] != null && row[1] != null) {
                    prodCounts.put((Long) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        for (CategorieDTO dto : dtos) {
            int scCount = sousCatCounts.getOrDefault(dto.getId(), 0);
            long prodCount = prodCounts.getOrDefault(dto.getId(), 0L);
            dto.setNombreSousCategories(scCount);
            dto.setHasChildren(scCount > 0);
            dto.setNombreProduits(prodCount);
        }
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

        // Attacher le parent si parentId ou parent est renseigné
        Long parentIdToAttach = categorie.getParentId();
        if (parentIdToAttach == null && categorie.getParent() != null) {
            parentIdToAttach = categorie.getParent().getId();
        }
        if (parentIdToAttach != null && parentIdToAttach > 0) {
            Categorie parent = getCategorieById(parentIdToAttach);
            categorie.setParent(parent);
        } else {
            categorie.setParent(null);
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
        Long newParentId = updated.getParentId();
        if (newParentId == null && updated.getParent() != null) {
            newParentId = updated.getParent().getId();
        }

        if (newParentId != null && newParentId > 0) {
            if (!newParentId.equals(existing.getId())) {
                Categorie parent = getCategorieById(newParentId);
                existing.setParent(parent);
            }
        } else if (newParentId != null && newParentId == 0) {
            existing.setParent(null);
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
