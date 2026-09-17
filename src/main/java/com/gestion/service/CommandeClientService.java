package com.gestion.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.CommandeClientMapper;
import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.dto.LigneCommandeClientDTO;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.LigneCommandeClient;
import com.gestion.persistent.model.Produit;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.ClientRepository;
import com.gestion.repository.CommandeClientRepository;
import com.gestion.repository.LigneCommandeClientRepository;
import com.gestion.repository.ProduitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommandeClientService {

    private final CommandeClientRepository commandeClientRepository;
    private final LigneCommandeClientRepository ligneCommandeClientRepository;
    private final ProduitRepository produitRepository;
    private final ClientRepository clientRepository;
    private final CommandeClientMapper commandeClientMapper;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;

    public CommandeClientService(CommandeClientRepository commandeClientRepository,
                                LigneCommandeClientRepository ligneCommandeClientRepository,
                                ProduitRepository produitRepository,
                                ClientRepository clientRepository,
                                CommandeClientMapper commandeClientMapper,
                                BonLivraisonClientRepository bonLivraisonClientRepository) {
        this.commandeClientRepository = commandeClientRepository;
        this.ligneCommandeClientRepository = ligneCommandeClientRepository;
        this.produitRepository = produitRepository;
        this.clientRepository = clientRepository;
        this.commandeClientMapper = commandeClientMapper;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional
    public CommandeClient createCommandeClient(CommandeClientDTO commandeDTO) {

        CommandeClient commande = new CommandeClient();
        commande.setNumeroCommande(generateNumeroCommandeClient());
        commande.setPointDeVenteId(getTenantId());
        if (commandeDTO.getClientId() != null) {
            clientRepository.findById(commandeDTO.getClientId()).ifPresent(commande::setClient);
        }
        commande.setClientNom(commandeDTO.getClientNom());
        commande.setClientTelephone(commandeDTO.getClientTelephone());
        commande.setClientEmail(commandeDTO.getClientEmail());
        commande.setAdresseLivraison(commandeDTO.getAdresseLivraison());
        commande.setStatut(commandeDTO.getStatut() != null ? commandeDTO.getStatut() : StatutCommandeClient.BROUILLON);
        commande.setDateCommande(commandeDTO.getDateCommande() != null ? commandeDTO.getDateCommande() : LocalDateTime.now());
        commande.setDateLivraisonPrevue(commandeDTO.getDateLivraisonPrevue());
        commande.setTauxTVA(commandeDTO.getTauxTVA());
        commande.setObservations(commandeDTO.getObservations());

        commande = commandeClientRepository.save(commande);

        // Créer les lignes de commande
        BigDecimal montantHT = BigDecimal.ZERO;
        for (LigneCommandeClientDTO ligneDTO : commandeDTO.getLignesCommande()) {
            LigneCommandeClient ligne = createLigneCommandeClient(commande, ligneDTO);
            montantHT = montantHT.add(ligne.getMontantLigne());
        }

        commande.setMontantHT(montantHT);
        commande.setMontantTTC(montantHT.multiply(BigDecimal.ONE.add(commande.getTauxTVA().divide(BigDecimal.valueOf(100)))));
        
        return commandeClientRepository.save(commande);
    }

    private LigneCommandeClient createLigneCommandeClient(CommandeClient commande, LigneCommandeClientDTO ligneDTO) {
        Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", ligneDTO.getProduitId()));

        LigneCommandeClient ligne = new LigneCommandeClient();
        ligne.setCommandeClient(commande);
        ligne.setProduit(produit);
        ligne.setQuantite(ligneDTO.getQuantite());
        ligne.setPrixUnitaire(ligneDTO.getPrixUnitaire());
        ligne.setMontantLigne(ligneDTO.getQuantite().multiply(ligneDTO.getPrixUnitaire()));
        ligne.setObservations(ligneDTO.getObservations());

        return ligneCommandeClientRepository.save(ligne);
    }

    public List<CommandeClient> getAllCommandesClient() {
        Long tenantId = getTenantId();
        return commandeClientRepository.findByPointDeVenteId(tenantId);
    }

    public CommandeClient getCommandeClientById(Long commandeId) {
        return getCommandeClientEntityById(commandeId);
    }

    @Transactional
    public CommandeClient updateStatut(Long commandeId, StatutCommandeClient nouveauStatut) {
        CommandeClient commande = getCommandeClientEntityById(commandeId);

        if (nouveauStatut == StatutCommandeClient.ANNULEE) {
            if (commande.getStatut() == StatutCommandeClient.ANNULEE) {
                throw new CommonException("Cette commande est déjà annulée.", HttpStatus.BAD_REQUEST);
            }
            if (commande.getStatut() == StatutCommandeClient.FACTUREE) {
                throw new CommonException("Impossible d'annuler une commande déjà facturée.", HttpStatus.BAD_REQUEST);
            }

            // RÈGLE : Impossible d'annuler une commande liée à des bons de livraison actifs (non annulés)
            Long tenantId = getTenantId();
            List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByCommandeClientIdAndPointDeVenteId(commandeId, tenantId);
            if (bls == null || bls.isEmpty()) {
                bls = bonLivraisonClientRepository.findAll().stream()
                        .filter(b -> b.getCommandeClient() != null && commandeId.equals(b.getCommandeClient().getId()))
                        .collect(Collectors.toList());
            }

            boolean hasActiveBl = bls.stream().anyMatch(bl -> bl.getStatut() != StatutLivraison.ANNULEE);
            if (hasActiveBl) {
                String numerosBl = bls.stream()
                        .filter(bl -> bl.getStatut() != StatutLivraison.ANNULEE)
                        .map(b -> b.getNumeroBl() != null ? b.getNumeroBl() : ("#" + b.getId()))
                        .collect(Collectors.joining(", "));
                throw new CommonException("Impossible d'annuler cette commande car elle possède un ou plusieurs bons de livraison actifs (" + 
                        numerosBl + "). Vous devez d'abord annuler ces bons de livraison.", HttpStatus.BAD_REQUEST);
            }
        }

        commande.setStatut(nouveauStatut);
        return commandeClientRepository.save(commande);
    }

    public List<CommandeClient> getCommandesByStatut(StatutCommandeClient statut) {
        Long tenantId = getTenantId();
        return commandeClientRepository.findByStatutAndPointDeVenteId(statut, tenantId);
    }

    private CommandeClient getCommandeClientEntityById(Long commandeId) {
        Long tenantId = getTenantId();
        return commandeClientRepository.findByIdAndPointDeVenteId(commandeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeClient", "id", commandeId));
    }

    private String generateNumeroCommandeClient() {
        return "VC-" + System.currentTimeMillis();
    }
}
