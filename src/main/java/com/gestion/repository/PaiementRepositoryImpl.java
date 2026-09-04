package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.PaiementSearchCriteria;
import com.gestion.persistent.model.Paiement;
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
public class PaiementRepositoryImpl implements PaiementRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroTransaction", "datePaiement", "modePaiement", "montant"
    );

    @Override
    public Page<Paiement> findByCriteria(PaiementSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Paiement> query = cb.createQuery(Paiement.class);
        Root<Paiement> root = query.from(Paiement.class);

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
                query.orderBy(cb.desc(root.get("datePaiement")));
            }
        } else {
            query.orderBy(cb.desc(root.get("datePaiement")));
        }

        List<Paiement> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Paiement> countRoot = countQuery.from(Paiement.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Paiement> root, PaiementSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getModePaiement() != null) {
            predicates.add(cb.equal(root.get("modePaiement"), criteria.getModePaiement()));
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getVenteId() != null) {
            predicates.add(cb.equal(root.get("vente").get("id"), criteria.getVenteId()));
        }

        if (criteria.getFactureId() != null) {
            predicates.add(cb.equal(root.get("facture").get("id"), criteria.getFactureId()));
        }

        if (criteria.getAnnule() != null) {
            predicates.add(cb.equal(root.get("annule"), criteria.getAnnule()));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("datePaiement"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("datePaiement"), criteria.getDateFin()));
        }

        if (criteria.getMontantMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("montant"), criteria.getMontantMin()));
        }

        if (criteria.getMontantMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("montant"), criteria.getMontantMax()));
        }

        return predicates;
    }
}
