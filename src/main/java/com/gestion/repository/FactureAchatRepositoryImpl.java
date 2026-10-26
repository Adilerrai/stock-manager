package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.FactureAchatSearchCriteria;
import com.gestion.persistent.model.FactureAchat;
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
public class FactureAchatRepositoryImpl implements FactureAchatRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroFacture", "dateFacture", "dateEcheance", "montantHt",
            "montantTva", "montantTtc", "statut"
    );

    @Override
    public List<FactureAchat> findByCriteria(FactureAchatSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FactureAchat> query = cb.createQuery(FactureAchat.class);
        Root<FactureAchat> root = query.from(FactureAchat.class);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("dateFacture")), cb.desc(root.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Page<FactureAchat> findByCriteria(FactureAchatSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FactureAchat> query = cb.createQuery(FactureAchat.class);
        Root<FactureAchat> root = query.from(FactureAchat.class);

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
                query.orderBy(cb.desc(root.get("dateFacture")), cb.desc(root.get("id")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateFacture")), cb.desc(root.get("id")));
        }

        List<FactureAchat> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<FactureAchat> countRoot = countQuery.from(FactureAchat.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<FactureAchat> root, FactureAchatSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = (criteria != null && criteria.getSocieteId() != null)
                ? criteria.getSocieteId()
                : TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        } else {
            predicates.add(cb.equal(root.get("pointDeVenteId"), 1L));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getNumeroFacture() != null && !criteria.getNumeroFacture().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroFacture")), "%" + criteria.getNumeroFacture().trim().toLowerCase() + "%"));
        }

        if (criteria.getFournisseurId() != null) {
            predicates.add(cb.equal(root.get("fournisseur").get("id"), criteria.getFournisseurId()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateFacture"), criteria.getDateDebut().atStartOfDay()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateFacture"), criteria.getDateFin().atTime(23, 59, 59, 999999999)));
        }

        if (criteria.getMontantMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("montantTtc"), criteria.getMontantMin()));
        }

        if (criteria.getMontantMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("montantTtc"), criteria.getMontantMax()));
        }

        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String pattern = "%" + criteria.getQuery().trim().toLowerCase() + "%";
            List<Predicate> orPreds = new ArrayList<>();
            orPreds.add(cb.like(cb.lower(root.get("numeroFacture")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("fournisseur").get("raisonSociale")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("observations")), pattern));
            predicates.add(cb.or(orPreds.toArray(new Predicate[0])));
        }

        return predicates;
    }
}
