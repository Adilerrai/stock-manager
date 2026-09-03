package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.BonPreparationDTO;
import com.gestion.persistent.dto.LigneBonPreparationDTO;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.enums.StatutPreparation;
import com.gestion.persistent.model.*;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.BonPreparationRepository;
import com.gestion.repository.CommandeClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BonPreparationService {

    private final BonPreparationRepository bonPreparationRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final UserRepository userRepository;

    public BonPreparationService(BonPreparationRepository bonPreparationRepository,
                                CommandeClientRepository commandeClientRepository,
                                BonLivraisonClientRepository bonLivraisonClientRepository,
                                UserRepository userRepository) {
        this.bonPreparationRepository = bonPreparationRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.userRepository = userRepository;
    }

    /**
     * Génère un bon de préparation pour le magasinier à partir d'une commande client
     */
    public BonPreparationDTO genererDepuisCommande(Long commandeClientId, Long magasinierId) {
        CommandeClient commande = commandeClientRepository.findById(commandeClientId)
                .orElseThrow(() -> new RuntimeException("Commande client non trouvée avec l'id: " + commandeClientId));

        // Vérifier si un bon de préparation existe déjà
        Optional<BonPreparation> existant = bonPreparationRepository.findByCommandeClientId(commandeClientId);
        if (existant.isPresent()) {
            return toDto(existant.get());
        }

        User magasinier = null;
        if (magasinierId != null) {
            magasinier = userRepository.findById(magasinierId).orElse(null);
        }

        BonPreparation bp = new BonPreparation();
        bp.setNumeroPreparation(genererNumeroPreparation());
        bp.setCommandeClient(commande);
        bp.setClient(commande.getClient());
        bp.setDateCreation(LocalDateTime.now());
        bp.setStatut(StatutPreparation.A_PREPARER);
        bp.setMagasinier(magasinier);

        Long tenantId = TenantContext.getCurrentTenant();
        bp.setPointDeVenteId(tenantId != null ? tenantId : 1L);

        if (commande.getLignesCommande() != null) {
            for (LigneCommandeClient lc : commande.getLignesCommande()) {
                LigneBonPreparation lbp = new LigneBonPreparation();
                lbp.setProduit(lc.getProduit());
                lbp.setQuantiteCommandee(lc.getQuantite() != null ? lc.getQuantite() : BigDecimal.ZERO);
                lbp.setQuantitePreparee(BigDecimal.ZERO);
                lbp.setEmplacementDepot("Zone de Stockage");
                lbp.setStatutLigne("A_PREPARER");
                bp.addLigne(lbp);
            }
        }

        BonPreparation saved = bonPreparationRepository.save(bp);
        return toDto(saved);
    }

    /**
     * Le magasinier valide les colis préparés :
     * 1. Met à jour les quantités préparées réelles.
     * 2. Passe le statut à PREPARE.
     * 3. Génère automatiquement le Bon de Livraison officiel pour les quantités préparées !
     */
    public BonPreparationDTO validerPreparation(Long preparationId, Map<Long, BigDecimal> quantitesPreparees, Long magasinierId) {
        BonPreparation bp = bonPreparationRepository.findById(preparationId)
                .orElseThrow(() -> new RuntimeException("Bon de préparation non trouvé"));

        if (magasinierId != null) {
            User mag = userRepository.findById(magasinierId).orElse(null);
            if (mag != null) bp.setMagasinier(mag);
        }

        boolean toutComplet = true;
        List<LigneBonLivraisonClient> lignesBL = new ArrayList<>();
        BigDecimal montantTotalBL = BigDecimal.ZERO;

        for (LigneBonPreparation ligne : bp.getLignes()) {
            Long pId = ligne.getProduit().getId();
            BigDecimal preparee = (quantitesPreparees != null && quantitesPreparees.containsKey(pId))
                    ? quantitesPreparees.get(pId)
                    : ligne.getQuantiteCommandee();

            ligne.setQuantitePreparee(preparee != null ? preparee : BigDecimal.ZERO);

            if (ligne.getQuantitePreparee().compareTo(ligne.getQuantiteCommandee()) < 0) {
                toutComplet = false;
                ligne.setStatutLigne(ligne.getQuantitePreparee().compareTo(BigDecimal.ZERO) > 0 ? "PARTIEL" : "MANQUANT");
            } else {
                ligne.setStatutLigne("COMPLET");
            }

            // Préparation de la ligne de BL officiel si quantite > 0
            if (ligne.getQuantitePreparee().compareTo(BigDecimal.ZERO) > 0) {
                LigneBonLivraisonClient lbl = new LigneBonLivraisonClient();
                lbl.setProduit(ligne.getProduit());
                lbl.setQuantiteLivree(ligne.getQuantitePreparee());
                BigDecimal prix = ligne.getProduit().getPrixVenteTTC() != null ? ligne.getProduit().getPrixVenteTTC() : BigDecimal.ZERO;
                lbl.setPrixVente(prix);
                montantTotalBL = montantTotalBL.add(prix.multiply(ligne.getQuantitePreparee()));
                lignesBL.add(lbl);
            }
        }

        bp.setDatePreparation(LocalDateTime.now());
        bp.setStatut(StatutPreparation.PREPARE);
        BonPreparation savedBP = bonPreparationRepository.save(bp);

        // Génération automatique du Bon de Livraison officiel Client
        if (!lignesBL.isEmpty()) {
            BonLivraisonClient bl = new BonLivraisonClient();
            bl.setNumeroBl("BL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%04d", System.currentTimeMillis() % 10000));
            bl.setClient(bp.getClient());
            bl.setCommandeClient(bp.getCommandeClient());
            bl.setDateBl(LocalDateTime.now());
            bl.setStatut(StatutLivraison.EN_ATTENTE);
            bl.setMontantTotal(montantTotalBL);
            bl.setObservations("Généré automatiquement depuis le Bon de Préparation " + bp.getNumeroPreparation());
            bl.setPointDeVenteId(bp.getPointDeVenteId());

            for (LigneBonLivraisonClient l : lignesBL) {
                bl.addLigne(l);
            }
            bonLivraisonClientRepository.save(bl);
        }

        return toDto(savedBP);
    }

    @Transactional(readOnly = true)
    public List<BonPreparationDTO> getTousLesBons() {
        return bonPreparationRepository.findAll().stream()
                .sorted(Comparator.comparing(BonPreparation::getDateCreation).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BonPreparationDTO getBonById(Long id) {
        BonPreparation bp = bonPreparationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bon de préparation non trouvé"));
        return toDto(bp);
    }

    @Transactional(readOnly = true)
    public List<BonPreparationDTO> getBonsParStatut(StatutPreparation statut) {
        return bonPreparationRepository.findByStatutOrderByDateCreationDesc(statut).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BonPreparationDTO toDto(BonPreparation bp) {
        BonPreparationDTO dto = new BonPreparationDTO();
        dto.setId(bp.getId());
        dto.setNumeroPreparation(bp.getNumeroPreparation());
        if (bp.getCommandeClient() != null) {
            dto.setCommandeClientId(bp.getCommandeClient().getId());
            dto.setCommandeClientNumero(bp.getCommandeClient().getNumeroCommande());
        }
        if (bp.getClient() != null) {
            dto.setClientId(bp.getClient().getId());
            dto.setClientNom(bp.getClient().getNomComplet() != null ? bp.getClient().getNomComplet() : bp.getClient().getNom());
        }
        dto.setDateCreation(bp.getDateCreation());
        dto.setDatePreparation(bp.getDatePreparation());
        dto.setStatut(bp.getStatut());
        if (bp.getMagasinier() != null) {
            dto.setMagasinierUserId(bp.getMagasinier().getId());
            dto.setMagasinierNom(bp.getMagasinier().getNomComplet() != null ? bp.getMagasinier().getNomComplet() : bp.getMagasinier().getUsername());
        }
        dto.setNotes(bp.getNotes());

        List<LigneBonPreparationDTO> lignesDto = new ArrayList<>();
        if (bp.getLignes() != null) {
            for (LigneBonPreparation l : bp.getLignes()) {
                LigneBonPreparationDTO ldto = new LigneBonPreparationDTO();
                ldto.setId(l.getId());
                if (l.getProduit() != null) {
                    ldto.setProduitId(l.getProduit().getId());
                    ldto.setProduitReference(l.getProduit().getReference());
                    ldto.setProduitNom(l.getProduit().getDesignation());
                }
                ldto.setQuantiteCommandee(l.getQuantiteCommandee());
                ldto.setQuantitePreparee(l.getQuantitePreparee());
                ldto.setEmplacementDepot(l.getEmplacementDepot());
                ldto.setStatutLigne(l.getStatutLigne());
                lignesDto.add(ldto);
            }
        }
        dto.setLignes(lignesDto);
        return dto;
    }

    private String genererNumeroPreparation() {
        String prefixe = "BP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = bonPreparationRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}
