package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CommandeSearchCriteria;
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
public class CommandeRepositoryImpl implements CommandeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroCommande", "dateCommande", "dateLivraisonPrevue",
            "dateLivraisonReelle", "montantTotal", "statut", "statutLivraison"
    );

    @Override
    public List<Commande> findByCriteria(CommandeSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Commande> query = cb.createQuery(Commande.class);
        Root<Commande> root = query.from(Commande.class);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("dateCommande")));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Page<Commande> findByCriteria(CommandeSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Commande> query = cb.createQuery(Commande.class);
        Root<Commande> root = query.from(Commande.class);

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
                query.orderBy(cb.desc(root.get("dateCommande")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateCommande")));
        }

        List<Commande> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Commande> countRoot = countQuery.from(Commande.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Commande> root, CommandeSearchCriteria criteria) {
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

        if (criteria.getNumeroCommande() != null && !criteria.getNumeroCommande().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroCommande")),
                    "%" + criteria.getNumeroCommande().trim().toLowerCase() + "%"));
        }

        if (criteria.getFournisseurId() != null) {
            predicates.add(cb.equal(root.get("fournisseur").get("id"), criteria.getFournisseurId()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }

        if (criteria.getDateDebutCommande() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateCommande"), criteria.getDateDebutCommande()));
        }

        if (criteria.getDateFinCommande() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateCommande"), criteria.getDateFinCommande()));
        }

        if (criteria.getDateDebutLivraison() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateLivraisonPrevue"), criteria.getDateDebutLivraison()));
        }

        if (criteria.getDateFinLivraison() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateLivraisonPrevue"), criteria.getDateFinLivraison()));
        }

        return predicates;
    }
}
