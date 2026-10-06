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
                    dto.setEntrepotNom(getNomPointDeVente(c.getPointDeVenteId()));

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
                dto.setEntrepotNom(getNomPointDeVente(c.getPointDeVenteId()));
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
                "Commandes fournisseurs à recevoir",
                "Fournisseur",
                items,
                nomPdv
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
                    dto.setEntrepotNom(getNomPointDeVente(c.getPointDeVenteId()));

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
                dto.setPrixUnitaire(c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO);
                dto.setMontantTotal(c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO);
                dto.setMontantRestant(c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO);
                dto.setArticlesEnAttente("Commande en attente d'expédition");
                dto.setNombreArticlesTotal(1);
                dto.setNombreArticlesRestants(1);
                dto.setPointDeVenteId(c.getPointDeVenteId());
                dto.setEntrepotNom(getNomPointDeVente(c.getPointDeVenteId()));
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
                "Commandes clients à livrer",
                "Client",
                items,
                nomPdv
        );
    }

    // =========================================================================
    // 3. GÉNÉRATEUR EXCEL APACHE POI
    // =========================================================================

    private byte[] buildWorkbook(String titreRapport,
                                 String labelTiers,
                                 List<LivraisonRetardDTO> items,
                                 String nomPointDeVente) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = labelTiers.equalsIgnoreCase("Client") ? "Commandes clients à livrer" : "Commandes fourn. à recevoir";
            Sheet sheet = workbook.createSheet(sheetName);
            sheet.setDisplayGridlines(true);

            // Formats numériques
            DataFormat dataFormat = workbook.createDataFormat();
            short integerFormat = dataFormat.getFormat("#,##0");

            // Fonts
            Font fontTitre = workbook.createFont();
            fontTitre.setFontHeightInPoints((short) 14);
            fontTitre.setBold(true);
            fontTitre.setColor(IndexedColors.ROYAL_BLUE.getIndex());

            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontHeightInPoints((short) 9);
            fontSubtitle.setItalic(true);
            fontSubtitle.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font fontHeader = workbook.createFont();
            fontHeader.setFontHeightInPoints((short) 10);
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());

            Font fontData = workbook.createFont();
            fontData.setFontHeightInPoints((short) 10);

            Font fontBold = workbook.createFont();
            fontBold.setFontHeightInPoints((short) 10);
            fontBold.setBold(true);

            Font fontRetard = workbook.createFont();
            fontRetard.setFontHeightInPoints((short) 10);
            fontRetard.setBold(true);
            fontRetard.setColor(IndexedColors.RED.getIndex());

            // Styles
            CellStyle styleTitle = workbook.createCellStyle();
            styleTitle.setFont(fontTitre);
            styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleSub = workbook.createCellStyle();
            styleSub.setFont(fontSubtitle);
            styleSub.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleHeader = workbook.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);
            styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            styleHeader.setBorderTop(BorderStyle.MEDIUM);
            styleHeader.setBorderBottom(BorderStyle.MEDIUM);
            styleHeader.setBorderLeft(BorderStyle.THIN);
            styleHeader.setBorderRight(BorderStyle.THIN);

            CellStyle styleText = createBorderedStyle(workbook, fontData, HorizontalAlignment.LEFT);
            CellStyle styleCenter = createBorderedStyle(workbook, fontData, HorizontalAlignment.CENTER);

            CellStyle styleQty = createBorderedStyle(workbook, fontData, HorizontalAlignment.RIGHT);
            styleQty.setDataFormat(integerFormat);

            CellStyle styleQtyRestante = createBorderedStyle(workbook, fontBold, HorizontalAlignment.RIGHT);
            styleQtyRestante.setDataFormat(integerFormat);

            CellStyle styleRetardCritique = createBorderedStyle(workbook, fontRetard, HorizontalAlignment.CENTER);
            CellStyle styleRetardNormal = createBorderedStyle(workbook, fontData, HorizontalAlignment.CENTER);

            CellStyle styleTotal = workbook.createCellStyle();
            styleTotal.setFont(fontBold);
            styleTotal.setBorderTop(BorderStyle.THIN);
            styleTotal.setBorderBottom(BorderStyle.DOUBLE);
            styleTotal.setBorderLeft(BorderStyle.THIN);
            styleTotal.setBorderRight(BorderStyle.THIN);
            styleTotal.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            styleTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleTotal.setAlignment(HorizontalAlignment.RIGHT);
            styleTotal.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle styleTotalQty = workbook.createCellStyle();
            styleTotalQty.cloneStyleFrom(styleTotal);
            styleTotalQty.setDataFormat(integerFormat);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // 1. Titre (Ligne 0)
            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            rowTitle.setHeightInPoints(26);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue(titreRapport);
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // 2. Sous-titre / Métadonnées (Ligne 1)
            Row rowMeta = sheet.createRow(rowIdx++);
            rowMeta.setHeightInPoints(18);
            Cell cellMeta = rowMeta.createCell(0);
            cellMeta.setCellValue("Entrepôt : " + nomPointDeVente + "   |   Date : " +
                    LocalDate.now().format(dtf) + "   |   Total lignes : " + items.size());
            cellMeta.setCellStyle(styleSub);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            // Ligne vide d'espacement (Ligne 2)
            Row rowSpacer = sheet.createRow(rowIdx++);
            rowSpacer.setHeightInPoints(8);

            // 3. En-têtes (Ligne 3) - 6 colonnes optimisées pour l'impression
            String[] headers = {
                    labelTiers,
                    "Commande",
                    "Produit",
                    "Qté commandée",
                    "Qté restante",
                    "Date (Retard)"
            };

            int headerRowIndex = rowIdx++;
            Row headerRow = sheet.createRow(headerRowIndex);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            // 4. Lignes de données
            BigDecimal grandTotalQteCmd = BigDecimal.ZERO;
            BigDecimal grandTotalQteRestante = BigDecimal.ZERO;

            for (LivraisonRetardDTO it : items) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                // Col 0: Client / Fournisseur
                Cell c0 = row.createCell(0);
                c0.setCellValue(it.getNomTiers() != null ? it.getNomTiers() : "-");
                c0.setCellStyle(styleText);

                // Col 1: Commande
                Cell c1 = row.createCell(1);
                c1.setCellValue(it.getNumeroCommande() != null ? it.getNumeroCommande() : "-");
                c1.setCellStyle(styleCenter);

                // Col 2: Produit
                Cell c2 = row.createCell(2);
                c2.setCellValue(it.getProduitNom() != null ? it.getProduitNom() : "-");
                c2.setCellStyle(styleText);

                // Col 3: Qté commandée
                Cell c3 = row.createCell(3);
                double qCmd = it.getQuantiteCommandee() != null ? it.getQuantiteCommandee().doubleValue() : 0.0;
                c3.setCellValue(qCmd);
                c3.setCellStyle(styleQty);
                grandTotalQteCmd = grandTotalQteCmd.add(it.getQuantiteCommandee() != null ? it.getQuantiteCommandee() : BigDecimal.ZERO);

                // Col 4: Qté restante
                Cell c4 = row.createCell(4);
                double qReste = it.getQuantiteRestante() != null ? it.getQuantiteRestante().doubleValue() : 0.0;
                c4.setCellValue(qReste);
                c4.setCellStyle(styleQtyRestante);
                grandTotalQteRestante = grandTotalQteRestante.add(it.getQuantiteRestante() != null ? it.getQuantiteRestante() : BigDecimal.ZERO);

                // Col 5: Date commande & Retard combinés
                Cell c5 = row.createCell(5);
                String dateCmd = it.getDateCommande() != null ? it.getDateCommande().format(dtf) : "-";
                long jr = it.getJoursRetard() != null ? it.getJoursRetard() : 0L;
                if (jr > 0) {
                    c5.setCellValue(dateCmd + " (" + jr + " j)");
                    c5.setCellStyle(styleRetardCritique);
                } else {
                    c5.setCellValue(dateCmd);
                    c5.setCellStyle(styleRetardNormal);
                }
            }

            // 5. Ligne de Totaux
            Row totalRow = sheet.createRow(rowIdx++);
            totalRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = totalRow.createCell(i);
                cell.setCellStyle(styleTotal);
            }
            totalRow.getCell(0).setCellValue("Total");
            totalRow.getCell(0).setCellStyle(styleTotal);

            totalRow.getCell(3).setCellValue(grandTotalQteCmd.doubleValue());
            totalRow.getCell(3).setCellStyle(styleTotalQty);

            totalRow.getCell(4).setCellValue(grandTotalQteRestante.doubleValue());
            totalRow.getCell(4).setCellStyle(styleTotalQty);

            // 6. Filtres automatiques Excel (sur les 6 colonnes)
            int lastDataRowIndex = Math.max(headerRowIndex, rowIdx - 2);
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastDataRowIndex, 0, headers.length - 1));

            // 7. Configuration d'impression (Optimisée A4 portrait/paysage pour 6 colonnes)
            PrintSetup printSetup = sheet.getPrintSetup();
            printSetup.setLandscape(false); // 6 colonnes tiennent parfaitement en Portrait A4
            printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
            sheet.setFitToPage(true);
            printSetup.setFitWidth((short) 1);
            printSetup.setFitHeight((short) 0);
            sheet.setRepeatingRows(new CellRangeAddress(headerRowIndex, headerRowIndex, 0, headers.length - 1));
            sheet.setMargin(Sheet.TopMargin, 0.4);
            sheet.setMargin(Sheet.BottomMargin, 0.4);
            sheet.setMargin(Sheet.LeftMargin, 0.4);
            sheet.setMargin(Sheet.RightMargin, 0.4);
            sheet.setPrintGridlines(true);

            // 8. Largeurs de colonnes auto-ajustées
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 3600));
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
