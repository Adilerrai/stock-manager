package com.gestion.service;

import com.gestion.persistent.model.*;
import com.gestion.persistent.enums.*;
import com.gestion.repository.*;
import com.gestion.util.FrenchNumberToWords;
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
import java.math.RoundingMode;
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

    static {
        System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
        System.setProperty("net.sf.jasperreports.default.font.name", "SansSerif");
    }

    public static final int LIGNES_PAR_PAGE = 25;

    public void completerLignesVides(List<Map<String, Object>> lignes) {
        if (lignes == null)
            return;
        int act = lignes.size();
        int reste = act % LIGNES_PAR_PAGE;
        int manquant = (reste == 0 && act > 0) ? 0 : (LIGNES_PAR_PAGE - reste);
        for (int i = 0; i < manquant; i++) {
            Map<String, Object> emptyRow = new HashMap<>();
            emptyRow.put("reference", "");
            emptyRow.put("designation", "");
            emptyRow.put("produitReference", "");
            emptyRow.put("produitDesignation", "");
            emptyRow.put("depotNom", "");
            emptyRow.put("quantite", null);
            emptyRow.put("quantiteCommandee", null);
            emptyRow.put("quantiteRecue", null);
            emptyRow.put("quantiteLivree", null);
            emptyRow.put("prixUnitaire", null);
            emptyRow.put("prixUnitaireHT", null);
            emptyRow.put("prixVente", null);
            emptyRow.put("tauxTVA", null);
            emptyRow.put("montantLigne", null);
            emptyRow.put("montantHT", null);
            emptyRow.put("montantTTC", null);
            emptyRow.put("montantTotal", null);
            emptyRow.put("etatStock", "");
            emptyRow.put("disponible", null);
            lignes.add(emptyRow);
        }
    }

    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    private final EntrepriseProfileService entrepriseProfileService;
    private final FactureRepository factureRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final DevisRepository devisRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final CommandeRepository commandeRepository;
    private final AvoirRepository avoirRepository;
    private final VenteRepository venteRepository;
    private final BordereauRemiseRepository bordereauRemiseRepository;
    private final ChequeEffetRepository chequeEffetRepository;
    private final LivraisonRepository livraisonRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final TresorerieService tresorerieService;
    private final TransfertStockRepository transfertStockRepository;
    private final StockRepository stockRepository;

    public ImpressionService(EntrepriseProfileService entrepriseProfileService,
            FactureRepository factureRepository,
            BonLivraisonClientRepository bonLivraisonClientRepository,
            DevisRepository devisRepository,
            CommandeClientRepository commandeClientRepository,
            CommandeRepository commandeRepository,
            AvoirRepository avoirRepository,
            VenteRepository venteRepository,
            BordereauRemiseRepository bordereauRemiseRepository,
            ChequeEffetRepository chequeEffetRepository,
            LivraisonRepository livraisonRepository,
            FactureAchatRepository factureAchatRepository,
            @org.springframework.context.annotation.Lazy TresorerieService tresorerieService,
            TransfertStockRepository transfertStockRepository,
            @org.springframework.context.annotation.Lazy StockRepository stockRepository) {
        this.entrepriseProfileService = entrepriseProfileService;
        this.factureRepository = factureRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.devisRepository = devisRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.commandeRepository = commandeRepository;
        this.avoirRepository = avoirRepository;
        this.venteRepository = venteRepository;
        this.bordereauRemiseRepository = bordereauRemiseRepository;
        this.chequeEffetRepository = chequeEffetRepository;
        this.livraisonRepository = livraisonRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.tresorerieService = tresorerieService;
        this.transfertStockRepository = transfertStockRepository;
        this.stockRepository = stockRepository;
    }

    // ==========================================
    // 1. Impression Facture Client
    // ==========================================
    public byte[] genererFacturePdf(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + factureId));

        if (facture.getStatut() == StatutFacture.BROUILLON || facture.getStatut() == StatutFacture.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer une facture en statut Brouillon ou Annulée. Elle doit être validée.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroFacture", facture.getNumeroFacture());
        params.put("dateFacture",
                facture.getDateFacture() != null ? facture.getDateFacture().format(DATE_FORMATTER) : "-");
        params.put("dateEcheance",
                facture.getDateEcheance() != null ? facture.getDateEcheance().format(DATE_FORMATTER) : "À réception");
        params.put("statutFacture", facture.getStatut() != null ? facture.getStatut().toString() : "EN_ATTENTE");
        params.put("emiseParNom",
                facture.getEmisePar() != null
                        ? (facture.getEmisePar().getNomComplet() != null ? facture.getEmisePar().getNomComplet()
                                : facture.getEmisePar().getUsername())
                        : "Direction");

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
            params.put("clientAdresse",
                    c.getAdresse() != null ? c.getAdresse() + " " + (c.getVille() != null ? c.getVille() : "") : "");
            params.put("clientNif", c.getNumeroIdentificationFiscale());
            params.put("clientRc", c.getNumeroRegistreCommerce());
            params.put("clientIce", c.getIce());
        }

        // Totaux
        params.put("montantHT", facture.getMontantHT());
        params.put("montantTVA", facture.getMontantTVA());
        params.put("montantTTC", facture.getMontantTTC());
        params.put("remiseGlobale", facture.getRemiseGlobale());
        params.put("montantFinal", facture.getMontantFinal());
        params.put("montantPaye", facture.getMontantPaye());
        params.put("montantRestant", facture.getMontantRestant());

        // Mode de paiement si existant
        String modePaiement = "";
        if (facture.getPaiements() != null && !facture.getPaiements().isEmpty()) {
            modePaiement = facture.getPaiements().stream()
                    .map(p -> p.getModePaiement() != null ? p.getModePaiement().toString() : "")
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));
        }
        params.put("modePaiement", !modePaiement.isEmpty() ? modePaiement : "Virement / Chèque");

        BigDecimal netFacture = (facture.getMontantFinal() != null && facture.getMontantFinal().compareTo(BigDecimal.ZERO) > 0)
                ? facture.getMontantFinal()
                : ((facture.getMontantTTC() != null && facture.getMontantTTC().compareTo(BigDecimal.ZERO) > 0)
                        ? facture.getMontantTTC()
                        : (facture.getMontantHT() != null ? facture.getMontantHT() : BigDecimal.ZERO));
        String devise = (String) params.getOrDefault("devise", "MAD");
        params.put("montantEnLettres", FrenchNumberToWords.convertir(netFacture, devise));

        // Lignes
        List<Map<String, Object>> lignes = new ArrayList<>();
        BigDecimal totalMoHT = BigDecimal.ZERO;
        BigDecimal totalMoTTC = BigDecimal.ZERO;
        BigDecimal moTauxTVA = null;
        boolean hasService = false;

        if (facture.getLignes() != null) {
            for (LigneFacture lf : facture.getLignes()) {
                if (isServiceProduit(lf.getProduit(), lf.getDesignation())) {
                    hasService = true;
                    totalMoHT = totalMoHT.add(lf.getMontantHT() != null ? lf.getMontantHT() : BigDecimal.ZERO);
                    totalMoTTC = totalMoTTC.add(lf.getMontantTTC() != null ? lf.getMontantTTC() : BigDecimal.ZERO);
                    if (moTauxTVA == null && lf.getTauxTVA() != null) {
                        moTauxTVA = lf.getTauxTVA();
                    }
                } else {
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
        }

        if (hasService) {
            Map<String, Object> mo = new HashMap<>();
            mo.put("reference", "MO");
            mo.put("designation", "MAIN D'ŒUVRE");
            mo.put("quantite", BigDecimal.ONE);
            mo.put("prixUnitaireHT", totalMoHT);
            mo.put("tauxTVA", moTauxTVA != null ? moTauxTVA : new BigDecimal("20.00"));
            mo.put("montantHT", totalMoHT);
            mo.put("montantTTC", totalMoTTC.compareTo(BigDecimal.ZERO) > 0 ? totalMoTTC : totalMoHT);
            lignes.add(mo);
        }

        return exportToPdf("facture_client", params, lignes);
    }

    // ==========================================
    // 2. Impression Bon de Livraison Client
    // ==========================================
    public byte[] genererBonLivraisonPdf(Long blId) {
        return genererBonLivraisonPdf(blId, true);
    }

    public byte[] genererBonLivraisonPdf(Long blId, Boolean avecPrix) {
        BonLivraisonClient bl = bonLivraisonClientRepository.findById(blId)
                .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé avec l'id: " + blId));

        if (bl.getStatut() == StatutLivraison.BROUILLON || bl.getStatut() == StatutLivraison.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer un bon de livraison en statut Brouillon ou Annulé. Il doit être validé.");
        }

        boolean afficherPrix = avecPrix == null || avecPrix.booleanValue();

        Map<String, Object> params = initCommonTenantParams();
        params.put("afficherPrix", afficherPrix);
        params.put("numeroBl", bl.getNumeroBl());
        params.put("dateBl", bl.getDateBl() != null ? bl.getDateBl().format(DATETIME_FORMATTER) : "-");
        params.put("statut", bl.getStatut() != null ? bl.getStatut().toString() : "LIVRÉ");
        String cmdRef = bl.getCommandeClient() != null ? bl.getCommandeClient().getNumeroCommande() : null;
        if ((cmdRef == null || cmdRef.trim().isEmpty() || "-".equals(cmdRef)) && bl.getCommandes() != null && !bl.getCommandes().isEmpty()) {
            cmdRef = bl.getCommandes().stream()
                    .map(CommandeClient::getNumeroCommande)
                    .filter(ref -> ref != null && !ref.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));
        }
        if (cmdRef == null || cmdRef.trim().isEmpty()) {
            cmdRef = "-";
        }
        params.put("commandeReference", cmdRef);

        String bcClient = null;
        if (bl.getCommandeClient() != null && bl.getCommandeClient().getNumeroCommandeExterne() != null
                && !bl.getCommandeClient().getNumeroCommandeExterne().trim().isEmpty()) {
            bcClient = bl.getCommandeClient().getNumeroCommandeExterne().trim();
        } else if (bl.getCommandes() != null && !bl.getCommandes().isEmpty()) {
            for (CommandeClient c : bl.getCommandes()) {
                if (c != null && c.getNumeroCommandeExterne() != null && !c.getNumeroCommandeExterne().trim().isEmpty()) {
                    bcClient = c.getNumeroCommandeExterne().trim();
                    break;
                }
            }
        }
        params.put("numeroCommandeExterne", bcClient != null ? bcClient : "");
        params.put("bcClient", bcClient != null ? bcClient : "");
        params.put("notes", bl.getObservations());

        if (bl.getClient() != null) {
            Client c = bl.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse",
                    c.getAdresse() != null ? c.getAdresse() + " " + (c.getVille() != null ? c.getVille() : "") : "");
            params.put("clientIce", c.getIce());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        String premierDepot = "Dépôt Principal";
        BigDecimal totalMoPrix = BigDecimal.ZERO;
        BigDecimal totalQuantite = BigDecimal.ZERO;
        BigDecimal totalLignesHT = BigDecimal.ZERO;
        boolean hasService = false;

        if (bl.getLignes() != null) {
            for (LigneBonLivraisonClient lbl : bl.getLignes()) {
                BigDecimal qte = lbl.getQuantiteLivree() != null ? lbl.getQuantiteLivree() : BigDecimal.ZERO;
                BigDecimal pu = lbl.getPrixVente() != null ? lbl.getPrixVente() : BigDecimal.ZERO;
                BigDecimal montantLigne = lbl.getMontantLigne() != null ? lbl.getMontantLigne() : pu.multiply(qte);

                totalQuantite = totalQuantite.add(qte);
                totalLignesHT = totalLignesHT.add(montantLigne);

                if (isServiceProduit(lbl.getProduit(), null)) {
                    hasService = true;
                    totalMoPrix = totalMoPrix.add(pu.multiply(qte.compareTo(BigDecimal.ZERO) > 0 ? qte : BigDecimal.ONE));
                } else {
                    Map<String, Object> map = new HashMap<>();
                    String ref = lbl.getProduit() != null ? lbl.getProduit().getReference() : "";
                    String des = lbl.getProduit() != null ? lbl.getProduit().getDesignation() : "Article";
                    map.put("reference", ref);
                    map.put("produitReference", ref);
                    map.put("designation", des);
                    map.put("produitDesignation", des);

                    String depotNom = lbl.getDepot() != null ? lbl.getDepot().getNom() : "-";
                    map.put("depotNom", depotNom);
                    if (lbl.getDepot() != null)
                        premierDepot = lbl.getDepot().getNom();

                    map.put("quantiteLivree", qte);
                    map.put("quantite", qte);
                    map.put("prixVente", pu);
                    map.put("prixUnitaire", pu);
                    map.put("prixUnitaireHT", pu);
                    map.put("montantLigne", montantLigne);
                    map.put("montantHT", montantLigne);
                    map.put("montantTotal", montantLigne);
                    lignes.add(map);
                }
            }
        }
        if (hasService) {
            Map<String, Object> mo = new HashMap<>();
            mo.put("reference", "MO");
            mo.put("produitReference", "MO");
            mo.put("designation", "MAIN D'ŒUVRE");
            mo.put("produitDesignation", "MAIN D'ŒUVRE");
            mo.put("depotNom", "-");
            mo.put("quantiteLivree", BigDecimal.ONE);
            mo.put("quantite", BigDecimal.ONE);
            mo.put("prixVente", totalMoPrix);
            mo.put("prixUnitaire", totalMoPrix);
            mo.put("prixUnitaireHT", totalMoPrix);
            mo.put("montantLigne", totalMoPrix);
            mo.put("montantHT", totalMoPrix);
            mo.put("montantTotal", totalMoPrix);
            lignes.add(mo);
        }
        params.put("depotNom", premierDepot);
        params.put("totalQuantite", totalQuantite);

        // Calculs des totaux financiers (HT, TVA, TTC)
        BigDecimal montantHT = (bl.getMontantTotal() != null && bl.getMontantTotal().compareTo(BigDecimal.ZERO) > 0)
                ? bl.getMontantTotal()
                : totalLignesHT;

        BigDecimal tauxTva = (bl.getCommandeClient() != null && bl.getCommandeClient().getTauxTVA() != null)
                ? bl.getCommandeClient().getTauxTVA()
                : new BigDecimal("20.00");
        BigDecimal montantTVA = montantHT.multiply(tauxTva).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal montantTTC = montantHT.add(montantTVA);

        params.put("montantHT", montantHT);
        params.put("montantTVA", montantTVA);
        params.put("montantTTC", montantTTC);
        params.put("montantTotal", montantTTC);

        String devise = (String) params.getOrDefault("devise", "MAD");
        params.put("montantEnLettres", FrenchNumberToWords.convertir(montantTTC, devise));

        return exportToPdf("bon_livraison_client", params, lignes);
    }

    // ==========================================
    // 3. Impression Devis Client
    // ==========================================
    public byte[] genererDevisPdf(Long devisId) {
        Devis devis = devisRepository.findById(devisId)
                .orElseThrow(() -> new RuntimeException("Devis non trouvé avec l'id: " + devisId));

        if (devis.getStatut() == StatutDevis.BROUILLON || devis.getStatut() == StatutDevis.REFUSE) {
            throw new IllegalStateException("Impossible d'imprimer un devis en statut Brouillon ou Refusé. Il doit être validé ou envoyé.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroDevis", devis.getNumeroDevis());
        params.put("numeroAchatClient", devis.getNumeroAchatClient() != null ? devis.getNumeroAchatClient() : "");
        params.put("dateDevis", devis.getDateDevis() != null ? devis.getDateDevis().format(DATE_FORMATTER) : "-");
        params.put("dateValidite",
                devis.getDateValidite() != null ? devis.getDateValidite().format(DATE_FORMATTER) : "30 jours");
        params.put("notes", devis.getNotes());
        params.put("montantHT", devis.getMontantHT());
        params.put("montantTVA", devis.getMontantTVA());
        params.put("montantTTC", devis.getMontantFinal() != null ? devis.getMontantFinal() : devis.getMontantTTC());

        String etabliPar = "-";
        if (devis.getCreePar() != null) {
            etabliPar = (devis.getCreePar().getNomComplet() != null && !devis.getCreePar().getNomComplet().isBlank())
                    ? devis.getCreePar().getNomComplet()
                    : devis.getCreePar().getUsername();
        }
        params.put("etabliPar", etabliPar);
        params.put("creeParNom", etabliPar);

        if (devis.getClient() != null) {
            Client c = devis.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse", c.getAdresse());
            params.put("clientIce", c.getIce());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        BigDecimal totalMoHT = BigDecimal.ZERO;
        BigDecimal moTauxTVA = null;
        boolean hasService = false;

        if (devis.getLignes() != null) {
            for (LigneDevis ld : devis.getLignes()) {
                String etatStock = ld.getEtatStock();
                Boolean disponible = ld.getDisponible();

                if (etatStock == null || etatStock.trim().isEmpty()) {
                    if (ld.getProduit() != null && ld.getProduit().getId() != null) {
                        Optional<Stock> stockOpt = stockRepository.findByProduitId(ld.getProduit().getId());
                        BigDecimal stockDispo = stockOpt.map(Stock::getQuantiteDisponible).orElse(BigDecimal.ZERO);
                        BigDecimal qte = ld.getQuantite() != null ? ld.getQuantite() : BigDecimal.ZERO;
                        if (stockDispo.compareTo(qte) >= 0 && stockDispo.compareTo(BigDecimal.ZERO) > 0) {
                            etatStock = "En stock";
                            disponible = true;
                        } else if (stockDispo.compareTo(BigDecimal.ZERO) > 0) {
                            etatStock = "Partiel (" + stockDispo.stripTrailingZeros().toPlainString() + ")";
                            disponible = false;
                        } else {
                            etatStock = "Sur commande";
                            disponible = false;
                        }
                    } else {
                        etatStock = "En stock";
                        disponible = true;
                    }
                }

                if (isServiceProduit(ld.getProduit(), ld.getDescription())) {
                    hasService = true;
                    totalMoHT = totalMoHT.add(ld.getMontantHT() != null ? ld.getMontantHT() : BigDecimal.ZERO);
                    if (moTauxTVA == null && ld.getTauxTVA() != null) {
                        moTauxTVA = ld.getTauxTVA();
                    }
                } else {
                    Map<String, Object> map = new HashMap<>();
                    map.put("reference", ld.getProduit() != null ? ld.getProduit().getReference() : "");
                    map.put("designation", ld.getProduit() != null ? ld.getProduit().getDesignation() : "Article");
                    map.put("quantite", ld.getQuantite() != null ? ld.getQuantite() : BigDecimal.ZERO);
                    map.put("prixUnitaireHT", ld.getPrixUnitaireHT() != null ? ld.getPrixUnitaireHT() : BigDecimal.ZERO);
                    map.put("tauxTVA", ld.getTauxTVA() != null ? ld.getTauxTVA() : new BigDecimal("20.00"));
                    map.put("montantHT", ld.getMontantHT() != null ? ld.getMontantHT() : BigDecimal.ZERO);
                    map.put("etatStock", etatStock);
                    map.put("disponible", disponible);
                    lignes.add(map);
                }
            }
        }
        if (hasService) {
            Map<String, Object> mo = new HashMap<>();
            mo.put("reference", "MO");
            mo.put("designation", "MAIN D'ŒUVRE");
            mo.put("quantite", BigDecimal.ONE);
            mo.put("prixUnitaireHT", totalMoHT);
            mo.put("tauxTVA", moTauxTVA != null ? moTauxTVA : new BigDecimal("20.00"));
            mo.put("montantHT", totalMoHT);
            mo.put("etatStock", "Disponible");
            mo.put("disponible", true);
            lignes.add(mo);
        }

        return exportToPdf("devis_client", params, lignes);
    }

    // ==========================================
    // 4. Impression Commande Client
    // ==========================================
    public byte[] genererCommandeClientPdf(Long commandeClientId) {
        CommandeClient commande = commandeClientRepository.findById(commandeClientId)
                .orElseThrow(() -> new RuntimeException("Commande client non trouvée avec l'id: " + commandeClientId));

        if (commande.getStatut() == StatutCommandeClient.BROUILLON || commande.getStatut() == StatutCommandeClient.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer une commande client en statut Brouillon ou Annulée. Elle doit être confirmée.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroCommande", commande.getNumeroCommande());
        params.put("numeroCommandeExterne", commande.getNumeroCommandeExterne() != null ? commande.getNumeroCommandeExterne() : "");
        params.put("dateCommande",
                commande.getDateCommande() != null ? commande.getDateCommande().format(DATETIME_FORMATTER) : "-");
        params.put("dateLivraisonPrevue",
                commande.getDateLivraisonPrevue() != null ? commande.getDateLivraisonPrevue().format(DATE_FORMATTER)
                        : "À convenir");
        params.put("statut", commande.getStatut() != null ? commande.getStatut().toString() : "ENREGISTRÉE");
        params.put("notes", commande.getObservations());

        BigDecimal ht = commande.getMontantHT() != null ? commande.getMontantHT() : BigDecimal.ZERO;
        BigDecimal ttc = commande.getMontantTTC() != null ? commande.getMontantTTC() : ht;
        BigDecimal tva = ttc.subtract(ht);
        if (tva.compareTo(BigDecimal.ZERO) <= 0 && ht.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taux = commande.getTauxTVA() != null ? commande.getTauxTVA() : new BigDecimal("20.00");
            tva = ht.multiply(taux).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            ttc = ht.add(tva);
        }

        params.put("montantHT", ht);
        params.put("montantTVA", tva);
        params.put("montantTTC", ttc);
        params.put("montantTotal", ttc);
        params.put("acompteVerse", BigDecimal.ZERO);
        params.put("soldeRestant", ttc);

        String devise = (String) params.getOrDefault("devise", "MAD");
        params.put("montantEnLettres", FrenchNumberToWords.convertir(ttc, devise));

        if (commande.getClient() != null) {
            Client c = commande.getClient();
            params.put("clientNom", c.getNomComplet());
            params.put("clientTelephone", c.getTelephone());
            params.put("clientAdresse",
                    c.getAdresse() != null ? c.getAdresse() + " " + (c.getVille() != null ? c.getVille() : "") : "");
            params.put("clientIce", c.getIce());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        BigDecimal totalMo = BigDecimal.ZERO;
        boolean hasService = false;
        BigDecimal tauxTvaCmd = commande.getTauxTVA() != null ? commande.getTauxTVA() : new BigDecimal("20.00");

        if (commande.getLignesCommande() != null) {
            for (LigneCommandeClient lc : commande.getLignesCommande()) {
                if (isServiceProduit(lc.getProduit(), null)) {
                    hasService = true;
                    totalMo = totalMo.add(lc.getMontantLigne() != null ? lc.getMontantLigne() : BigDecimal.ZERO);
                } else {
                    Map<String, Object> map = new HashMap<>();
                    String ref = lc.getProduit() != null ? lc.getProduit().getReference() : "";
                    String des = lc.getProduit() != null ? lc.getProduit().getDesignation() : "Article";
                    BigDecimal qte = lc.getQuantite() != null ? lc.getQuantite() : BigDecimal.ZERO;
                    BigDecimal pu = lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO;
                    BigDecimal mtLigne = lc.getMontantLigne() != null ? lc.getMontantLigne() : pu.multiply(qte);

                    map.put("reference", ref);
                    map.put("designation", des);
                    map.put("quantite", qte);
                    map.put("prixUnitaire", pu);
                    map.put("prixUnitaireHT", pu);
                    map.put("montantTotal", mtLigne);
                    map.put("montantHT", mtLigne);
                    map.put("tauxTVA", tauxTvaCmd);
                    BigDecimal mtTtcLigne = mtLigne.add(mtLigne.multiply(tauxTvaCmd).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                    map.put("montantTTC", mtTtcLigne);
                    lignes.add(map);
                }
            }
        }
        if (hasService) {
            Map<String, Object> mo = new HashMap<>();
            mo.put("reference", "MO");
            mo.put("designation", "MAIN D'ŒUVRE");
            mo.put("quantite", BigDecimal.ONE);
            mo.put("prixUnitaire", totalMo);
            mo.put("prixUnitaireHT", totalMo);
            mo.put("montantTotal", totalMo);
            mo.put("montantHT", totalMo);
            mo.put("tauxTVA", tauxTvaCmd);
            BigDecimal moTtc = totalMo.add(totalMo.multiply(tauxTvaCmd).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            mo.put("montantTTC", moTtc);
            lignes.add(mo);
        }

        return exportToPdf("commande_client", params, lignes);
    }

    // ==========================================
    // 5. Impression Commande Fournisseur
    // ==========================================
    public byte[] genererCommandeFournisseurPdf(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande fournisseur non trouvée avec l'id: " + commandeId));

        if (commande.getStatut() == StatutCommande.BROUILLON || commande.getStatut() == StatutCommande.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer une commande fournisseur en statut Brouillon ou Annulée. Elle doit être validée.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroCommande", commande.getNumeroCommande());
        params.put("dateCommande",
                commande.getDateCommande() != null ? commande.getDateCommande().format(DATETIME_FORMATTER) : "-");
        params.put("dateLivraisonPrevue",
                commande.getDateLivraisonPrevue() != null ? commande.getDateLivraisonPrevue().format(DATE_FORMATTER)
                        : "À convenir");
        params.put("statut", commande.getStatut() != null ? commande.getStatut().toString() : "TRANSMISE");
        params.put("observations", commande.getObservations());
        BigDecimal total = commande.getMontantTotal() != null ? commande.getMontantTotal() : BigDecimal.ZERO;
        BigDecimal ht = commande.getMontantHT() != null ? commande.getMontantHT() : total;
        BigDecimal tva = commande.getMontantTVA() != null ? commande.getMontantTVA() : BigDecimal.ZERO;
        BigDecimal ttc = commande.getMontantTTC() != null ? commande.getMontantTTC() 
                : (total.compareTo(BigDecimal.ZERO) > 0 ? total : ht.add(tva));

        params.put("montantTotal", total);
        params.put("montantHT", ht);
        params.put("montantTVA", tva);
        params.put("montantTTC", ttc);
        String devise = commande.getDevise() != null && !commande.getDevise().isBlank() 
                ? commande.getDevise() 
                : (commande.getFournisseur() != null && commande.getFournisseur().getDevise() != null && !commande.getFournisseur().getDevise().isBlank()
                        ? commande.getFournisseur().getDevise()
                        : (String) params.getOrDefault("devise", "MAD"));
        params.put("devise", devise);

        boolean isInternational = (commande.getFournisseur() != null && commande.getFournisseur().getTypeFournisseur() == TypeFournisseur.INTERNATIONAL)
                || (!"MAD".equalsIgnoreCase(devise));
        params.put("isInternational", isInternational);

        if (commande.getFournisseur() != null) {
            Fournisseur f = commande.getFournisseur();
            params.put("fournisseurNom", f.getRaisonSociale());
            params.put("fournisseurTelephone", f.getTelephone());
            params.put("fournisseurAdresse", f.getAdresse());
            params.put("fournisseurIce", f.getIce());
            params.put("fournisseurPays", f.getPays());
            params.put("fournisseurVille", f.getVille());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (commande.getLignesCommande() != null) {
            for (LigneCommande lc : commande.getLignesCommande()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", lc.getProduit() != null ? lc.getProduit().getReference() : "");
                map.put("designation", lc.getProduit() != null ? lc.getProduit().getDesignation() : "Article");
                map.put("quantite", lc.getQuantiteCommandee() != null ? BigDecimal.valueOf(lc.getQuantiteCommandee())
                        : BigDecimal.ZERO);

                BigDecimal pu = lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO;
                BigDecimal puHT = lc.getPrixUnitaireHT() != null ? lc.getPrixUnitaireHT() : pu;
                BigDecimal tauxTva = lc.getTauxTVA() != null ? lc.getTauxTVA() 
                        : (commande.getTauxTVA() != null ? commande.getTauxTVA() : BigDecimal.ZERO);
                BigDecimal mtHT = lc.getMontantHT() != null ? lc.getMontantHT() : lc.getMontantLigne();
                BigDecimal mtTTC = lc.getMontantTTC() != null ? lc.getMontantTTC() : lc.getMontantLigne();
                BigDecimal mtTotal = lc.getMontantLigne() != null ? lc.getMontantLigne() : (mtHT != null ? mtHT : BigDecimal.ZERO);

                map.put("prixUnitaire", pu);
                map.put("prixUnitaireHT", puHT);
                map.put("tauxTVA", tauxTva);
                map.put("montantHT", mtHT);
                map.put("montantTTC", mtTTC);
                map.put("montantTotal", mtTotal);
                map.put("montantLigne", mtTotal);
                lignes.add(map);
            }
        }

        return exportToPdf("commande_fournisseur", params, lignes);
    }

    // ==========================================
    // 5b. Impression Bon de Réception Fournisseur (Achat)
    // ==========================================
    public byte[] genererBonReceptionPdf(Long livraisonId) {
        Livraison livraison = livraisonRepository.findById(livraisonId)
                .orElseThrow(() -> new RuntimeException("Livraison / Réception non trouvée avec l'id: " + livraisonId));

        if (livraison.getStatut() == StatutLivraison.BROUILLON || livraison.getStatut() == StatutLivraison.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer un bon de réception en statut Brouillon ou Annulé.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroReception", livraison.getNumeroLivraison() != null ? livraison.getNumeroLivraison() : "REC-" + livraisonId);
        params.put("dateReception", livraison.getDateLivraison() != null ? livraison.getDateLivraison().format(DATETIME_FORMATTER) : "-");
        params.put("statut", livraison.getStatut() != null ? livraison.getStatut().toString() : "VALIDÉE");
        params.put("commandeReference", (livraison.getCommande() != null && livraison.getCommande().getNumeroCommande() != null)
                ? livraison.getCommande().getNumeroCommande() : "Direct");
        params.put("transporteur", livraison.getTransporteur() != null ? livraison.getTransporteur() : "-");
        params.put("numeroSuivi", livraison.getNumeroSuivi() != null ? livraison.getNumeroSuivi() : "");
        params.put("observations", livraison.getObservations() != null ? livraison.getObservations() : "");
        params.put("montantTotal", livraison.getMontantTotal() != null ? livraison.getMontantTotal() : BigDecimal.ZERO);

        Fournisseur f = (livraison.getCommande() != null) ? livraison.getCommande().getFournisseur() : null;
        if (f != null) {
            params.put("fournisseurNom", f.getRaisonSociale() != null ? f.getRaisonSociale() : f.getNom());
            params.put("fournisseurTelephone", f.getTelephone());
            params.put("fournisseurAdresse", f.getAdresse());
            params.put("fournisseurIce", f.getIce());
            params.put("fournisseurPays", f.getPays());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (livraison.getLignesLivraison() != null) {
            for (LigneLivraison ll : livraison.getLignesLivraison()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", ll.getProduit() != null ? ll.getProduit().getReference() : "");
                map.put("designation", ll.getProduit() != null ? ll.getProduit().getDesignation() : "Article");
                map.put("quantiteCommandee", BigDecimal.valueOf(ll.getQuantiteLivree() != null ? ll.getQuantiteLivree() : 0));
                map.put("quantiteRecue", BigDecimal.valueOf(ll.getQuantiteLivree() != null ? ll.getQuantiteLivree() : 0));
                map.put("prixUnitaire", ll.getPrixProduit() != null ? ll.getPrixProduit() : BigDecimal.ZERO);
                BigDecimal total = (ll.getPrixProduit() != null && ll.getQuantiteLivree() != null)
                        ? ll.getPrixProduit().multiply(BigDecimal.valueOf(ll.getQuantiteLivree())) : BigDecimal.ZERO;
                map.put("montantTotal", total);
                lignes.add(map);
            }
        }

        return exportToPdf("bon_reception", params, lignes);
    }

    // ==========================================
    // 5c. Impression Facture d'Achat Fournisseur
    // ==========================================
    public byte[] genererFactureAchatPdf(Long factureAchatId) {
        FactureAchat facture = factureAchatRepository.findById(factureAchatId)
                .orElseThrow(() -> new RuntimeException("Facture d'achat non trouvée avec l'id: " + factureAchatId));

        if (facture.getStatut() == StatutFacture.BROUILLON || facture.getStatut() == StatutFacture.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer une facture d'achat en statut Brouillon ou Annulée. Elle doit être validée.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroFacture", facture.getNumeroFacture());
        params.put("dateFacture", facture.getDateFacture() != null ? facture.getDateFacture().format(DATE_FORMATTER) : "-");
        params.put("dateEcheance", facture.getDateEcheance() != null ? facture.getDateEcheance().format(DATE_FORMATTER) : "À réception");
        params.put("statutFacture", facture.getStatut() != null ? facture.getStatut().toString() : "EN_ATTENTE");
        params.put("observations", facture.getObservations() != null ? facture.getObservations() : "");
        params.put("montantHT", facture.getMontantHt() != null ? facture.getMontantHt() : BigDecimal.ZERO);
        params.put("montantTVA", facture.getMontantTva() != null ? facture.getMontantTva() : BigDecimal.ZERO);
        params.put("montantTTC", facture.getMontantTtc() != null ? facture.getMontantTtc() : BigDecimal.ZERO);

        String devise = (String) params.getOrDefault("devise", "MAD");
        params.put("montantEnLettres", FrenchNumberToWords.convertir(facture.getMontantTtc() != null ? facture.getMontantTtc() : BigDecimal.ZERO, devise));

        if (facture.getFournisseur() != null) {
            Fournisseur f = facture.getFournisseur();
            params.put("fournisseurNom", f.getRaisonSociale() != null ? f.getRaisonSociale() : f.getNom());
            params.put("fournisseurTelephone", f.getTelephone());
            params.put("fournisseurAdresse", f.getAdresse());
            params.put("fournisseurIce", f.getIce());
            params.put("fournisseurNif", f.getNumeroIdentificationFiscale());
            params.put("fournisseurRc", f.getNumeroRegistreCommerce());
        }

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (facture.getLignes() != null) {
            for (LigneFactureAchat lfa : facture.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", lfa.getProduit() != null ? lfa.getProduit().getReference() : "");
                map.put("designation", lfa.getProduit() != null ? lfa.getProduit().getDesignation() : "Article");
                map.put("quantite", lfa.getQuantite() != null ? lfa.getQuantite() : BigDecimal.ZERO);
                map.put("prixUnitaireHT", lfa.getPrixUnitaireHt() != null ? lfa.getPrixUnitaireHt() : BigDecimal.ZERO);
                map.put("tauxTVA", lfa.getTauxTva() != null ? lfa.getTauxTva() : BigDecimal.ZERO);
                map.put("montantHT", lfa.getMontantHt() != null ? lfa.getMontantHt() : BigDecimal.ZERO);
                map.put("montantTTC", lfa.getMontantTtc() != null ? lfa.getMontantTtc() : BigDecimal.ZERO);
                lignes.add(map);
            }
        }

        return exportToPdf("facture_achat", params, lignes);
    }

    // ==========================================
    // 6. Impression Avoir Client
    // ==========================================
    public byte[] genererAvoirPdf(Long avoirId) {
        Avoir avoir = avoirRepository.findById(avoirId)
                .orElseThrow(() -> new RuntimeException("Avoir non trouvé avec l'id: " + avoirId));

        if (avoir.getStatut() == StatutAvoir.BROUILLON || avoir.getStatut() == StatutAvoir.ANNULE) {
            throw new IllegalStateException("Impossible d'imprimer un avoir en statut Brouillon ou Annulé. Il doit être validé.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroAvoir", avoir.getNumeroAvoir());
        params.put("dateAvoir", avoir.getDateAvoir() != null ? avoir.getDateAvoir().format(DATE_FORMATTER) : "-");
        params.put("factureOrigineNumero",
                avoir.getNumeroFactureOrigine() != null ? avoir.getNumeroFactureOrigine() : "-");
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
        BigDecimal totalMoHT = BigDecimal.ZERO;
        BigDecimal totalMoTTC = BigDecimal.ZERO;
        boolean hasService = false;

        if (avoir.getLignes() != null) {
            for (LigneAvoir la : avoir.getLignes()) {
                if (isServiceProduit(la.getProduit(), la.getDesignation())) {
                    hasService = true;
                    BigDecimal mt = la.getMontantTTC() != null ? la.getMontantTTC() : BigDecimal.ZERO;
                    totalMoTTC = totalMoTTC.add(mt);
                    totalMoHT = totalMoHT.add(la.getPrixUnitaireHT() != null ? la.getPrixUnitaireHT() : mt);
                } else {
                    Map<String, Object> map = new HashMap<>();
                    map.put("reference", la.getProduit() != null ? la.getProduit().getReference() : "");
                    map.put("designation", la.getProduit() != null ? la.getProduit().getDesignation() : "Article");
                    map.put("quantite", la.getQuantite() != null ? la.getQuantite() : BigDecimal.ZERO);
                    map.put("prixUnitaire", la.getPrixUnitaireHT() != null ? la.getPrixUnitaireHT() : BigDecimal.ZERO);
                    map.put("montantTotal", la.getMontantTTC() != null ? la.getMontantTTC() : BigDecimal.ZERO);
                    lignes.add(map);
                }
            }
        }
        if (hasService) {
            Map<String, Object> mo = new HashMap<>();
            mo.put("reference", "MO");
            mo.put("designation", "MAIN D'ŒUVRE");
            mo.put("quantite", BigDecimal.ONE);
            mo.put("prixUnitaire", totalMoHT);
            mo.put("montantTotal", totalMoTTC.compareTo(BigDecimal.ZERO) > 0 ? totalMoTTC : totalMoHT);
            lignes.add(mo);
        }

        return exportToPdf("avoir_client", params, lignes);
    }

    // ==========================================
    // 7. Impression Ticket Vente POS (80mm)
    // ==========================================
    public byte[] genererTicketVentePdf(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id: " + venteId));

        if (vente.getStatut() == StatutVente.ANNULEE) {
            throw new IllegalStateException("Impossible d'imprimer le ticket d'une vente annulée.");
        }

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroTicket", vente.getNumeroTicket());
        params.put("dateVente", vente.getDateVente() != null ? vente.getDateVente().format(DATETIME_FORMATTER) : "-");
        params.put("vendeurNom",
                vente.getVendeur() != null
                        ? (vente.getVendeur().getNomComplet() != null ? vente.getVendeur().getNomComplet()
                                : vente.getVendeur().getUsername())
                        : "Caisse");
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
        BigDecimal totalMoHT = BigDecimal.ZERO;
        BigDecimal totalMoTTC = BigDecimal.ZERO;
        boolean hasService = false;

        if (vente.getLignes() != null) {
            for (LigneVente lv : vente.getLignes()) {
                if (isServiceProduit(lv.getProduit(), lv.getDesignation())) {
                    hasService = true;
                    totalMoHT = totalMoHT.add(lv.getMontantHT() != null ? lv.getMontantHT() : BigDecimal.ZERO);
                    totalMoTTC = totalMoTTC.add(lv.getMontantTTC() != null ? lv.getMontantTTC() : BigDecimal.ZERO);
                } else {
                    Map<String, Object> map = new HashMap<>();
                    map.put("designation", lv.getDesignation() != null ? lv.getDesignation()
                            : (lv.getProduit() != null ? lv.getProduit().getDesignation() : "Article"));
                    map.put("quantite", lv.getQuantite() != null ? lv.getQuantite() : BigDecimal.ONE);
                    map.put("prixUnitaireHT", lv.getPrixUnitaireHT() != null ? lv.getPrixUnitaireHT() : BigDecimal.ZERO);
                    map.put("montantTTC", lv.getMontantTTC() != null ? lv.getMontantTTC() : BigDecimal.ZERO);
                    lignes.add(map);
                }
            }
        }
        if (hasService) {
            Map<String, Object> mo = new HashMap<>();
            mo.put("designation", "MAIN D'ŒUVRE");
            mo.put("quantite", BigDecimal.ONE);
            mo.put("prixUnitaireHT", totalMoHT);
            mo.put("montantTTC", totalMoTTC.compareTo(BigDecimal.ZERO) > 0 ? totalMoTTC : totalMoHT);
            lignes.add(mo);
        }

        return exportToPdf("ticket_vente_pos", params, lignes);
    }

    private boolean isServiceProduit(Produit p, String designation) {
        if (p != null && p.getTypeProduit() == TypeProduit.SERVICE) {
            return true;
        }
        if (designation != null) {
            String d = designation.trim().toUpperCase();
            return d.equals("MAIN D'OEUVRE") || d.equals("MAIN D'ŒUVRE") || d.contains("MAIN D'OEUVRE") || d.contains("MAIN D'ŒUVRE");
        }
        return false;
    }

    // ==========================================
    // 8. Impression Bordereau de Remise Chèques
    // ==========================================
    public byte[] genererBordereauRemisePdf(Long bordereauId) {
        BordereauRemise bordereau = bordereauRemiseRepository.findById(bordereauId)
                .orElseThrow(() -> new RuntimeException("Bordereau de remise introuvable: " + bordereauId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroBordereau", bordereau.getNumeroBordereau() != null ? bordereau.getNumeroBordereau() : "-");
        params.put("dateRemise", bordereau.getDateRemise() != null ? bordereau.getDateRemise().format(DATE_FORMATTER)
                : java.time.LocalDate.now().format(DATE_FORMATTER));
        params.put("nomBanqueDepot", bordereau.getNomBanque() != null ? bordereau.getNomBanque() : "-");
        params.put("compteBancaire", bordereau.getCompteBancaire() != null ? bordereau.getCompteBancaire() : "-");
        params.put("nombreCheques", bordereau.getNombreValeurs() != null ? bordereau.getNombreValeurs() : 0);
        params.put("montantTotal", bordereau.getMontantTotal() != null ? bordereau.getMontantTotal() : BigDecimal.ZERO);

        List<ChequeEffet> cheques = chequeEffetRepository.findByBordereauRemiseId(bordereauId);
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (ChequeEffet c : cheques) {
            Map<String, Object> m = new HashMap<>();
            m.put("numeroPiece", c.getNumeroPiece());
            m.put("tireur", c.getTireur() != null ? c.getTireur() : "-");
            m.put("banqueEmettrice", c.getBanqueEmettrice() != null ? c.getBanqueEmettrice() : "-");
            m.put("dateEcheance", c.getDateEcheance() != null ? c.getDateEcheance().format(DATE_FORMATTER) : "-");
            m.put("montant", c.getMontant() != null ? c.getMontant() : BigDecimal.ZERO);
            lignes.add(m);
        }

        return exportToPdf("bordereau_remise", params, lignes);
    }

    // ==========================================
    // 9. Impression Relevé de Compte Client
    // ==========================================
    public byte[] genererReleveClientPdf(Long clientId, java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        com.gestion.persistent.dto.ReleveClientDTO releve = tresorerieService.genererReleveClient(clientId, dateDebut,
                dateFin);

        Map<String, Object> params = initCommonTenantParams();
        params.put("clientNom", releve.getClientNom() != null ? releve.getClientNom() : "Client");
        params.put("clientTelephone", releve.getTelephone() != null ? releve.getTelephone() : "-");
        params.put("clientAdresse", releve.getEmail() != null ? releve.getEmail() : "-");
        params.put("clientIce", releve.getIce() != null ? releve.getIce() : "-");

        String periodeStr = "Au " + java.time.LocalDate.now().format(DATE_FORMATTER);
        if (dateDebut != null && dateFin != null) {
            periodeStr = "Du " + dateDebut.format(DATE_FORMATTER) + " au " + dateFin.format(DATE_FORMATTER);
        } else if (dateDebut != null) {
            periodeStr = "À partir du " + dateDebut.format(DATE_FORMATTER);
        }
        params.put("periode", periodeStr);
        params.put("totalDebit", releve.getTotalFactures() != null ? releve.getTotalFactures() : BigDecimal.ZERO);
        params.put("totalCredit", releve.getTotalPaiements() != null ? releve.getTotalPaiements() : BigDecimal.ZERO);
        params.put("soldeFinal", releve.getSoldeActuel() != null ? releve.getSoldeActuel() : BigDecimal.ZERO);

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (releve.getOperations() != null) {
            for (com.gestion.persistent.dto.ReleveClientDTO.LigneReleveDTO op : releve.getOperations()) {
                Map<String, Object> m = new HashMap<>();
                m.put("datePiece", op.getDate() != null ? op.getDate().format(DATE_FORMATTER) : "-");
                m.put("referencePiece", op.getReference() != null ? op.getReference() : "-");
                m.put("libelle", op.getLibelle() != null ? op.getLibelle() : "-");
                m.put("debit", op.getDebit() != null ? op.getDebit() : BigDecimal.ZERO);
                m.put("credit", op.getCredit() != null ? op.getCredit() : BigDecimal.ZERO);
                m.put("soldeProgressif", op.getSoldeProgressif() != null ? op.getSoldeProgressif() : BigDecimal.ZERO);
                lignes.add(m);
            }
        }

        return exportToPdf("releve_compte_client", params, lignes);
    }

    // ==========================================
    // 9b. Impression Relevé de Compte Fournisseur (Achat)
    // ==========================================
    public byte[] genererReleveFournisseurPdf(Long fournisseurId, java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        com.gestion.persistent.dto.ReleveFournisseurDTO releve = tresorerieService.genererReleveFournisseur(fournisseurId, dateDebut,
                dateFin);

        Map<String, Object> params = initCommonTenantParams();
        params.put("fournisseurNom", releve.getFournisseurNom() != null ? releve.getFournisseurNom() : "Fournisseur");
        params.put("fournisseurTelephone", releve.getTelephone() != null ? releve.getTelephone() : "-");
        params.put("fournisseurAdresse", releve.getEmail() != null ? releve.getEmail() : "-");
        params.put("fournisseurIce", releve.getIce() != null ? releve.getIce() : "-");

        String periodeStr = "Au " + java.time.LocalDate.now().format(DATE_FORMATTER);
        if (dateDebut != null && dateFin != null) {
            periodeStr = "Du " + dateDebut.format(DATE_FORMATTER) + " au " + dateFin.format(DATE_FORMATTER);
        } else if (dateDebut != null) {
            periodeStr = "À partir du " + dateDebut.format(DATE_FORMATTER);
        }
        params.put("periode", periodeStr);
        params.put("totalDebit", releve.getTotalReglements() != null ? releve.getTotalReglements() : BigDecimal.ZERO);
        params.put("totalCredit", releve.getTotalAchats() != null ? releve.getTotalAchats() : BigDecimal.ZERO);
        params.put("soldeFinal", releve.getSoldeActuel() != null ? releve.getSoldeActuel() : BigDecimal.ZERO);

        List<Map<String, Object>> lignes = new ArrayList<>();
        if (releve.getOperations() != null) {
            for (com.gestion.persistent.dto.ReleveFournisseurDTO.LigneReleveDTO op : releve.getOperations()) {
                Map<String, Object> m = new HashMap<>();
                m.put("datePiece", op.getDate() != null ? op.getDate().format(DATE_FORMATTER) : "-");
                m.put("referencePiece", op.getReference() != null ? op.getReference() : "-");
                m.put("libelle", op.getLibelle() != null ? op.getLibelle() : "-");
                m.put("debit", op.getDebit() != null ? op.getDebit() : BigDecimal.ZERO);
                m.put("credit", op.getCredit() != null ? op.getCredit() : BigDecimal.ZERO);
                m.put("soldeProgressif", op.getSoldeProgressif() != null ? op.getSoldeProgressif() : BigDecimal.ZERO);
                lignes.add(m);
            }
        }

        return exportToPdf("releve_compte_fournisseur", params, lignes);
    }

    // ==========================================
    // 12. Impression Bon de Transfert de Stock
    // ==========================================
    public byte[] genererTransfertStockPdf(Long transfertId) {
        TransfertStock transfert = transfertStockRepository.findByIdWithDetails(transfertId)
                .orElseThrow(() -> new RuntimeException("Transfert de stock non trouvé avec l'id: " + transfertId));

        Map<String, Object> params = initCommonTenantParams();
        params.put("numeroTransfert", transfert.getNumeroTransfert());
        params.put("dateTransfert", transfert.getDateTransfert() != null ? transfert.getDateTransfert().format(DATE_FORMATTER) : "-");
        params.put("dateExpedition", transfert.getDateExpedition() != null ? transfert.getDateExpedition().format(DATETIME_FORMATTER) : "-");
        params.put("dateReception", transfert.getDateReception() != null ? transfert.getDateReception().format(DATE_FORMATTER) : "-");
        params.put("statut", transfert.getStatut() != null ? transfert.getStatut().toString() : "BROUILLON");
        params.put("depotSourceNom", transfert.getDepotSource() != null ? transfert.getDepotSource().getNom() : "-");
        params.put("depotDestinationNom", transfert.getDepotDestination() != null ? transfert.getDepotDestination().getNom() : "-");
        params.put("motif", transfert.getMotif() != null ? transfert.getMotif() : "-");
        params.put("notes", transfert.getNotes() != null ? transfert.getNotes() : "");

        String creePar = (transfert.getCreePar() != null)
                ? (transfert.getCreePar().getNomComplet() != null ? transfert.getCreePar().getNomComplet() : transfert.getCreePar().getUsername())
                : "-";
        params.put("creeParNom", creePar);

        String expediePar = (transfert.getExpediePar() != null)
                ? (transfert.getExpediePar().getNomComplet() != null ? transfert.getExpediePar().getNomComplet() : transfert.getExpediePar().getUsername())
                : (transfert.getValidePar() != null ? (transfert.getValidePar().getNomComplet() != null ? transfert.getValidePar().getNomComplet() : transfert.getValidePar().getUsername()) : "-");
        params.put("expedieParNom", expediePar);

        String recuPar = (transfert.getRecuPar() != null)
                ? (transfert.getRecuPar().getNomComplet() != null ? transfert.getRecuPar().getNomComplet() : transfert.getRecuPar().getUsername())
                : "-";
        params.put("recuParNom", recuPar);

        List<Map<String, Object>> lignes = new ArrayList<>();
        BigDecimal totalQuantite = BigDecimal.ZERO;
        int totalArticles = 0;

        if (transfert.getLignes() != null) {
            for (LigneTransfertStock l : transfert.getLignes()) {
                Map<String, Object> map = new HashMap<>();
                map.put("reference", l.getProduit() != null ? l.getProduit().getReference() : "");
                map.put("designation", l.getProduit() != null ? l.getProduit().getDesignation() : "Article");
                BigDecimal qte = l.getQuantite() != null ? l.getQuantite() : BigDecimal.ZERO;
                map.put("quantite", qte);
                map.put("quantiteDemandee", l.getQuantiteDemandee());
                map.put("quantiteExpediee", l.getQuantiteExpediee());
                map.put("quantiteRecue", l.getQuantiteRecue());
                map.put("notes", l.getNotes() != null ? l.getNotes() : "");

                totalQuantite = totalQuantite.add(qte);
                totalArticles++;
                lignes.add(map);
            }
        }

        params.put("totalQuantite", totalQuantite);
        params.put("totalArticles", totalArticles);

        return exportToPdf("transfert_stock", params, lignes);
    }

    // ==========================================
    // Utilitaires : Paramètres Tenant & Export
    // ==========================================
    private Map<String, Object> initCommonTenantParams() {
        Map<String, Object> params = new HashMap<>();
        EntrepriseProfile profile = entrepriseProfileService.getProfileEntityByCurrentTenant();

        params.put("nomEntreprise",
                profile.getNomEntreprise() != null ? profile.getNomEntreprise() : "");
        params.put("activiteEntreprise", profile.getActivite() != null ? profile.getActivite() : "");
        params.put("adresseEntreprise", (profile.getAdresse() != null ? profile.getAdresse() : "") +
                (profile.getVille() != null && !profile.getVille().isEmpty() ? " - " + profile.getVille() : ""));
        params.put("telephoneEntreprise", profile.getTelephone() != null ? profile.getTelephone() : "");
        params.put("emailEntreprise", profile.getEmail() != null ? profile.getEmail() : "");
        params.put("rcEntreprise", profile.getRegistreCommerce() != null ? profile.getRegistreCommerce() : "");
        params.put("nifEntreprise",
                profile.getNumeroIdentificationFiscale() != null ? profile.getNumeroIdentificationFiscale() : "");
        params.put("nisEntreprise",
                profile.getNumeroIdentificationStatistique() != null ? profile.getNumeroIdentificationStatistique()
                        : "");
        params.put("aiEntreprise", profile.getArticleImposition() != null ? profile.getArticleImposition() : "");
        params.put("patenteEntreprise", profile.getPatente() != null ? profile.getPatente() : "");
        params.put("iceEntreprise", profile.getIce() != null ? profile.getIce() : "");
        params.put("cnssEntreprise", profile.getCnss() != null ? profile.getCnss() : "");
        params.put("gsmEntreprise", profile.getGsm() != null ? profile.getGsm() : "");
        params.put("telephoneSecondaire", profile.getGsm() != null ? profile.getGsm() : "");
        params.put("ribEntreprise", profile.getCompteBancaireRib() != null ? profile.getCompteBancaireRib() : "");
        params.put("banqueEntreprise", profile.getNomBanque() != null ? profile.getNomBanque() : "");
        params.put("piedPage", profile.getPiedPage() != null ? profile.getPiedPage() : "");
        params.put("devise", profile.getDevise() != null && !profile.getDevise().trim().isEmpty() ? profile.getDevise() : "MAD");

        // Couleur primaire dynamique du tenant (défaut : Bleu roi #0B3A6E)
        String couleurPrimaire = "#0B3A6E";
        if (profile.getCouleursTenant() != null && !profile.getCouleursTenant().isBlank()) {
            String raw = profile.getCouleursTenant().trim();
            if (raw.startsWith("#") && raw.length() >= 7) {
                couleurPrimaire = raw.substring(0, 7);
            } else if (raw.contains("#")) {
                int idx = raw.indexOf("#");
                if (idx + 7 <= raw.length()) {
                    couleurPrimaire = raw.substring(idx, idx + 7);
                }
            }
        }
        params.put("couleurPrimaire", couleurPrimaire);

        // Injection du Logo en java.awt.Image
        if (profile.hasLogo()) {
            try {
                Image logo = ImageIO.read(new ByteArrayInputStream(profile.getLogoData()));
                params.put("logoImage", logo);
            } catch (Exception e) {
                log.warn("Impossible de lire l'image du logo pour le tenant {}: {}", profile.getPointDeVenteId(),
                        e.getMessage());
                params.put("logoImage", null);
            }
        } else {
            params.put("logoImage", null);
        }

        return params;
    }

    private byte[] exportToPdf(String reportName, Map<String, Object> params, List<Map<String, Object>> dataList) {
        try {
            // Seuls les vrais articles sont envoyés au rapport (pas de lignes vides forcées)
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
                throw new RuntimeException("Échec de la compilation du rapport Jasper " + path + ": " + e.getMessage(),
                        e);
            }
        });
    }
}
