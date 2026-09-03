package com.gestion.service;

import com.gestion.persistent.dto.LigneRapprochementDTO;
import com.gestion.persistent.dto.RapprochementAchatDTO;
import com.gestion.persistent.enums.StatutConformiteAchat;
import com.gestion.persistent.model.*;
import com.gestion.repository.FactureAchatRepository;
import com.gestion.repository.LivraisonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class RapprochementAchatService {

    private final FactureAchatRepository factureAchatRepository;
    private final LivraisonRepository livraisonRepository;

    public RapprochementAchatService(FactureAchatRepository factureAchatRepository,
                                     LivraisonRepository livraisonRepository) {
        this.factureAchatRepository = factureAchatRepository;
        this.livraisonRepository = livraisonRepository;
    }

    /**
     * Effectue le rapprochement 3-Way d'une facture d'achat fournisseur :
     * Bon de Commande <-> Bon de Réception/Livraison <-> Facture Achat
     */
    public RapprochementAchatDTO rapprocherFactureAchat(Long factureAchatId) {
        FactureAchat facture = factureAchatRepository.findById(factureAchatId)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée"));

        RapprochementAchatDTO dto = new RapprochementAchatDTO();
        dto.setFactureAchatId(facture.getId());
        dto.setNumeroFactureAchat(facture.getNumeroFacture());
        dto.setDateFacture(facture.getDateFacture());
        dto.setMontantFactureHT(facture.getMontantHt() != null ? facture.getMontantHt() : BigDecimal.ZERO);
        dto.setMontantFactureTTC(facture.getMontantTtc() != null ? facture.getMontantTtc() : BigDecimal.ZERO);

        if (facture.getFournisseur() != null) {
            dto.setFournisseurId(facture.getFournisseur().getId());
            dto.setFournisseurNom(facture.getFournisseur().getRaisonSociale());
        }

        // Trouver les livraisons récentes de ce fournisseur
        List<Livraison> livraisons = livraisonRepository.findAll();
        Livraison livraisonConcernee = null;
        if (facture.getFournisseur() != null) {
            for (Livraison liv : livraisons) {
                if (liv.getCommande() != null && liv.getCommande().getFournisseur() != null
                        && liv.getCommande().getFournisseur().getId().equals(facture.getFournisseur().getId())) {
                    livraisonConcernee = liv;
                    break;
                }
            }
        }

        Commande commandeConcernee = (livraisonConcernee != null) ? livraisonConcernee.getCommande() : null;

        if (livraisonConcernee != null) {
            dto.setLivraisonId(livraisonConcernee.getId());
            dto.setNumeroLivraison(livraisonConcernee.getNumeroLivraison());
        }

        if (commandeConcernee != null) {
            dto.setCommandeFournisseurId(commandeConcernee.getId());
            dto.setNumeroCommandeFournisseur(commandeConcernee.getNumeroCommande());
            dto.setMontantCommandeHT(commandeConcernee.getMontantTotal() != null ? commandeConcernee.getMontantTotal() : BigDecimal.ZERO);
        }

        List<LigneRapprochementDTO> lignesDTO = new ArrayList<>();
        boolean litigeDetecte = false;
        StringBuilder motifs = new StringBuilder();
        BigDecimal ecartFinancierTotal = BigDecimal.ZERO;

        if (facture.getLignes() != null) {
            for (LigneFactureAchat lf : facture.getLignes()) {
                LigneRapprochementDTO ldto = new LigneRapprochementDTO();
                if (lf.getProduit() != null) {
                    ldto.setProduitId(lf.getProduit().getId());
                    ldto.setProduitReference(lf.getProduit().getReference());
                    ldto.setProduitNom(lf.getProduit().getDesignation());
                }

                BigDecimal qteFacturee = lf.getQuantite() != null ? lf.getQuantite() : BigDecimal.ZERO;
                BigDecimal prixFacture = lf.getPrixUnitaireHt() != null ? lf.getPrixUnitaireHt() : BigDecimal.ZERO;
                ldto.setQuantiteFacturee(qteFacturee);
                ldto.setPrixUnitaireFacture(prixFacture);

                // Chercher quantité livrée correspondante
                BigDecimal qteLivree = qteFacturee; // Défaut conforme si non trouvé
                if (livraisonConcernee != null && livraisonConcernee.getLignesLivraison() != null && lf.getProduit() != null) {
                    for (LigneLivraison ll : livraisonConcernee.getLignesLivraison()) {
                        if (ll.getProduit() != null && ll.getProduit().getId().equals(lf.getProduit().getId())) {
                            qteLivree = ll.getQuantiteLivree() != null ? BigDecimal.valueOf(ll.getQuantiteLivree()) : BigDecimal.ZERO;
                            break;
                        }
                    }
                }
                ldto.setQuantiteLivree(qteLivree);

                // Chercher quantité et prix commandés correspondants
                BigDecimal qteCommandee = qteFacturee;
                BigDecimal prixCommande = prixFacture;
                if (commandeConcernee != null && commandeConcernee.getLignesCommande() != null && lf.getProduit() != null) {
                    for (LigneCommande lc : commandeConcernee.getLignesCommande()) {
                        if (lc.getProduit() != null && lc.getProduit().getId().equals(lf.getProduit().getId())) {
                            qteCommandee = lc.getQuantiteCommandee() != null ? BigDecimal.valueOf(lc.getQuantiteCommandee()) : BigDecimal.ZERO;
                            prixCommande = lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO;
                            break;
                        }
                    }
                }
                ldto.setQuantiteCommandee(qteCommandee);
                ldto.setPrixUnitaireCommande(prixCommande);

                // Calculs des écarts
                BigDecimal ecartQte = qteFacturee.subtract(qteLivree);
                BigDecimal ecartPrix = prixFacture.subtract(prixCommande);
                ldto.setEcartQuantite(ecartQte);
                ldto.setEcartPrixUnitaire(ecartPrix);

                BigDecimal montantAttendu = qteLivree.multiply(prixCommande);
                BigDecimal montantFactureLigne = qteFacturee.multiply(prixFacture);
                BigDecimal ecartLigne = montantFactureLigne.subtract(montantAttendu);
                ldto.setEcartFinancierTotal(ecartLigne);
                ecartFinancierTotal = ecartFinancierTotal.add(ecartLigne);

                // Qualification du statut de la ligne
                boolean qteSup = ecartQte.compareTo(BigDecimal.ZERO) > 0;
                boolean prixSup = ecartPrix.compareTo(BigDecimal.ZERO) > 0;

                if (qteSup && prixSup) {
                    ldto.setStatutLigne(StatutConformiteAchat.LITIGE_MAJEUR);
                    litigeDetecte = true;
                    motifs.append("Litige majeur sur ").append(ldto.getProduitNom()).append(" ; ");
                } else if (qteSup) {
                    ldto.setStatutLigne(StatutConformiteAchat.SURFACTURATION_QUANTITE);
                    litigeDetecte = true;
                    motifs.append("Surfacturation de ").append(ecartQte).append(" unités sur ").append(ldto.getProduitNom()).append(" ; ");
                } else if (prixSup) {
                    ldto.setStatutLigne(StatutConformiteAchat.PRIX_SUPERIEUR_COMMANDE);
                    litigeDetecte = true;
                    motifs.append("Surcoût unitaire de ").append(ecartPrix).append(" DA sur ").append(ldto.getProduitNom()).append(" ; ");
                } else if (qteLivree.compareTo(qteCommandee) < 0) {
                    ldto.setStatutLigne(StatutConformiteAchat.LIVRAISON_PARTIELLE);
                } else {
                    ldto.setStatutLigne(StatutConformiteAchat.CONFORME);
                }

                lignesDTO.add(ldto);
            }
        }

        dto.setLignes(lignesDTO);
        dto.setEcartFinancierGlobal(ecartFinancierTotal);

        if (litigeDetecte) {
            dto.setStatutGlobal(StatutConformiteAchat.LITIGE_MAJEUR);
            dto.setBloquerPaiement(true);
            dto.setMotifBlocage(motifs.toString());
        } else {
            dto.setStatutGlobal(StatutConformiteAchat.CONFORME);
            dto.setBloquerPaiement(false);
            dto.setMotifBlocage("Facture conforme aux livraisons et commandes.");
        }

        return dto;
    }

    /**
     * Retourne la liste des rapprochements présentant des litiges bloquants
     */
    public List<RapprochementAchatDTO> getRapprochementsLitigieux() {
        List<FactureAchat> factures = factureAchatRepository.findAll();
        List<RapprochementAchatDTO> litiges = new ArrayList<>();

        for (FactureAchat f : factures) {
            try {
                RapprochementAchatDTO r = rapprocherFactureAchat(f.getId());
                if (Boolean.TRUE.equals(r.getBloquerPaiement())) {
                    litiges.add(r);
                }
            } catch (Exception ignored) {}
        }

        return litiges;
    }
}
