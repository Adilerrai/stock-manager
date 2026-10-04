package com.gestion.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.CommandeClientMapper;
import com.gestion.persistent.dto.CommandeClientDTO;
import com.gestion.persistent.dto.CommandeClientSearchCriteria;
import com.gestion.persistent.dto.LigneCommandeClientDTO;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.ActionAudit;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
    private final CodificationService codificationService;
    private final StockService stockService;
    private final AuditService auditService;

    public CommandeClientService(CommandeClientRepository commandeClientRepository,
                                LigneCommandeClientRepository ligneCommandeClientRepository,
                                ProduitRepository produitRepository,
                                ClientRepository clientRepository,
                                CommandeClientMapper commandeClientMapper,
                                BonLivraisonClientRepository bonLivraisonClientRepository,
                                CodificationService codificationService,
                                StockService stockService,
                                AuditService auditService) {
        this.commandeClientRepository = commandeClientRepository;
        this.ligneCommandeClientRepository = ligneCommandeClientRepository;
        this.produitRepository = produitRepository;
        this.clientRepository = clientRepository;
        this.commandeClientMapper = commandeClientMapper;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.codificationService = codificationService;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    public Page<CommandeClient> searchCommandesClient(CommandeClientSearchCriteria criteria, Pageable pageable) {
        return commandeClientRepository.findByCriteria(criteria, pageable);
    }

    @Transactional
    public CommandeClient createCommandeClient(CommandeClientDTO commandeDTO) {

        CommandeClient commande = new CommandeClient();
        if (commandeDTO.getNumeroCommande() != null && !commandeDTO.getNumeroCommande().isBlank()) {
            commande.setNumeroCommande(commandeDTO.getNumeroCommande().trim());
        } else {
            commande.setNumeroCommande(generateNumeroCommandeClient());
        }
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
        commande.setTauxTVA(commandeDTO.getTauxTVA() != null ? commandeDTO.getTauxTVA() : BigDecimal.valueOf(20));
        commande.setRemiseGlobalePourcentage(commandeDTO.getRemiseGlobalePourcentage());
        commande.setRemiseGlobaleMontant(commandeDTO.getRemiseGlobaleMontant());
        commande.setIsRecurrente(commandeDTO.getIsRecurrente());
        commande.setFrequenceRecurrence(commandeDTO.getFrequenceRecurrence());
        commande.setProchaineDateRecurrence(commandeDTO.getProchaineDateRecurrence());
        commande.setObservations(commandeDTO.getObservations());

        commande = commandeClientRepository.save(commande);

        // Créer les lignes de commande
        if (commandeDTO.getLignesCommande() != null) {
            for (LigneCommandeClientDTO ligneDTO : commandeDTO.getLignesCommande()) {
                LigneCommandeClient ligne = createLigneCommandeClient(commande, ligneDTO);
                commande.getLignesCommande().add(ligne);
            }
        }

        commande.recalculerMontants();
        commande = commandeClientRepository.save(commande);

        // Si la commande est créée directement au statut CONFIRMEE, réserver le stock
        if (commande.getStatut() == StatutCommandeClient.CONFIRMEE) {
            boolean toutReserve = reserverStockPourCommande(commande);
            if (!toutReserve) {
                commande.setStatut(StatutCommandeClient.BACKORDER);
                commande = commandeClientRepository.save(commande);
            }
        }

        return commande;
    }

    private LigneCommandeClient createLigneCommandeClient(CommandeClient commande, LigneCommandeClientDTO ligneDTO) {
        Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", ligneDTO.getProduitId()));

        // RÈGLE : Bloquer si le prix de vente unitaire est inférieur au prix de vente minimum autorisé
        if (produit.getPrixVenteMin() != null && produit.getPrixVenteMin().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pu = ligneDTO.getPrixUnitaire() != null ? ligneDTO.getPrixUnitaire() : BigDecimal.ZERO;
            if (pu.compareTo(produit.getPrixVenteMin()) < 0) {
                String nomArticle = produit.getDesignation() != null ? produit.getDesignation() : (produit.getNom() != null ? produit.getNom() : ("#" + produit.getId()));
                throw new CommonException("Impossible d'enregistrer la commande : le prix unitaire (" + pu + " MAD) pour l'article '" +
                        nomArticle + "' est inférieur au prix de vente minimum autorisé (" + produit.getPrixVenteMin() + " MAD).", HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal qte = ligneDTO.getQuantite() != null ? ligneDTO.getQuantite() : BigDecimal.ONE;
        LigneCommandeClient ligne = new LigneCommandeClient();
        ligne.setCommandeClient(commande);
        ligne.setProduit(produit);
        ligne.setQuantite(qte);
        ligne.setQuantiteCommandee(ligneDTO.getQuantiteCommandee() != null ? ligneDTO.getQuantiteCommandee() : qte);
        ligne.setQuantiteLivree(BigDecimal.ZERO);
        ligne.setQuantiteReliquat(ligne.getQuantiteCommandee());
        ligne.setPrixUnitaire(ligneDTO.getPrixUnitaire() != null ? ligneDTO.getPrixUnitaire() : BigDecimal.ZERO);
        ligne.setRemisePourcentage(ligneDTO.getRemisePourcentage() != null ? ligneDTO.getRemisePourcentage() : BigDecimal.ZERO);
        ligne.setRemiseMontant(ligneDTO.getRemiseMontant() != null ? ligneDTO.getRemiseMontant() : BigDecimal.ZERO);
        ligne.setObservations(ligneDTO.getObservations());
        ligne.calculerMontantLigne();

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
        StatutCommandeClient ancienStatut = commande.getStatut();

        if (nouveauStatut == StatutCommandeClient.ANNULEE) {
            if (ancienStatut == StatutCommandeClient.ANNULEE) {
                throw new CommonException("Cette commande est déjà annulée.", HttpStatus.BAD_REQUEST);
            }
            if (ancienStatut == StatutCommandeClient.FACTUREE) {
                throw new CommonException("Impossible d'annuler une commande déjà facturée.", HttpStatus.BAD_REQUEST);
            }

            // RÈGLE : Impossible d'annuler une commande liée à des bons de livraison actifs (non annulés)
            Long tenantId = getTenantId();
            List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByCommandeClientIdAndPointDeVenteId(commandeId, tenantId);
            if (bls == null) {
                bls = Collections.emptyList();
            }

            boolean hasFacturedBl = bls.stream().anyMatch(bl -> bl.getFacture() != null || Boolean.TRUE.equals(bl.isFacture()));
            if (hasFacturedBl) {
                throw new CommonException("Impossible d'annuler cette commande car un ou plusieurs bons de livraison associés sont déjà facturés. Vous devez d'abord annuler les factures.", HttpStatus.BAD_REQUEST);
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

            // Libérer le stock réservé non livré
            libererStockPourCommande(commande);
        }

        if (nouveauStatut == StatutCommandeClient.CONFIRMEE) {
            validerPrixMinPourCommande(commande, nouveauStatut);

            // Si elle n'était pas déjà confirmée, réserver le stock disponible
            if (ancienStatut != StatutCommandeClient.CONFIRMEE) {
                boolean toutReserve = reserverStockPourCommande(commande);
                if (!toutReserve) {
                    // Si rupture de stock, basculer en BACKORDER (en attente d'approvisionnement)
                    nouveauStatut = StatutCommandeClient.BACKORDER;
                }
            }
        }

        if (nouveauStatut == StatutCommandeClient.LIVREE) {
            validerPrixMinPourCommande(commande, nouveauStatut);
        }

        commande.setStatut(nouveauStatut);
        return commandeClientRepository.save(commande);
    }

    private void validerPrixMinPourCommande(CommandeClient commande, StatutCommandeClient nouveauStatut) {
        if (commande.getLignesCommande() != null) {
            for (LigneCommandeClient ligne : commande.getLignesCommande()) {
                if (Boolean.TRUE.equals(ligne.getAnnulee())) continue;
                Produit produit = ligne.getProduit();
                if (produit != null && produit.getPrixVenteMin() != null && produit.getPrixVenteMin().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pu = ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : BigDecimal.ZERO;
                    if (pu.compareTo(produit.getPrixVenteMin()) < 0) {
                        String nomArticle = produit.getDesignation() != null ? produit.getDesignation() : (produit.getNom() != null ? produit.getNom() : ("#" + produit.getId()));
                        throw new CommonException("Impossible de passer la commande au statut " + nouveauStatut + " : le prix unitaire (" +
                                pu + " MAD) pour l'article '" + nomArticle + "' est inférieur au prix de vente minimum autorisé (" +
                                produit.getPrixVenteMin() + " MAD).", HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }
    }

    private boolean reserverStockPourCommande(CommandeClient commande) {
        boolean toutReserve = true;
        if (commande.getLignesCommande() != null) {
            for (LigneCommandeClient ligne : commande.getLignesCommande()) {
                if (Boolean.TRUE.equals(ligne.getAnnulee())) continue;
                if (ligne.getProduit() != null) {
                    BigDecimal aReserver = ligne.getQuantiteReliquat() != null ? ligne.getQuantiteReliquat() : ligne.calculerReliquat();
                    if (aReserver.compareTo(BigDecimal.ZERO) > 0) {
                        boolean ok = stockService.reserverStock(ligne.getProduit().getId(), aReserver);
                        if (!ok) {
                            toutReserve = false;
                        }
                    }
                }
            }
        }
        return toutReserve;
    }

    private void libererStockPourCommande(CommandeClient commande) {
        if (commande.getLignesCommande() != null) {
            for (LigneCommandeClient ligne : commande.getLignesCommande()) {
                if (Boolean.TRUE.equals(ligne.getAnnulee())) continue;
                if (ligne.getProduit() != null) {
                    BigDecimal aLiberer = ligne.getQuantiteReliquat() != null ? ligne.getQuantiteReliquat() : ligne.calculerReliquat();
                    if (aLiberer.compareTo(BigDecimal.ZERO) > 0) {
                        stockService.libererStock(ligne.getProduit().getId(), aLiberer);
                    }
                }
            }
        }
    }

    @Transactional
    public CommandeClient annulerLigneCommande(Long commandeId, Long ligneId, String motif) {
        CommandeClient commande = getCommandeClientEntityById(commandeId);
        LigneCommandeClient ligne = ligneCommandeClientRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("LigneCommandeClient", "id", ligneId));

        if (!ligne.getCommandeClient().getId().equals(commandeId)) {
            throw new CommonException("La ligne spécifiée n'appartient pas à cette commande.", HttpStatus.BAD_REQUEST);
        }

        if (Boolean.TRUE.equals(ligne.getAnnulee())) {
            throw new CommonException("Cette ligne est déjà annulée.", HttpStatus.BAD_REQUEST);
        }

        // Si la commande avait réservé du stock, libérer la part non livrée
        if (commande.getStatut() == StatutCommandeClient.CONFIRMEE || commande.getStatut() == StatutCommandeClient.BACKORDER) {
            BigDecimal aLiberer = ligne.getQuantiteReliquat() != null ? ligne.getQuantiteReliquat() : ligne.calculerReliquat();
            if (aLiberer.compareTo(BigDecimal.ZERO) > 0 && ligne.getProduit() != null) {
                stockService.libererStock(ligne.getProduit().getId(), aLiberer);
            }
        }

        ligne.setAnnulee(true);
        ligne.setMotifAnnulation(motif);
        ligne.setQuantiteReliquat(BigDecimal.ZERO);
        ligneCommandeClientRepository.save(ligne);

        commande.recalculerMontants();

        // Si toutes les lignes sont annulées, annuler la commande
        boolean toutesAnnulees = commande.getLignesCommande().stream()
                .allMatch(l -> Boolean.TRUE.equals(l.getAnnulee()));
        if (toutesAnnulees) {
            commande.setStatut(StatutCommandeClient.ANNULEE);
        }

        return commandeClientRepository.save(commande);
    }

    @Transactional
    public CommandeClient genererProchaineCommandeRecurrente(Long commandeId) {
        CommandeClient source = getCommandeClientEntityById(commandeId);
        if (!Boolean.TRUE.equals(source.getIsRecurrente())) {
            throw new CommonException("Cette commande n'est pas configurée comme commande récurrente.", HttpStatus.BAD_REQUEST);
        }

        CommandeClientDTO dto = new CommandeClientDTO();
        dto.setClientId(source.getClient() != null ? source.getClient().getId() : null);
        dto.setClientNom(source.getClientNom());
        dto.setClientTelephone(source.getClientTelephone());
        dto.setClientEmail(source.getClientEmail());
        dto.setAdresseLivraison(source.getAdresseLivraison());
        dto.setStatut(StatutCommandeClient.BROUILLON);
        dto.setDateCommande(LocalDateTime.now());
        dto.setTauxTVA(source.getTauxTVA());
        dto.setRemiseGlobalePourcentage(source.getRemiseGlobalePourcentage());
        dto.setRemiseGlobaleMontant(source.getRemiseGlobaleMontant());
        dto.setObservations("Générée automatiquement d'après la commande récurrente " + source.getNumeroCommande());

        List<LigneCommandeClientDTO> lignesDTO = new java.util.ArrayList<>();
        if (source.getLignesCommande() != null) {
            for (LigneCommandeClient lc : source.getLignesCommande()) {
                if (Boolean.TRUE.equals(lc.getAnnulee())) continue;
                LigneCommandeClientDTO ldto = new LigneCommandeClientDTO();
                ldto.setProduitId(lc.getProduit().getId());
                ldto.setQuantite(lc.getQuantiteCommandee());
                ldto.setPrixUnitaire(lc.getPrixUnitaire());
                ldto.setRemisePourcentage(lc.getRemisePourcentage());
                ldto.setRemiseMontant(lc.getRemiseMontant());
                ldto.setObservations(lc.getObservations());
                lignesDTO.add(ldto);
            }
        }
        dto.setLignesCommande(lignesDTO);

        CommandeClient nouvelle = createCommandeClient(dto);

        // Mettre à jour la date de prochaine occurrence sur la commande source
        LocalDateTime prochaine = source.getProchaineDateRecurrence() != null ? source.getProchaineDateRecurrence() : LocalDateTime.now();
        if ("HEBDOMADAIRE".equalsIgnoreCase(source.getFrequenceRecurrence())) {
            source.setProchaineDateRecurrence(prochaine.plusWeeks(1));
        } else if ("TRIMESTRIEL".equalsIgnoreCase(source.getFrequenceRecurrence())) {
            source.setProchaineDateRecurrence(prochaine.plusMonths(3));
        } else {
            source.setProchaineDateRecurrence(prochaine.plusMonths(1));
        }
        commandeClientRepository.save(source);

        return nouvelle;
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
        return codificationService.genererNumero(com.gestion.persistent.enums.TypeDocumentCodification.COMMANDE_CLIENT);
    }
}
