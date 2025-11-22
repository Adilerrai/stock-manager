package com.ceramique.repository;

import com.ceramique.persistent.dto.FournisseurSearchCriteria;
import com.ceramique.persistent.model.Fournisseur;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class FournisseurRepositoryImpl implements FournisseurRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Fournisseur> findByCriteria(FournisseurSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Fournisseur> query = cb.createQuery(Fournisseur.class);
        Root<Fournisseur> root = query.from(Fournisseur.class);

        List<Predicate> predicates = new ArrayList<>();

        // Point de vente obligatoire - géré via l'annotation @MultitenantSearchMethod
        // qui filtre automatiquement par tenant/point de vente

        // Raison sociale
        if (criteria.getRaisonSociale() != null && !criteria.getRaisonSociale().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("raisonSociale")),
                    "%" + criteria.getRaisonSociale().toLowerCase() + "%"));
        }

        // Adresse
        if (criteria.getAdresse() != null && !criteria.getAdresse().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("adresse")),
                    "%" + criteria.getAdresse().toLowerCase() + "%"));
        }

        // Téléphone
        if (criteria.getTelephone() != null && !criteria.getTelephone().trim().isEmpty()) {
            predicates.add(cb.like(root.get("telephone"),
                    "%" + criteria.getTelephone() + "%"));
        }

        // Email
        if (criteria.getEmail() != null && !criteria.getEmail().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("email")),
                    "%" + criteria.getEmail().toLowerCase() + "%"));
        }

        // Contact
        if (criteria.getContact() != null && !criteria.getContact().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("contact")),
                    "%" + criteria.getContact().toLowerCase() + "%"));
        }

        // Actif
        if (criteria.getActif() != null) {
            predicates.add(cb.equal(root.get("actif"), criteria.getActif()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        // Tri avec validation des noms de propriétés (déduplication)
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            java.util.Set<String> used = new java.util.HashSet<>();
            pageable.getSort().forEach(order -> {
                String propertyMapped = mapPropertyName(order.getProperty());
                if (isValidProperty(propertyMapped) && used.add(propertyMapped)) {
                    Path<?> path = root.get(propertyMapped); // obtenir une seule instance du path
                    orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
                }
            });
            if (!orders.isEmpty()) {
                query.orderBy(orders);
            } else {
                query.orderBy(cb.asc(root.get("raisonSociale")));
            }
        } else {
            query.orderBy(cb.asc(root.get("raisonSociale")));
        }

        // Pagination
        List<Fournisseur> resultList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        // Total count: reconstruire les prédicats pour countRoot afin d'éviter la réutilisation de Path sur un autre Root
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Fournisseur> countRoot = countQuery.from(Fournisseur.class);
        List<Predicate> countPredicates = new ArrayList<>();
        // Raison sociale
        if (criteria.getRaisonSociale() != null && !criteria.getRaisonSociale().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("raisonSociale")), "%" + criteria.getRaisonSociale().toLowerCase() + "%"));
        }
        // Adresse
        if (criteria.getAdresse() != null && !criteria.getAdresse().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("adresse")), "%" + criteria.getAdresse().toLowerCase() + "%"));
        }
        // Téléphone
        if (criteria.getTelephone() != null && !criteria.getTelephone().trim().isEmpty()) {
            countPredicates.add(cb.like(countRoot.get("telephone"), "%" + criteria.getTelephone() + "%"));
        }
        // Email
        if (criteria.getEmail() != null && !criteria.getEmail().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("email")), "%" + criteria.getEmail().toLowerCase() + "%"));
        }
        // Contact
        if (criteria.getContact() != null && !criteria.getContact().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("contact")), "%" + criteria.getContact().toLowerCase() + "%"));
        }
        // Actif
        if (criteria.getActif() != null) {
            countPredicates.add(cb.equal(countRoot.get("actif"), criteria.getActif()));
        }
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(resultList, pageable, totalCount);
    }

    /**
     * Mappe les noms de propriétés pour gérer les variations (raisonSocial -> raisonSociale)
     */
    private String mapPropertyName(String property) {
        if ("raisonSocial".equals(property)) {
            return "raisonSociale";
        }
        return property;
    }

    /**
     * Vérifie si la propriété existe dans l'entité Fournisseur
     */
    private boolean isValidProperty(String property) {
        return List.of("id", "raisonSociale", "adresse", "telephone", "email",
                       "contact", "actif", "dateCreation").contains(property);
    }
}
