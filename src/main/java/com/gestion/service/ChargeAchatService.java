package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.ChargeAchatDTO;
import com.gestion.persistent.enums.StatutCharge;
import com.gestion.persistent.model.ChargeAchat;
import com.gestion.persistent.model.Commande;
import com.gestion.persistent.model.FactureAchat;
import com.gestion.persistent.model.Fournisseur;
import com.gestion.persistent.model.LigneCommande;
import com.gestion.repository.ChargeAchatRepository;
import com.gestion.repository.CommandeRepository;
import com.gestion.repository.FactureAchatRepository;
import com.gestion.repository.FournisseurRepository;
import com.gestion.repository.LigneCommandeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChargeAchatService {

    private final ChargeAchatRepository chargeAchatRepository;
    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final FournisseurRepository fournisseurRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final CoutRevientService coutRevientService;

    public ChargeAchatService(ChargeAchatRepository chargeAchatRepository,
                              CommandeRepository commandeRepository,
                              LigneCommandeRepository ligneCommandeRepository,
                              FournisseurRepository fournisseurRepository,
                              FactureAchatRepository factureAchatRepository,
                              CoutRevientService coutRevientService) {
        this.chargeAchatRepository = chargeAchatRepository;
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.coutRevientService = coutRevientService;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    public List<ChargeAchatDTO> listerChargesParCommande(Long commandeId) {
        return chargeAchatRepository.findByCommandeOrderByType(commandeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ChargeAchatDTO getChargeById(Long id) {
        ChargeAchat charge = chargeAchatRepository.findByIdAndPointDeVenteId(id, getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("ChargeAchat", "id", id));
        return toDto(charge);
    }

    @Transactional
    public ChargeAchatDTO ajouterCharge(Long commandeId, ChargeAchatDTO dto) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", commandeId));

        ChargeAchat charge = new ChargeAchat();
        charge.setCommande(commande);
        charge.setPointDeVenteId(commande.getPointDeVenteId() != null ? commande.getPointDeVenteId() : getTenantId());
        charge.setDateCharge(dto.getDateCharge() != null ? dto.getDateCharge() : LocalDateTime.now());

        appliquerDtoAEntite(dto, charge, commande);

        charge = chargeAchatRepository.save(charge);

        // Recalcul automatique du coût de revient
        coutRevientService.recalculerEtPersister(commandeId);

        return toDto(charge);
    }

    @Transactional
    public ChargeAchatDTO modifierCharge(Long id, ChargeAchatDTO dto) {
        ChargeAchat charge = chargeAchatRepository.findByIdAndPointDeVenteId(id, getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("ChargeAchat", "id", id));

        appliquerDtoAEntite(dto, charge, charge.getCommande());

        charge = chargeAchatRepository.save(charge);

        // Recalcul automatique du coût de revient
        coutRevientService.recalculerEtPersister(charge.getCommande().getId());

        return toDto(charge);
    }

    @Transactional
    public void supprimerCharge(Long id) {
        ChargeAchat charge = chargeAchatRepository.findByIdAndPointDeVenteId(id, getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("ChargeAchat", "id", id));

        if (Boolean.TRUE.equals(charge.getAppliqueeAuStock())) {
            throw new CommonException("Impossible de supprimer une charge déjà intégrée au stock valorisé.", HttpStatus.BAD_REQUEST);
        }

        Long commandeId = charge.getCommande().getId();
        chargeAchatRepository.delete(charge);

        // Recalcul automatique du coût de revient
        coutRevientService.recalculerEtPersister(commandeId);
    }

    @Transactional
    public ChargeAchatDTO lierFactureAchat(Long chargeId, Long factureAchatId) {
        ChargeAchat charge = chargeAchatRepository.findByIdAndPointDeVenteId(chargeId, getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("ChargeAchat", "id", chargeId));

        FactureAchat facture = factureAchatRepository.findById(factureAchatId)
                .orElseThrow(() -> new ResourceNotFoundException("FactureAchat", "id", factureAchatId));

        charge.setFactureAchat(facture);
        charge.setNumeroFactureAchat(facture.getNumeroFacture());
        if (facture.getMontantTtc() != null) {
            charge.setMontantReelDevise(facture.getMontantHt() != null ? facture.getMontantHt() : facture.getMontantTtc());
            charge.setMontantTvaMad(facture.getMontantTva());
            charge.setStatut(StatutCharge.REELLE);
        }

        charge = chargeAchatRepository.save(charge);
        coutRevientService.recalculerEtPersister(charge.getCommande().getId());

        return toDto(charge);
    }

    private void appliquerDtoAEntite(ChargeAchatDTO dto, ChargeAchat charge, Commande commande) {
        if (dto.getTypeCharge() != null) charge.setTypeCharge(dto.getTypeCharge());
        charge.setLibelle(dto.getLibelle());
        charge.setNumeroFactureExterne(dto.getNumeroFactureExterne());
        if (dto.getDevise() != null) charge.setDevise(dto.getDevise());
        if (dto.getTauxChange() != null) charge.setTauxChange(dto.getTauxChange());
        if (dto.getMontantEstimeDevise() != null) charge.setMontantEstimeDevise(dto.getMontantEstimeDevise());
        if (dto.getMontantReelDevise() != null) {
            charge.setMontantReelDevise(dto.getMontantReelDevise());
            charge.setStatut(StatutCharge.REELLE);
        }
        if (dto.getMontantTvaMad() != null) charge.setMontantTvaMad(dto.getMontantTvaMad());
        if (dto.getCleRepartition() != null) charge.setCleRepartition(dto.getCleRepartition());
        if (dto.getIncorporable() != null) charge.setIncorporable(dto.getIncorporable());
        charge.setObservations(dto.getObservations());

        // Ligne ciblée
        if (dto.getLigneCommandeId() != null) {
            LigneCommande ligne = ligneCommandeRepository.findById(dto.getLigneCommandeId())
                    .orElseThrow(() -> new ResourceNotFoundException("LigneCommande", "id", dto.getLigneCommandeId()));
            charge.setLigneCommande(ligne);
            charge.setCleRepartition(com.gestion.persistent.enums.CleRepartition.DIRECTE);
        } else {
            charge.setLigneCommande(null);
        }

        // Prestataire / Fournisseur
        if (dto.getPrestataireId() != null) {
            Fournisseur prestataire = fournisseurRepository.findById(dto.getPrestataireId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", dto.getPrestataireId()));
            charge.setPrestataire(prestataire);
        } else {
            charge.setPrestataire(null);
        }

        // Facture achat liée
        if (dto.getFactureAchatId() != null) {
            FactureAchat facture = factureAchatRepository.findById(dto.getFactureAchatId())
                    .orElseThrow(() -> new ResourceNotFoundException("FactureAchat", "id", dto.getFactureAchatId()));
            charge.setFactureAchat(facture);
        }
    }

    public ChargeAchatDTO toDto(ChargeAchat c) {
        ChargeAchatDTO dto = new ChargeAchatDTO();
        dto.setId(c.getId());
        if (c.getCommande() != null) dto.setCommandeId(c.getCommande().getId());
        if (c.getLigneCommande() != null) {
            dto.setLigneCommandeId(c.getLigneCommande().getId());
            if (c.getLigneCommande().getProduit() != null) {
                dto.setLigneCommandeDesignation(c.getLigneCommande().getProduit().getDesignation());
            }
        }
        dto.setTypeCharge(c.getTypeCharge());
        dto.setTypeChargeLibelle(c.getTypeCharge() != null ? c.getTypeCharge().getLibelle() : null);
        dto.setLibelle(c.getLibelle());
        if (c.getPrestataire() != null) {
            dto.setPrestataireId(c.getPrestataire().getId());
            dto.setPrestataireNom(c.getPrestataire().getRaisonSociale());
        }
        if (c.getFactureAchat() != null) {
            dto.setFactureAchatId(c.getFactureAchat().getId());
            dto.setNumeroFactureAchat(c.getFactureAchat().getNumeroFacture());
        }
        dto.setNumeroFactureExterne(c.getNumeroFactureExterne());
        dto.setDevise(c.getDevise());
        dto.setTauxChange(c.getTauxChange());
        dto.setMontantEstimeDevise(c.getMontantEstimeDevise());
        dto.setMontantReelDevise(c.getMontantReelDevise());
        dto.setMontantRetenuMad(c.getMontantRetenuMad());
        dto.setMontantTvaMad(c.getMontantTvaMad());
        dto.setCleRepartition(c.getCleRepartition());
        dto.setIncorporable(c.getIncorporable());
        dto.setStatut(c.getStatut());
        dto.setDateCharge(c.getDateCharge());
        dto.setAppliqueeAuStock(c.getAppliqueeAuStock());
        dto.setObservations(c.getObservations());
        return dto;
    }
}
