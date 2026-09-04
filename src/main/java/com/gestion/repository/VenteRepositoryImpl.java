package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.VenteSearchCriteria;
import com.gestion.persistent.model.Vente;
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
public class VenteRepositoryImpl implements VenteRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroVente", "dateVente", "montantHT", "montantTVA",
            "montantTTC", "montantFinal", "montantPaye", "montantRestant", "statut"
    );

    @Override
    public Page<Vente> findByCriteria(VenteSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Vente> query = cb.createQuery(Vente.class);
        Root<Vente> root = query.from(Vente.class);

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
                query.orderBy(cb.desc(root.get("dateVente")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateVente")));
        }

        List<Vente> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Vente> countRoot = countQuery.from(Vente.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Vente> root, VenteSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getCaissierId() != null) {
            predicates.add(cb.equal(root.get("caissier").get("id"), criteria.getCaissierId()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (Boolean.TRUE.equals(criteria.getPayee())) {
            predicates.add(cb.lessThanOrEqualTo(root.get("montantRestant"), BigDecimal.ZERO));
        } else if (Boolean.FALSE.equals(criteria.getPayee())) {
            predicates.add(cb.greaterThan(root.get("montantRestant"), BigDecimal.ZERO));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateVente"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateVente"), criteria.getDateFin()));
        }

        if (criteria.getMontantMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("montantFinal"), criteria.getMontantMin()));
        }

        if (criteria.getMontantMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("montantFinal"), criteria.getMontantMax()));
        }

        return predicates;
    }
}
