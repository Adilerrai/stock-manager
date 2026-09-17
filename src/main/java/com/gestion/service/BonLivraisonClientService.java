package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.TenantContext;
import org.springframework.http.HttpStatus;
import com.gestion.mapper.BonLivraisonClientMapper;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.BonLivraisonClientSearchCriteria;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.TypeMouvement;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.LigneBonLivraisonClient;
import com.gestion.persistent.model.Client;
import com.gestion.persistent.model.CommandeClient;
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

    public BonLivraisonClientService(BonLivraisonClientRepository bonLivraisonClientRepository,
                                     ClientRepository clientRepository,
                                     CommandeClientRepository commandeClientRepository,
                                     ProduitRepository produitRepository,
                                     DepotRepository depotRepository,
                                     MouvementStockService mouvementStockService,
                                     BonLivraisonClientMapper bonLivraisonClientMapper) {
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.clientRepository = clientRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
        this.mouvementStockService = mouvementStockService;
        this.bonLivraisonClientMapper = bonLivraisonClientMapper;
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

        BigDecimal montantTotal = BigDecimal.ZERO;

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
                if (ligne.getDepot() == null) {
                    depotRepository.findAll().stream().findFirst().ifPresent(ligne::setDepot);
                }

                ligne.setQuantiteLivree(ligneDto.getQuantiteLivree() != null ? ligneDto.getQuantiteLivree() : BigDecimal.ONE);
                ligne.setPrixVente(ligneDto.getPrixVente() != null ? ligneDto.getPrixVente() : BigDecimal.ZERO);

                BigDecimal montantLigne = ligne.getPrixVente().multiply(ligne.getQuantiteLivree());
                montantTotal = montantTotal.add(montantLigne);

                bl.getLignes().add(ligne);
            }
        }

        bl.setMontantTotal(montantTotal);
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

        // Subtract stock for each line item
        for (LigneBonLivraisonClient ligne : bl.getLignes()) {
            if (ligne.getProduit() == null) {
                throw new CommonException("Une ligne de livraison ne comporte aucun produit associé", HttpStatus.BAD_REQUEST);
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
                        "Expédition BL client " + bl.getNumeroBl()
                );
            } catch (Exception ex) {
                String nomProd = ligne.getProduit().getNom() != null ? ligne.getProduit().getNom() : String.valueOf(ligne.getProduit().getId());
                throw new CommonException("Impossible de déstocker le produit '" + nomProd + "' : " + 
                        (ex.getMessage() != null ? ex.getMessage() : "erreur lors du déstockage"), HttpStatus.BAD_REQUEST);
            }
        }

        bl.setStatut(StatutLivraison.LIVREE);

        // Update linked order status if any
        if (bl.getCommandeClient() != null) {
            CommandeClient commande = bl.getCommandeClient();
            commande.setStatut(StatutCommandeClient.LIVREE);
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
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = bonLivraisonClientRepository.count() + 1;
        String candidate = "BL-CLI-" + dateStr + "-" + String.format("%04d", count);
        while (bonLivraisonClientRepository.existsByNumeroBl(candidate)) {
            count++;
            candidate = "BL-CLI-" + dateStr + "-" + String.format("%04d", count);
        }
        return candidate;
    }
}
