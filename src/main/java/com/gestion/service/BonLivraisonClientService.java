package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
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
        Long tenantId = TenantContext.getCurrentTenant();
        bl.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        bl.setDateBl(LocalDateTime.now());
        bl.setNumeroBl(genererNumeroBL());
        bl.setStatut(StatutLivraison.EN_ATTENTE);

        // Load and validate client
        Long clientId = dto.getClientId();
        if (clientId == null && bl.getClient() != null) {
            clientId = bl.getClient().getId();
        }
        if (clientId == null) {
            throw new IllegalArgumentException("Le clientId est obligatoire pour créer un bon de livraison");
        }
        final Long targetClientId = clientId;
        Client client = clientRepository.findById(targetClientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + targetClientId));
        bl.setClient(client);

        // If order linked, load it
        if (dto.getCommandeClientId() != null) {
            CommandeClient commande = commandeClientRepository.findById(dto.getCommandeClientId())
                    .orElseThrow(() -> new RuntimeException("Commande client non trouvée"));
            bl.setCommandeClient(commande);
        }

        BigDecimal montantTotal = BigDecimal.ZERO;

        if (dto.getLignes() != null) {
            for (var ligneDto : dto.getLignes()) {
                LigneBonLivraisonClient ligne = new LigneBonLivraisonClient();
                ligne.setBonLivraisonClient(bl);

                // Validate product
                if (ligneDto.getProduitId() != null) {
                    ligne.setProduit(produitRepository.findById(ligneDto.getProduitId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + ligneDto.getProduitId())));
                }

                // Resolve depot
                if (ligneDto.getDepotId() != null) {
                    ligne.setDepot(depotRepository.findById(ligneDto.getDepotId())
                            .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + ligneDto.getDepotId())));
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
        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(blId, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé avec l'id: " + blId));

        if (bl.getStatut() == StatutLivraison.LIVREE) {
            throw new IllegalStateException("Ce bon de livraison est déjà validé et expédié");
        }

        // Subtract stock for each line item
        for (LigneBonLivraisonClient ligne : bl.getLignes()) {
            mouvementStockService.creerMouvement(
                    ligne.getProduit().getId(),
                    ligne.getDepot() != null ? ligne.getDepot().getId() : null,
                    TypeMouvement.SORTIE_VENTE,
                    ligne.getQuantiteLivree(),
                    ligne.getLot() != null && ligne.getLot().getQualite() != null ? 
                            ligne.getLot().getQualite() : QualiteProduit.PREMIERE_QUALITE,
                    bl.getNumeroBl(),
                    "Expédition BL client " + bl.getNumeroBl()
            );
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
                .map(bonLivraisonClientMapper::toDto)
                .collect(Collectors.toList());
    }

    private String genererNumeroBL() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = bonLivraisonClientRepository.count() + 1;
        return "BL-CLI-" + dateStr + "-" + String.format("%04d", count);
    }
}
