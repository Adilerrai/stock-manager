package com.gestion.service;

import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.gestion.persistent.dto.ModulesAbonnementDTO;
import com.gestion.persistent.dto.PieceCommercialeEnAttenteDTO;
import com.gestion.persistent.dto.RapportDeversementDTO;
import com.gestion.persistent.dto.StatutPasserelleDTO;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.Facture;
import com.gestion.persistent.model.FactureAchat;
import com.gestion.persistent.model.Paiement;
import com.gestion.persistent.model.ReglementFournisseur;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PasserelleComptableService {

    private final FactureRepository factureRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final PaiementRepository paiementRepository;
    private final ReglementFournisseurRepository reglementFournisseurRepository;
    private final EcritureComptableRepository ecritureRepository;
    private final ComptabiliteService comptabiliteService;
    private final PointDeVenteRepository pointDeVenteRepository;

    public PasserelleComptableService(FactureRepository factureRepository,
                                      FactureAchatRepository factureAchatRepository,
                                      PaiementRepository paiementRepository,
                                      ReglementFournisseurRepository reglementFournisseurRepository,
                                      EcritureComptableRepository ecritureRepository,
                                      ComptabiliteService comptabiliteService,
                                      PointDeVenteRepository pointDeVenteRepository) {
        this.factureRepository = factureRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.paiementRepository = paiementRepository;
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.ecritureRepository = ecritureRepository;
        this.comptabiliteService = comptabiliteService;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // STATUT DE LA PASSERELLE (ANALYSE DES FLUX COMMERCIAUX VS COMPTABLES)
    // =========================================================================

    @Transactional(readOnly = true)
    public StatutPasserelleDTO getStatutPasserelle(LocalDate dateDebut, LocalDate dateFin) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.now().withDayOfMonth(1);
        LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();

        LocalDateTime dtDebut = dDebut.atStartOfDay();
        LocalDateTime dtFin = dFin.atTime(LocalTime.MAX);

        StatutPasserelleDTO statut = new StatutPasserelleDTO();
        statut.setDateDebut(dDebut);
        statut.setDateFin(dFin);

        // 1. Factures Ventes
        List<Facture> ventes = factureRepository.findFacturesByPeriodeAndPointDeVenteId(dDebut, dFin, tenantId).stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE && f.getStatut() != StatutFacture.BROUILLON)
                .collect(Collectors.toList());

        statut.setFacturesVentesTotal(ventes.size());
        for (Facture f : ventes) {
            boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(f.getNumeroFacture(), tenantId).isPresent();
            if (deversee) {
                statut.setFacturesVentesDeversees(statut.getFacturesVentesDeversees() + 1);
            } else {
                statut.setFacturesVentesEnAttente(statut.getFacturesVentesEnAttente() + 1);
                statut.setMontantVentesEnAttente(statut.getMontantVentesEnAttente().add(f.getMontantFinal() != null ? f.getMontantFinal() : BigDecimal.ZERO));
            }
        }

        // 2. Factures Achats
        List<FactureAchat> achats = factureAchatRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId).stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE)
                .collect(Collectors.toList());

        statut.setFacturesAchatsTotal(achats.size());
        for (FactureAchat a : achats) {
            boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(a.getNumeroFacture(), tenantId).isPresent();
            if (deversee) {
                statut.setFacturesAchatsDeversees(statut.getFacturesAchatsDeversees() + 1);
            } else {
                statut.setFacturesAchatsEnAttente(statut.getFacturesAchatsEnAttente() + 1);
                statut.setMontantAchatsEnAttente(statut.getMontantAchatsEnAttente().add(a.getMontantTtc() != null ? a.getMontantTtc() : BigDecimal.ZERO));
            }
        }

        // 3. Paiements Clients (Encaissements)
        List<Paiement> paiements = paiementRepository.findPaiementsByPeriodeAndTenant(dtDebut, dtFin, tenantId);
        statut.setPaiementsClientsTotal(paiements.size());
        for (Paiement p : paiements) {
            boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(p.getNumeroPaiement(), tenantId).isPresent();
            if (deversee) {
                statut.setPaiementsClientsDeversees(statut.getPaiementsClientsDeversees() + 1);
            } else {
                statut.setPaiementsClientsEnAttente(statut.getPaiementsClientsEnAttente() + 1);
                statut.setMontantPaiementsEnAttente(statut.getMontantPaiementsEnAttente().add(p.getMontant() != null ? p.getMontant() : BigDecimal.ZERO));
            }
        }

        // 4. Règlements Fournisseurs (Décaissements)
        List<ReglementFournisseur> reglements = reglementFournisseurRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId);
        statut.setReglementsFournisseursTotal(reglements.size());
        for (ReglementFournisseur r : reglements) {
            boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(r.getNumeroReglement(), tenantId).isPresent();
            if (deversee) {
                statut.setReglementsFournisseursDeversees(statut.getReglementsFournisseursDeversees() + 1);
            } else {
                statut.setReglementsFournisseursEnAttente(statut.getReglementsFournisseursEnAttente() + 1);
                statut.setMontantReglementsEnAttente(statut.getMontantReglementsEnAttente().add(r.getMontant() != null ? r.getMontant() : BigDecimal.ZERO));
            }
        }

        int totalAttente = statut.getFacturesVentesEnAttente() + statut.getFacturesAchatsEnAttente()
                + statut.getPaiementsClientsEnAttente() + statut.getReglementsFournisseursEnAttente();
        statut.setTotalPiecesEnAttente(totalAttente);
        statut.setToutEstSynchronise(totalAttente == 0);

        return statut;
    }

    // =========================================================================
    // CONSULTATION DÉTAILLÉE DES PIÈCES COMMERCIALES EN ATTENTE DE DÉVERSEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PieceCommercialeEnAttenteDTO> getPiecesEnAttente(LocalDate dateDebut, LocalDate dateFin, String typeFiltre) {
        Long tenantId = getTenantId();
        LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.now().withDayOfMonth(1);
        LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();

        LocalDateTime dtDebut = dDebut.atStartOfDay();
        LocalDateTime dtFin = dFin.atTime(LocalTime.MAX);

        List<PieceCommercialeEnAttenteDTO> pieces = new ArrayList<>();

        // Ventes
        if (typeFiltre == null || typeFiltre.equalsIgnoreCase("TOUT") || typeFiltre.equalsIgnoreCase("VENTE")) {
            List<Facture> factures = factureRepository.findFacturesByPeriodeAndPointDeVenteId(dDebut, dFin, tenantId).stream()
                    .filter(f -> f.getStatut() != StatutFacture.ANNULEE && f.getStatut() != StatutFacture.BROUILLON)
                    .collect(Collectors.toList());

            for (Facture f : factures) {
                boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(f.getNumeroFacture(), tenantId).isPresent();
                String clientNom = (f.getClient() != null) ? f.getClient().getNom() : "Client";
                pieces.add(new PieceCommercialeEnAttenteDTO(
                        "FACTURE_VENTE",
                        f.getId(),
                        f.getNumeroFacture(),
                        f.getDateFacture(),
                        clientNom,
                        f.getMontantHT(),
                        f.getMontantTVA(),
                        f.getMontantFinal(),
                        "VE",
                        deversee
                ));
            }
        }

        // Achats
        if (typeFiltre == null || typeFiltre.equalsIgnoreCase("TOUT") || typeFiltre.equalsIgnoreCase("ACHAT")) {
            List<FactureAchat> achats = factureAchatRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId).stream()
                    .filter(a -> a.getStatut() != StatutFacture.ANNULEE)
                    .collect(Collectors.toList());

            for (FactureAchat a : achats) {
                boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(a.getNumeroFacture(), tenantId).isPresent();
                String fournisseurNom = (a.getFournisseur() != null) ? a.getFournisseur().getNom() : "Fournisseur";
                LocalDate date = (a.getDateFacture() != null) ? a.getDateFacture().toLocalDate() : LocalDate.now();
                pieces.add(new PieceCommercialeEnAttenteDTO(
                        "FACTURE_ACHAT",
                        a.getId(),
                        a.getNumeroFacture(),
                        date,
                        fournisseurNom,
                        a.getMontantHt(),
                        a.getMontantTva(),
                        a.getMontantTtc(),
                        "AC",
                        deversee
                ));
            }
        }

        // Règlements Clients
        if (typeFiltre == null || typeFiltre.equalsIgnoreCase("TOUT") || typeFiltre.equalsIgnoreCase("ENCAISSEMENT")) {
            List<Paiement> paiements = paiementRepository.findPaiementsByPeriodeAndTenant(dtDebut, dtFin, tenantId);
            for (Paiement p : paiements) {
                boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(p.getNumeroPaiement(), tenantId).isPresent();
                String clientNom = (p.getClient() != null) ? p.getClient().getNom() : "Client";
                String journal = (p.getModePaiement() != null && p.getModePaiement().name().equals("ESPECES")) ? "CA" : "BQ";
                LocalDate date = (p.getDatePaiement() != null) ? p.getDatePaiement().toLocalDate() : LocalDate.now();
                pieces.add(new PieceCommercialeEnAttenteDTO(
                        "ENCAISSEMENT_CLIENT",
                        p.getId(),
                        p.getNumeroPaiement(),
                        date,
                        clientNom,
                        p.getMontant(),
                        BigDecimal.ZERO,
                        p.getMontant(),
                        journal,
                        deversee
                ));
            }
        }

        // Règlements Fournisseurs
        if (typeFiltre == null || typeFiltre.equalsIgnoreCase("TOUT") || typeFiltre.equalsIgnoreCase("DECAISSEMENT")) {
            List<ReglementFournisseur> reglements = reglementFournisseurRepository.findByPeriodeAndPointDeVenteId(dtDebut, dtFin, tenantId);
            for (ReglementFournisseur r : reglements) {
                boolean deversee = ecritureRepository.findByReferencePieceAndPointDeVenteId(r.getNumeroReglement(), tenantId).isPresent();
                String fNom = (r.getFactureAchat() != null && r.getFactureAchat().getFournisseur() != null) ?
                        r.getFactureAchat().getFournisseur().getNom() : "Fournisseur";
                String journal = (r.getModePaiement() != null && r.getModePaiement().name().equals("ESPECES")) ? "CA" : "BQ";
                LocalDate date = (r.getDateReglement() != null) ? r.getDateReglement().toLocalDate() : LocalDate.now();
                pieces.add(new PieceCommercialeEnAttenteDTO(
                        "REGLEMENT_FOURNISSEUR",
                        r.getId(),
                        r.getNumeroReglement(),
                        date,
                        fNom,
                        r.getMontant(),
                        BigDecimal.ZERO,
                        r.getMontant(),
                        journal,
                        deversee
                ));
            }
        }

        return pieces;
    }

    // =========================================================================
    // EXÉCUTION DU DÉVERSEMENT (CHOISIE ET DÉCLENCHÉE PAR L'UTILISATEUR)
    // =========================================================================

    public RapportDeversementDTO deverserVentes(LocalDate dateDebut, LocalDate dateFin, List<Long> factureIds) {
        Long tenantId = getTenantId();
        RapportDeversementDTO rapport = new RapportDeversementDTO();

        List<Facture> cibles;
        if (factureIds != null && !factureIds.isEmpty()) {
            cibles = factureRepository.findAllById(factureIds).stream()
                    .filter(f -> f.getPointDeVenteId().equals(tenantId) && f.getStatut() != StatutFacture.ANNULEE)
                    .collect(Collectors.toList());
        } else {
            LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.now().withDayOfMonth(1);
            LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();
            cibles = factureRepository.findFacturesByPeriodeAndPointDeVenteId(dDebut, dFin, tenantId).stream()
                    .filter(f -> f.getStatut() != StatutFacture.ANNULEE && f.getStatut() != StatutFacture.BROUILLON)
                    .collect(Collectors.toList());
        }

        for (Facture f : cibles) {
            boolean existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(f.getNumeroFacture(), tenantId).isPresent();
            if (!existante) {
                try {
                    comptabiliteService.genererEcritureVente(f);
                    rapport.setNombreVentesDeversees(rapport.getNombreVentesDeversees() + 1);
                    rapport.getPiecesGenerees().add("Vente " + f.getNumeroFacture());
                    rapport.setTotalDebit(rapport.getTotalDebit().add(f.getMontantFinal()));
                    rapport.setTotalCredit(rapport.getTotalCredit().add(f.getMontantFinal()));
                } catch (Exception e) {
                    rapport.getErreurs().add("Erreur déversement facture " + f.getNumeroFacture() + " : " + e.getMessage());
                }
            }
        }

        rapport.setTotalPiecesDeversees(rapport.getNombreVentesDeversees());
        return rapport;
    }

    public RapportDeversementDTO deverserAchats(LocalDate dateDebut, LocalDate dateFin, List<Long> factureAchatIds) {
        Long tenantId = getTenantId();
        RapportDeversementDTO rapport = new RapportDeversementDTO();

        List<FactureAchat> cibles;
        if (factureAchatIds != null && !factureAchatIds.isEmpty()) {
            cibles = factureAchatRepository.findAllById(factureAchatIds).stream()
                    .filter(a -> a.getPointDeVenteId().equals(tenantId) && a.getStatut() != StatutFacture.ANNULEE)
                    .collect(Collectors.toList());
        } else {
            LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.now().withDayOfMonth(1);
            LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();
            cibles = factureAchatRepository.findByPeriodeAndPointDeVenteId(dDebut.atStartOfDay(), dFin.atTime(LocalTime.MAX), tenantId).stream()
                    .filter(a -> a.getStatut() != StatutFacture.ANNULEE)
                    .collect(Collectors.toList());
        }

        for (FactureAchat a : cibles) {
            boolean existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(a.getNumeroFacture(), tenantId).isPresent();
            if (!existante) {
                try {
                    comptabiliteService.genererEcritureAchat(a);
                    rapport.setNombreAchatsDeversees(rapport.getNombreAchatsDeversees() + 1);
                    rapport.getPiecesGenerees().add("Achat " + a.getNumeroFacture());
                    rapport.setTotalDebit(rapport.getTotalDebit().add(a.getMontantTtc()));
                    rapport.setTotalCredit(rapport.getTotalCredit().add(a.getMontantTtc()));
                } catch (Exception e) {
                    rapport.getErreurs().add("Erreur déversement achat " + a.getNumeroFacture() + " : " + e.getMessage());
                }
            }
        }

        rapport.setTotalPiecesDeversees(rapport.getNombreAchatsDeversees());
        return rapport;
    }

    public RapportDeversementDTO deverserPaiementsClients(LocalDate dateDebut, LocalDate dateFin, List<Long> paiementIds) {
        Long tenantId = getTenantId();
        RapportDeversementDTO rapport = new RapportDeversementDTO();

        List<Paiement> cibles;
        if (paiementIds != null && !paiementIds.isEmpty()) {
            cibles = paiementRepository.findAllById(paiementIds).stream()
                    .filter(p -> p.getPointDeVenteId().equals(tenantId) && !Boolean.TRUE.equals(p.getAnnule()))
                    .collect(Collectors.toList());
        } else {
            LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.now().withDayOfMonth(1);
            LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();
            cibles = paiementRepository.findPaiementsByPeriodeAndTenant(dDebut.atStartOfDay(), dFin.atTime(LocalTime.MAX), tenantId);
        }

        for (Paiement p : cibles) {
            boolean existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(p.getNumeroPaiement(), tenantId).isPresent();
            if (!existante) {
                try {
                    comptabiliteService.genererEcriturePaiementClient(p);
                    rapport.setNombrePaiementsClientsDeversees(rapport.getNombrePaiementsClientsDeversees() + 1);
                    rapport.getPiecesGenerees().add("Encaissement " + p.getNumeroPaiement());
                    rapport.setTotalDebit(rapport.getTotalDebit().add(p.getMontant()));
                    rapport.setTotalCredit(rapport.getTotalCredit().add(p.getMontant()));
                } catch (Exception e) {
                    rapport.getErreurs().add("Erreur déversement paiement " + p.getNumeroPaiement() + " : " + e.getMessage());
                }
            }
        }

        rapport.setTotalPiecesDeversees(rapport.getNombrePaiementsClientsDeversees());
        return rapport;
    }

    public RapportDeversementDTO deverserReglementsFournisseurs(LocalDate dateDebut, LocalDate dateFin, List<Long> reglementIds) {
        Long tenantId = getTenantId();
        RapportDeversementDTO rapport = new RapportDeversementDTO();

        List<ReglementFournisseur> cibles;
        if (reglementIds != null && !reglementIds.isEmpty()) {
            cibles = reglementFournisseurRepository.findAllById(reglementIds).stream()
                    .filter(r -> r.getPointDeVenteId().equals(tenantId))
                    .collect(Collectors.toList());
        } else {
            LocalDate dDebut = (dateDebut != null) ? dateDebut : LocalDate.now().withDayOfMonth(1);
            LocalDate dFin = (dateFin != null) ? dateFin : LocalDate.now();
            cibles = reglementFournisseurRepository.findByPeriodeAndPointDeVenteId(dDebut.atStartOfDay(), dFin.atTime(LocalTime.MAX), tenantId);
        }

        for (ReglementFournisseur r : cibles) {
            boolean existante = ecritureRepository.findByReferencePieceAndPointDeVenteId(r.getNumeroReglement(), tenantId).isPresent();
            if (!existante) {
                try {
                    comptabiliteService.genererEcritureReglementFournisseur(r);
                    rapport.setNombreReglementsFournisseursDeversees(rapport.getNombreReglementsFournisseursDeversees() + 1);
                    rapport.getPiecesGenerees().add("Décaissement " + r.getNumeroReglement());
                    rapport.setTotalDebit(rapport.getTotalDebit().add(r.getMontant()));
                    rapport.setTotalCredit(rapport.getTotalCredit().add(r.getMontant()));
                } catch (Exception e) {
                    rapport.getErreurs().add("Erreur déversement règlement " + r.getNumeroReglement() + " : " + e.getMessage());
                }
            }
        }

        rapport.setTotalPiecesDeversees(rapport.getNombreReglementsFournisseursDeversees());
        return rapport;
    }

    public RapportDeversementDTO deverserTout(LocalDate dateDebut, LocalDate dateFin) {
        RapportDeversementDTO rVentes = deverserVentes(dateDebut, dateFin, null);
        RapportDeversementDTO rAchats = deverserAchats(dateDebut, dateFin, null);
        RapportDeversementDTO rPaiements = deverserPaiementsClients(dateDebut, dateFin, null);
        RapportDeversementDTO rReglements = deverserReglementsFournisseurs(dateDebut, dateFin, null);

        RapportDeversementDTO global = new RapportDeversementDTO();
        global.setNombreVentesDeversees(rVentes.getNombreVentesDeversees());
        global.setNombreAchatsDeversees(rAchats.getNombreAchatsDeversees());
        global.setNombrePaiementsClientsDeversees(rPaiements.getNombrePaiementsClientsDeversees());
        global.setNombreReglementsFournisseursDeversees(rReglements.getNombreReglementsFournisseursDeversees());

        global.setTotalPiecesDeversees(rVentes.getTotalPiecesDeversees() + rAchats.getTotalPiecesDeversees()
                + rPaiements.getTotalPiecesDeversees() + rReglements.getTotalPiecesDeversees());

        global.setTotalDebit(rVentes.getTotalDebit().add(rAchats.getTotalDebit()).add(rPaiements.getTotalDebit()).add(rReglements.getTotalDebit()));
        global.setTotalCredit(rVentes.getTotalCredit().add(rAchats.getTotalCredit()).add(rPaiements.getTotalCredit()).add(rReglements.getTotalCredit()));

        global.getPiecesGenerees().addAll(rVentes.getPiecesGenerees());
        global.getPiecesGenerees().addAll(rAchats.getPiecesGenerees());
        global.getPiecesGenerees().addAll(rPaiements.getPiecesGenerees());
        global.getPiecesGenerees().addAll(rReglements.getPiecesGenerees());

        global.getErreurs().addAll(rVentes.getErreurs());
        global.getErreurs().addAll(rAchats.getErreurs());
        global.getErreurs().addAll(rPaiements.getErreurs());
        global.getErreurs().addAll(rReglements.getErreurs());

        return global;
    }

    // =========================================================================
    // GESTION DES MODULES & ABONNEMENTS (COMMERCIAL SEUL, COMPTA SEULE, ERP)
    // =========================================================================

    @Transactional(readOnly = true)
    public ModulesAbonnementDTO getModulesAbonnement() {
        Long tenantId = getTenantId();
        ModulesAbonnementDTO dto = new ModulesAbonnementDTO();
        dto.setTenantId(tenantId);

        pointDeVenteRepository.findById(tenantId).ifPresent(pv -> {
            dto.setNomEntreprise(pv.getNomPointDeVente() != null ? pv.getNomPointDeVente() : pv.getNom());
            dto.setModuleCommercialActif(pv.getModuleCommercialActif());
            dto.setModuleComptabiliteActif(pv.getModuleComptabiliteActif());
            dto.setModuleFiscaliteActif(pv.getModuleFiscaliteActif());

            if (Boolean.TRUE.equals(pv.getModuleCommercialActif()) && Boolean.TRUE.equals(pv.getModuleComptabiliteActif())) {
                dto.setOffreActive("ERP_INTEGRE_COMPLET");
            } else if (Boolean.TRUE.equals(pv.getModuleComptabiliteActif())) {
                dto.setOffreActive("PACK_COMPTABILITE_FIDUCIAIRE");
            } else {
                dto.setOffreActive("PACK_COMMERCIAL");
            }
        });

        return dto;
    }

    public ModulesAbonnementDTO configurerModules(ModulesAbonnementDTO dto) {
        Long tenantId = getTenantId();
        PointDeVente pv = pointDeVenteRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant introuvable ID: " + tenantId));

        if (dto.getModuleCommercialActif() != null) pv.setModuleCommercialActif(dto.getModuleCommercialActif());
        if (dto.getModuleComptabiliteActif() != null) pv.setModuleComptabiliteActif(dto.getModuleComptabiliteActif());
        if (dto.getModuleFiscaliteActif() != null) pv.setModuleFiscaliteActif(dto.getModuleFiscaliteActif());

        pointDeVenteRepository.save(pv);
        return getModulesAbonnement();
    }
}
