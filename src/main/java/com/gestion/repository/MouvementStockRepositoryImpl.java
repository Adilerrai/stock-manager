package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.MouvementStockSearchCriteria;
import com.gestion.persistent.model.MouvementStock;
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
public class MouvementStockRepositoryImpl implements MouvementStockRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "dateMouvement", "typeMouvement", "quantite"
    );

    @Override
    public Page<MouvementStock> findByCriteria(MouvementStockSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MouvementStock> query = cb.createQuery(MouvementStock.class);
        Root<MouvementStock> root = query.from(MouvementStock.class);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
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
                query.orderBy(cb.desc(root.get("dateMouvement")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateMouvement")));
        }

        List<MouvementStock> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<MouvementStock> countRoot = countQuery.from(MouvementStock.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<MouvementStock> root, MouvementStockSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getProduitId() != null) {
            predicates.add(cb.equal(root.get("produit").get("id"), criteria.getProduitId()));
        }

        if (criteria.getDepotId() != null) {
            predicates.add(cb.equal(root.get("depot").get("id"), criteria.getDepotId()));
        }

        if (criteria.getTypeMouvement() != null) {
            predicates.add(cb.equal(root.get("typeMouvement"), criteria.getTypeMouvement()));
        }

        if (criteria.getNumeroLot() != null && !criteria.getNumeroLot().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("lot").get("numeroLot")), "%" + criteria.getNumeroLot().trim().toLowerCase() + "%"));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateMouvement"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateMouvement"), criteria.getDateFin()));
        }

        return predicates;
    }
}
