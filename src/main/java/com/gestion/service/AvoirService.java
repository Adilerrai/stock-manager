package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.*;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AvoirService {

    private final AvoirRepository avoirRepository;
    private final LigneAvoirRepository ligneAvoirRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final FactureRepository factureRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final MouvementStockService mouvementStockService;
    private final ClientService clientService;
    private final CodificationService codificationService;

    public AvoirService(AvoirRepository avoirRepository,
                        LigneAvoirRepository ligneAvoirRepository,
                        ClientRepository clientRepository,
                        FournisseurRepository fournisseurRepository,
                        ProduitRepository produitRepository,
                        UserRepository userRepository,
                        FactureRepository factureRepository,
                        FactureAchatRepository factureAchatRepository,
                        MouvementStockService mouvementStockService,
                        ClientService clientService,
                        CodificationService codificationService) {
        this.avoirRepository = avoirRepository;
        this.ligneAvoirRepository = ligneAvoirRepository;
        this.clientRepository = clientRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.mouvementStockService = mouvementStockService;
        this.clientService = clientService;
        this.codificationService = codificationService;
    }

    public Avoir creerAvoir(Avoir avoir, Long userId) {
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            avoir.setCreePar(user);
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            avoir.setPointDeVenteId(tenantId);
        }

        if (avoir.getTypeAvoir() == TypeAvoir.CLIENT) {
            if (avoir.getClient() == null || avoir.getClient().getId() == null) {
                throw new RuntimeException("Le client est obligatoire pour un avoir client");
            }
            Client client = clientRepository.findById(avoir.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            avoir.setClient(client);
            if (avoir.getNumeroAvoir() == null || avoir.getNumeroAvoir().isBlank()) {
                avoir.setNumeroAvoir(genererNumeroAvoir("AVR-CLI-", TypeAvoir.CLIENT));
            }
        } else {
            if (avoir.getFournisseur() == null || avoir.getFournisseur().getId() == null) {
                throw new RuntimeException("Le fournisseur est obligatoire pour un avoir fournisseur");
            }
            Fournisseur fournisseur = fournisseurRepository.findById(avoir.getFournisseur().getId())
                    .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));
            avoir.setFournisseur(fournisseur);
            if (avoir.getNumeroAvoir() == null || avoir.getNumeroAvoir().isBlank()) {
                avoir.setNumeroAvoir(genererNumeroAvoir("AVR-FRS-", TypeAvoir.FOURNISSEUR));
            }
        }

        if (avoir.getDateAvoir() == null) {
            avoir.setDateAvoir(LocalDate.now());
        }
        if (avoir.getStatut() == null) {
            avoir.setStatut(StatutAvoir.BROUILLON);
        }
        if (avoir.getNatureAvoir() == null) {
            avoir.setNatureAvoir(NatureAvoir.RETOUR_MARCHANDISE);
        }
        avoir.setDateCreation(LocalDateTime.now());

        if (avoir.getLignes() != null) {
            for (LigneAvoir ligne : avoir.getLignes()) {
                if (ligne.getProduit() != null && ligne.getProduit().getId() != null) {
                    Produit p = produitRepository.findById(ligne.getProduit().getId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + ligne.getProduit().getId()));
                    ligne.setProduit(p);
                    if (ligne.getDesignation() == null || ligne.getDesignation().isBlank()) {
                        ligne.setDesignation(p.getNom() != null ? p.getNom() : p.getDesignation());
                    }
                }
                // Si avoir purement commercial, aucun retour physique en stock
                if (avoir.getNatureAvoir() == NatureAvoir.AVOIR_COMMERCIAL) {
                    ligne.setRemettreEnStock(false);
                }
                ligne.setAvoir(avoir);
                ligne.calculerMontants();
            }
        }

        avoir.calculerTotaux();

        // Contrôle anti-dépassement par rapport à la facture d'origine (si renseignée)
        validerAntiDepassement(avoir);

        return avoirRepository.save(avoir);
    }

    public Avoir validerAvoir(Long id, Long depotId) {
        Avoir avoir = getAvoirById(id);
        if (avoir.getStatut() == StatutAvoir.VALIDE) {
            throw new RuntimeException("Cet avoir est déjà validé");
        }

        // Mouvements de stock si restitution physique de marchandises (exclus pour avoirs commerciaux)
        if (avoir.getNatureAvoir() != NatureAvoir.AVOIR_COMMERCIAL && avoir.getLignes() != null) {
            for (LigneAvoir ligne : avoir.getLignes()) {
                if (Boolean.TRUE.equals(ligne.getRemettreEnStock()) && ligne.getProduit() != null) {
                    if (avoir.getTypeAvoir() == TypeAvoir.CLIENT) {
                        mouvementStockService.creerMouvement(
                                ligne.getProduit().getId(),
                                depotId,
                                TypeMouvement.AJUSTEMENT_POSITIF,
                                ligne.getQuantite(),
                                QualiteProduit.PREMIERE_QUALITE,
                                avoir.getNumeroAvoir(),
                                "Retour marchandise avoir client " + avoir.getNumeroAvoir()
                        );
                    } else {
                        mouvementStockService.creerMouvement(
                                ligne.getProduit().getId(),
                                depotId,
                                TypeMouvement.AJUSTEMENT_NEGATIF,
                                ligne.getQuantite(),
                                QualiteProduit.PREMIERE_QUALITE,
                                avoir.getNumeroAvoir(),
                                "Retour marchandise avoir fournisseur " + avoir.getNumeroAvoir()
                        );
                    }
                }
            }
        }

        avoir.setStatut(StatutAvoir.VALIDE);
        Avoir saved = avoirRepository.save(avoir);

        // Déduction financière sur la dette et la facture
        BigDecimal montantAvoir = saved.getMontantTTC() != null ? saved.getMontantTTC() : BigDecimal.ZERO;
        if (saved.getTypeAvoir() == TypeAvoir.CLIENT && saved.getClient() != null) {
            if (montantAvoir.compareTo(BigDecimal.ZERO) > 0) {
                // Diminuer le crédit utilisé du client
                clientService.diminuerCreditUtilise(saved.getClient().getId(), montantAvoir);
            }

            // Déduire sur la facture d'origine si spécifiée
            if (saved.getFactureOrigineId() != null) {
                factureRepository.findById(saved.getFactureOrigineId()).ifPresent(f -> {
                    BigDecimal reste = f.getMontantRestant() != null ? f.getMontantRestant() : f.getMontantFinal();
                    BigDecimal nouveauRestant = reste.subtract(montantAvoir);
                    if (nouveauRestant.compareTo(BigDecimal.ZERO) <= 0) {
                        f.setMontantRestant(BigDecimal.ZERO);
                        f.setStatut(com.gestion.persistent.enums.StatutFacture.PAYEE_TOTALEMENT);
                    } else {
                        f.setMontantRestant(nouveauRestant);
                        f.setStatut(com.gestion.persistent.enums.StatutFacture.PAYEE_PARTIELLEMENT);
                    }
                    factureRepository.save(f);
                });
            }
        } else if (saved.getTypeAvoir() == TypeAvoir.FOURNISSEUR) {
            // Ajustement facture d'achat si spécifiée
            if (saved.getFactureOrigineId() != null) {
                factureAchatRepository.findById(saved.getFactureOrigineId()).ifPresent(fa -> {
                    // Maintien de cohérence sur facture d'achat
                    if (fa.getStatut() == com.gestion.persistent.enums.StatutFacture.EN_ATTENTE && montantAvoir.compareTo(fa.getMontantTtc()) >= 0) {
                        fa.setStatut(com.gestion.persistent.enums.StatutFacture.ANNULEE);
                        factureAchatRepository.save(fa);
                    }
                });
            }
        }

        return saved;
    }

    public Avoir creerAvoirDepuisFacture(Long factureId, String motif, Long userId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée: " + factureId));

        Avoir avoir = new Avoir();
        avoir.setTypeAvoir(TypeAvoir.CLIENT);
        avoir.setClient(facture.getClient());
        avoir.setFactureOrigineId(facture.getId());
        avoir.setNumeroFactureOrigine(facture.getNumeroFacture());
        avoir.setDateAvoir(LocalDate.now());
        avoir.setMotif(motif != null ? motif : "Avoir sur facture " + facture.getNumeroFacture());
        avoir.setStatut(StatutAvoir.BROUILLON);

        List<LigneAvoir> lignesAvoir = new ArrayList<>();
        if (facture.getLignes() != null) {
            for (LigneFacture lf : facture.getLignes()) {
                LigneAvoir la = new LigneAvoir();
                la.setAvoir(avoir);
                la.setProduit(lf.getProduit());
                la.setQuantite(lf.getQuantite());
                la.setPrixUnitaireHT(lf.getPrixUnitaireHT());
                la.setTauxTVA(lf.getTauxTVA());
                la.setRemettreEnStock(true);
                la.calculerMontants();
                lignesAvoir.add(la);
            }
        }
        avoir.setLignes(lignesAvoir);
        avoir.calculerTotaux();

        return creerAvoir(avoir, userId);
    }

    public Avoir creerAvoirDepuisFactureAchat(Long factureAchatId, String motif, Long userId) {
        FactureAchat facture = factureAchatRepository.findById(factureAchatId)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée: " + factureAchatId));

        Avoir avoir = new Avoir();
        avoir.setTypeAvoir(TypeAvoir.FOURNISSEUR);
        avoir.setFournisseur(facture.getFournisseur());
        avoir.setFactureOrigineId(facture.getId());
        avoir.setNumeroFactureOrigine(facture.getNumeroFacture());
        avoir.setDateAvoir(LocalDate.now());
        avoir.setMotif(motif != null ? motif : "Avoir sur facture achat " + facture.getNumeroFacture());
        avoir.setStatut(StatutAvoir.BROUILLON);

        List<LigneAvoir> lignesAvoir = new ArrayList<>();
        if (facture.getLignes() != null) {
            for (LigneFactureAchat lf : facture.getLignes()) {
                LigneAvoir la = new LigneAvoir();
                la.setAvoir(avoir);
                la.setProduit(lf.getProduit());
                la.setQuantite(lf.getQuantite());
                la.setPrixUnitaireHT(lf.getPrixUnitaireHt());
                la.setTauxTVA(lf.getTauxTva());
                la.setRemettreEnStock(true);
                la.calculerMontants();
                lignesAvoir.add(la);
            }
        }
        avoir.setLignes(lignesAvoir);
        avoir.calculerTotaux();

        return creerAvoir(avoir, userId);
    }

    /**
     * Contrôle anti-dépassement strict des quantités et montants par rapport à la facture d'origine.
     */
    public void validerAntiDepassement(Avoir avoir) {
        if (avoir.getFactureOrigineId() == null) {
            return;
        }

        if (avoir.getTypeAvoir() == TypeAvoir.CLIENT) {
            Facture facture = factureRepository.findById(avoir.getFactureOrigineId())
                    .orElseThrow(() -> new RuntimeException("Facture d'origine non trouvée: " + avoir.getFactureOrigineId()));

            List<Avoir> avoirsExistants = avoirRepository.findByFactureOrigineIdAndTypeAvoirAndStatutNot(
                    facture.getId(), TypeAvoir.CLIENT, StatutAvoir.ANNULE);

            if (avoir.getId() != null) {
                avoirsExistants = avoirsExistants.stream()
                        .filter(a -> !a.getId().equals(avoir.getId()))
                        .toList();
            }

            // 1. Contrôle montant global TTC
            BigDecimal totalFactureTTC = facture.getMontantTTC() != null ? facture.getMontantTTC()
                    : (facture.getMontantFinal() != null ? facture.getMontantFinal() : BigDecimal.ZERO);
            BigDecimal totalDejaAvoirTTC = avoirsExistants.stream()
                    .map(a -> a.getMontantTTC() != null ? a.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal montantNouvelAvoirTTC = avoir.getMontantTTC() != null ? avoir.getMontantTTC() : BigDecimal.ZERO;

            if (totalDejaAvoirTTC.add(montantNouvelAvoirTTC).compareTo(totalFactureTTC) > 0) {
                BigDecimal resteAvoiriable = totalFactureTTC.subtract(totalDejaAvoirTTC);
                if (resteAvoiriable.compareTo(BigDecimal.ZERO) < 0) resteAvoiriable = BigDecimal.ZERO;
                throw new IllegalArgumentException(String.format(
                        "Dépassement du montant de la facture %s : Total facture = %s DH, Déjà avoirié = %s DH, Demandé = %s DH. Reste maximum avoiriable = %s DH",
                        facture.getNumeroFacture(), totalFactureTTC, totalDejaAvoirTTC, montantNouvelAvoirTTC, resteAvoiriable));
            }

            // 2. Contrôle des quantités par produit
            if (avoir.getLignes() != null) {
                for (LigneAvoir la : avoir.getLignes()) {
                    if (la.getProduit() != null && la.getProduit().getId() != null) {
                        Long prodId = la.getProduit().getId();
                        String nomProd = la.getProduit().getNom() != null ? la.getProduit().getNom() : la.getDesignation();

                        BigDecimal qFacturee = BigDecimal.ZERO;
                        if (facture.getLignes() != null) {
                            for (LigneFacture lf : facture.getLignes()) {
                                if (lf.getProduit() != null && prodId.equals(lf.getProduit().getId())) {
                                    qFacturee = qFacturee.add(lf.getQuantite() != null ? lf.getQuantite() : BigDecimal.ZERO);
                                }
                            }
                        }

                        if (qFacturee.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException(String.format(
                                    "Le produit '%s' ne figure pas sur la facture d'origine %s",
                                    nomProd, facture.getNumeroFacture()));
                        }

                        BigDecimal qDejaAvoir = BigDecimal.ZERO;
                        for (Avoir a : avoirsExistants) {
                            if (a.getLignes() != null) {
                                for (LigneAvoir ela : a.getLignes()) {
                                    if (ela.getProduit() != null && prodId.equals(ela.getProduit().getId())) {
                                        qDejaAvoir = qDejaAvoir.add(ela.getQuantite() != null ? ela.getQuantite() : BigDecimal.ZERO);
                                    }
                                }
                            }
                        }

                        BigDecimal qDispo = qFacturee.subtract(qDejaAvoir);
                        if (qDispo.compareTo(BigDecimal.ZERO) < 0) qDispo = BigDecimal.ZERO;

                        BigDecimal qDemandee = la.getQuantite() != null ? la.getQuantite() : BigDecimal.ZERO;
                        if (qDemandee.compareTo(qDispo) > 0) {
                            throw new IllegalArgumentException(String.format(
                                    "Dépassement de quantité pour le produit '%s' sur la facture %s : Facturé = %s, Déjà avoirié = %s, Demandé = %s. Maximum disponible = %s",
                                    nomProd, facture.getNumeroFacture(), qFacturee, qDejaAvoir, qDemandee, qDispo));
                        }
                    }
                }
            }
        } else if (avoir.getTypeAvoir() == TypeAvoir.FOURNISSEUR) {
            FactureAchat factureAchat = factureAchatRepository.findById(avoir.getFactureOrigineId())
                    .orElseThrow(() -> new RuntimeException("Facture d'achat d'origine non trouvée: " + avoir.getFactureOrigineId()));

            List<Avoir> avoirsExistants = avoirRepository.findByFactureOrigineIdAndTypeAvoirAndStatutNot(
                    factureAchat.getId(), TypeAvoir.FOURNISSEUR, StatutAvoir.ANNULE);

            if (avoir.getId() != null) {
                avoirsExistants = avoirsExistants.stream()
                        .filter(a -> !a.getId().equals(avoir.getId()))
                        .toList();
            }

            BigDecimal totalFactureTTC = factureAchat.getMontantTtc() != null ? factureAchat.getMontantTtc() : BigDecimal.ZERO;
            BigDecimal totalDejaAvoirTTC = avoirsExistants.stream()
                    .map(a -> a.getMontantTTC() != null ? a.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal montantNouvelAvoirTTC = avoir.getMontantTTC() != null ? avoir.getMontantTTC() : BigDecimal.ZERO;

            if (totalDejaAvoirTTC.add(montantNouvelAvoirTTC).compareTo(totalFactureTTC) > 0) {
                BigDecimal resteAvoiriable = totalFactureTTC.subtract(totalDejaAvoirTTC);
                if (resteAvoiriable.compareTo(BigDecimal.ZERO) < 0) resteAvoiriable = BigDecimal.ZERO;
                throw new IllegalArgumentException(String.format(
                        "Dépassement du montant de la facture d'achat %s : Total facture = %s DH, Déjà avoirié = %s DH, Demandé = %s DH. Reste maximum avoiriable = %s DH",
                        factureAchat.getNumeroFacture(), totalFactureTTC, totalDejaAvoirTTC, montantNouvelAvoirTTC, resteAvoiriable));
            }

            if (avoir.getLignes() != null) {
                for (LigneAvoir la : avoir.getLignes()) {
                    if (la.getProduit() != null && la.getProduit().getId() != null) {
                        Long prodId = la.getProduit().getId();
                        String nomProd = la.getProduit().getNom() != null ? la.getProduit().getNom() : la.getDesignation();

                        BigDecimal qFacturee = BigDecimal.ZERO;
                        if (factureAchat.getLignes() != null) {
                            for (LigneFactureAchat lf : factureAchat.getLignes()) {
                                if (lf.getProduit() != null && prodId.equals(lf.getProduit().getId())) {
                                    qFacturee = qFacturee.add(lf.getQuantite() != null ? lf.getQuantite() : BigDecimal.ZERO);
                                }
                            }
                        }

                        if (qFacturee.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException(String.format(
                                    "Le produit '%s' ne figure pas sur la facture d'achat %s",
                                    nomProd, factureAchat.getNumeroFacture()));
                        }

                        BigDecimal qDejaAvoir = BigDecimal.ZERO;
                        for (Avoir a : avoirsExistants) {
                            if (a.getLignes() != null) {
                                for (LigneAvoir ela : a.getLignes()) {
                                    if (ela.getProduit() != null && prodId.equals(ela.getProduit().getId())) {
                                        qDejaAvoir = qDejaAvoir.add(ela.getQuantite() != null ? ela.getQuantite() : BigDecimal.ZERO);
                                    }
                                }
                            }
                        }

                        BigDecimal qDispo = qFacturee.subtract(qDejaAvoir);
                        if (qDispo.compareTo(BigDecimal.ZERO) < 0) qDispo = BigDecimal.ZERO;

                        BigDecimal qDemandee = la.getQuantite() != null ? la.getQuantite() : BigDecimal.ZERO;
                        if (qDemandee.compareTo(qDispo) > 0) {
                            throw new IllegalArgumentException(String.format(
                                    "Dépassement de quantité pour le produit '%s' sur la facture d'achat %s : Facturé = %s, Déjà avoirié = %s, Demandé = %s. Maximum disponible = %s",
                                    nomProd, factureAchat.getNumeroFacture(), qFacturee, qDejaAvoir, qDemandee, qDispo));
                        }
                    }
                }
            }
        }
    }

    /**
     * Calcule pour chaque ligne d'une facture client les quantités facturées, déjà avoiriées et restant disponibles.
     */
    @Transactional(readOnly = true)
    public FactureLignesAvoiriablesDTO calculerLignesAvoirablesFacture(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée: " + factureId));

        FactureLignesAvoiriablesDTO dto = new FactureLignesAvoiriablesDTO();
        dto.setFactureId(facture.getId());
        dto.setNumeroFacture(facture.getNumeroFacture());
        dto.setDateFacture(facture.getDateFacture());
        dto.setTypeAvoir(TypeAvoir.CLIENT);
        if (facture.getClient() != null) {
            dto.setTiersId(facture.getClient().getId());
            dto.setTiersNom(facture.getClient().getNomComplet() != null ? facture.getClient().getNomComplet() : facture.getClient().getNom());
        }

        BigDecimal totalFactureHT = facture.getMontantHT() != null ? facture.getMontantHT() : BigDecimal.ZERO;
        BigDecimal totalFactureTTC = facture.getMontantTTC() != null ? facture.getMontantTTC()
                : (facture.getMontantFinal() != null ? facture.getMontantFinal() : BigDecimal.ZERO);
        dto.setMontantFactureHT(totalFactureHT);
        dto.setMontantFactureTTC(totalFactureTTC);

        List<Avoir> avoirsExistants = avoirRepository.findByFactureOrigineIdAndTypeAvoirAndStatutNot(
                facture.getId(), TypeAvoir.CLIENT, StatutAvoir.ANNULE);

        BigDecimal montantDejaAvoirTTC = avoirsExistants.stream()
                .map(a -> a.getMontantTTC() != null ? a.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setMontantDejaAvoirTTC(montantDejaAvoirTTC);

        BigDecimal montantRestant = totalFactureTTC.subtract(montantDejaAvoirTTC);
        dto.setMontantRestantAvoirTTC(montantRestant.compareTo(BigDecimal.ZERO) > 0 ? montantRestant : BigDecimal.ZERO);

        List<LigneAvoiriableDTO> lignesDTO = new ArrayList<>();
        if (facture.getLignes() != null) {
            for (LigneFacture lf : facture.getLignes()) {
                LigneAvoiriableDTO lDto = new LigneAvoiriableDTO();
                lDto.setLigneFactureId(lf.getId());
                if (lf.getProduit() != null) {
                    lDto.setProduitId(lf.getProduit().getId());
                    lDto.setReference(lf.getProduit().getReference());
                    lDto.setDesignation(lf.getProduit().getNom() != null ? lf.getProduit().getNom() : lf.getDesignation());
                } else {
                    lDto.setDesignation(lf.getDesignation());
                    lDto.setReference(lf.getReference());
                }

                BigDecimal qFact = lf.getQuantite() != null ? lf.getQuantite() : BigDecimal.ZERO;
                lDto.setQuantiteFacturee(qFact);
                lDto.setPrixUnitaireHT(lf.getPrixUnitaireHT() != null ? lf.getPrixUnitaireHT() : BigDecimal.ZERO);
                lDto.setTauxTVA(lf.getTauxTVA() != null ? lf.getTauxTVA() : BigDecimal.valueOf(20));
                lDto.setMontantTTCFacture(lf.getMontantTTC() != null ? lf.getMontantTTC() : BigDecimal.ZERO);

                BigDecimal qDejaAvoir = BigDecimal.ZERO;
                if (lf.getProduit() != null) {
                    Long pid = lf.getProduit().getId();
                    for (Avoir ea : avoirsExistants) {
                        if (ea.getLignes() != null) {
                            for (LigneAvoir ela : ea.getLignes()) {
                                if (ela.getProduit() != null && pid.equals(ela.getProduit().getId())) {
                                    qDejaAvoir = qDejaAvoir.add(ela.getQuantite() != null ? ela.getQuantite() : BigDecimal.ZERO);
                                }
                            }
                        }
                    }
                }
                lDto.setQuantiteDejaAvoiriee(qDejaAvoir);

                BigDecimal qDispo = qFact.subtract(qDejaAvoir);
                if (qDispo.compareTo(BigDecimal.ZERO) < 0) qDispo = BigDecimal.ZERO;
                lDto.setQuantiteDisponible(qDispo);
                lDto.setEntierementAvoiriee(qDispo.compareTo(BigDecimal.ZERO) <= 0);

                lignesDTO.add(lDto);
            }
        }
        dto.setLignes(lignesDTO);
        return dto;
    }

    /**
     * Calcule pour chaque ligne d'une facture d'achat fournisseur les quantités facturées, déjà avoiriées et restant disponibles.
     */
    @Transactional(readOnly = true)
    public FactureLignesAvoiriablesDTO calculerLignesAvoirablesFactureAchat(Long factureAchatId) {
        FactureAchat facture = factureAchatRepository.findById(factureAchatId)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée: " + factureAchatId));

        FactureLignesAvoiriablesDTO dto = new FactureLignesAvoiriablesDTO();
        dto.setFactureId(facture.getId());
        dto.setNumeroFacture(facture.getNumeroFacture());
        dto.setDateFacture(facture.getDateFacture() != null ? facture.getDateFacture().toLocalDate() : LocalDate.now());
        dto.setTypeAvoir(TypeAvoir.FOURNISSEUR);
        if (facture.getFournisseur() != null) {
            dto.setTiersId(facture.getFournisseur().getId());
            dto.setTiersNom(facture.getFournisseur().getNom());
        }

        BigDecimal totalFactureHT = facture.getMontantHt() != null ? facture.getMontantHt() : BigDecimal.ZERO;
        BigDecimal totalFactureTTC = facture.getMontantTtc() != null ? facture.getMontantTtc() : BigDecimal.ZERO;
        dto.setMontantFactureHT(totalFactureHT);
        dto.setMontantFactureTTC(totalFactureTTC);

        List<Avoir> avoirsExistants = avoirRepository.findByFactureOrigineIdAndTypeAvoirAndStatutNot(
                facture.getId(), TypeAvoir.FOURNISSEUR, StatutAvoir.ANNULE);

        BigDecimal montantDejaAvoirTTC = avoirsExistants.stream()
                .map(a -> a.getMontantTTC() != null ? a.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setMontantDejaAvoirTTC(montantDejaAvoirTTC);

        BigDecimal montantRestant = totalFactureTTC.subtract(montantDejaAvoirTTC);
        dto.setMontantRestantAvoirTTC(montantRestant.compareTo(BigDecimal.ZERO) > 0 ? montantRestant : BigDecimal.ZERO);

        List<LigneAvoiriableDTO> lignesDTO = new ArrayList<>();
        if (facture.getLignes() != null) {
            for (LigneFactureAchat lf : facture.getLignes()) {
                LigneAvoiriableDTO lDto = new LigneAvoiriableDTO();
                lDto.setLigneFactureId(lf.getId());
                if (lf.getProduit() != null) {
                    lDto.setProduitId(lf.getProduit().getId());
                    lDto.setReference(lf.getProduit().getReference());
                    lDto.setDesignation(lf.getProduit().getNom());
                }

                BigDecimal qFact = lf.getQuantite() != null ? lf.getQuantite() : BigDecimal.ZERO;
                lDto.setQuantiteFacturee(qFact);
                lDto.setPrixUnitaireHT(lf.getPrixUnitaireHt() != null ? lf.getPrixUnitaireHt() : BigDecimal.ZERO);
                lDto.setTauxTVA(lf.getTauxTva() != null ? lf.getTauxTva() : BigDecimal.valueOf(20));
                lDto.setMontantTTCFacture(lf.getMontantTtc() != null ? lf.getMontantTtc() : BigDecimal.ZERO);

                BigDecimal qDejaAvoir = BigDecimal.ZERO;
                if (lf.getProduit() != null) {
                    Long pid = lf.getProduit().getId();
                    for (Avoir ea : avoirsExistants) {
                        if (ea.getLignes() != null) {
                            for (LigneAvoir ela : ea.getLignes()) {
                                if (ela.getProduit() != null && pid.equals(ela.getProduit().getId())) {
                                    qDejaAvoir = qDejaAvoir.add(ela.getQuantite() != null ? ela.getQuantite() : BigDecimal.ZERO);
                                }
                            }
                        }
                    }
                }
                lDto.setQuantiteDejaAvoiriee(qDejaAvoir);

                BigDecimal qDispo = qFact.subtract(qDejaAvoir);
                if (qDispo.compareTo(BigDecimal.ZERO) < 0) qDispo = BigDecimal.ZERO;
                lDto.setQuantiteDisponible(qDispo);
                lDto.setEntierementAvoiriee(qDispo.compareTo(BigDecimal.ZERO) <= 0);

                lignesDTO.add(lDto);
            }
        }
        dto.setLignes(lignesDTO);
        return dto;
    }

    /**
     * Crée un avoir partiel (sélection de lignes / quantités) ou un avoir commercial (remise financière).
     */
    public Avoir creerAvoirPartiel(CreerAvoirPartielDTO dto, Long userId) {
        if (dto == null) {
            throw new IllegalArgumentException("La requête de création d'avoir partiel est obligatoire");
        }

        Avoir avoir = new Avoir();
        avoir.setTypeAvoir(dto.getTypeAvoir() != null ? dto.getTypeAvoir() : TypeAvoir.CLIENT);
        avoir.setNatureAvoir(dto.getNatureAvoir() != null ? dto.getNatureAvoir() : NatureAvoir.RETOUR_MARCHANDISE);
        avoir.setDateAvoir(dto.getDateAvoir() != null ? dto.getDateAvoir() : LocalDate.now());
        avoir.setMotif(dto.getMotif());
        avoir.setNotes(dto.getNotes());
        avoir.setStatut(StatutAvoir.BROUILLON);

        // Rattachement facture origine
        if (dto.getFactureId() != null) {
            avoir.setFactureOrigineId(dto.getFactureId());
            if (avoir.getTypeAvoir() == TypeAvoir.CLIENT) {
                Facture f = factureRepository.findById(dto.getFactureId())
                        .orElseThrow(() -> new RuntimeException("Facture non trouvée: " + dto.getFactureId()));
                avoir.setNumeroFactureOrigine(f.getNumeroFacture());
                avoir.setClient(f.getClient());
                if (avoir.getMotif() == null || avoir.getMotif().isBlank()) {
                    avoir.setMotif("Avoir partiel sur facture " + f.getNumeroFacture());
                }
            } else {
                FactureAchat fa = factureAchatRepository.findById(dto.getFactureId())
                        .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée: " + dto.getFactureId()));
                avoir.setNumeroFactureOrigine(fa.getNumeroFacture());
                avoir.setFournisseur(fa.getFournisseur());
                if (avoir.getMotif() == null || avoir.getMotif().isBlank()) {
                    avoir.setMotif("Avoir partiel sur facture achat " + fa.getNumeroFacture());
                }
            }
        }

        List<LigneAvoir> lignes = new ArrayList<>();

        // Cas 1 : Lignes d'articles partielles spécifiées
        if (dto.getLignes() != null && !dto.getLignes().isEmpty()) {
            for (LigneAvoirPartielDTO lp : dto.getLignes()) {
                if (lp.getQuantite() == null || lp.getQuantite().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                LigneAvoir la = new LigneAvoir();
                la.setAvoir(avoir);
                if (lp.getProduitId() != null) {
                    Produit p = produitRepository.findById(lp.getProduitId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + lp.getProduitId()));
                    la.setProduit(p);
                    la.setDesignation(p.getNom() != null ? p.getNom() : p.getDesignation());
                } else {
                    la.setDesignation(lp.getDesignation());
                }
                la.setQuantite(lp.getQuantite());
                la.setPrixUnitaireHT(lp.getPrixUnitaireHT() != null ? lp.getPrixUnitaireHT() : BigDecimal.ZERO);
                la.setTauxTVA(lp.getTauxTVA() != null ? lp.getTauxTVA() : BigDecimal.valueOf(20));

                if (avoir.getNatureAvoir() == NatureAvoir.AVOIR_COMMERCIAL) {
                    la.setRemettreEnStock(false);
                } else {
                    la.setRemettreEnStock(lp.getRemettreEnStock() != null ? lp.getRemettreEnStock() : true);
                }

                la.setMotifRetour(lp.getMotifRetour() != null ? lp.getMotifRetour() : MotifRetour.AUTRE);
                la.setMotif(lp.getMotif());
                la.calculerMontants();
                lignes.add(la);
            }
        } else if (dto.getMontantHTForfaitaire() != null && dto.getMontantHTForfaitaire().compareTo(BigDecimal.ZERO) > 0) {
            // Cas 2 : Avoir commercial forfaitaire sur montant global (sans article spécifique)
            avoir.setNatureAvoir(NatureAvoir.AVOIR_COMMERCIAL);
            LigneAvoir la = new LigneAvoir();
            la.setAvoir(avoir);
            la.setDesignation(dto.getLibelleForfaitaire() != null && !dto.getLibelleForfaitaire().isBlank()
                    ? dto.getLibelleForfaitaire() : "Geste commercial / Remise exceptionnelle");
            la.setQuantite(BigDecimal.ONE);
            la.setPrixUnitaireHT(dto.getMontantHTForfaitaire());
            la.setTauxTVA(dto.getTauxTVAForfaitaire() != null ? dto.getTauxTVAForfaitaire() : BigDecimal.valueOf(20));
            la.setRemettreEnStock(false);
            la.setMotifRetour(MotifRetour.AUTRE);
            la.setMotif(dto.getMotif());
            la.calculerMontants();
            lignes.add(la);
        } else {
            throw new IllegalArgumentException("L'avoir doit contenir au moins une ligne d'article avec une quantité > 0 ou un montant forfaitaire.");
        }

        avoir.setLignes(lignes);
        avoir.calculerTotaux();

        return creerAvoir(avoir, userId);
    }

    public Avoir getAvoirById(Long id) {
        return avoirRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avoir non trouvé avec l'id: " + id));
    }

    public List<Avoir> getAllAvoirs() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return avoirRepository.findByPointDeVenteIdOrderByDateAvoirDesc(tenantId);
        }
        return java.util.Collections.emptyList();
    }

    public Page<Avoir> searchAvoirs(AvoirSearchCriteria criteria, Pageable pageable) {
        return avoirRepository.findByCriteria(criteria, pageable);
    }

    public List<Avoir> getAvoirsByType(TypeAvoir typeAvoir) {
        return avoirRepository.findByTypeAvoirOrderByDateAvoirDesc(typeAvoir);
    }

    public List<Avoir> getAvoirsByClient(Long clientId) {
        return avoirRepository.findByClientIdOrderByDateAvoirDesc(clientId);
    }

    public List<Avoir> getAvoirsByFournisseur(Long fournisseurId) {
        return avoirRepository.findByFournisseurIdOrderByDateAvoirDesc(fournisseurId);
    }

    public void supprimerAvoir(Long id) {
        Avoir avoir = getAvoirById(id);
        if (avoir.getStatut() == StatutAvoir.VALIDE) {
            throw new RuntimeException("Impossible de supprimer un avoir validé");
        }
        avoirRepository.delete(avoir);
    }

    @Transactional(readOnly = true)
    public List<StatistiqueMotifRetourDTO> getStatistiquesMotifsRetour(LocalDate debut, LocalDate fin) {
        if (debut == null) debut = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        List<Object[]> rows = avoirRepository.findStatsCausesRetour(debut, fin);
        List<StatistiqueMotifRetourDTO> liste = new ArrayList<>();

        BigDecimal grandTotal = BigDecimal.ZERO;
        if (rows != null) {
            for (Object[] r : rows) {
                if (r[2] != null) {
                    grandTotal = grandTotal.add(new BigDecimal(r[2].toString()));
                }
            }
            for (Object[] r : rows) {
                MotifRetour motif = (r[0] != null) ? (MotifRetour) r[0] : MotifRetour.AUTRE;
                Long count = r[1] != null ? ((Number) r[1]).longValue() : 0L;
                BigDecimal montant = r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO;

                StatistiqueMotifRetourDTO dto = new StatistiqueMotifRetourDTO(motif, count, montant);
                if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pct = montant.multiply(new BigDecimal("100")).divide(grandTotal, 2, RoundingMode.HALF_UP);
                    dto.setPourcentageMontant(pct);
                } else {
                    dto.setPourcentageMontant(BigDecimal.ZERO);
                }
                liste.add(dto);
            }
        }
        return liste;
    }

    private String genererNumeroAvoir(String prefixe, TypeAvoir typeAvoir) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long effectiveTenantId = tenantId != null ? tenantId : 1L;
        com.gestion.persistent.enums.TypeDocumentCodification typeDoc = (typeAvoir == TypeAvoir.FOURNISSEUR)
                ? com.gestion.persistent.enums.TypeDocumentCodification.AVOIR_FOURNISSEUR
                : com.gestion.persistent.enums.TypeDocumentCodification.AVOIR_CLIENT;
        String numero;
        int attempts = 0;
        do {
            numero = codificationService.genererNumero(typeDoc, effectiveTenantId);
            attempts++;
        } while (avoirRepository.findByPointDeVenteIdAndNumeroAvoir(effectiveTenantId, numero).isPresent() && attempts < 1000);
        return numero;
    }
}
