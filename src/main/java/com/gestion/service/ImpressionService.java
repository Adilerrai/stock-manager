package com.gestion.service;

import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ImpressionService {

    private static final Logger log = LoggerFactory.getLogger(ImpressionService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    private final EntrepriseProfileService entrepriseProfileService;
    private final FactureRepository factureRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final DevisRepository devisRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final CommandeRepository commandeRepository;
    private final AvoirRepository avoirRepository;
    private final VenteRepository venteRepository;

    public ImpressionService(EntrepriseProfileService entrepriseProfileService,
                             FactureRepository factureRepository,
                             BonLivraisonClientRepository bonLivraisonClientRepository,
                             DevisRepository devisRepository,
                             CommandeClientRepository commandeClientRepository,
                             CommandeRepository commandeRepository,
                             AvoirRepository avoirRepository,
                             VenteRepository venteRepository) {
        this.entrepriseProfileService = entrepriseProfileService;
        this.factureRepository = factureRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.devisRepository = devisRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.commandeRepository = commandeRepository;
        this.avoirRepository = avoirRepository;
        this.venteRepository = venteRepository;
    }

    // ==========================================
    // 1. Impression Facture Client
    // ==========================================
    public byte[] genererFacturePdf(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + factureId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroFacture", facture.getNumeroFacture());
        params.put("dateFacture", facture.getDateFacture() != null ? facture.getDateFacture().format(DATE_FORMATTER) : "-");
        params.put("dateEcheance", facture.getDateEcheance() != null ? facture.getDateEcheance().format(DATE_FORMATTER) : "À réception");
        params.put("statutFacture", facture.getStatut() != null ? facture.getStatut().toString() : "EN_ATTENTE");
        params.put("emiseParNom", facture.getEmisePar() != null ? 
                (facture.getEmisePar().getNomComplet() != null ? facture.getEmisePar().getNomComplet() : facture.getEmisePar().getUsername()) : "Direction");

        // Numéros des BLs associés si existants
        if (facture.getBonsLivraison() != null && !facture.getBonsLivraison().isEmpty()) {
            String blNums = facture.getBonsLivraison().stream()
                    .map(BonLivraisonClient::getNumeroBl)
                    .collect(Collectors.joining(", "));
            params.put("bonLivraisonNumeros", blNums);
        } else {
            params.put("bonLivraisonNumeros", "");
        }

        params.put("notes", facture.getNotes());

        // Infos Client
        if (facture.getClient() != null) {
            Client c = facture.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse", c.getAdresse() != null ? c.getAdresse() + " " + (c.getVille() != null ? c.getVille() : "") : "");
            params.put("clientNif", c.getNumeroIdentificationFiscale());
            params.put("clientRc", c.getNumeroRegistreCommerce());
        }

        // Totaux
        params.put("montantHT", facture.getMontantHT());
        params.put("montantTVA", facture.getMontantTVA());
        params.put("montantTTC", facture.getMontantTTC());
        params.put("remiseGlobale", facture.getRemiseGlobale());
        params.put("montantFinal", facture.getMontantFinal());
        params.put("montantPaye", facture.getMontantPaye());
        params.put("montantRestant", facture.getMontantRestant());

        // Lignes
        List<Map<String, Object>> lignes = new ArrayList<>();
        if (facture.getLignes() != null) {
            for (LigneFacture lf : facture.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", lf.getReference() != null ? lf.getReference() : "");
                map.put("designation", lf.getDesignation() != null ? lf.getDesignation() : "");
                map.put("quantite", lf.getQuantite() != null ? lf.getQuantite() : BigDecimal.ZERO);
                map.put("prixUnitaireHT", lf.getPrixUnitaireHT() != null ? lf.getPrixUnitaireHT() : BigDecimal.ZERO);
                map.put("tauxTVA", lf.getTauxTVA() != null ? lf.getTauxTVA() : BigDecimal.ZERO);
                map.put("montantHT", lf.getMontantHT() != null ? lf.getMontantHT() : BigDecimal.ZERO);
                map.put("montantTTC", lf.getMontantTTC() != null ? lf.getMontantTTC() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("facture_client", params, lignes);
    }

    // ==========================================
    // 2. Impression Bon de Livraison Client
    // ==========================================
    public byte[] genererBonLivraisonPdf(Long blId) {
        BonLivraisonClient bl = bonLivraisonClientRepository.findById(blId)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé avec l'id: " + blId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroBl", bl.getNumeroBl());
        params.put("dateBl", bl.getDateBl() != null ? bl.getDateBl().format(DATETIME_FORMATTER) : "-");
        params.put("statut", bl.getStatut() != null ? bl.getStatut().toString() : "LIVRÉ");
        params.put("commandeReference", bl.getCommandeClient() != null ? bl.getCommandeClient().getNumeroCommande() : "-");
        params.put("notes", bl.getObservations());
        params.put("montantTotal", bl.getMontantTotal());

        if (bl.getClient() != null) {
            Client c = bl.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse", c.getAdresse() != null ? c.getAdresse() + " " + (c.getVille() != null ? c.getVille() : "") : "");
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        String premierDepot = "Dépôt Principal";
        if (bl.getLignes() != null) {
            for (LigneBonLivraisonClient lbl : bl.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("produitReference", lbl.getProduit() != null ? lbl.getProduit().getReference() : "");
                map.put("produitDesignation", lbl.getProduit() != null ? lbl.getProduit().getDesignation() : "Article");
                String depotNom = lbl.getDepot() != null ? lbl.getDepot().getNom() : "-";
                map.put("depotNom", depotNom);
                if (lbl.getDepot() != null) premierDepot = lbl.getDepot().getNom();
                map.put("quantiteLivree", lbl.getQuantiteLivree() != null ? lbl.getQuantiteLivree() : BigDecimal.ZERO);
                map.put("prixVente", lbl.getPrixVente() != null ? lbl.getPrixVente() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }
        params.put("depotNom", premierDepot);

        return exportToPdf("bon_livraison_client", params, lignes);
    }

    // ==========================================
    // 3. Impression Devis Client
    // ==========================================
    public byte[] genererDevisPdf(Long devisId) {
        Devis devis = devisRepository.findById(devisId)
                .orElseThrow(() -> new RuntimeException("Devis non trouvé avec l'id: " + devisId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroDevis", devis.getNumeroDevis());
        params.put("dateDevis", devis.getDateDevis() != null ? devis.getDateDevis().format(DATE_FORMATTER) : "-");
        params.put("dateValidite", devis.getDateValidite() != null ? devis.getDateValidite().format(DATE_FORMATTER) : "30 jours");
        params.put("notes", devis.getNotes());
        params.put("montantHT", devis.getMontantHT());
        params.put("montantTVA", devis.getMontantTVA());
        params.put("montantTTC", devis.getMontantFinal() != null ? devis.getMontantFinal() : devis.getMontantTTC());

        if (devis.getClient() != null) {
            Client c = devis.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse", c.getAdresse());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (devis.getLignes() != null) {
            for (LigneDevis ld : devis.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", ld.getProduit() != null ? ld.getProduit().getReference() : "");
                map.put("designation", ld.getProduit() != null ? ld.getProduit().getDesignation() : "Article");
                map.put("quantite", ld.getQuantite() != null ? ld.getQuantite() : BigDecimal.ZERO);
                map.put("prixUnitaireHT", ld.getPrixUnitaireHT() != null ? ld.getPrixUnitaireHT() : BigDecimal.ZERO);
                map.put("tauxTVA", ld.getTauxTVA() != null ? ld.getTauxTVA() : new BigDecimal("19.00"));
                map.put("montantHT", ld.getMontantHT() != null ? ld.getMontantHT() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("devis_client", params, lignes);
    }

    // ==========================================
    // 4. Impression Commande Client
    // ==========================================
    public byte[] genererCommandeClientPdf(Long commandeClientId) {
        CommandeClient commande = commandeClientRepository.findById(commandeClientId)
                .orElseThrow(() -> new RuntimeException("Commande client non trouvée avec l'id: " + commandeClientId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroCommande", commande.getNumeroCommande());
        params.put("dateCommande", commande.getDateCommande() != null ? commande.getDateCommande().format(DATETIME_FORMATTER) : "-");
        params.put("dateLivraisonPrevue", commande.getDateLivraisonPrevue() != null ? commande.getDateLivraisonPrevue().format(DATE_FORMATTER) : "À convenir");
        params.put("statut", commande.getStatut() != null ? commande.getStatut().toString() : "ENREGISTRÉE");
        params.put("notes", commande.getObservations());
        params.put("montantTotal", commande.getMontantTTC() != null ? commande.getMontantTTC() : commande.getMontantHT());
        params.put("acompteVerse", BigDecimal.ZERO);
        params.put("soldeRestant", commande.getMontantTTC() != null ? commande.getMontantTTC() : commande.getMontantHT());

        if (commande.getClient() != null) {
            Client c = commande.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse", c.getAdresse());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (commande.getLignesCommande() != null) {
            for (LigneCommandeClient lc : commande.getLignesCommande()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", lc.getProduit() != null ? lc.getProduit().getReference() : "");
                map.put("designation", lc.getProduit() != null ? lc.getProduit().getDesignation() : "Article");
                map.put("quantite", lc.getQuantite() != null ? lc.getQuantite() : BigDecimal.ZERO);
                map.put("prixUnitaire", lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO);
                map.put("montantTotal", lc.getMontantLigne() != null ? lc.getMontantLigne() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("commande_client", params, lignes);
    }

    // ==========================================
    // 5. Impression Commande Fournisseur
    // ==========================================
    public byte[] genererCommandeFournisseurPdf(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande fournisseur non trouvée avec l'id: " + commandeId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroCommande", commande.getNumeroCommande());
        params.put("dateCommande", commande.getDateCommande() != null ? commande.getDateCommande().format(DATETIME_FORMATTER) : "-");
        params.put("dateLivraisonPrevue", commande.getDateLivraisonPrevue() != null ? commande.getDateLivraisonPrevue().format(DATE_FORMATTER) : "À convenir");
        params.put("statut", commande.getStatut() != null ? commande.getStatut().toString() : "TRANSMISE");
        params.put("observations", commande.getObservations());
        params.put("montantTotal", commande.getMontantTotal());

        if (commande.getFournisseur() != null) {
            Fournisseur f = commande.getFournisseur();
            params.put("fournisseurNom", f.getRaisonSociale());
            params.put("fournisseurTelephone", f.getTelephone());
            params.put("fournisseurAdresse", f.getAdresse());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (commande.getLignesCommande() != null) {
            for (LigneCommande lc : commande.getLignesCommande()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", lc.getProduit() != null ? lc.getProduit().getReference() : "");
                map.put("designation", lc.getProduit() != null ? lc.getProduit().getDesignation() : "Article");
                map.put("quantite", lc.getQuantiteCommandee() != null ? BigDecimal.valueOf(lc.getQuantiteCommandee()) : BigDecimal.ZERO);
                map.put("prixUnitaire", lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO);
                map.put("montantTotal", lc.getMontantLigne() != null ? lc.getMontantLigne() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("commande_fournisseur", params, lignes);
    }

    // ==========================================
    // 6. Impression Avoir Client
    // ==========================================
    public byte[] genererAvoirPdf(Long avoirId) {
        Avoir avoir = avoirRepository.findById(avoirId)
                .orElseThrow(() -> new RuntimeException("Avoir non trouvé avec l'id: " + avoirId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroAvoir", avoir.getNumeroAvoir());
        params.put("dateAvoir", avoir.getDateAvoir() != null ? avoir.getDateAvoir().format(DATE_FORMATTER) : "-");
        params.put("factureOrigineNumero", avoir.getNumeroFactureOrigine() != null ? avoir.getNumeroFactureOrigine() : "-");
        params.put("motif", avoir.getMotif() != null ? avoir.getMotif() : "Retour marchandise");
        params.put("montantHT", avoir.getMontantHT());
        params.put("montantTVA", avoir.getMontantTVA());
        params.put("montantTTC", avoir.getMontantTTC());

        if (avoir.getClient() != null) {
            Client c = avoir.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse", c.getAdresse());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (avoir.getLignes() != null) {
            for (LigneAvoir la : avoir.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", la.getProduit() != null ? la.getProduit().getReference() : "");
                map.put("designation", la.getProduit() != null ? la.getProduit().getDesignation() : "Article");
                map.put("quantite", la.getQuantite() != null ? la.getQuantite() : BigDecimal.ZERO);
                map.put("prixUnitaire", la.getPrixUnitaireHT() != null ? la.getPrixUnitaireHT() : BigDecimal.ZERO);
                map.put("montantTotal", la.getMontantTTC() != null ? la.getMontantTTC() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("avoir_client", params, lignes);
    }

    // ==========================================
    // 7. Impression Ticket Vente POS (80mm)
    // ==========================================
    public byte[] genererTicketVentePdf(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + venteId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroTicket", vente.getNumeroTicket());
        params.put("dateVente", vente.getDateVente() != null ? vente.getDateVente().format(DATETIME_FORMATTER) : "-");
        params.put("vendeurNom", vente.getVendeur() != null ? 
                (vente.getVendeur().getNomComplet() != null ? vente.getVendeur().getNomComplet() : vente.getVendeur().getUsername()) : "Caisse");
        params.put("clientNom", vente.getClient() != null ? vente.getClient().getNomComplet() : "Client Passage");

        params.put("totalHT", vente.getMontantHT());
        params.put("totalTVA", vente.getMontantTVA());
        params.put("totalTTC", vente.getMontantFinal() != null ? vente.getMontantFinal() : vente.getMontantTTC());
        params.put("montantPaye", vente.getMontantPaye() != null ? vente.getMontantPaye() : vente.getMontantFinal());
        params.put("monnaieRendue", BigDecimal.ZERO);

        String modePaiement = "ESPÈCES";
        if (vente.getPaiements() != null && !vente.getPaiements().isEmpty()) {
            Paiement p = vente.getPaiements().get(0);
            if (p.getModePaiement() != null) {
                modePaiement = p.getModePaiement().toString();
            }
        }
        params.put("modePaiement", modePaiement);

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (vente.getLignes() != null) {
            for (LigneVente lv : vente.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("designation", lv.getDesignation() != null ? lv.getDesignation() : "Article");
                map.put("quantite", lv.getQuantite() != null ? lv.getQuantite() : BigDecimal.ONE);
                map.put("prixUnitaireHT", lv.getPrixUnitaireHT() != null ? lv.getPrixUnitaireHT() : BigDecimal.ZERO);
                map.put("montantTTC", lv.getMontantTTC() != null ? lv.getMontantTTC() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("ticket_vente_pos", params, lignes);
    }

    // ==========================================
    // Utilitaires : Paramètres Tenant & Export
    // ==========================================
    private Map<String, Object> initCommonTenantParams() {
        Map<String, Object> params = new HashMap<>();
        EntrepriseProfile profile = entrepriseProfileService.getProfileEntityByCurrentTenant();

        params.put("nomEntreprise", profile.getNomEntreprise() != null ? profile.getNomEntreprise() : "ENTREPRISE SAAS");
        params.put("activiteEntreprise", profile.getActivite() != null ? profile.getActivite() : "");
        params.put("adresseEntreprise", (profile.getAdresse() != null ? profile.getAdresse() : "") + 
                (profile.getVille() != null ? " - " + profile.getVille() : ""));
        params.put("telephoneEntreprise", profile.getTelephone() != null ? profile.getTelephone() : "-");
        params.put("emailEntreprise", profile.getEmail() != null ? profile.getEmail() : "");
        params.put("rcEntreprise", profile.getRegistreCommerce() != null ? profile.getRegistreCommerce() : "-");
        params.put("nifEntreprise", profile.getNumeroIdentificationFiscale() != null ? profile.getNumeroIdentificationFiscale() : "-");
        params.put("nisEntreprise", profile.getNumeroIdentificationStatistique() != null ? profile.getNumeroIdentificationStatistique() : "-");
        params.put("aiEntreprise", profile.getArticleImposition() != null ? profile.getArticleImposition() : "-");
        params.put("ribEntreprise", profile.getCompteBancaireRib() != null ? profile.getCompteBancaireRib() : "-");
        params.put("banqueEntreprise", profile.getNomBanque() != null ? profile.getNomBanque() : "-");
        params.put("piedPage", profile.getPiedPage() != null ? profile.getPiedPage() : "");
        params.put("devise", profile.getDevise() != null ? profile.getDevise() : "DZD");

        // Injection du Logo en java.awt.Image
        if (profile.hasLogo()) {
            try {
                Image logo = ImageIO.read(new ByteArrayInputStream(profile.getLogoData()));
                params.put("logoImage", logo);
            } catch (Exception e) {
                log.warn("Impossible de lire l'image du logo pour le tenant {}: {}", profile.getPointDeVenteId(), e.getMessage());
                params.put("logoImage", null);
            }
        } else {
            params.put("logoImage", null);
        }

        return params;
    }

    private byte[] exportToPdf(String reportName, Map<String, Object> params, List<Map<String, Object>> dataList) {
        try {
            JasperReport report = getCompiledReport(reportName);
            JRDataSource dataSource = (dataList != null && !dataList.isEmpty()) 
                    ? new JRBeanCollectionDataSource(dataList) 
                    : new JREmptyDataSource();
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport {}: {}", reportName, e.getMessage(), e);
            throw new RuntimeException("Erreur de génération du PDF (" + reportName + "): " + e.getMessage(), e);
        }
    }

    private JasperReport getCompiledReport(String reportName) {
        return reportCache.computeIfAbsent(reportName, name -> {
            String path = "reports/" + name + ".jrxml";
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new RuntimeException("Template Jasper introuvable dans le classpath: " + path);
                }
                log.info("Compilation du template Jasper: {}", path);
                return JasperCompileManager.compileReport(is);
            } catch (Exception e) {
                throw new RuntimeException("Échec de la compilation du rapport Jasper " + path + ": " + e.getMessage(), e);
            }
        });
    }
}
