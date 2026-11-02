package com.gestion.repository;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.FournisseurSearchCriteria;
import com.gestion.persistent.enums.TypeFournisseur;
import com.gestion.persistent.model.Fournisseur;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recherche multicritère des fournisseurs (Criteria API) avec pagination et tri côté serveur.
 * Toutes les requêtes sont systématiquement restreintes au tenant courant (point de vente).
 */
@Repository
public class FournisseurRepositoryImpl implements FournisseurRepositoryCustom {

    /** Propriétés triables exposées à l'API (liste blanche : évite l'injection de tri). */
    private static final Set<String> SORTABLE_PROPERTIES = Set.of(
            "id", "raisonSociale", "adresse", "telephone", "email", "contact", "actif", "dateCreation",
            "ville", "codePostal", "pays", "delaiPaiementJours", "typeFournisseur", "devise", "ice");

    /** Alias acceptés côté front → propriété JPA réelle. */
    private static final Map<String, String> PROPERTY_ALIASES = Map.of(
            "raisonSocial", "raisonSociale",
            "nom", "raisonSociale",
            "delaiPaiement", "delaiPaiementJours");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Fournisseur> findByCriteria(FournisseurSearchCriteria criteria, Pageable pageable) {
        FournisseurSearchCriteria crit = criteria != null ? criteria : new FournisseurSearchCriteria();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        Long tenantId = crit.getSocieteId() != null ? crit.getSocieteId() : TenantContext.getCurrentTenant();

        // Requête principale (page demandée)
        CriteriaQuery<Fournisseur> query = cb.createQuery(Fournisseur.class);
        Root<Fournisseur> root = query.from(Fournisseur.class);
        query.where(buildPredicates(cb, root, crit, tenantId).toArray(new Predicate[0]));
        query.orderBy(buildOrders(cb, root, pageable));

        List<Fournisseur> resultList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Requête de comptage : les prédicats sont reconstruits sur un Root distinct
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Fournisseur> countRoot = countQuery.from(Fournisseur.class);
        countQuery.select(cb.count(countRoot))
                .where(buildPredicates(cb, countRoot, crit, tenantId).toArray(new Predicate[0]));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, totalCount);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Fournisseur> root,
                                            FournisseurSearchCriteria c, Long tenantId) {
        List<Predicate> predicates = new ArrayList<>();

        // Isolation multi-tenant obligatoire (aucun tenant => aucun résultat)
        predicates.add(cb.equal(root.get("pointDeVenteId"), tenantId != null ? tenantId : -1L));

        // Recherche globale (barre de recherche) : OU sur les colonnes textuelles principales
        if (hasText(c.getQuery())) {
            String pattern = likePattern(c.getQuery());
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("raisonSociale")), pattern),
                    cb.like(cb.lower(root.get("contact")), pattern),
                    cb.like(cb.lower(root.get("telephone")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("adresse")), pattern),
                    cb.like(cb.lower(root.get("ville")), pattern),
                    cb.like(cb.lower(root.get("ice")), pattern)));
        }

        // Critères ciblés (recherche avancée) : ET entre eux
        addLike(cb, root, predicates, "raisonSociale", c.getRaisonSociale());
        addLike(cb, root, predicates, "adresse", c.getAdresse());
        addLike(cb, root, predicates, "telephone", c.getTelephone());
        addLike(cb, root, predicates, "email", c.getEmail());
        addLike(cb, root, predicates, "contact", c.getContact());
        addLike(cb, root, predicates, "ville", c.getVille());
        addLike(cb, root, predicates, "ice", c.getIce());

        if (hasText(c.getDevise())) {
            String devise = c.getDevise().trim().toUpperCase();
            Predicate deviseEq = cb.equal(cb.upper(root.get("devise")), devise);
            // Les anciens enregistrements sans devise sont considérés en MAD (cf. Fournisseur#getDevise)
            predicates.add("MAD".equals(devise) ? cb.or(deviseEq, cb.isNull(root.get("devise"))) : deviseEq);
        }

        if (c.getTypeFournisseur() != null) {
            Predicate typeEq = cb.equal(root.get("typeFournisseur"), c.getTypeFournisseur());
            // Les anciens enregistrements sans type sont considérés NATIONAL (valeur par défaut de l'entité)
            predicates.add(c.getTypeFournisseur() == TypeFournisseur.NATIONAL
                    ? cb.or(typeEq, cb.isNull(root.get("typeFournisseur")))
                    : typeEq);
        }

        if (c.getActif() != null) {
            predicates.add(cb.equal(root.get("actif"), c.getActif()));
        }

        return predicates;
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<Fournisseur> root, Pageable pageable) {
        List<Order> orders = new ArrayList<>();
        Set<String> used = new HashSet<>();

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(sortOrder -> {
                String property = mapPropertyName(sortOrder.getProperty());
                if (property != null && SORTABLE_PROPERTIES.contains(property) && used.add(property)) {
                    Path<?> path = root.get(property);
                    if (String.class.equals(path.getJavaType())) {
                        // Tri insensible à la casse pour les colonnes texte
                        Expression<String> lowered = cb.lower(root.get(property));
                        orders.add(sortOrder.isAscending() ? cb.asc(lowered) : cb.desc(lowered));
                    } else {
                        orders.add(sortOrder.isAscending() ? cb.asc(path) : cb.desc(path));
                    }
                }
            });
        }

        if (orders.isEmpty()) {
            orders.add(cb.asc(cb.lower(root.get("raisonSociale"))));
            used.add("raisonSociale");
        }
        // Tri secondaire stable pour une pagination déterministe
        if (!used.contains("id")) {
            orders.add(cb.asc(root.get("id")));
        }
        return orders;
    }

    private static void addLike(CriteriaBuilder cb, Root<Fournisseur> root, List<Predicate> predicates,
                                String property, String value) {
        if (hasText(value)) {
            predicates.add(cb.like(cb.lower(root.get(property)), likePattern(value)));
        }
    }

    private static String likePattern(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Mappe les noms de propriétés pour gérer les variations (raisonSocial -> raisonSociale, nom -> raisonSociale).
     */
    private static String mapPropertyName(String property) {
        if (property == null) {
            return null;
        }
        return PROPERTY_ALIASES.getOrDefault(property, property);
    }
}
