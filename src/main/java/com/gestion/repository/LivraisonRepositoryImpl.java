package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.LivraisonSearchCriteria;
import com.gestion.persistent.model.Livraison;
import com.gestion.persistent.model.Commande;
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
public class LivraisonRepositoryImpl implements LivraisonRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroLivraison", "dateLivraison", "montantTotal", "statut", "transporteur"
    );

    @Override
    public List<Livraison> findByCriteria(LivraisonSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Livraison> query = cb.createQuery(Livraison.class);
        Root<Livraison> root = query.from(Livraison.class);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("dateLivraison")));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Page<Livraison> findByCriteria(LivraisonSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Livraison> query = cb.createQuery(Livraison.class);
        Root<Livraison> root = query.from(Livraison.class);

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
                query.orderBy(cb.desc(root.get("dateLivraison")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateLivraison")));
        }

        List<Livraison> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Livraison> countRoot = countQuery.from(Livraison.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Livraison> root, LivraisonSearchCriteria criteria) {
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

        if (criteria.getNumeroLivraison() != null && !criteria.getNumeroLivraison().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroLivraison")),
                "%" + criteria.getNumeroLivraison().toLowerCase() + "%"));
        }

        if (criteria.getNumeroSuivi() != null && !criteria.getNumeroSuivi().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroSuivi")),
                "%" + criteria.getNumeroSuivi().toLowerCase() + "%"));
        }

        if (criteria.getCommandeId() != null) {
            predicates.add(cb.equal(root.get("commande").get("id"), criteria.getCommandeId()));
        }

        if (criteria.getTransporteur() != null && !criteria.getTransporteur().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("transporteur")),
                "%" + criteria.getTransporteur().toLowerCase() + "%"));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (criteria.getDateDebutLivraison() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateLivraison"), criteria.getDateDebutLivraison()));
        }

        if (criteria.getDateFinLivraison() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateLivraison"), criteria.getDateFinLivraison()));
        }

        return predicates;
    }
}

