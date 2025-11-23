package com.ceramique.repository;

import com.ceramique.persistent.dto.ProduitSearchCriteria;
import com.ceramique.persistent.model.Produit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProduitRepositoryImpl implements ProduitRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;



    @Override
    public Page<Produit> findByCriteria(ProduitSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Produit> query = cb.createQuery(Produit.class);
        Root<Produit> root = query.from(Produit.class);
        
        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.asc(root.get("description")));
        
        // Count query pour la pagination
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Produit> countRoot = countQuery.from(Produit.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        List<Produit> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Produit> root, 
                                           ProduitSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        

        // Nom (utiliser 'description' au lieu de 'designation')
        if (criteria.getNom() != null && !criteria.getNom().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("description")), 
                "%" + criteria.getNom().toLowerCase() + "%"));
        }
        
        // Référence
        if (criteria.getReference() != null && !criteria.getReference().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("reference")), 
                "%" + criteria.getReference().toLowerCase() + "%"));
        }
        
        // Description
        if (criteria.getDescription() != null && !criteria.getDescription().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("description")), 
                "%" + criteria.getDescription().toLowerCase() + "%"));
        }
        
        // Prix minimum (utiliser 'prixVente')
        if (criteria.getPrixMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("prixVente"), criteria.getPrixMin()));
        }
        
        // Prix maximum (utiliser 'prixVente')
        if (criteria.getPrixMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("prixVente"), criteria.getPrixMax()));
        }
        
        // Supprimer le critère 'actif' car il n'existe pas dans l'entité
        // if (criteria.getActif() != null) {
        //     predicates.add(cb.equal(root.get("actif"), criteria.getActif()));
        // }
        
        // Supprimer le critère 'categorie' car il n'existe pas dans l'entité
        // if (criteria.getCategorie() != null && !criteria.getCategorie().trim().isEmpty()) {
        //     predicates.add(cb.like(cb.lower(root.get("categorie")), 
        //         "%" + criteria.getCategorie().toLowerCase() + "%"));
        // }
        
        return predicates;
    }
}
