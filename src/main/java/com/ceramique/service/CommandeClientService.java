package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.ceramique.mapper.CommandeClientMapper;
import com.ceramique.persistent.dto.CommandeClientDTO;
import com.ceramique.persistent.dto.LigneCommandeClientDTO;
import com.ceramique.persistent.enums.StatutCommandeClient;
import com.ceramique.persistent.model.CommandeClient;
import com.ceramique.persistent.model.LigneCommandeClient;
import com.ceramique.persistent.model.Produit;
import com.ceramique.repository.CommandeClientRepository;
import com.ceramique.repository.LigneCommandeClientRepository;
import com.ceramique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommandeClientService {

    private final CommandeClientRepository commandeClientRepository;
    private final LigneCommandeClientRepository ligneCommandeClientRepository;
    private final ProduitRepository produitRepository;
    private final CommandeClientMapper commandeClientMapper;

    public CommandeClientService(CommandeClientRepository commandeClientRepository,
                                LigneCommandeClientRepository ligneCommandeClientRepository,
                                ProduitRepository produitRepository,
                                CommandeClientMapper commandeClientMapper) {
        this.commandeClientRepository = commandeClientRepository;
        this.ligneCommandeClientRepository = ligneCommandeClientRepository;
        this.produitRepository = produitRepository;
        this.commandeClientMapper = commandeClientMapper;
    }

    @Transactional
    public CommandeClient createCommandeClient(CommandeClientDTO commandeDTO) {

        CommandeClient commande = new CommandeClient();
        commande.setNumeroCommande(generateNumeroCommandeClient());
        commande.setClientNom(commandeDTO.getClientNom());
        commande.setClientTelephone(commandeDTO.getClientTelephone());
        commande.setClientEmail(commandeDTO.getClientEmail());
        commande.setAdresseLivraison(commandeDTO.getAdresseLivraison());
        commande.setStatut(StatutCommandeClient.BROUILLON);
        commande.setDateCommande(LocalDateTime.now());
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
        return commandeClientRepository.findAll();
    }

    public CommandeClient getCommandeClientById(Long commandeId) {
        return getCommandeClientEntityById(commandeId);
    }

    @Transactional
    public CommandeClient updateStatut(Long commandeId, StatutCommandeClient nouveauStatut) {
        CommandeClient commande = getCommandeClientEntityById(commandeId);
        commande.setStatut(nouveauStatut);
        return commandeClientRepository.save(commande);
    }

    public List<CommandeClient> getCommandesByStatut(StatutCommandeClient statut) {
        return commandeClientRepository.findByStatut(statut);
    }

    private CommandeClient getCommandeClientEntityById(Long commandeId) {
        return commandeClientRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("CommandeClient", "id", commandeId));
    }

    private String generateNumeroCommandeClient() {
        return "VC-" + System.currentTimeMillis();
    }
}