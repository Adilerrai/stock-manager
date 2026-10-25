package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ReglementFournisseurSearchCriteria;
import com.gestion.persistent.model.ReglementFournisseur;
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
public class ReglementFournisseurRepositoryImpl implements ReglementFournisseurRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "numeroReglement", "dateReglement", "montant", "modePaiement",
            "dateEcheance", "nomBanque", "numeroCheque", "referencePaiement"
    );

    @Override
    public List<ReglementFournisseur> findByCriteria(ReglementFournisseurSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReglementFournisseur> query = cb.createQuery(ReglementFournisseur.class);
        Root<ReglementFournisseur> root = query.from(ReglementFournisseur.class);

        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("dateReglement")), cb.desc(root.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Page<ReglementFournisseur> findByCriteria(ReglementFournisseurSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReglementFournisseur> query = cb.createQuery(ReglementFournisseur.class);
        Root<ReglementFournisseur> root = query.from(ReglementFournisseur.class);

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
                query.orderBy(cb.desc(root.get("dateReglement")), cb.desc(root.get("id")));
            }
        } else {
            query.orderBy(cb.desc(root.get("dateReglement")), cb.desc(root.get("id")));
        }

        List<ReglementFournisseur> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ReglementFournisseur> countRoot = countQuery.from(ReglementFournisseur.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<ReglementFournisseur> root, ReglementFournisseurSearchCriteria criteria) {
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

        if (criteria.getNumeroReglement() != null && !criteria.getNumeroReglement().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroReglement")), "%" + criteria.getNumeroReglement().trim().toLowerCase() + "%"));
        }

        if (criteria.getModePaiement() != null) {
            predicates.add(cb.equal(root.get("modePaiement"), criteria.getModePaiement()));
        }

        if (criteria.getFactureAchatId() != null) {
            predicates.add(cb.equal(root.get("factureAchat").get("id"), criteria.getFactureAchatId()));
        }

        if (criteria.getNumeroFacture() != null && !criteria.getNumeroFacture().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("factureAchat").get("numeroFacture")), "%" + criteria.getNumeroFacture().trim().toLowerCase() + "%"));
        }

        if (criteria.getFournisseurId() != null) {
            predicates.add(cb.equal(root.get("factureAchat").get("fournisseur").get("id"), criteria.getFournisseurId()));
        }

        if (criteria.getReferencePaiement() != null && !criteria.getReferencePaiement().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("referencePaiement")), "%" + criteria.getReferencePaiement().trim().toLowerCase() + "%"));
        }

        if (criteria.getNumeroCheque() != null && !criteria.getNumeroCheque().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroCheque")), "%" + criteria.getNumeroCheque().trim().toLowerCase() + "%"));
        }

        if (criteria.getNomBanque() != null && !criteria.getNomBanque().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("nomBanque")), "%" + criteria.getNomBanque().trim().toLowerCase() + "%"));
        }

        if (criteria.getDateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateReglement"), criteria.getDateDebut()));
        }

        if (criteria.getDateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateReglement"), criteria.getDateFin()));
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

        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            String pattern = "%" + criteria.getQuery().trim().toLowerCase() + "%";
            List<Predicate> orPreds = new ArrayList<>();
            orPreds.add(cb.like(cb.lower(root.get("numeroReglement")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("referencePaiement")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("numeroCheque")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("nomBanque")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("notes")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("factureAchat").get("numeroFacture")), pattern));
            orPreds.add(cb.like(cb.lower(root.get("factureAchat").get("fournisseur").get("raisonSociale")), pattern));
            predicates.add(cb.or(orPreds.toArray(new Predicate[0])));
        }

        return predicates;
    }
}
