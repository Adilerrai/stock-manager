package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ClientSearchCriteria;
import com.gestion.persistent.model.Client;
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
public class ClientRepositoryImpl implements ClientRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "nom", "prenom", "nomComplet", "telephone", "email",
            "ville", "categorie", "tarif", "creditAutorise", "creditUtilise",
            "pointsFidelite", "actif", "dateCreation", "dateDerniereVisite"
    );

    @Override
    public Page<Client> findByCriteria(ClientSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Client> query = cb.createQuery(Client.class);
        Root<Client> root = query.from(Client.class);

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
                query.orderBy(cb.desc(root.get("id")));
            }
        } else {
            query.orderBy(cb.desc(root.get("id")));
        }

        List<Client> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Client> countRoot = countQuery.from(Client.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Client> root, ClientSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId));
        }

        if (criteria == null) {
            return predicates;
        }

        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String pattern = "%" + criteria.getQuery().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nomComplet")), pattern),
                    cb.like(cb.lower(root.get("nom")), pattern),
                    cb.like(cb.lower(root.get("prenom")), pattern),
                    cb.like(cb.lower(root.get("telephone")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("ice")), pattern)
            ));
        }

        if (criteria.getNom() != null && !criteria.getNom().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("nom")), "%" + criteria.getNom().trim().toLowerCase() + "%"));
        }

        if (criteria.getPrenom() != null && !criteria.getPrenom().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("prenom")), "%" + criteria.getPrenom().trim().toLowerCase() + "%"));
        }

        if (criteria.getTelephone() != null && !criteria.getTelephone().trim().isEmpty()) {
            predicates.add(cb.like(root.get("telephone"), "%" + criteria.getTelephone().trim() + "%"));
        }

        if (criteria.getEmail() != null && !criteria.getEmail().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("email")), "%" + criteria.getEmail().trim().toLowerCase() + "%"));
        }

        if (criteria.getIce() != null && !criteria.getIce().trim().isEmpty()) {
            predicates.add(cb.like(root.get("ice"), "%" + criteria.getIce().trim() + "%"));
        }

        if (criteria.getCategorie() != null) {
            predicates.add(cb.equal(root.get("categorie"), criteria.getCategorie()));
        }

        if (criteria.getTarif() != null) {
            predicates.add(cb.equal(root.get("tarif"), criteria.getTarif()));
        }

        if (criteria.getCommercialId() != null) {
            predicates.add(cb.equal(root.get("commercial").get("id"), criteria.getCommercialId()));
        }

        if (criteria.getActif() != null) {
            predicates.add(cb.equal(root.get("actif"), criteria.getActif()));
        }

        if (Boolean.TRUE.equals(criteria.getDepassementCredit())) {
            predicates.add(cb.greaterThan(root.get("creditUtilise"), root.get("creditAutorise")));
        }

        return predicates;
    }
}
