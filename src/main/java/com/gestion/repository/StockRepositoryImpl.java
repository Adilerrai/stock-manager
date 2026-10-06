package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.StockSearchCriteria;
import com.gestion.persistent.model.Produit;
import com.gestion.persistent.model.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class StockRepositoryImpl implements StockRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "quantiteDisponible", "quantiteReservee", "seuilAlerte", "derniereMaj"
    );

    @Override
    public Page<Stock> findByCriteria(StockSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Main Query
        CriteriaQuery<Stock> query = cb.createQuery(Stock.class);
        Root<Stock> root = query.from(Stock.class);
        Join<Stock, Produit> produitJoin = root.join("produit", JoinType.INNER);

        List<Predicate> predicates = buildPredicates(cb, root, produitJoin, criteria);
        query.where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                String prop = order.getProperty();
                if (ALLOWED_SORT_PROPERTIES.contains(prop)) {
                    orders.add(order.isAscending() ? cb.asc(root.get(prop)) : cb.desc(root.get(prop)));
                } else if ("produitNom".equalsIgnoreCase(prop) || "designation".equalsIgnoreCase(prop)) {
                    orders.add(order.isAscending() ? cb.asc(produitJoin.get("description")) : cb.desc(produitJoin.get("description")));
                } else if ("produitReference".equalsIgnoreCase(prop) || "reference".equalsIgnoreCase(prop)) {
                    orders.add(order.isAscending() ? cb.asc(produitJoin.get("reference")) : cb.desc(produitJoin.get("reference")));
                }
            });
            if (!orders.isEmpty()) {
                query.orderBy(orders);
            } else {
                query.orderBy(cb.desc(root.get("id")));
            }
        } else {
            query.orderBy(cb.desc(root.get("id")));
        }

        List<Stock> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // 2. Count Query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Stock> countRoot = countQuery.from(Stock.class);
        Join<Stock, Produit> countProduitJoin = countRoot.join("produit", JoinType.INNER);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, countProduitJoin, criteria);

        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Stock> root, Join<Stock, Produit> produitJoin,
                                           StockSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        // Multi-tenant isolation
        Long tenantId = (criteria != null && criteria.getSocieteId() != null)
                ? criteria.getSocieteId()
                : TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(produitJoin.get("pointDeVenteId"), tenantId));
        } else {
            predicates.add(cb.equal(produitJoin.get("pointDeVenteId"), -1L));
        }

        if (criteria == null) {
            return predicates;
        }

        // Recherche textuelle globale (Désignation, Référence, Code-barres)
        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String q = "%" + criteria.getQuery().trim().toLowerCase() + "%";
            List<Predicate> qPreds = new ArrayList<>();
            qPreds.add(cb.like(cb.lower(produitJoin.get("description")), q));
            qPreds.add(cb.like(cb.lower(produitJoin.get("reference")), q));
            if (produitJoin.get("designation") != null) {
                qPreds.add(cb.like(cb.lower(produitJoin.get("designation")), q));
            }
            if (produitJoin.get("codeBarre") != null) {
                qPreds.add(cb.like(cb.lower(produitJoin.get("codeBarre")), q));
            }
            predicates.add(cb.or(qPreds.toArray(new Predicate[0])));
        }

        // Filtre produitId direct
        if (criteria.getProduitId() != null) {
            predicates.add(cb.equal(produitJoin.get("id"), criteria.getProduitId()));
        }

        // Filtre nom de produit
        if (criteria.getProduitNom() != null && !criteria.getProduitNom().trim().isEmpty()) {
            String nom = "%" + criteria.getProduitNom().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(produitJoin.get("description")), nom),
                    cb.like(cb.lower(produitJoin.get("designation")), nom)
            ));
        }

        // Filtre référence produit
        if (criteria.getProduitReference() != null && !criteria.getProduitReference().trim().isEmpty()) {
            String ref = "%" + criteria.getProduitReference().trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(produitJoin.get("reference")), ref));
        }

        // Filtre catégorie
        if (criteria.getCategorieId() != null) {
            predicates.add(cb.equal(produitJoin.get("categorie").get("id"), criteria.getCategorieId()));
        }

        // Filtre quantité min / max
        if (criteria.getQuantiteMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("quantiteDisponible"), criteria.getQuantiteMin()));
        }
        if (criteria.getQuantiteMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("quantiteDisponible"), criteria.getQuantiteMax()));
        }

        // Filtre statut stock (OK, ALERTE, RUPTURE)
        if (criteria.getStatut() != null && !criteria.getStatut().trim().isEmpty() && !criteria.getStatut().equalsIgnoreCase("ALL")) {
            String statut = criteria.getStatut().trim().toUpperCase();
            if ("RUPTURE".equals(statut)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantiteDisponible"), BigDecimal.ZERO));
            } else if ("ALERTE".equals(statut)) {
                predicates.add(cb.and(
                        cb.greaterThan(root.get("quantiteDisponible"), BigDecimal.ZERO),
                        cb.lessThanOrEqualTo(root.get("quantiteDisponible"), root.get("seuilAlerte"))
                ));
            } else if ("OK".equals(statut)) {
                predicates.add(cb.greaterThan(root.get("quantiteDisponible"), root.get("seuilAlerte")));
            }
        }

        return predicates;
    }
}
