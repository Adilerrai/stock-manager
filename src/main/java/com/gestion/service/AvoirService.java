package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.StatistiqueMotifRetourDTO;
import com.gestion.persistent.enums.MotifRetour;
import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.enums.StatutAvoir;
import com.gestion.persistent.enums.TypeAvoir;
import com.gestion.persistent.enums.TypeMouvement;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
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

    public AvoirService(AvoirRepository avoirRepository,
                        LigneAvoirRepository ligneAvoirRepository,
                        ClientRepository clientRepository,
                        FournisseurRepository fournisseurRepository,
                        ProduitRepository produitRepository,
                        UserRepository userRepository,
                        FactureRepository factureRepository,
                        FactureAchatRepository factureAchatRepository,
                        MouvementStockService mouvementStockService) {
        this.avoirRepository = avoirRepository;
        this.ligneAvoirRepository = ligneAvoirRepository;
        this.clientRepository = clientRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.mouvementStockService = mouvementStockService;
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
            avoir.setNumeroAvoir(genererNumeroAvoir("AVR-CLI-", TypeAvoir.CLIENT));
        } else {
            if (avoir.getFournisseur() == null || avoir.getFournisseur().getId() == null) {
                throw new RuntimeException("Le fournisseur est obligatoire pour un avoir fournisseur");
            }
            Fournisseur fournisseur = fournisseurRepository.findById(avoir.getFournisseur().getId())
                    .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé"));
            avoir.setFournisseur(fournisseur);
            avoir.setNumeroAvoir(genererNumeroAvoir("AVR-FRS-", TypeAvoir.FOURNISSEUR));
        }

        if (avoir.getDateAvoir() == null) {
            avoir.setDateAvoir(LocalDate.now());
        }
        if (avoir.getStatut() == null) {
            avoir.setStatut(StatutAvoir.BROUILLON);
        }
        avoir.setDateCreation(LocalDateTime.now());

        if (avoir.getLignes() != null) {
            for (LigneAvoir ligne : avoir.getLignes()) {
                if (ligne.getProduit() != null && ligne.getProduit().getId() != null) {
                    Produit p = produitRepository.findById(ligne.getProduit().getId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + ligne.getProduit().getId()));
                    ligne.setProduit(p);
                }
                ligne.setAvoir(avoir);
                ligne.calculerMontants();
            }
        }

        avoir.calculerTotaux();
        return avoirRepository.save(avoir);
    }

    public Avoir validerAvoir(Long id, Long depotId) {
        Avoir avoir = getAvoirById(id);
        if (avoir.getStatut() == StatutAvoir.VALIDE) {
            throw new RuntimeException("Cet avoir est déjà validé");
        }

        // Mouvements de stock si restitution de marchandises
        if (avoir.getLignes() != null) {
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
        return avoirRepository.save(avoir);
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

    public Avoir getAvoirById(Long id) {
        return avoirRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avoir non trouvé avec l'id: " + id));
    }

    public List<Avoir> getAllAvoirs() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return avoirRepository.findByPointDeVenteIdOrderByDateAvoirDesc(tenantId);
        }
        return avoirRepository.findAll();
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
        String base = prefixe + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = avoirRepository.countByTypeAvoir(typeAvoir) + 1;
        return base + String.format("%04d", count);
    }
}
