package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.gestion.persistent.dto.LivraisonRetardDTO;
import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.StatutLivraison;
import com.gestion.persistent.model.*;
import com.gestion.repository.BonLivraisonClientRepository;
import com.gestion.repository.CommandeClientRepository;
import com.gestion.repository.CommandeRepository;
import com.gestion.repository.LivraisonRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LivraisonRetardService {

    private final CommandeRepository commandeRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final LivraisonRepository livraisonRepository;
    private final BonLivraisonClientRepository bonLivraisonClientRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public LivraisonRetardService(CommandeRepository commandeRepository,
                                  CommandeClientRepository commandeClientRepository,
                                  LivraisonRepository livraisonRepository,
                                  BonLivraisonClientRepository bonLivraisonClientRepository,
                                  PointDeVenteRepository pointDeVenteRepository) {
        this.commandeRepository = commandeRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.livraisonRepository = livraisonRepository;
        this.bonLivraisonClientRepository = bonLivraisonClientRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    private Long resolveTenant(Long pdvId) {
        if (pdvId != null) return pdvId;
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    private String getNomPointDeVente(Long pdvId) {
        if (pdvId == null) return "Principal";
        return pointDeVenteRepository.findById(pdvId)
                .map(p -> p.getNom() != null ? p.getNom() : ("PDV #" + p.getId()))
                .orElse("Point de Vente #" + pdvId);
    }

    // =========================================================================
    // 1. FOURNISSEURS (ACHATS - ARTICLES ET LIGNES NON LIVRÉES)
    // =========================================================================

    public List<LivraisonRetardDTO> getRetardsFournisseurs(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<Commande> commandes = commandeRepository.findByPointDeVenteId(pdvId);
        LocalDateTime now = LocalDateTime.now();

        List<LivraisonRetardDTO> items = new ArrayList<>();

        for (Commande c : commandes) {
            // Ignorer uniquement les commandes annulées ou totalement livrées
            if (c.getStatut() == StatutCommande.ANNULEE) continue;
            if (c.getStatut() == StatutCommande.LIVREE && c.getStatutLivraison() == StatutLivraison.LIVREE) continue;

            // BLs liés
            List<Livraison> livs = livraisonRepository.findByCommande_Id(c.getId());
            String numeroBl = (livs != null && !livs.isEmpty())
                    ? livs.stream().map(Livraison::getNumeroLivraison).filter(Objects::nonNull).collect(Collectors.joining(", "))
                    : "-";

            String nomTiers = (c.getFournisseur() != null && c.getFournisseur().getNom() != null)
                    ? c.getFournisseur().getNom() : "Fournisseur non spécifié";
            String telTiers = c.getFournisseur() != null ? c.getFournisseur().getTelephone() : null;
            String emailTiers = c.getFournisseur() != null ? c.getFournisseur().getEmail() : null;

            Long joursRetard = 0L;
            if (c.getDateLivraisonPrevue() != null && c.getDateLivraisonPrevue().isBefore(now)) {
                joursRetard = ChronoUnit.DAYS.between(c.getDateLivraisonPrevue().toLocalDate(), LocalDate.now());
                if (joursRetard <= 0) joursRetard = 1L;
            }

            if (c.getLignesCommande() != null && !c.getLignesCommande().isEmpty()) {
                for (LigneCommande lc : c.getLignesCommande()) {
                    int qteCmd = lc.getQuantiteCommandee() != null ? lc.getQuantiteCommandee() : 0;
                    int qteLiv = lc.getQuantiteLivree() != null ? lc.getQuantiteLivree() : 0;
                    int reste = qteCmd - qteLiv;

                    // Si la ligne est déjà totalement reçue, on passe à la suivante
                    if (reste <= 0) continue;

                    LivraisonRetardDTO dto = new LivraisonRetardDTO();
                    dto.setId(lc.getId());
                    dto.setLigneId(lc.getId());
                    dto.setCommandeId(c.getId());
                    dto.setType("FOURNISSEUR");
                    dto.setNumeroCommande(c.getNumeroCommande() != null ? c.getNumeroCommande() : ("CF #" + c.getId()));
                    dto.setNumeroBl(numeroBl);
                    dto.setNomTiers(nomTiers);
                    dto.setTelephoneTiers(telTiers);
                    dto.setEmailTiers(emailTiers);
                    dto.setDateCommande(c.getDateCommande());
                    dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());
                    dto.setJoursRetard(joursRetard);

                    boolean estPartielle = qteLiv > 0;
                    dto.setStatut(estPartielle ? "Partiellement livrée" : "Non livrée");
                    dto.setStatutCode(estPartielle ? "PARTIELLE" : "NON_LIVREE");

                    Produit p = lc.getProduit();
                    if (p != null) {
                        dto.setProduitId(p.getId());
                        dto.setProduitReference(p.getReference() != null ? p.getReference() : "-");
                        String pNom = p.getDesignation() != null ? p.getDesignation() : p.getNom();
                        dto.setProduitNom(pNom != null ? pNom : "Article #" + p.getId());
                    } else {
                        dto.setProduitNom("Article");
                        dto.setProduitReference("-");
                    }

                    dto.setQuantiteCommandee(BigDecimal.valueOf(qteCmd));
                    dto.setQuantiteLivree(BigDecimal.valueOf(qteLiv));
                    dto.setQuantiteRestante(BigDecimal.valueOf(reste));

                    BigDecimal pu = lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO;
                    dto.setPrixUnitaire(pu);

                    BigDecimal montantRestant = pu.multiply(BigDecimal.valueOf(reste));
                    dto.setMontantRestant(montantRestant);
                    dto.setMontantTotal(montantRestant);

                    dto.setArticlesEnAttente(dto.getProduitNom() + " (" + reste + "/" + qteCmd + " restant)");
                    dto.setNombreArticlesTotal(1);
                    dto.setNombreArticlesRestants(1);
                    dto.setPointDeVenteId(c.getPointDeVenteId());

                    items.add(dto);
                }
            } else {
                // Si la commande n'a pas de lignes détaillées mais n'est pas livrée
                LivraisonRetardDTO dto = new LivraisonRetardDTO();
                dto.setId(c.getId());
                dto.setCommandeId(c.getId());
                dto.setType("FOURNISSEUR");
                dto.setNumeroCommande(c.getNumeroCommande() != null ? c.getNumeroCommande() : ("CF #" + c.getId()));
                dto.setNumeroBl(numeroBl);
                dto.setNomTiers(nomTiers);
                dto.setTelephoneTiers(telTiers);
                dto.setEmailTiers(emailTiers);
                dto.setDateCommande(c.getDateCommande());
                dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());
                dto.setJoursRetard(joursRetard);
                dto.setStatut("Non livrée");
                dto.setStatutCode("NON_LIVREE");
                dto.setProduitNom("Articles de la commande");
                dto.setProduitReference("-");
                dto.setQuantiteCommandee(BigDecimal.ONE);
                dto.setQuantiteLivree(BigDecimal.ZERO);
                dto.setQuantiteRestante(BigDecimal.ONE);
                dto.setPrixUnitaire(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);
                dto.setMontantTotal(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);
                dto.setMontantRestant(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);
                dto.setArticlesEnAttente("Commande en attente de réception");
                dto.setNombreArticlesTotal(1);
                dto.setNombreArticlesRestants(1);
                dto.setPointDeVenteId(c.getPointDeVenteId());
                items.add(dto);
            }
        }

        // Tri : les retards les plus critiques d'abord, puis les commandes les plus récentes
        items.sort((a, b) -> {
            long rA = a.getJoursRetard() != null ? a.getJoursRetard() : 0L;
            long rB = b.getJoursRetard() != null ? b.getJoursRetard() : 0L;
            int cmp = Long.compare(rB, rA);
            if (cmp != 0) return cmp;
            if (a.getDateCommande() != null && b.getDateCommande() != null) {
                return b.getDateCommande().compareTo(a.getDateCommande());
            }
            return 0;
        });

        return items;
    }

    public byte[] genererExcelRetardsFournisseurs(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<LivraisonRetardDTO> items = getRetardsFournisseurs(pdvId);
        String nomPdv = getNomPointDeVente(pdvId);

        return buildWorkbook(
                "SUIVI DES ARTICLES FOURNISSEURS NON LIVRÉS & RELIQUATS",
                "Fournisseur",
                items,
                nomPdv,
                IndexedColors.DARK_BLUE.getIndex(),
                IndexedColors.WHITE.getIndex()
        );
    }

    // =========================================================================
    // 2. CLIENTS (VENTES - ARTICLES ET COMMANDES NON LIVRÉES)
    // =========================================================================

    public List<LivraisonRetardDTO> getRetardsClients(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<CommandeClient> commandes = commandeClientRepository.findByPointDeVenteId(pdvId);
        LocalDateTime now = LocalDateTime.now();

        List<LivraisonRetardDTO> items = new ArrayList<>();

        for (CommandeClient c : commandes) {
            if (c.getStatut() == StatutCommandeClient.ANNULEE) continue;
            if (c.getStatut() == StatutCommandeClient.LIVREE) continue;

            // BLs liés
            List<BonLivraisonClient> bls = bonLivraisonClientRepository.findByCommandeClientIdAndPointDeVenteId(c.getId(), pdvId);
            String numeroBl = (bls != null && !bls.isEmpty())
                    ? bls.stream().map(BonLivraisonClient::getNumeroBl).filter(Objects::nonNull).collect(Collectors.joining(", "))
                    : "-";

            String nomTiers = "Client non spécifié";
            String telTiers = null;
            String emailTiers = null;
            if (c.getClient() != null) {
                nomTiers = c.getClient().getNomComplet();
                telTiers = c.getClient().getTelephone();
                emailTiers = c.getClient().getEmail();
            } else if (c.getClientNom() != null) {
                nomTiers = c.getClientNom();
                telTiers = c.getClientTelephone();
                emailTiers = c.getClientEmail();
            }

            Long joursRetard = 0L;
            if (c.getDateLivraisonPrevue() != null && c.getDateLivraisonPrevue().isBefore(now)) {
                joursRetard = ChronoUnit.DAYS.between(c.getDateLivraisonPrevue().toLocalDate(), LocalDate.now());
                if (joursRetard <= 0) joursRetard = 1L;
            }

            if (c.getLignesCommande() != null && !c.getLignesCommande().isEmpty()) {
                for (LigneCommandeClient lc : c.getLignesCommande()) {
                    BigDecimal qteCmd = lc.getQuantiteCommandee() != null ? lc.getQuantiteCommandee()
                            : (lc.getQuantite() != null ? lc.getQuantite() : BigDecimal.ZERO);
                    BigDecimal qteLiv = lc.getQuantiteLivree() != null ? lc.getQuantiteLivree() : BigDecimal.ZERO;
                    BigDecimal reste = qteCmd.subtract(qteLiv);

                    // Si ligne totalement livrée, passer
                    if (reste.compareTo(BigDecimal.ZERO) <= 0) continue;

                    LivraisonRetardDTO dto = new LivraisonRetardDTO();
                    dto.setId(lc.getId());
                    dto.setLigneId(lc.getId());
                    dto.setCommandeId(c.getId());
                    dto.setType("CLIENT");
                    dto.setNumeroCommande(c.getNumeroCommande() != null ? c.getNumeroCommande() : ("CC #" + c.getId()));
                    dto.setNumeroBl(numeroBl);
                    dto.setNomTiers(nomTiers);
                    dto.setTelephoneTiers(telTiers);
                    dto.setEmailTiers(emailTiers);
                    dto.setDateCommande(c.getDateCommande());
                    dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());
                    dto.setJoursRetard(joursRetard);

                    boolean estPartielle = qteLiv.compareTo(BigDecimal.ZERO) > 0;
                    dto.setStatut(estPartielle ? "Partiellement expédiée" : "Non livrée");
                    dto.setStatutCode(estPartielle ? "PARTIELLE" : "NON_LIVREE");

                    Produit p = lc.getProduit();
                    if (p != null) {
                        dto.setProduitId(p.getId());
                        dto.setProduitReference(p.getReference() != null ? p.getReference() : "-");
                        String pNom = p.getDesignation() != null ? p.getDesignation() : p.getNom();
                        dto.setProduitNom(pNom != null ? pNom : "Article #" + p.getId());
                    } else {
                        dto.setProduitNom("Article");
                        dto.setProduitReference("-");
                    }

                    dto.setQuantiteCommandee(qteCmd);
                    dto.setQuantiteLivree(qteLiv);
                    dto.setQuantiteRestante(reste);

                    BigDecimal pu = lc.getPrixUnitaire() != null ? lc.getPrixUnitaire() : BigDecimal.ZERO;
                    dto.setPrixUnitaire(pu);

                    BigDecimal montantRestant = pu.multiply(reste);
                    dto.setMontantRestant(montantRestant);
                    dto.setMontantTotal(montantRestant);

                    dto.setArticlesEnAttente(dto.getProduitNom() + " (" + reste.stripTrailingZeros().toPlainString() + "/" + qteCmd.stripTrailingZeros().toPlainString() + " restant)");
                    dto.setNombreArticlesTotal(1);
                    dto.setNombreArticlesRestants(1);
                    dto.setPointDeVenteId(c.getPointDeVenteId());

                    items.add(dto);
                }
            } else {
                LivraisonRetardDTO dto = new LivraisonRetardDTO();
                dto.setId(c.getId());
                dto.setCommandeId(c.getId());
                dto.setType("CLIENT");
                dto.setNumeroCommande(c.getNumeroCommande() != null ? c.getNumeroCommande() : ("CC #" + c.getId()));
                dto.setNumeroBl(numeroBl);
                dto.setNomTiers(nomTiers);
                dto.setTelephoneTiers(telTiers);
                dto.setEmailTiers(emailTiers);
                dto.setDateCommande(c.getDateCommande());
                dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());
                dto.setJoursRetard(joursRetard);
                dto.setStatut("Non livrée");
                dto.setStatutCode("NON_LIVREE");
                dto.setProduitNom("Articles de la commande");
                dto.setProduitReference("-");
                dto.setQuantiteCommandee(BigDecimal.ONE);
                dto.setQuantiteLivree(BigDecimal.ZERO);
                dto.setQuantiteRestante(BigDecimal.ONE);
                dto.setPrixUnitaire(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);
                dto.setMontantTotal(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);
                dto.setMontantRestant(c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO);
                dto.setArticlesEnAttente("Commande en attente d'expédition");
                dto.setNombreArticlesTotal(1);
                dto.setNombreArticlesRestants(1);
                dto.setPointDeVenteId(c.getPointDeVenteId());
                items.add(dto);
            }
        }

        // Tri
        items.sort((a, b) -> {
            long rA = a.getJoursRetard() != null ? a.getJoursRetard() : 0L;
            long rB = b.getJoursRetard() != null ? b.getJoursRetard() : 0L;
            int cmp = Long.compare(rB, rA);
            if (cmp != 0) return cmp;
            if (a.getDateCommande() != null && b.getDateCommande() != null) {
                return b.getDateCommande().compareTo(a.getDateCommande());
            }
            return 0;
        });

        return items;
    }

    public byte[] genererExcelRetardsClients(Long requestedPdvId) {
        Long pdvId = resolveTenant(requestedPdvId);
        List<LivraisonRetardDTO> items = getRetardsClients(pdvId);
        String nomPdv = getNomPointDeVente(pdvId);

        return buildWorkbook(
                "SUIVI DES ARTICLES CLIENTS NON LIVRÉS & RELIQUATS D'EXPÉDITION",
                "Client",
                items,
                nomPdv,
                IndexedColors.TEAL.getIndex(),
                IndexedColors.WHITE.getIndex()
        );
    }

    // =========================================================================
    // 3. GÉNÉRATEUR EXCEL APACHE POI
    // =========================================================================

    private byte[] buildWorkbook(String titreRapport,
                                 String labelTiers,
                                 List<LivraisonRetardDTO> items,
                                 String nomPointDeVente,
                                 short headerBgColor,
                                 short headerTextColor) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Lignes Non Livrées");
            sheet.setDisplayGridlines(true);

            // Formats
            DataFormat dataFormat = workbook.createDataFormat();
            short currencyFormat = dataFormat.getFormat("#,##0.00 \"MAD\"");
            short integerFormat = dataFormat.getFormat("#,##0");

            // Fonts
            Font fontTitre = workbook.createFont();
            fontTitre.setFontHeightInPoints((short) 15);
            fontTitre.setBold(true);
            fontTitre.setColor(IndexedColors.DARK_BLUE.getIndex());

            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontHeightInPoints((short) 10);
            fontSubtitle.setItalic(true);
            fontSubtitle.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font fontHeader = workbook.createFont();
            fontHeader.setFontHeightInPoints((short) 10);
            fontHeader.setBold(true);
            fontHeader.setColor(headerTextColor);

            Font fontData = workbook.createFont();
            fontData.setFontHeightInPoints((short) 10);

            Font fontBold = workbook.createFont();
            fontBold.setFontHeightInPoints((short) 10);
            fontBold.setBold(true);

            Font fontCritique = workbook.createFont();
            fontCritique.setFontHeightInPoints((short) 10);
            fontCritique.setBold(true);
            fontCritique.setColor(IndexedColors.RED.getIndex());

            Font fontSuccess = workbook.createFont();
            fontSuccess.setFontHeightInPoints((short) 10);
            fontSuccess.setBold(true);
            fontSuccess.setColor(IndexedColors.GREEN.getIndex());

            // Styles
            CellStyle styleTitle = workbook.createCellStyle();
            styleTitle.setFont(fontTitre);
            styleTitle.setAlignment(HorizontalAlignment.LEFT);
            styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleSub = workbook.createCellStyle();
            styleSub.setFont(fontSubtitle);

            CellStyle styleHeader = workbook.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(headerBgColor);
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);
            styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            styleHeader.setBorderTop(BorderStyle.MEDIUM);
            styleHeader.setBorderBottom(BorderStyle.MEDIUM);
            styleHeader.setBorderLeft(BorderStyle.THIN);
            styleHeader.setBorderRight(BorderStyle.THIN);
            styleHeader.setWrapText(true);

            CellStyle styleText = createBorderedStyle(workbook, fontData, HorizontalAlignment.LEFT);
            CellStyle styleCenter = createBorderedStyle(workbook, fontData, HorizontalAlignment.CENTER);
            CellStyle styleRight = createBorderedStyle(workbook, fontData, HorizontalAlignment.RIGHT);

            CellStyle styleQty = createBorderedStyle(workbook, fontData, HorizontalAlignment.RIGHT);
            styleQty.setDataFormat(integerFormat);

            CellStyle styleCurrency = createBorderedStyle(workbook, fontData, HorizontalAlignment.RIGHT);
            styleCurrency.setDataFormat(currencyFormat);

            CellStyle styleRetardCritique = createBorderedStyle(workbook, fontCritique, HorizontalAlignment.CENTER);
            CellStyle styleRetardNormal = createBorderedStyle(workbook, fontData, HorizontalAlignment.CENTER);
            CellStyle styleDansDelais = createBorderedStyle(workbook, fontSuccess, HorizontalAlignment.CENTER);

            CellStyle styleTotal = workbook.createCellStyle();
            styleTotal.setFont(fontBold);
            styleTotal.setBorderTop(BorderStyle.DOUBLE);
            styleTotal.setBorderBottom(BorderStyle.DOUBLE);
            styleTotal.setBorderLeft(BorderStyle.THIN);
            styleTotal.setBorderRight(BorderStyle.THIN);
            styleTotal.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            styleTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleTotal.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleTotalCurrency = workbook.createCellStyle();
            styleTotalCurrency.cloneStyleFrom(styleTotal);
            styleTotalCurrency.setDataFormat(currencyFormat);

            CellStyle styleTotalQty = workbook.createCellStyle();
            styleTotalQty.cloneStyleFrom(styleTotal);
            styleTotalQty.setDataFormat(integerFormat);

            // Dates format
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // 1. Titre
            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            rowTitle.setHeightInPoints(24);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue(titreRapport);
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

            // 2. Métadonnées
            Row rowMeta = sheet.createRow(rowIdx++);
            Cell cellMeta = rowMeta.createCell(0);
            cellMeta.setCellValue("Point de Vente : " + nomPointDeVente + "  |  Date de génération : " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                    "  |  Total lignes non livrées : " + items.size());
            cellMeta.setCellStyle(styleSub);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 12));

            rowIdx++; // Ligne vide

            // 3. En-tête des colonnes
            String[] headers = {
                    "N° Commande",
                    "Date Commande",
                    labelTiers,
                    "Contact",
                    "Réf Article",
                    "Désignation Article (Item)",
                    "Qté Commandée",
                    "Qté Livrée",
                    "Qté Restante (Non Livrée)",
                    "Prix Unit. HT",
                    "Montant Restant",
                    "Date Prévue",
                    "Statut Ligne",
                    "Échéance / Retard"
            };

            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            // 4. Données
            BigDecimal grandTotalMontant = BigDecimal.ZERO;
            BigDecimal grandTotalQteRestante = BigDecimal.ZERO;

            for (LivraisonRetardDTO it : items) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                // Col 0: N° Commande
                Cell c0 = row.createCell(0);
                c0.setCellValue(it.getNumeroCommande() != null ? it.getNumeroCommande() : "-");
                c0.setCellStyle(styleCenter);

                // Col 1: Date Commande
                Cell c1 = row.createCell(1);
                c1.setCellValue(it.getDateCommande() != null ? it.getDateCommande().format(dtf) : "-");
                c1.setCellStyle(styleCenter);

                // Col 2: Tiers
                Cell c2 = row.createCell(2);
                c2.setCellValue(it.getNomTiers() != null ? it.getNomTiers() : "-");
                c2.setCellStyle(styleText);

                // Col 3: Contact
                Cell c3 = row.createCell(3);
                String contact = it.getTelephoneTiers() != null ? it.getTelephoneTiers() : "";
                if (it.getEmailTiers() != null) {
                    contact = contact.isEmpty() ? it.getEmailTiers() : (contact + " / " + it.getEmailTiers());
                }
                c3.setCellValue(contact.isEmpty() ? "-" : contact);
                c3.setCellStyle(styleText);

                // Col 4: Réf Article
                Cell c4 = row.createCell(4);
                c4.setCellValue(it.getProduitReference() != null ? it.getProduitReference() : "-");
                c4.setCellStyle(styleCenter);

                // Col 5: Désignation Article
                Cell c5 = row.createCell(5);
                c5.setCellValue(it.getProduitNom() != null ? it.getProduitNom() : "-");
                c5.setCellStyle(styleText);

                // Col 6: Qté Commandée
                Cell c6 = row.createCell(6);
                double qCmd = it.getQuantiteCommandee() != null ? it.getQuantiteCommandee().doubleValue() : 0.0;
                c6.setCellValue(qCmd);
                c6.setCellStyle(styleQty);

                // Col 7: Qté Livrée
                Cell c7 = row.createCell(7);
                double qLiv = it.getQuantiteLivree() != null ? it.getQuantiteLivree().doubleValue() : 0.0;
                c7.setCellValue(qLiv);
                c7.setCellStyle(styleQty);

                // Col 8: Qté Restante
                Cell c8 = row.createCell(8);
                double qReste = it.getQuantiteRestante() != null ? it.getQuantiteRestante().doubleValue() : 0.0;
                c8.setCellValue(qReste);
                c8.setCellStyle(styleQty);
                grandTotalQteRestante = grandTotalQteRestante.add(BigDecimal.valueOf(qReste));

                // Col 9: Prix Unitaire
                Cell c9 = row.createCell(9);
                double pu = it.getPrixUnitaire() != null ? it.getPrixUnitaire().doubleValue() : 0.0;
                c9.setCellValue(pu);
                c9.setCellStyle(styleCurrency);

                // Col 10: Montant Restant
                Cell c10 = row.createCell(10);
                BigDecimal mnt = it.getMontantRestant() != null ? it.getMontantRestant()
                        : (it.getMontantTotal() != null ? it.getMontantTotal() : BigDecimal.ZERO);
                c10.setCellValue(mnt.doubleValue());
                c10.setCellStyle(styleCurrency);
                grandTotalMontant = grandTotalMontant.add(mnt);

                // Col 11: Date Prévue
                Cell c11 = row.createCell(11);
                c11.setCellValue(it.getDateLivraisonPrevue() != null ? it.getDateLivraisonPrevue().format(dtf) : "Non planifiée");
                c11.setCellStyle(styleCenter);

                // Col 12: Statut Ligne
                Cell c12 = row.createCell(12);
                c12.setCellValue(it.getStatut() != null ? it.getStatut() : "Non livrée");
                c12.setCellStyle(styleCenter);

                // Col 13: Échéance / Retard
                Cell c13 = row.createCell(13);
                long jr = it.getJoursRetard() != null ? it.getJoursRetard() : 0L;
                if (jr > 0) {
                    c13.setCellValue("Retard: +" + jr + " j");
                    c13.setCellStyle(jr >= 15 ? styleRetardCritique : styleRetardNormal);
                } else if (it.getDateLivraisonPrevue() != null) {
                    c13.setCellValue("Dans les délais");
                    c13.setCellStyle(styleDansDelais);
                } else {
                    c13.setCellValue("En attente");
                    c13.setCellStyle(styleCenter);
                }
            }

            // 5. Ligne Totaux
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = totalRow.createCell(i);
                cell.setCellStyle(styleTotal);
            }
            totalRow.getCell(0).setCellValue("TOTAL GLOBAL");
            totalRow.getCell(0).setCellStyle(styleTotal);

            // Total Qte Restante
            Cell totalQteCell = totalRow.getCell(8);
            totalQteCell.setCellValue(grandTotalQteRestante.doubleValue());
            totalQteCell.setCellStyle(styleTotalQty);

            // Total Montant Restant
            Cell totalMntCell = totalRow.getCell(10);
            totalMntCell.setCellValue(grandTotalMontant.doubleValue());
            totalMntCell.setCellStyle(styleTotalCurrency);

            // Ajustement automatique des largeurs de colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 3200));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel POI : " + e.getMessage(), e);
        }
    }

    private CellStyle createBorderedStyle(Workbook workbook, Font font, HorizontalAlignment align) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }
}
