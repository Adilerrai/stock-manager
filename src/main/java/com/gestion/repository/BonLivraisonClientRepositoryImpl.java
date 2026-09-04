package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.BonLivraisonClientSearchCriteria;
import com.gestion.persistent.model.BonLivraisonClient;
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
public class BonLivraisonClientRepositoryImpl implements BonLivraisonClientRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroBL", "dateBl", "statut"
    );

    @Override
    public Page<BonLivraisonClient> findByCriteria(BonLivraisonClientSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BonLivraisonClient> query = cb.createQuery(BonLivraisonClient.class);
        Root<BonLivraisonClient> root = query.from(BonLivraisonClient.class);

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
                query.orderBy(cb.desc(root.get("dateBl")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateBl")));
        }

        List<BonLivraisonClient> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<BonLivraisonClient> countRoot = countQuery.from(BonLivraisonClient.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<BonLivraisonClient> root, BonLivraisonClientSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getNumeroBL() != null && !criteria.getNumeroBL().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroBL")), "%" + criteria.getNumeroBL().trim().toLowerCase() + "%"));
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getCommandeClientId() != null) {
            predicates.add(cb.equal(root.get("commandeClient").get("id"), criteria.getCommandeClientId()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (Boolean.TRUE.equals(criteria.getFacturee())) {
            predicates.add(cb.isNotNull(root.get("facture")));
        } else if (Boolean.FALSE.equals(criteria.getFacturee())) {
            predicates.add(cb.isNull(root.get("facture")));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateBl"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateBl"), criteria.getDateFin()));
        }

        return predicates;
    }
}
