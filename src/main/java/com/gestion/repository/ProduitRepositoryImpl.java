package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ProduitSearchCriteria;
import com.gestion.persistent.model.Categorie;
import com.gestion.persistent.model.Produit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class ProduitRepositoryImpl implements ProduitRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "reference", "designation", "description", "categorieArticle", "groupeArticle", "prixVente", "dateCreation"
    );

    @Override
    public Page<Produit> findByCriteria(ProduitSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Produit> query = cb.createQuery(Produit.class);
        Root<Produit> root = query.from(Produit.class);
        root.fetch("categorie", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);

        query.where(predicates.toArray(new Predicate[0]));

        if (pageable != null && pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                String prop = order.getProperty();
                if (ALLOWED_SORT_PROPERTIES.contains(prop)) {
                    orders.add(order.isAscending() ? cb.asc(root.get(prop)) : cb.desc(root.get(prop)));
                }
            });
            if (!orders.isEmpty()) {
                query.orderBy(orders);
            } else {
                query.orderBy(cb.asc(root.get("description")));
            }
        } else {
            query.orderBy(cb.asc(root.get("description")));
        }

        // Count query pour la pagination
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Produit> countRoot = countQuery.from(Produit.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        List<Produit> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Produit> root,
                                            ProduitSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = (criteria != null && criteria.getSocieteId() != null)
                ? criteria.getSocieteId()
                : TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        } else {
            predicates.add(cb.equal(root.get("pointDeVenteId"), -1L));
        }

        if (criteria == null) {
            return predicates;
        }

        // Recherche globale par mot-clé (search)
        if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
            String pattern = "%" + criteria.getSearch().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("reference")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.and(cb.isNotNull(root.get("designation")), cb.like(cb.lower(root.get("designation")), pattern)),
                    cb.and(cb.isNotNull(root.get("categorieArticle")), cb.like(cb.lower(root.get("categorieArticle")), pattern))
            ));
        }

        // Nom ou Désignation
        String nomOuDes = (criteria.getNom() != null && !criteria.getNom().trim().isEmpty())
                ? criteria.getNom().trim()
                : (criteria.getDesignation() != null && !criteria.getDesignation().trim().isEmpty() ? criteria.getDesignation().trim() : null);
        if (nomOuDes != null) {
            String pattern = "%" + nomOuDes.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.and(cb.isNotNull(root.get("designation")), cb.like(cb.lower(root.get("designation")), pattern))
            ));
        }

        // Référence
        if (criteria.getReference() != null && !criteria.getReference().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("reference")),
                    "%" + criteria.getReference().trim().toLowerCase() + "%"));
        }

        // Description
        if (criteria.getDescription() != null && !criteria.getDescription().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("description")),
                    "%" + criteria.getDescription().trim().toLowerCase() + "%"));
        }

        // Prix minimum (utiliser 'prixVente')
        if (criteria.getPrixMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("prixVente"), criteria.getPrixMin()));
        }

        // Prix maximum (utiliser 'prixVente')
        if (criteria.getPrixMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("prixVente"), criteria.getPrixMax()));
        }

        // Actif
        if (criteria.getActif() != null) {
            if (Boolean.TRUE.equals(criteria.getActif())) {
                predicates.add(cb.or(cb.isNull(root.get("actif")), cb.isTrue(root.get("actif"))));
            } else {
                predicates.add(cb.isFalse(root.get("actif")));
            }
        }

        // Catégorie par ID
        if (criteria.getCategorieId() != null) {
            predicates.add(cb.equal(root.get("categorie").get("id"), criteria.getCategorieId()));
        }

        // Catégorie par libellé
        if (criteria.getCategorie() != null && !criteria.getCategorie().trim().isEmpty()) {
            String catPattern = "%" + criteria.getCategorie().trim().toLowerCase() + "%";
            Join<Produit, Categorie> catJoin = root.join("categorie", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.and(cb.isNotNull(root.get("categorieArticle")), cb.like(cb.lower(root.get("categorieArticle")), catPattern)),
                    cb.and(cb.isNotNull(catJoin.get("nom")), cb.like(cb.lower(catJoin.get("nom")), catPattern))
            ));
        }

        // Groupe d'article
        if (criteria.getGroupeArticle() != null && !criteria.getGroupeArticle().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("groupeArticle")),
                    "%" + criteria.getGroupeArticle().trim().toLowerCase() + "%"));
        }

        return predicates;
    }
}
