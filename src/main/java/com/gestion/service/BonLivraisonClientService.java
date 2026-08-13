package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class BonLivraisonClientService {

    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final ClientRepository clientRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final ProduitRepository produitRepository;
    private final DepotRepository depotRepository;
    private final MouvementStockService mouvementStockService;

    public BonLivraisonClientService(BonLivraisonClientRepository bonLivraisonClientRepository,
                                     ClientRepository clientRepository,
                                     CommandeClientRepository commandeClientRepository,
                                     ProduitRepository produitRepository,
                                     DepotRepository depotRepository,
                                     MouvementStockService mouvementStockService) {
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.clientRepository = clientRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
        this.mouvementStockService = mouvementStockService;
    }

    public BonLivraisonClient creerBonLivraisonClient(BonLivraisonClient bl) {
        Long tenantId = TenantContext.getCurrentTenant();
        bl.setPointDeVenteId(tenantId != null ? tenantId : 1L);
        bl.setDateBl(LocalDateTime.now());
        bl.setNumeroBl(genererNumeroBL());
        bl.setStatut(StatutLivraison.EN_ATTENTE);

        // Load and validate client
        Client client = clientRepository.findById(bl.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        bl.setClient(client);

        // If order linked, load it
        if (bl.getCommandeClient() != null && bl.getCommandeClient().getId() != null) {
            CommandeClient commande = commandeClientRepository.findById(bl.getCommandeClient().getId())
                    .orElseThrow(() -> new RuntimeException("Commande client non trouvée"));
            bl.setCommandeClient(commande);
        }

        BigDecimal montantTotal = BigDecimal.ZERO;

        for (LigneBonLivraisonClient ligne : bl.getLignes()) {
            ligne.setBonLivraisonClient(bl);
            
            // Validate product
            ligne.setProduit(produitRepository.findById(ligne.getProduit().getId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé")));

            // Resolve depot
            if (ligne.getDepot() != null && ligne.getDepot().getId() != null) {
                ligne.setDepot(depotRepository.findById(ligne.getDepot().getId())
                        .orElseThrow(() -> new RuntimeException("Dépôt non trouvé")));
            }

            BigDecimal montantLigne = ligne.getPrixVente().multiply(ligne.getQuantiteLivree());
            montantTotal = montantTotal.add(montantLigne);
        }

        bl.setMontantTotal(montantTotal);

        return bonLivraisonClientRepository.save(bl);
    }

    public BonLivraisonClient validerEtExpedierBL(Long blId) {
        Long tenantId = TenantContext.getCurrentTenant();
        BonLivraisonClient bl = bonLivraisonClientRepository.findByIdAndPointDeVenteId(blId, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé"));

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

        return bonLivraisonClientRepository.save(bl);
    }

    public List<BonLivraisonClient> getBonsLivraison() {
        Long tenantId = TenantContext.getCurrentTenant();
        return bonLivraisonClientRepository.findByPointDeVenteId(tenantId != null ? tenantId : 1L);
    }

    public BonLivraisonClient getBonLivraisonById(Long id) {
        Long tenantId = TenantContext.getCurrentTenant();
        return bonLivraisonClientRepository.findByIdAndPointDeVenteId(id, tenantId != null ? tenantId : 1L)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé"));
    }

    private String genererNumeroBL() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = bonLivraisonClientRepository.count() + 1;
        return "BL-CLI-" + dateStr + "-" + String.format("%04d", count);
    }
}

