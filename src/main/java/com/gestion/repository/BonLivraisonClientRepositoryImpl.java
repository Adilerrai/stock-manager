package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.BonLivraisonClientSearchCriteria;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.CommandeClient;
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
            "id", "numeroBL", "numeroBl", "dateBl", "statut", "montantTotal", "clientNom", "client"
    );

    @Override
    public Page<BonLivraisonClient> findByCriteria(BonLivraisonClientSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BonLivraisonClient> query = cb.createQuery(BonLivraisonClient.class);
        Root<BonLivraisonClient> root = query.from(BonLivraisonClient.class);

        // Fetch joins ToOne pour éviter le N+1 sur client, commandeClient, facture, livreur
        root.fetch("client", JoinType.LEFT);
        root.fetch("commandeClient", JoinType.LEFT);
        root.fetch("facture", JoinType.LEFT);
        root.fetch("livreur", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, query, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                String prop = order.getProperty();
                if (ALLOWED_SORT_PROPERTIES.contains(prop)) {
                    if ("clientNom".equals(prop) || "client".equals(prop)) {
                        orders.add(order.isAscending() ? cb.asc(root.get("client").get("nom")) : cb.desc(root.get("client").get("nom")));
                    } else {
                        String actualProp = "numeroBL".equals(prop) ? "numeroBl" : prop;
                        orders.add(order.isAscending() ? cb.asc(root.get(actualProp)) : cb.desc(root.get(actualProp)));
                    }
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
        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, CommonAbstractCriteria query, Root<BonLivraisonClient> root, BonLivraisonClientSearchCriteria criteria) {
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
            predicates.add(cb.notEqual(root.get("statut"), StatutLivraison.ANNULEE));
            return predicates;
        }

        String numBl = criteria.getNumeroBl();
        if (numBl != null && !numBl.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroBl")), "%" + numBl.trim().toLowerCase() + "%"));
        }

        String numCmd = criteria.getNumeroCommande();
        String numCmdExt = criteria.getNumeroCommandeExterne();
        if (numCmd != null && !numCmd.trim().isEmpty()) {
            String cmdPattern = "%" + numCmd.trim().toLowerCase() + "%";
            Path<CommandeClient> directCmd = root.get("commandeClient");
            Predicate directMatch = cb.and(
                    cb.isNotNull(directCmd),
                    cb.or(
                            cb.like(cb.lower(directCmd.get("numeroCommande")), cmdPattern),
                            cb.and(
                                    cb.isNotNull(directCmd.get("numeroCommandeExterne")),
                                    cb.like(cb.lower(directCmd.get("numeroCommandeExterne")), cmdPattern)
                            )
                    )
            );

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BonLivraisonClient> subRoot = subquery.from(BonLivraisonClient.class);
            Join<BonLivraisonClient, CommandeClient> cmdJoin = subRoot.join("commandes");
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(subRoot.get("id"), root.get("id")),
                    cb.or(
                            cb.like(cb.lower(cmdJoin.get("numeroCommande")), cmdPattern),
                            cb.and(
                                     cb.isNotNull(cmdJoin.get("numeroCommandeExterne")),
                                     cb.like(cb.lower(cmdJoin.get("numeroCommandeExterne")), cmdPattern)
                            )
                    )
            );

            predicates.add(cb.or(directMatch, cb.exists(subquery)));
        } else if (numCmdExt != null && !numCmdExt.trim().isEmpty()) {
            String cmdExtPattern = "%" + numCmdExt.trim().toLowerCase() + "%";
            Path<CommandeClient> directCmd = root.get("commandeClient");
            Predicate directMatch = cb.and(
                    cb.isNotNull(directCmd),
                    cb.isNotNull(directCmd.get("numeroCommandeExterne")),
                    cb.like(cb.lower(directCmd.get("numeroCommandeExterne")), cmdExtPattern)
            );

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BonLivraisonClient> subRoot = subquery.from(BonLivraisonClient.class);
            Join<BonLivraisonClient, CommandeClient> cmdJoin = subRoot.join("commandes");
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(subRoot.get("id"), root.get("id")),
                    cb.and(
                            cb.isNotNull(cmdJoin.get("numeroCommandeExterne")),
                            cb.like(cb.lower(cmdJoin.get("numeroCommandeExterne")), cmdExtPattern)
                    )
            );

            predicates.add(cb.or(directMatch, cb.exists(subquery)));
        }

        if (criteria.getCommandeClientId() != null) {
            Long targetCmdId = criteria.getCommandeClientId();
            Predicate directCmdIdMatch = cb.equal(root.get("commandeClient").get("id"), targetCmdId);

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BonLivraisonClient> subRoot = subquery.from(BonLivraisonClient.class);
            Join<BonLivraisonClient, CommandeClient> cmdJoin = subRoot.join("commandes");
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(subRoot.get("id"), root.get("id")),
                    cb.equal(cmdJoin.get("id"), targetCmdId)
            );

            predicates.add(cb.or(directCmdIdMatch, cb.exists(subquery)));
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getLivreurId() != null) {
            predicates.add(cb.equal(root.get("livreur").get("id"), criteria.getLivreurId()));
        }

        if (criteria.getClientNom() != null && !criteria.getClientNom().trim().isEmpty()) {
            String clientPattern = "%" + criteria.getClientNom().trim().toLowerCase() + "%";
            Path<Client> clientPath = root.get("client");
            predicates.add(cb.or(
                    cb.like(cb.lower(clientPath.get("nom")), clientPattern),
                    cb.like(cb.lower(clientPath.get("nomComplet")), clientPattern)
            ));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        } else if (!Boolean.TRUE.equals(criteria.getInclureAnnulees())) {
            predicates.add(cb.notEqual(root.get("statut"), StatutLivraison.ANNULEE));
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
