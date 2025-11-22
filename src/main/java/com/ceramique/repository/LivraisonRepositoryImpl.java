package com.ceramique.repository;

import com.ceramique.persistent.dto.LivraisonSearchCriteria;
import com.ceramique.persistent.model.Livraison;
import com.ceramique.persistent.model.Commande;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class LivraisonRepositoryImpl implements LivraisonRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Livraison> findByCriteria(LivraisonSearchCriteria criteria, Long pointDeVenteId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Livraison> query = cb.createQuery(Livraison.class);
        Root<Livraison> root = query.from(Livraison.class);
        
        // Join avec commande
        Join<Livraison, Commande> commandeJoin = root.join("commande", JoinType.INNER);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Point de vente obligatoire via commande
        predicates.add(cb.equal(commandeJoin.get("pointDeVente").get("id"), pointDeVenteId));
        
        // Numéro de livraison
        if (criteria.getNumeroLivraison() != null && !criteria.getNumeroLivraison().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroLivraison")), 
                "%" + criteria.getNumeroLivraison().toLowerCase() + "%"));
        }
        
        // Numéro de suivi
        if (criteria.getNumeroSuivi() != null && !criteria.getNumeroSuivi().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroSuivi")), 
                "%" + criteria.getNumeroSuivi().toLowerCase() + "%"));
        }
        
        // Commande ID
        if (criteria.getCommandeId() != null) {
            predicates.add(cb.equal(commandeJoin.get("id"), criteria.getCommandeId()));
        }
        
        // Transporteur
        if (criteria.getTransporteur() != null && !criteria.getTransporteur().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("transporteur")), 
                "%" + criteria.getTransporteur().toLowerCase() + "%"));
        }
        
        // Statut
        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }
        
        // Date début livraison
        if (criteria.getDateDebutLivraison() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateLivraison"), criteria.getDateDebutLivraison()));
        }
        
        // Date fin livraison
        if (criteria.getDateFinLivraison() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateLivraison"), criteria.getDateFinLivraison()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("dateLivraison")));
        
        return entityManager.createQuery(query).getResultList();
    }
}