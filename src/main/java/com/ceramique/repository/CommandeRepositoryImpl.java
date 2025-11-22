package com.ceramique.repository;

import com.ceramique.persistent.dto.CommandeSearchCriteria;
import com.ceramique.persistent.model.Commande;
import com.ceramique.persistent.model.Fournisseur;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CommandeRepositoryImpl implements CommandeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Commande> findByCriteria(CommandeSearchCriteria criteria, Long pointDeVenteId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Commande> query = cb.createQuery(Commande.class);
        Root<Commande> root = query.from(Commande.class);
        
        // Join avec fournisseur si nécessaire
        Join<Commande, Fournisseur> fournisseurJoin = root.join("fournisseur", JoinType.LEFT);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Point de vente obligatoire
        predicates.add(cb.equal(root.get("pointDeVente").get("id"), pointDeVenteId));
        
        // Numéro de commande
        if (criteria.getNumeroCommande() != null && !criteria.getNumeroCommande().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("numeroCommande")), 
                "%" + criteria.getNumeroCommande().toLowerCase() + "%"));
        }
        
        // Fournisseur
        if (criteria.getFournisseurId() != null) {
            predicates.add(cb.equal(fournisseurJoin.get("id"), criteria.getFournisseurId()));
        }
        
        // Statut
        if (criteria.getStatut() != null) {
            predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
        }
        
        // Date début commande
        if (criteria.getDateDebutCommande() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateCommande"), criteria.getDateDebutCommande()));
        }
        
        // Date fin commande
        if (criteria.getDateFinCommande() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateCommande"), criteria.getDateFinCommande()));
        }
        
        // Date début livraison prévue
        if (criteria.getDateDebutLivraison() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateLivraisonPrevue"), criteria.getDateDebutLivraison()));
        }
        
        // Date fin livraison prévue
        if (criteria.getDateFinLivraison() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dateLivraisonPrevue"), criteria.getDateFinLivraison()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("dateCommande")));
        
        return entityManager.createQuery(query).getResultList();
    }
}