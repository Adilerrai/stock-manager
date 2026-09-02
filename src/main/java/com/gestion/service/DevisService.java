package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.StatutDevis;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DevisService {

    private final DevisRepository devisRepository;
    private final LigneDevisRepository ligneDevisRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final FactureRepository factureRepository;

    public DevisService(DevisRepository devisRepository,
                        LigneDevisRepository ligneDevisRepository,
                        ClientRepository clientRepository,
                        ProduitRepository produitRepository,
                        UserRepository userRepository,
                        @Lazy CommandeClientRepository commandeClientRepository,
                        @Lazy FactureRepository factureRepository) {
        this.devisRepository = devisRepository;
        this.ligneDevisRepository = ligneDevisRepository;
        this.clientRepository = clientRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.factureRepository = factureRepository;
    }

    public Devis creerDevis(Devis devis, Long userId) {
        if (devis.getClient() != null && devis.getClient().getId() != null) {
            Client client = clientRepository.findById(devis.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + devis.getClient().getId()));
            devis.setClient(client);
        } else {
            throw new RuntimeException("Le client est obligatoire pour créer un devis");
        }

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            devis.setCreePar(user);
        }

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            devis.setPointDeVenteId(tenantId);
        }

        devis.setNumeroDevis(genererNumeroDevis());
        if (devis.getDateDevis() == null) {
            devis.setDateDevis(LocalDate.now());
        }
        if (devis.getDateValidite() == null) {
            devis.setDateValidite(devis.getDateDevis().plusDays(30));
        }
        if (devis.getStatut() == null) {
            devis.setStatut(StatutDevis.BROUILLON);
        }
        devis.setDateCreation(LocalDateTime.now());

        if (devis.getLignes() != null) {
            for (LigneDevis ligne : devis.getLignes()) {
                if (ligne.getProduit() != null && ligne.getProduit().getId() != null) {
                    Produit p = produitRepository.findById(ligne.getProduit().getId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + ligne.getProduit().getId()));
                    ligne.setProduit(p);
                    if (ligne.getPrixUnitaireHT() == null || ligne.getPrixUnitaireHT().compareTo(BigDecimal.ZERO) == 0) {
                        ligne.setPrixUnitaireHT(p.getPrixVenteHt() != null ? p.getPrixVenteHt() : p.getPrixVente());
                    }
                }
                ligne.setDevis(devis);
                ligne.calculerMontants();
            }
        }

        devis.calculerTotaux();
        return devisRepository.save(devis);
    }

    public Devis modifierDevis(Long id, Devis maj) {
        Devis existant = getDevisById(id);

        if (maj.getDateDevis() != null) existant.setDateDevis(maj.getDateDevis());
        if (maj.getDateValidite() != null) existant.setDateValidite(maj.getDateValidite());
        if (maj.getRemiseGlobale() != null) existant.setRemiseGlobale(maj.getRemiseGlobale());
        if (maj.getNotes() != null) existant.setNotes(maj.getNotes());
        if (maj.getConditionsPaiement() != null) existant.setConditionsPaiement(maj.getConditionsPaiement());
        if (maj.getStatut() != null) existant.setStatut(maj.getStatut());

        if (maj.getClient() != null && maj.getClient().getId() != null) {
            Client c = clientRepository.findById(maj.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            existant.setClient(c);
        }

        if (maj.getLignes() != null) {
            existant.getLignes().clear();
            for (LigneDevis l : maj.getLignes()) {
                if (l.getProduit() != null && l.getProduit().getId() != null) {
                    Produit p = produitRepository.findById(l.getProduit().getId())
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
                    l.setProduit(p);
                }
                l.setDevis(existant);
                l.calculerMontants();
                existant.getLignes().add(l);
            }
        }

        existant.calculerTotaux();
        return devisRepository.save(existant);
    }

    public Devis getDevisById(Long id) {
        return devisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devis non trouvé avec l'id: " + id));
    }

    public List<Devis> getAllDevis() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return devisRepository.findByPointDeVenteIdOrderByDateDevisDesc(tenantId);
        }
        return devisRepository.findAll();
    }

    public List<Devis> getDevisByClient(Long clientId) {
        return devisRepository.findByClientIdOrderByDateDevisDesc(clientId);
    }

    public List<Devis> getDevisByStatut(StatutDevis statut) {
        return devisRepository.findByStatutOrderByDateDevisDesc(statut);
    }

    public Devis changerStatut(Long id, StatutDevis statut) {
        Devis devis = getDevisById(id);
        devis.setStatut(statut);
        return devisRepository.save(devis);
    }

    public void supprimerDevis(Long id) {
        Devis devis = getDevisById(id);
        devisRepository.delete(devis);
    }

    public CommandeClient transformerEnCommandeClient(Long devisId, Long userId) {
        Devis devis = getDevisById(devisId);

        CommandeClient commande = new CommandeClient();
        commande.setNumeroCommande("CMD-" + System.currentTimeMillis());
        commande.setClient(devis.getClient());
        if (devis.getClient() != null) {
            commande.setClientNom(devis.getClient().getNomComplet() != null ? devis.getClient().getNomComplet() : devis.getClient().getNom());
            commande.setClientTelephone(devis.getClient().getTelephone());
            commande.setClientEmail(devis.getClient().getEmail());
            commande.setAdresseLivraison(devis.getClient().getAdresse());
        }
        commande.setDateCommande(LocalDateTime.now());
        commande.setStatut(StatutCommandeClient.CONFIRMEE);
        commande.setMontantHT(devis.getMontantHT());
        commande.setMontantTTC(devis.getMontantFinal());
        commande.setObservations("Générée automatiquement depuis le Devis " + devis.getNumeroDevis());

        List<LigneCommandeClient> lignesCmd = new ArrayList<>();
        if (devis.getLignes() != null) {
            for (LigneDevis ld : devis.getLignes()) {
                LigneCommandeClient lc = new LigneCommandeClient();
                lc.setCommandeClient(commande);
                lc.setProduit(ld.getProduit());
                lc.setQuantite(ld.getQuantite());
                lc.setPrixUnitaire(ld.getPrixUnitaireHT());
                lc.setMontantLigne(ld.getMontantHT());
                lc.setObservations("Issu du devis " + devis.getNumeroDevis());
                lignesCmd.add(lc);
            }
        }
        commande.setLignesCommande(lignesCmd);

        CommandeClient commandeEnregistree = commandeClientRepository.save(commande);

        devis.setStatut(StatutDevis.TRANSFORME_EN_COMMANDE);
        devis.setCommandeGenereeId(commandeEnregistree.getId());
        devisRepository.save(devis);

        return commandeEnregistree;
    }

    public Facture transformerEnFacture(Long devisId, Long userId) {
        Devis devis = getDevisById(devisId);

        Facture facture = new Facture();
        facture.setNumeroFacture("FAC-" + System.currentTimeMillis());
        facture.setDateFacture(LocalDate.now());
        facture.setDateEcheance(LocalDate.now().plusDays(30));
        facture.setClient(devis.getClient());

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            facture.setEmisePar(user);
        }

        facture.setMontantHT(devis.getMontantHT());
        facture.setMontantTVA(devis.getMontantTVA());
        facture.setMontantTTC(devis.getMontantTTC());
        facture.setRemiseGlobale(devis.getRemiseGlobale());
        facture.setMontantFinal(devis.getMontantFinal());
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setMontantRestant(devis.getMontantFinal());
        facture.setStatut(StatutFacture.EN_ATTENTE);
        facture.setNotes("Facture générée depuis le devis " + devis.getNumeroDevis());

        List<LigneFacture> lignesFac = new ArrayList<>();
        if (devis.getLignes() != null) {
            for (LigneDevis ld : devis.getLignes()) {
                LigneFacture lf = new LigneFacture();
                lf.setFacture(facture);
                lf.setProduit(ld.getProduit());
                lf.setQuantite(ld.getQuantite());
                lf.setPrixUnitaireHT(ld.getPrixUnitaireHT());
                lf.setTauxTVA(ld.getTauxTVA());
                lf.setRemisePourcentage(ld.getTauxRemise() != null ? ld.getTauxRemise() : BigDecimal.ZERO);
                lf.setMontantHT(ld.getMontantHT());
                lf.setMontantTVA(ld.getMontantTVA());
                lf.setMontantTTC(ld.getMontantTTC());
                lignesFac.add(lf);
            }
        }
        facture.setLignes(lignesFac);

        Facture factureEnregistree = factureRepository.save(facture);

        devis.setFactureGenereeId(factureEnregistree.getId());
        devisRepository.save(devis);

        return factureEnregistree;
    }

    private String genererNumeroDevis() {
        String prefixe = "DEV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = devisRepository.countAllDevis() + 1;
        return prefixe + String.format("%04d", count);
    }
}
