package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.FactureSearchCriteria;
import com.gestion.persistent.model.Facture;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class FactureRepositoryImpl implements FactureRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroFacture", "dateFacture", "dateEcheance", "montantHT",
            "montantTVA", "montantTTC", "montantFinal", "montantPaye", "montantRestant", "statut"
    );

    @Override
    public Page<Facture> findByCriteria(FactureSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Facture> query = cb.createQuery(Facture.class);
        Root<Facture> root = query.from(Facture.class);

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
                query.orderBy(cb.desc(root.get("dateFacture")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateFacture")));
        }

        List<Facture> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Facture> countRoot = countQuery.from(Facture.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Facture> root, FactureSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getNumeroFacture() != null && !criteria.getNumeroFacture().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroFacture")), "%" + criteria.getNumeroFacture().trim().toLowerCase() + "%"));
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (Boolean.TRUE.equals(criteria.getEstEchue())) {
            predicates.add(cb.lessThan(root.get("dateEcheance"), LocalDate.now()));
            predicates.add(cb.greaterThan(root.get("montantRestant"), BigDecimal.ZERO));
            predicates.add(cb.or(cb.isNull(root.get("annulee")), cb.isFalse(root.get("annulee"))));
        }

        if (Boolean.TRUE.equals(criteria.getPayee())) {
            predicates.add(cb.lessThanOrEqualTo(root.get("montantRestant"), BigDecimal.ZERO));
        } else if (Boolean.FALSE.equals(criteria.getPayee())) {
            predicates.add(cb.greaterThan(root.get("montantRestant"), BigDecimal.ZERO));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateFacture"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateFacture"), criteria.getDateFin()));
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
