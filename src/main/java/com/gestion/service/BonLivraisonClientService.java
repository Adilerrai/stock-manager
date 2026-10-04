package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.TenantContext;
import org.springframework.http.HttpStatus;
import com.gestion.mapper.BonLivraisonClientMapper;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.BonLivraisonClientSearchCriteria;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.TypeMouvement;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.LigneBonLivraisonClient;
import com.gestion.persistent.model.LigneCommandeClient;
import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.CommandeClientRepository;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.DepotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BonLivraisonClientService {

    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final ClientRepository clientRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final ProduitRepository produitRepository;
    private final DepotRepository depotRepository;
    private final MouvementStockService mouvementStockService;
    private final BonLivraisonClientMapper bonLivraisonClientMapper;
    private final CodificationService codificationService;
    private final com.gestion.repository.StockRepository stockRepository;
    private final AuditService auditService;

    public BonLivraisonClientService(BonLivraisonClientRepository bonLivraisonClientRepository,
                                     ClientRepository clientRepository,
                                     CommandeClientRepository commandeClientRepository,
                                     ProduitRepository produitRepository,
                                     DepotRepository depotRepository,
                                     MouvementStockService mouvementStockService,
                                     BonLivraisonClientMapper bonLivraisonClientMapper,
                                     CodificationService codificationService,
                                     com.gestion.repository.StockRepository stockRepository,
                                     AuditService auditService) {
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.clientRepository = clientRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
        this.mouvementStockService = mouvementStockService;
        this.bonLivraisonClientMapper = bonLivraisonClientMapper;
        this.codificationService = codificationService;
        this.stockRepository = stockRepository;
        this.auditService = auditService;
    }

    public Page<BonLivraisonClient> searchBonsLivraison(BonLivraisonClientSearchCriteria criteria, Pageable pageable) {
        return bonLivraisonClientRepository.findByCriteria(criteria, pageable);
    }

    public BonLivraisonClientDTO creerBonLivraisonClient(BonLivraisonClientDTO dto) {
        BonLivraisonClient bl = bonLivraisonClientMapper.toEntity(dto);
        if (bl.getLignes() == null) {
            bl.setLignes(new java.util.ArrayList<>());
        }
        Long tenantId = TenantContext.getCurrentTenant();
        bl.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        bl.setDateBl(LocalDateTime.now());
        bl.setNumeroBl(genererNumeroBL());
        bl.setStatut(StatutLivraison.EN_ATTENTE);

        // If order linked, load it first
        CommandeClient commande = null;
        if (dto.getCommandeClientId() != null) {
            commande = commandeClientRepository.findById(dto.getCommandeClientId()).orElse(null);
            if (commande != null) {
                if (commande.getStatut() == com.gestion.persistent.enums.StatutCommandeClient.BROUILLON) {
                    String numCmd = commande.getNumeroCommande() != null ? commande.getNumeroCommande() : ("#" + commande.getId());
                    throw new CommonException("Impossible de créer un bon de livraison : la commande client " + numCmd + 
                            " est au statut BROUILLON. Veuillez d'abord la confirmer / valider.", HttpStatus.BAD_REQUEST);
                }
                bl.setCommandeClient(commande);
            }
        }

        // Load and validate client
        Long clientId = dto.getClientId();
        if (clientId == null && bl.getClient() != null) {
            clientId = bl.getClient().getId();
        }
        if (clientId == null && commande != null && commande.getClient() != null) {
            clientId = commande.getClient().getId();
        }
        if (clientId == null) {
            throw new IllegalArgumentException("Le clientId est obligatoire pour créer un bon de livraison");
        }
        final Long targetClientId = clientId;
        Client client = clientRepository.findById(targetClientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + targetClientId));
        bl.setClient(client);

        // Report des remises globales
        if (dto.getRemiseGlobalePourcentage() != null) {
            bl.setRemiseGlobalePourcentage(dto.getRemiseGlobalePourcentage());
        } else if (commande != null && commande.getRemiseGlobalePourcentage() != null) {
            bl.setRemiseGlobalePourcentage(commande.getRemiseGlobalePourcentage());
        }

        if (dto.getRemiseGlobaleMontant() != null) {
            bl.setRemiseGlobaleMontant(dto.getRemiseGlobaleMontant());
        } else if (commande != null && commande.getRemiseGlobaleMontant() != null) {
            bl.setRemiseGlobaleMontant(commande.getRemiseGlobaleMontant());
        }

        if (dto.getLignes() != null) {
            for (var ligneDto : dto.getLignes()) {
                LigneBonLivraisonClient ligne = new LigneBonLivraisonClient();
                ligne.setBonLivraisonClient(bl);

                // Validate product
                if (ligneDto.getProduitId() == null) {
                    throw new CommonException("L'identifiant du produit est obligatoire pour chaque ligne", HttpStatus.BAD_REQUEST);
                }
                ligne.setProduit(produitRepository.findById(ligneDto.getProduitId())
                        .orElseThrow(() -> new CommonException("Produit non trouvé avec l'id: " + ligneDto.getProduitId(), HttpStatus.NOT_FOUND)));

                // Resolve depot safely (optional)
                if (ligneDto.getDepotId() != null) {
                    depotRepository.findById(ligneDto.getDepotId()).ifPresent(ligne::setDepot);
                }
                if (ligne.getDepot() == null && tenantId != null) {
                    depotRepository.findByPointDeVenteIdAndActifTrue(tenantId).stream().findFirst().ifPresent(ligne::setDepot);
                }

                ligne.setQuantiteLivree(ligneDto.getQuantiteLivree() != null ? ligneDto.getQuantiteLivree() : BigDecimal.ONE);

                BigDecimal puBrut = ligneDto.getPrixVenteBrut() != null ? ligneDto.getPrixVenteBrut() : 
                        (ligneDto.getPrixVente() != null ? ligneDto.getPrixVente() : BigDecimal.ZERO);
                BigDecimal remisePct = ligneDto.getRemisePourcentage() != null ? ligneDto.getRemisePourcentage() : BigDecimal.ZERO;
                BigDecimal remiseMt = ligneDto.getRemiseMontant() != null ? ligneDto.getRemiseMontant() : BigDecimal.ZERO;

                // Si issu d'une commande et pas précisé, chercher sur la ligne de commande correspondante
                if (commande != null && commande.getLignesCommande() != null && remisePct.compareTo(BigDecimal.ZERO) == 0 && remiseMt.compareTo(BigDecimal.ZERO) == 0) {
                    for (LigneCommandeClient lcmd : commande.getLignesCommande()) {
                        if (lcmd.getProduit() != null && lcmd.getProduit().getId().equals(ligneDto.getProduitId())) {
                            if (lcmd.getRemisePourcentage() != null && lcmd.getRemisePourcentage().compareTo(BigDecimal.ZERO) > 0) {
                                remisePct = lcmd.getRemisePourcentage();
                            }
                            if (lcmd.getRemiseMontant() != null && lcmd.getRemiseMontant().compareTo(BigDecimal.ZERO) > 0) {
                                remiseMt = lcmd.getRemiseMontant();
                            }
                            if (puBrut.compareTo(BigDecimal.ZERO) == 0 && lcmd.getPrixUnitaire() != null) {
                                puBrut = lcmd.getPrixUnitaire();
                            }
                            break;
                        }
                    }
                }

                ligne.setPrixVenteBrut(puBrut);
                ligne.setRemisePourcentage(remisePct);
                ligne.setRemiseMontant(remiseMt);
                ligne.calculerMontantLigne();

                // RÈGLE : Bloquer si le prix de vente net est inférieur au prix de vente minimum autorisé
                Produit produit = ligne.getProduit();
                if (produit != null && produit.getPrixVenteMin() != null && produit.getPrixVenteMin().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal puNet = ligne.getPrixVente() != null ? ligne.getPrixVente() : BigDecimal.ZERO;
                    if (puNet.compareTo(produit.getPrixVenteMin()) < 0) {
                        String nomArticle = produit.getDesignation() != null ? produit.getDesignation() : (produit.getNom() != null ? produit.getNom() : ("#" + produit.getId()));
                        throw new CommonException("Impossible de créer le bon de livraison : le prix de vente net (" + puNet + " MAD) pour l'article '" +
                                nomArticle + "' est inférieur au prix de vente minimum autorisé (" + produit.getPrixVenteMin() + " MAD).", HttpStatus.BAD_REQUEST);
                    }
                }

                bl.getLignes().add(ligne);
            }
        }

        bl.recalculerMontantTotal();
        BonLivraisonClient saved = bonLivraisonClientRepository.save(bl);
        return bonLivraisonClientMapper.toDto(saved);
    }

    public BonLivraisonClientDTO validerEtExpedierBL(Long blId) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;

        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(blId, effectiveTenantId)
                .or(() -> bonLivraisonClientRepository.findById(blId))
                .orElseThrow(() -> new CommonException("Bon de livraison non trouvé avec l'id: " + blId, HttpStatus.NOT_FOUND));

        if (bl.getStatut() == StatutLivraison.LIVREE) {
            throw new CommonException("Ce bon de livraison est déjà validé et expédié", HttpStatus.BAD_REQUEST);
        }

        if (bl.getLignes() == null || bl.getLignes().isEmpty()) {
            throw new CommonException("Le bon de livraison ne contient aucune ligne à expédier", HttpStatus.BAD_REQUEST);
        }

        boolean isFromOrder = bl.getCommandeClient() != null;

        // Subtract stock for each line item
        for (LigneBonLivraisonClient ligne : bl.getLignes()) {
            if (ligne.getProduit() == null) {
                throw new CommonException("Une ligne de livraison ne comporte aucun produit associé", HttpStatus.BAD_REQUEST);
            }

            // RÈGLE : Bloquer validation si le prix de vente est inférieur au prix minimum autorisé
            Produit produit = ligne.getProduit();
            if (produit.getPrixVenteMin() != null && produit.getPrixVenteMin().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pu = ligne.getPrixVente() != null ? ligne.getPrixVente() : BigDecimal.ZERO;
                if (pu.compareTo(produit.getPrixVenteMin()) < 0) {
                    String nomArticle = produit.getDesignation() != null ? produit.getDesignation() : (produit.getNom() != null ? produit.getNom() : ("#" + produit.getId()));
                    throw new CommonException("Impossible d'expédier le bon de livraison : le prix de vente (" + pu + " MAD) pour l'article '" +
                            nomArticle + "' est inférieur au prix de vente minimum autorisé (" + produit.getPrixVenteMin() + " MAD).", HttpStatus.BAD_REQUEST);
                }
            }

            try {
                mouvementStockService.creerMouvement(
                        ligne.getProduit().getId(),
                        ligne.getDepot() != null ? ligne.getDepot().getId() : null,
                        TypeMouvement.SORTIE_VENTE,
                        ligne.getQuantiteLivree() != null ? ligne.getQuantiteLivree() : BigDecimal.ONE,
                        ligne.getLot() != null && ligne.getLot().getQualite() != null ? 
                                ligne.getLot().getQualite() : QualiteProduit.PREMIERE_QUALITE,
                        bl.getNumeroBl(),
                        "Expédition BL client " + bl.getNumeroBl(),
                        isFromOrder
                );
            } catch (Exception ex) {
                String nomProd = ligne.getProduit().getNom() != null ? ligne.getProduit().getNom() : String.valueOf(ligne.getProduit().getId());
                throw new CommonException("Impossible de déstocker le produit '" + nomProd + "' : " + 
                        (ex.getMessage() != null ? ex.getMessage() : "erreur lors du déstockage"), HttpStatus.BAD_REQUEST);
            }
        }

        bl.setStatut(StatutLivraison.LIVREE);

        // Update linked order status and reliquats if any
        if (bl.getCommandeClient() != null) {
            CommandeClient commande = bl.getCommandeClient();
            if (commande.getLignesCommande() != null) {
                for (LigneBonLivraisonClient ligneBl : bl.getLignes()) {
                    if (ligneBl.getProduit() == null) continue;
                    for (LigneCommandeClient ligneCmd : commande.getLignesCommande()) {
                        if (ligneCmd.getProduit() != null && ligneCmd.getProduit().getId().equals(ligneBl.getProduit().getId())) {
                            BigDecimal dejaLivre = ligneCmd.getQuantiteLivree() != null ? ligneCmd.getQuantiteLivree() : BigDecimal.ZERO;
                            BigDecimal qteBl = ligneBl.getQuantiteLivree() != null ? ligneBl.getQuantiteLivree() : BigDecimal.ZERO;
                            ligneCmd.setQuantiteLivree(dejaLivre.add(qteBl));
                            ligneCmd.setQuantiteReliquat(ligneCmd.calculerReliquat());
                            break;
                        }
                    }
                }

                // Déterminer si tout est livré ou livraison partielle
                boolean toutLivre = true;
                boolean auMoinsUneLivraison = false;
                for (LigneCommandeClient lc : commande.getLignesCommande()) {
                    if (Boolean.TRUE.equals(lc.getAnnulee())) continue;
                    BigDecimal rel = lc.getQuantiteReliquat() != null ? lc.getQuantiteReliquat() : lc.calculerReliquat();
                    if (rel.compareTo(BigDecimal.ZERO) > 0) {
                        toutLivre = false;
                    }
                    if (lc.getQuantiteLivree() != null && lc.getQuantiteLivree().compareTo(BigDecimal.ZERO) > 0) {
                        auMoinsUneLivraison = true;
                    }
                }

                if (toutLivre) {
                    bl.setStatut(StatutLivraison.LIVREE);
                    commande.setStatut(StatutCommandeClient.LIVREE);
                } else if (auMoinsUneLivraison) {
                    bl.setStatut(StatutLivraison.PARTIELLE);
                    commande.setStatut(StatutCommandeClient.LIVREE_PARTIELLE);
                }
            } else {
                commande.setStatut(StatutCommandeClient.LIVREE);
            }
            commandeClientRepository.save(commande);
        }

        BonLivraisonClient saved = bonLivraisonClientRepository.save(bl);
        return bonLivraisonClientMapper.toDto(saved);
    }

    public BonLivraisonClientDTO annulerBonLivraisonClient(Long blId) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;

        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(blId, effectiveTenantId)
                .or(() -> bonLivraisonClientRepository.findById(blId))
                .orElseThrow(() -> new CommonException("Bon de livraison non trouvé avec l'id: " + blId, HttpStatus.NOT_FOUND));

        if (bl.getStatut() == StatutLivraison.ANNULEE) {
            throw new CommonException("Ce bon de livraison est déjà annulé", HttpStatus.BAD_REQUEST);
        }

        if (bl.getFacture() != null || Boolean.TRUE.equals(bl.isFacture())) {
            String numFacture = bl.getFacture() != null && bl.getFacture().getNumeroFacture() != null
                    ? " (Facture N° " + bl.getFacture().getNumeroFacture() + ")"
                    : "";
            throw new CommonException("Impossible d'annuler un bon de livraison déjà facturé" + numFacture + ". Vous devez d'abord annuler la facture correspondante.", HttpStatus.BAD_REQUEST);
        }

        // Si le bon était déjà expédié / validé, on réintègre la marchandise dans le stock
        if (bl.getStatut() == StatutLivraison.LIVREE) {
            for (LigneBonLivraisonClient ligne : bl.getLignes()) {
                if (ligne.getProduit() != null) {
                    try {
                        mouvementStockService.creerMouvement(
                                ligne.getProduit().getId(),
                                ligne.getDepot() != null ? ligne.getDepot().getId() : null,
                                TypeMouvement.ENTREE_LIVRAISON,
                                ligne.getQuantiteLivree() != null ? ligne.getQuantiteLivree() : BigDecimal.ONE,
                                ligne.getLot() != null && ligne.getLot().getQualite() != null ? 
                                        ligne.getLot().getQualite() : QualiteProduit.PREMIERE_QUALITE,
                                bl.getNumeroBl(),
                                "Réintégration stock suite annulation BL " + bl.getNumeroBl()
                        );
                    } catch (Exception ex) {
                        String nomProd = ligne.getProduit().getNom() != null ? ligne.getProduit().getNom() : String.valueOf(ligne.getProduit().getId());
                        throw new CommonException("Impossible de réintégrer le stock pour le produit '" + nomProd + "' : " + 
                                (ex.getMessage() != null ? ex.getMessage() : "erreur inconnue"), HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // Si lié à une commande client, on repasse la commande en CONFIRMEE
            if (bl.getCommandeClient() != null) {
                CommandeClient commande = bl.getCommandeClient();
                if (commande.getStatut() == StatutCommandeClient.LIVREE) {
                    commande.setStatut(StatutCommandeClient.CONFIRMEE);
                    commandeClientRepository.save(commande);
                }
            }
        }

        bl.setStatut(StatutLivraison.ANNULEE);
        BonLivraisonClient saved = bonLivraisonClientRepository.save(bl);
        return bonLivraisonClientMapper.toDto(saved);
    }


    public BonLivraisonClientDTO remettreEnBrouillon(Long blId) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;

        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(blId, effectiveTenantId)
                .or(() -> bonLivraisonClientRepository.findById(blId))
                .orElseThrow(() -> new CommonException("Bon de livraison non trouvé avec l'id: " + blId, HttpStatus.NOT_FOUND));

        if (bl.getFacture() != null || Boolean.TRUE.equals(bl.isFacture())) {
            String numFacture = bl.getFacture() != null && bl.getFacture().getNumeroFacture() != null
                    ? " (Facture N° " + bl.getFacture().getNumeroFacture() + ")"
                    : "";
            throw new CommonException("Impossible de remettre en brouillon un bon de livraison déjà facturé" + numFacture + ". Vous devez d'abord annuler la facture correspondante.", HttpStatus.BAD_REQUEST);
        }

        // Si le bon était validé/expédié (LIVREE ou PARTIELLE), réintégrer le stock
        if (bl.getStatut() == StatutLivraison.LIVREE || bl.getStatut() == StatutLivraison.PARTIELLE) {
            for (LigneBonLivraisonClient ligne : bl.getLignes()) {
                if (ligne.getProduit() != null) {
                    try {
                        mouvementStockService.creerMouvement(
                                ligne.getProduit().getId(),
                                ligne.getDepot() != null ? ligne.getDepot().getId() : null,
                                TypeMouvement.ENTREE_LIVRAISON,
                                ligne.getQuantiteLivree() != null ? ligne.getQuantiteLivree() : BigDecimal.ONE,
                                ligne.getLot() != null && ligne.getLot().getQualite() != null ? 
                                        ligne.getLot().getQualite() : QualiteProduit.PREMIERE_QUALITE,
                                bl.getNumeroBl(),
                                "Réintégration stock suite remise en brouillon BL " + bl.getNumeroBl()
                        );
                    } catch (Exception ex) {
                        String nomProd = ligne.getProduit().getNom() != null ? ligne.getProduit().getNom() : String.valueOf(ligne.getProduit().getId());
                        throw new CommonException("Impossible de réintégrer le stock pour le produit '" + nomProd + "' : " + 
                                (ex.getMessage() != null ? ex.getMessage() : "erreur inconnue"), HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // Réajuster les quantités livrées sur la commande client liée
            if (bl.getCommandeClient() != null) {
                CommandeClient commande = bl.getCommandeClient();
                if (commande.getLignesCommande() != null) {
                    for (LigneBonLivraisonClient ligneBl : bl.getLignes()) {
                        if (ligneBl.getProduit() == null) continue;
                        for (LigneCommandeClient ligneCmd : commande.getLignesCommande()) {
                            if (ligneCmd.getProduit() != null && ligneCmd.getProduit().getId().equals(ligneBl.getProduit().getId())) {
                                BigDecimal dejaLivre = ligneCmd.getQuantiteLivree() != null ? ligneCmd.getQuantiteLivree() : BigDecimal.ZERO;
                                BigDecimal qteBl = ligneBl.getQuantiteLivree() != null ? ligneBl.getQuantiteLivree() : BigDecimal.ZERO;
                                BigDecimal newQte = dejaLivre.subtract(qteBl);
                                if (newQte.compareTo(BigDecimal.ZERO) < 0) newQte = BigDecimal.ZERO;
                                ligneCmd.setQuantiteLivree(newQte);
                                ligneCmd.setQuantiteReliquat(ligneCmd.calculerReliquat());
                                break;
                            }
                        }
                    }

                    boolean auMoinsUneLivraison = false;
                    for (LigneCommandeClient lc : commande.getLignesCommande()) {
                        if (Boolean.TRUE.equals(lc.getAnnulee())) continue;
                        if (lc.getQuantiteLivree() != null && lc.getQuantiteLivree().compareTo(BigDecimal.ZERO) > 0) {
                            auMoinsUneLivraison = true;
                        }
                    }

                    if (auMoinsUneLivraison) {
                        commande.setStatut(StatutCommandeClient.LIVREE_PARTIELLE);
                    } else {
                        commande.setStatut(StatutCommandeClient.CONFIRMEE);
                    }
                    commandeClientRepository.save(commande);
                }
            }
        }

        bl.setStatut(StatutLivraison.BROUILLON);
        BonLivraisonClient saved = bonLivraisonClientRepository.save(bl);
        return bonLivraisonClientMapper.toDto(saved);
    }

    public BonLivraisonClientDTO updateBonLivraisonClient(Long id, BonLivraisonClientDTO dto) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;

        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(id, effectiveTenantId)
                .or(() -> bonLivraisonClientRepository.findById(id))
                .orElseThrow(() -> new CommonException("Bon de livraison non trouvé avec l'id: " + id, HttpStatus.NOT_FOUND));

        if (bl.getFacture() != null || Boolean.TRUE.equals(bl.isFacture())) {
            throw new CommonException("Impossible de modifier un bon de livraison déjà facturé.", HttpStatus.BAD_REQUEST);
        }

        if (bl.getStatut() == StatutLivraison.LIVREE || bl.getStatut() == StatutLivraison.PARTIELLE) {
            throw new CommonException("Ce bon de livraison est déjà expédié. Veuillez d'abord le remettre en brouillon pour le modifier.", HttpStatus.BAD_REQUEST);
        }

        if (dto.getClientId() != null && (bl.getClient() == null || !bl.getClient().getId().equals(dto.getClientId()))) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new CommonException("Client non trouvé avec l'id: " + dto.getClientId(), HttpStatus.NOT_FOUND));
            bl.setClient(client);
        }

        if (dto.getObservations() != null) {
            bl.setObservations(dto.getObservations());
        }
        if (dto.getRemiseGlobalePourcentage() != null) {
            bl.setRemiseGlobalePourcentage(dto.getRemiseGlobalePourcentage());
        }
        if (dto.getRemiseGlobaleMontant() != null) {
            bl.setRemiseGlobaleMontant(dto.getRemiseGlobaleMontant());
        }

        if (dto.getLignes() != null) {
            if (bl.getLignes() != null) {
                bl.getLignes().clear();
            } else {
                bl.setLignes(new java.util.ArrayList<>());
            }

            for (var ligneDto : dto.getLignes()) {
                LigneBonLivraisonClient ligne = new LigneBonLivraisonClient();
                ligne.setBonLivraisonClient(bl);
                if (ligneDto.getProduitId() == null) {
                    throw new CommonException("L'identifiant du produit est obligatoire pour chaque ligne", HttpStatus.BAD_REQUEST);
                }
                ligne.setProduit(produitRepository.findById(ligneDto.getProduitId())
                        .orElseThrow(() -> new CommonException("Produit non trouvé avec l'id: " + ligneDto.getProduitId(), HttpStatus.NOT_FOUND)));

                if (ligneDto.getDepotId() != null) {
                    depotRepository.findById(ligneDto.getDepotId()).ifPresent(ligne::setDepot);
                }
                if (ligne.getDepot() == null && tenantId != null) {
                    depotRepository.findByPointDeVenteIdAndActifTrue(tenantId).stream().findFirst().ifPresent(ligne::setDepot);
                }

                ligne.setQuantiteLivree(ligneDto.getQuantiteLivree() != null ? ligneDto.getQuantiteLivree() : BigDecimal.ONE);

                BigDecimal puBrut = ligneDto.getPrixVenteBrut() != null ? ligneDto.getPrixVenteBrut() :
                        (ligneDto.getPrixVente() != null ? ligneDto.getPrixVente() : BigDecimal.ZERO);
                BigDecimal remisePct = ligneDto.getRemisePourcentage() != null ? ligneDto.getRemisePourcentage() : BigDecimal.ZERO;
                BigDecimal remiseMt = ligneDto.getRemiseMontant() != null ? ligneDto.getRemiseMontant() : BigDecimal.ZERO;

                ligne.setPrixVenteBrut(puBrut);
                ligne.setRemisePourcentage(remisePct);
                ligne.setRemiseMontant(remiseMt);
                ligne.calculerMontantLigne();

                Produit produit = ligne.getProduit();
                if (produit != null && produit.getPrixVenteMin() != null && produit.getPrixVenteMin().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal puNet = ligne.getPrixVente() != null ? ligne.getPrixVente() : BigDecimal.ZERO;
                    if (puNet.compareTo(produit.getPrixVenteMin()) < 0) {
                        String nomArticle = produit.getDesignation() != null ? produit.getDesignation() : (produit.getNom() != null ? produit.getNom() : ("#" + produit.getId()));
                        throw new CommonException("Impossible d'enregistrer le bon de livraison : le prix de vente net (" + puNet + " MAD) pour l'article '" +
                                nomArticle + "' est inférieur au prix de vente minimum autorisé (" + produit.getPrixVenteMin() + " MAD).", HttpStatus.BAD_REQUEST);
                    }
                }

                bl.getLignes().add(ligne);
            }
        }

        bl.recalculerMontantTotal();
        BonLivraisonClient saved = bonLivraisonClientRepository.save(bl);
        return bonLivraisonClientMapper.toDto(saved);
    }

    public List<BonLivraisonClientDTO> getBonsLivraison() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByPointDeVenteId(tenantId != null ? tenantId : 1L);
        return bls.stream()
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList());
    }

    public BonLivraisonClientDTO getBonLivraisonById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé avec l'id: " + id));
        return bonLivraisonClientMapper.toDto(bl);
    }

    public List<BonLivraisonClientDTO> getBonsLivraisonNonFactures(Long clientId) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;
        List<BonLivraisonClient> bls;
        if (clientId != null) {
            bls = bonLivraisonClientRepository.findByClientIdAndFactureIsNullAndPointDeVenteId(clientId, tenantId);
        } else {
            bls = bonLivraisonClientRepository.findByFactureIsNullAndPointDeVenteId(tenantId);
        }
        return bls.stream()
                .filter(b -> b.getStatut() == com.gestion.persistent.enums.StatutLivraison.LIVREE)
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList());
    }

    private String genererNumeroBL() {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        String numero;
        int attempts = 0;
        do {
            numero = codificationService.genererNumero(com.gestion.persistent.enums.TypeDocumentCodification.BL_CLIENT, effectiveTenantId);
            attempts++;
        } while (bonLivraisonClientRepository.findByPointDeVenteIdAndNumeroBl(effectiveTenantId, numero).isPresent() && attempts < 1000);
        return numero;
    }
}
