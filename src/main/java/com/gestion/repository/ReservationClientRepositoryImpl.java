package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ReservationClientSearchCriteria;
import com.gestion.persistent.enums.StatutReservationClient;
import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.ReservationClient;
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
public class ReservationClientRepositoryImpl implements ReservationClientRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroReservation", "dateReservation", "dateLivraisonSouhaitee", "statut", "dateCreation"
    );

    @Override
    public Page<ReservationClient> findByCriteria(ReservationClientSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReservationClient> query = cb.createQuery(ReservationClient.class);
        Root<ReservationClient> root = query.from(ReservationClient.class);
        root.fetch("client", JoinType.LEFT);
        root.fetch("creePar", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));

        if (pageable != null && pageable.getSort().isSorted()) {
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
                query.orderBy(cb.desc(root.get("dateReservation")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateReservation")));
        }

        List<ReservationClient> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ReservationClient> countRoot = countQuery.from(ReservationClient.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<ReservationClient> root, ReservationClientSearchCriteria criteria) {
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
            predicates.add(cb.notEqual(root.get("statut"), StatutReservationClient.ANNULEE));
            return predicates;
        }

        if (criteria.getNumeroReservation() != null && !criteria.getNumeroReservation().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroReservation")),
                    "%" + criteria.getNumeroReservation().trim().toLowerCase() + "%"));
        }

        if (criteria.getClientId() != null) {
            predicates.add(cb.equal(root.get("client").get("id"), criteria.getClientId()));
        }

        if (criteria.getClientNom() != null && !criteria.getClientNom().trim().isEmpty()) {
            String pattern = "%" + criteria.getClientNom().trim().toLowerCase() + "%";
            Path<Client> clientPath = root.get("client");
            predicates.add(cb.or(
                    cb.like(cb.lower(clientPath.get("nom")), pattern),
                    cb.like(cb.lower(clientPath.get("nomComplet")), pattern)
            ));
        }

        if (criteria.getClientIce() != null && !criteria.getClientIce().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("client").get("ice")),
                    "%" + criteria.getClientIce().trim().toLowerCase() + "%"));
        }

        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        } else if (!Boolean.TRUE.equals(criteria.getInclureAnnulees())) {
            predicates.add(cb.notEqual(root.get("statut"), StatutReservationClient.ANNULEE));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateReservation"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateReservation"), criteria.getDateFin()));
        }

        if (criteria.getDateLivraisonSouhaiteeDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateLivraisonSouhaitee"), criteria.getDateLivraisonSouhaiteeDebut()));
        }

        if (criteria.getDateLivraisonSouhaiteeFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateLivraisonSouhaitee"), criteria.getDateLivraisonSouhaiteeFin()));
        }

        if (criteria.getDevisGenereId() != null) {
            predicates.add(cb.equal(root.get("devisGenereId"), criteria.getDevisGenereId()));
        }

        if (criteria.getCommandeGenereeId() != null) {
            predicates.add(cb.equal(root.get("commandeGenereeId"), criteria.getCommandeGenereeId()));
        }

        return predicates;
    }
}

