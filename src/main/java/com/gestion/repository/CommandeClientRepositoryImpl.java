package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.CommandeClientSearchCriteria;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.LigneCommandeClient;
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
public class CommandeClientRepositoryImpl implements CommandeClientRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroCommande", "numeroCommandeExterne", "dateCommande", "dateLivraisonPrevue",
            "montantHT", "montantTTC", "statut", "clientNom"
    );

    @Override
    public Page<CommandeClient> findByCriteria(CommandeClientSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CommandeClient> query = cb.createQuery(CommandeClient.class);
        Root<CommandeClient> root = query.from(CommandeClient.class);

        List<Predicate> predicates = buildPredicates(cb, query, root, criteria);
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

        List<CommandeClient> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CommandeClient> countRoot = countQuery.from(CommandeClient.class);
        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, CommonAbstractCriteria query, Root<CommandeClient> root, CommandeClientSearchCriteria criteria) {
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
            predicates.add(cb.notEqual(root.get("statut"), StatutCommandeClient.ANNULEE));
            return predicates;
        }

        if (criteria.getNumeroCommande() != null && !criteria.getNumeroCommande().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroCommande")), "%" + criteria.getNumeroCommande().trim().toLowerCase() + "%"));
        }

        if (criteria.getNumeroCommandeExterne() != null && !criteria.getNumeroCommandeExterne().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroCommandeExterne")), "%" + criteria.getNumeroCommandeExterne().trim().toLowerCase() + "%"));
        }

        if (criteria.getClientNom() != null && !criteria.getClientNom().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("clientNom")), "%" + criteria.getClientNom().trim().toLowerCase() + "%"));
        }

        if (criteria.getClientTelephone() != null && !criteria.getClientTelephone().trim().isEmpty()) {
            predicates.add(cb.like(root.get("clientTelephone"), "%" + criteria.getClientTelephone().trim() + "%"));
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        } else if (!Boolean.TRUE.equals(criteria.getInclureAnnulees())) {
            predicates.add(cb.notEqual(root.get("statut"), StatutCommandeClient.ANNULEE));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateCommande"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateCommande"), criteria.getDateFin()));
        }

        if (criteria.getMontantMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("montantTTC"), criteria.getMontantMin()));
        }

        if (criteria.getMontantMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("montantTTC"), criteria.getMontantMax()));
        }

        if (criteria.getProduitId() != null || (criteria.getProduitQuery() != null && !criteria.getProduitQuery().trim().isEmpty())) {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<LigneCommandeClient> ligneRoot = subquery.from(LigneCommandeClient.class);
            subquery.select(ligneRoot.get("commandeClient").get("id"));

            List<Predicate> subPreds = new ArrayList<>();
            subPreds.add(cb.equal(ligneRoot.get("commandeClient"), root));

            if (criteria.getProduitId() != null) {
                subPreds.add(cb.equal(ligneRoot.get("produit").get("id"), criteria.getProduitId()));
            }

            if (criteria.getProduitQuery() != null && !criteria.getProduitQuery().trim().isEmpty()) {
                String pattern = "%" + criteria.getProduitQuery().trim().toLowerCase() + "%";
                subPreds.add(cb.or(
                        cb.like(cb.lower(ligneRoot.get("produit").get("reference")), pattern),
                        cb.like(cb.lower(ligneRoot.get("produit").get("designation")), pattern)
                ));
            }

            subquery.where(subPreds.toArray(new Predicate[0]));
            predicates.add(cb.exists(subquery));
        }

        return predicates;
    }
}
