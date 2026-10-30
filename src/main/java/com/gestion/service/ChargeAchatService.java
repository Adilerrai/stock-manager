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
    private final com.gestion.repository.DepenseRepository depenseRepository;

    public ChargeAchatService(ChargeAchatRepository chargeAchatRepository,
                              CommandeRepository commandeRepository,
                              LigneCommandeRepository ligneCommandeRepository,
                              FournisseurRepository fournisseurRepository,
                              FactureAchatRepository factureAchatRepository,
                              CoutRevientService coutRevientService,
                              com.gestion.repository.DepenseRepository depenseRepository) {
        this.chargeAchatRepository = chargeAchatRepository;
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.coutRevientService = coutRevientService;
        this.depenseRepository = depenseRepository;
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
        synchroniserDepense(charge, commande);
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
        synchroniserDepense(charge, charge.getCommande());
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

        if (charge.getDepense() != null) {
            depenseRepository.delete(charge.getDepense());
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
        if (c.getDepense() != null) {
            dto.setDepenseId(c.getDepense().getId());
            dto.setReferenceDepense(c.getDepense().getReference());
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

    private void synchroniserDepense(ChargeAchat charge, Commande commande) {
        if (charge == null || commande == null) return;
        java.math.BigDecimal montant = charge.getMontantRetenuMad();
        if (montant == null || montant.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (charge.getMontantReelDevise() != null && charge.getTauxChange() != null) {
                montant = charge.getMontantReelDevise().multiply(charge.getTauxChange());
            }
        }
        if (montant == null || montant.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }

        com.gestion.persistent.model.Depense depense = charge.getDepense();
        if (depense == null) {
            depense = new com.gestion.persistent.model.Depense();
            depense.setReference("DEP-IMP-" + System.currentTimeMillis());
        }

        String label = (charge.getLibelle() != null && !charge.getLibelle().isBlank())
                ? charge.getLibelle()
                : (charge.getTypeCharge() != null ? charge.getTypeCharge().getLibelle() : "Frais d'approche");
        depense.setDesignation("[Import] " + label + " - CMD " + commande.getNumeroCommande());
        depense.setMontant(montant);
        depense.setDateDepense(charge.getDateCharge() != null ? charge.getDateCharge().toLocalDate() : java.time.LocalDate.now());
        depense.setCategorie(mapperTypeChargeVersCategorieDepense(charge.getTypeCharge()));
        depense.setModePaiement(com.gestion.persistent.enums.ModePaiement.VIREMENT);

        if (charge.getPrestataire() != null) {
            depense.setFournisseur(charge.getPrestataire());
            depense.setBeneficiaire(charge.getPrestataire().getRaisonSociale());
        } else if (charge.getTypeCharge() != null) {
            depense.setBeneficiaire(charge.getTypeCharge().getLibelle());
        }

        depense.setNumeroFactureJustificatif(charge.getNumeroFactureExterne());
        depense.setCommande(commande);
        depense.setEstFraisImport(true);
        depense.setTypeFraisImport(charge.getTypeCharge() != null ? charge.getTypeCharge().name() : null);
        depense.setPointDeVenteId(charge.getPointDeVenteId() != null ? charge.getPointDeVenteId() : getTenantId());

        depense = depenseRepository.save(depense);
        charge.setDepense(depense);
    }

    private com.gestion.persistent.enums.CategorieDepense mapperTypeChargeVersCategorieDepense(com.gestion.persistent.enums.TypeCharge type) {
        if (type == null) return com.gestion.persistent.enums.CategorieDepense.AUTRES_CHARGES;
        switch (type) {
            case DROITS_DOUANE:
            case TVA_IMPORT:
                return com.gestion.persistent.enums.CategorieDepense.DROITS_DOUANE_IMPORT;
            case TRANSITAIRE:
                return com.gestion.persistent.enums.CategorieDepense.HONORAIRES_TRANSIT_IMPORT;
            case FRET_INTERNATIONAL:
            case TRANSPORT_ORIGINE:
                return com.gestion.persistent.enums.CategorieDepense.FRET_TRANSPORT_INTERNATIONAL;
            case MAGASINAGE:
            case PENALITE:
                return com.gestion.persistent.enums.CategorieDepense.MAGASINAGE_PORT_IMPORT;
            case ASSURANCE:
                return com.gestion.persistent.enums.CategorieDepense.ASSURANCE_TRANSPORT_IMPORT;
            case FRAIS_BANCAIRES:
                return com.gestion.persistent.enums.CategorieDepense.FRAIS_BANCAIRES;
            case TRANSPORT_LOCAL:
                return com.gestion.persistent.enums.CategorieDepense.TRANSPORT_CARBURANT;
            default:
                return com.gestion.persistent.enums.CategorieDepense.AUTRES_CHARGES;
        }
    }
}
