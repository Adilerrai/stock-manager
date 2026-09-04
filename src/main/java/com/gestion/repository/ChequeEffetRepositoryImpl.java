package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ChequeEffetSearchCriteria;
import com.gestion.persistent.model.ChequeEffet;
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
public class ChequeEffetRepositoryImpl implements ChequeEffetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroPiece", "type", "sens", "statut", "montant", "dateEmission", "dateEcheance"
    );

    @Override
    public Page<ChequeEffet> findByCriteria(ChequeEffetSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ChequeEffet> query = cb.createQuery(ChequeEffet.class);
        Root<ChequeEffet> root = query.from(ChequeEffet.class);

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
                query.orderBy(cb.asc(root.get("dateEcheance")));
            }
        } else {
            query.orderBy(cb.asc(root.get("dateEcheance")));
        }

        List<ChequeEffet> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ChequeEffet> countRoot = countQuery.from(ChequeEffet.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<ChequeEffet> root, ChequeEffetSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getNumeroPiece() != null && !criteria.getNumeroPiece().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroPiece")), "%" + criteria.getNumeroPiece().trim().toLowerCase() + "%"));
        }

        if (criteria.getType() != null) {
            predicates.add(cb.equal(root.get("type"), criteria.getType()));
        }

        if (criteria.getSens() != null) {
            predicates.add(cb.equal(root.get("sens"), criteria.getSens()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (criteria.getTireur() != null && !criteria.getTireur().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("tireur")), "%" + criteria.getTireur().trim().toLowerCase() + "%"));
        }

        if (criteria.getBanque() != null && !criteria.getBanque().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("banque")), "%" + criteria.getBanque().trim().toLowerCase() + "%"));
        }

        if (criteria.getDateEcheanceDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateEcheance"), criteria.getDateEcheanceDebut()));
        }

        if (criteria.getDateEcheanceFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateEcheance"), criteria.getDateEcheanceFin()));
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
