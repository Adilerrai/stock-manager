package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.DashboardDelaisPaiementDTO;
import com.gestion.persistent.dto.DeclarationDelaisPaiementDTO;
import com.gestion.persistent.dto.DelaisPaiementItemDTO;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DelaisPaiementService {

    private final FactureAchatRepository factureAchatRepository;
    private final ReglementFournisseurRepository reglementFournisseurRepository;
    private final FactureRepository factureRepository;
    private final SocieteRepository societeRepository;

    // Constantes Loi 69-21
    public static final int DELAI_LEGAL_DEFAUT_JOURS = 60;
    public static final int DELAI_MAX_CONTRACTUEL_JOURS = 120;
    public static final BigDecimal TAUX_PREMIER_MOIS = new BigDecimal("3.00");
    public static final BigDecimal TAUX_MOIS_SUPPLEMENTAIRE = new BigDecimal("0.85");

    public DelaisPaiementService(FactureAchatRepository factureAchatRepository,
                                 ReglementFournisseurRepository reglementFournisseurRepository,
                                 FactureRepository factureRepository,
                                 SocieteRepository societeRepository) {
        this.factureAchatRepository = factureAchatRepository;
        this.reglementFournisseurRepository = reglementFournisseurRepository;
        this.factureRepository = factureRepository;
        this.societeRepository = societeRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // 1. CALCUL DE LA DÉCLARATION TRIMESTRIELLE LOI 69-21
    // =========================================================================

    public DeclarationDelaisPaiementDTO calculerDeclarationTrimestrielle(int annee, int trimestre) {
        Long tenantId = getTenantId();

        if (trimestre < 1 || trimestre > 4) {
            throw new IllegalArgumentException("Le trimestre doit être compris entre 1 et 4.");
        }

        LocalDate dateDebut = getDebutTrimestre(annee, trimestre);
        LocalDate dateFin = getFinTrimestre(annee, trimestre);
        LocalDate dateLimiteDepot = dateFin.plusMonths(1);

        DeclarationDelaisPaiementDTO decl = new DeclarationDelaisPaiementDTO();
        decl.setAnnee(annee);
        decl.setTrimestre(trimestre);
        decl.setDateDebutPeriode(dateDebut);
        decl.setDateFinPeriode(dateFin);
        decl.setDateLimiteDepot(dateLimiteDepot);

        // Informations Société
        Societe societe = societeRepository.findById(tenantId)
                .orElseGet(() -> societeRepository.findByTenantIdAndIsParDefautTrue(tenantId)
                        .orElseGet(Societe::new));

        decl.setRaisonSociale(societe.getRaisonSociale() != null ? societe.getRaisonSociale() : "Entreprise " + tenantId);
        decl.setIdentifiantFiscal(societe.getIdentifiantFiscal() != null ? societe.getIdentifiantFiscal() : "");
        decl.setIce(societe.getIce() != null ? societe.getIce() : "");
        decl.setRegistreCommerce(societe.getRc() != null ? societe.getRc() : "");

        // Récupération des factures d'achat émises jusqu'à la fin de la période
        // (les factures antérieures non payées ou payées en retard pendant ce trimestre sont également analysées)
        List<FactureAchat> factures = factureAchatRepository.findByPointDeVenteId(tenantId).stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE)
                .filter(f -> {
                    LocalDate d = f.getDateFacture() != null ? f.getDateFacture().toLocalDate() : LocalDate.now();
                    return !d.isAfter(dateFin);
                })
                .sorted(Comparator.comparing(FactureAchat::getDateFacture).reversed())
                .collect(Collectors.toList());

        List<DelaisPaiementItemDTO> items = new ArrayList<>();

        for (FactureAchat f : factures) {
            DelaisPaiementItemDTO item = analyserFactureAchat(f, dateFin, tenantId);

            // Ne retenir dans la déclaration que les factures :
            // 1. Soit émises durant le trimestre
            // 2. Soit antérieures mais en retard / impayées à la date de fin du trimestre
            // 3. Soit payées en retard durant le trimestre
            LocalDate dFacture = item.getDateFacture();
            boolean emiseDansTrimestre = !dFacture.isBefore(dateDebut) && !dFacture.isAfter(dateFin);
            boolean payeeDansTrimestre = item.getDatePaiementEffective() != null
                    && !item.getDatePaiementEffective().isBefore(dateDebut)
                    && !item.getDatePaiementEffective().isAfter(dateFin);
            boolean enRetardFinTrimestre = item.getJoursRetard() > 0 && "EN_RETARD_IMPAYE".equals(item.getStatutDelai());

            if (emiseDansTrimestre || payeeDansTrimestre || enRetardFinTrimestre) {
                items.add(item);
                decl.setTotalFacturesConcernees(decl.getTotalFacturesConcernees() + 1);
                decl.setMontantTotalTtcFactures(decl.getMontantTotalTtcFactures().add(item.getMontantTtc()));

                if (item.getJoursRetard() > 0 && item.getMontantAmende().compareTo(BigDecimal.ZERO) > 0) {
                    decl.setTotalFacturesEnRetard(decl.getTotalFacturesEnRetard() + 1);
                    decl.setMontantTotalEnRetard(decl.getMontantTotalEnRetard().add(item.getMontantRestant().compareTo(BigDecimal.ZERO) > 0 ? item.getMontantRestant() : item.getMontantTtc()));
                    decl.setTotalAmendesDgiExigibles(decl.getTotalAmendesDgiExigibles().add(item.getMontantAmende()));

                    // Ventilation par tranches
                    ventilerTranche(decl, item);
                } else {
                    decl.setTotalFacturesDansLesDelais(decl.getTotalFacturesDansLesDelais() + 1);
                }
            }
        }

        decl.setFactures(items);
        return decl;
    }

    // =========================================================================
    // 2. TABLEAU DE BORD PRÉDICTIF DES DÉLAIS DE PAIEMENT
    // =========================================================================

    public DashboardDelaisPaiementDTO getDashboardPrevisionnel() {
        Long tenantId = getTenantId();
        LocalDate aujourdhui = LocalDate.now();

        DashboardDelaisPaiementDTO dash = new DashboardDelaisPaiementDTO();

        List<FactureAchat> factures = factureAchatRepository.findByPointDeVenteId(tenantId).stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE)
                .collect(Collectors.toList());

        List<DelaisPaiementItemDTO> alertes = new ArrayList<>();

        for (FactureAchat f : factures) {
            DelaisPaiementItemDTO item = analyserFactureAchat(f, aujourdhui, tenantId);

            if (item.getMontantRestant().compareTo(BigDecimal.ZERO) > 0) {
                dash.setTotalFacturesImpayees(dash.getTotalFacturesImpayees() + 1);

                if ("EN_RETARD_IMPAYE".equals(item.getStatutDelai())) {
                    dash.setTotalFacturesEnRetard(dash.getTotalFacturesEnRetard() + 1);
                    dash.setDetteFournisseurEnRetard(dash.getDetteFournisseurEnRetard().add(item.getMontantRestant()));
                    dash.setAmendeTotaleEstimeeEnCours(dash.getAmendeTotaleEstimeeEnCours().add(item.getMontantAmende()));

                    // Tranches de retard
                    long jr = item.getJoursRetard();
                    if (jr <= 30) {
                        dash.setMontantRetard0a30J(dash.getMontantRetard0a30J().add(item.getMontantRestant()));
                    } else if (jr <= 60) {
                        dash.setMontantRetard31a60J(dash.getMontantRetard31a60J().add(item.getMontantRestant()));
                    } else {
                        dash.setMontantRetardPlus60J(dash.getMontantRetardPlus60J().add(item.getMontantRestant()));
                    }

                    alertes.add(item);
                } else if ("A_RISQUE_15J".equals(item.getStatutDelai())) {
                    dash.setTotalFacturesARisque15Jours(dash.getTotalFacturesARisque15Jours() + 1);
                    dash.setDetteFournisseurDansLesDelais(dash.getDetteFournisseurDansLesDelais().add(item.getMontantRestant()));
                    alertes.add(item);
                } else {
                    dash.setDetteFournisseurDansLesDelais(dash.getDetteFournisseurDansLesDelais().add(item.getMontantRestant()));
                }
            }
        }

        // Tri des alertes : factures avec le plus grand retard et amende la plus élevée en premier
        alertes.sort(Comparator.comparing(DelaisPaiementItemDTO::getMontantAmende).reversed()
                .thenComparing(DelaisPaiementItemDTO::getDateEcheanceLegale));
        dash.setAlertesImmediates(alertes);

        // Score de respect des délais (100 - % factures en retard)
        if (dash.getTotalFacturesImpayees() > 0) {
            double tauxRespect = (double) (dash.getTotalFacturesImpayees() - dash.getTotalFacturesEnRetard()) / dash.getTotalFacturesImpayees() * 100.0;
            dash.setScoreRespectDelaisPourcentage((int) Math.max(0, Math.round(tauxRespect)));
        } else {
            dash.setScoreRespectDelaisPourcentage(100);
        }

        return dash;
    }

    // =========================================================================
    // 3. EXPORT EDI XML SIMPL-DÉLAIS DE PAIEMENT (DGI MAROC)
    // =========================================================================

    public byte[] genererXmlSimplDelaisPaiement(int annee, int trimestre) {
        DeclarationDelaisPaiementDTO decl = calculerDeclarationTrimestrielle(annee, trimestre);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<DeclarationDelaisPaiement xmlns=\"http://simpl.portail.dgi.gov.ma/delais-paiement\">\n");

        // En-tête déclarant
        xml.append("  <Entete>\n");
        xml.append("    <IdentifiantFiscal>").append(escapeXml(decl.getIdentifiantFiscal())).append("</IdentifiantFiscal>\n");
        xml.append("    <ICE>").append(escapeXml(decl.getIce())).append("</ICE>\n");
        xml.append("    <RegistreCommerce>").append(escapeXml(decl.getRegistreCommerce())).append("</RegistreCommerce>\n");
        xml.append("    <RaisonSociale>").append(escapeXml(decl.getRaisonSociale())).append("</RaisonSociale>\n");
        xml.append("    <Exercice>").append(decl.getAnnee()).append("</Exercice>\n");
        xml.append("    <Trimestre>").append(decl.getTrimestre()).append("</Trimestre>\n");
        xml.append("    <DateDebutPeriode>").append(decl.getDateDebutPeriode()).append("</DateDebutPeriode>\n");
        xml.append("    <DateFinPeriode>").append(decl.getDateFinPeriode()).append("</DateFinPeriode>\n");
        xml.append("    <DateGeneration>").append(LocalDate.now()).append("</DateGeneration>\n");
        xml.append("    <LoiReference>Loi 69-21 modifiant la loi 15-95</LoiReference>\n");
        xml.append("  </Entete>\n");

        // Synthèse globale
        xml.append("  <Synthese>\n");
        xml.append("    <TotalFacturesConcernees>").append(decl.getTotalFacturesConcernees()).append("</TotalFacturesConcernees>\n");
        xml.append("    <TotalFacturesDansLesDelais>").append(decl.getTotalFacturesDansLesDelais()).append("</TotalFacturesDansLesDelais>\n");
        xml.append("    <TotalFacturesEnRetard>").append(decl.getTotalFacturesEnRetard()).append("</TotalFacturesEnRetard>\n");
        xml.append("    <MontantTotalTTC>").append(fmt(decl.getMontantTotalTtcFactures())).append("</MontantTotalTTC>\n");
        xml.append("    <MontantTotalEnRetardTTC>").append(fmt(decl.getMontantTotalEnRetard())).append("</MontantTotalEnRetardTTC>\n");
        xml.append("    <TotalAmendesDgiExigibles>").append(fmt(decl.getTotalAmendesDgiExigibles())).append("</TotalAmendesDgiExigibles>\n");
        xml.append("  </Synthese>\n");

        // Détail des tranches
        xml.append("  <TranchesRetard>\n");
        xml.append("    <Tranche1_1a30Jours>\n");
        xml.append("      <NombreFactures>").append(decl.getNbFacturesTranche1()).append("</NombreFactures>\n");
        xml.append("      <MontantTTC>").append(fmt(decl.getMontantTranche1())).append("</MontantTTC>\n");
        xml.append("      <Amende3Pourcent>").append(fmt(decl.getAmendesTranche1())).append("</Amende3Pourcent>\n");
        xml.append("    </Tranche1_1a30Jours>\n");
        xml.append("    <Tranche2_31a60Jours>\n");
        xml.append("      <NombreFactures>").append(decl.getNbFacturesTranche2()).append("</NombreFactures>\n");
        xml.append("      <MontantTTC>").append(fmt(decl.getMontantTranche2())).append("</MontantTTC>\n");
        xml.append("      <AmendeCumulee>").append(fmt(decl.getAmendesTranche2())).append("</AmendeCumulee>\n");
        xml.append("    </Tranche2_31a60Jours>\n");
        xml.append("    <Tranche3_61a90Jours>\n");
        xml.append("      <NombreFactures>").append(decl.getNbFacturesTranche3()).append("</NombreFactures>\n");
        xml.append("      <MontantTTC>").append(fmt(decl.getMontantTranche3())).append("</MontantTTC>\n");
        xml.append("      <AmendeCumulee>").append(fmt(decl.getAmendesTranche3())).append("</AmendeCumulee>\n");
        xml.append("    </Tranche3_61a90Jours>\n");
        xml.append("    <Tranche4_Plus90Jours>\n");
        xml.append("      <NombreFactures>").append(decl.getNbFacturesTranche4()).append("</NombreFactures>\n");
        xml.append("      <MontantTTC>").append(fmt(decl.getMontantTranche4())).append("</MontantTTC>\n");
        xml.append("      <AmendeCumulee>").append(fmt(decl.getAmendesTranche4())).append("</AmendeCumulee>\n");
        xml.append("    </Tranche4_Plus90Jours>\n");
        xml.append("  </TranchesRetard>\n");

        // Liste des factures
        xml.append("  <Factures>\n");
        for (DelaisPaiementItemDTO item : decl.getFactures()) {
            xml.append("    <Facture>\n");
            xml.append("      <NumeroFacture>").append(escapeXml(item.getNumeroFacture())).append("</NumeroFacture>\n");
            xml.append("      <DateFacture>").append(item.getDateFacture()).append("</DateFacture>\n");
            xml.append("      <DateEcheanceLegale>").append(item.getDateEcheanceLegale()).append("</DateEcheanceLegale>\n");
            xml.append("      <DatePaiementEffective>").append(item.getDatePaiementEffective() != null ? item.getDatePaiementEffective().toString() : "").append("</DatePaiementEffective>\n");
            xml.append("      <Fournisseur>\n");
            xml.append("        <Nom>").append(escapeXml(item.getTiersNom())).append("</Nom>\n");
            xml.append("        <ICE>").append(escapeXml(item.getTiersIce())).append("</ICE>\n");
            xml.append("        <IF>").append(escapeXml(item.getTiersIdentifiantFiscal())).append("</IF>\n");
            xml.append("      </Fournisseur>\n");
            xml.append("      <MontantHT>").append(fmt(item.getMontantHt())).append("</MontantHT>\n");
            xml.append("      <MontantTVA>").append(fmt(item.getMontantTva())).append("</MontantTVA>\n");
            xml.append("      <MontantTTC>").append(fmt(item.getMontantTtc())).append("</MontantTTC>\n");
            xml.append("      <MontantPaye>").append(fmt(item.getMontantPaye())).append("</MontantPaye>\n");
            xml.append("      <MontantRestant>").append(fmt(item.getMontantRestant())).append("</MontantRestant>\n");
            xml.append("      <DelaiApplicableJours>").append(item.getDelaiApplicableJours()).append("</DelaiApplicableJours>\n");
            xml.append("      <JoursRetard>").append(item.getJoursRetard()).append("</JoursRetard>\n");
            xml.append("      <MoisRetard>").append(item.getMoisRetard()).append("</MoisRetard>\n");
            xml.append("      <TauxAmendePourcent>").append(fmt(item.getTauxAmendePourcentage())).append("</TauxAmendePourcent>\n");
            xml.append("      <MontantAmendeDGI>").append(fmt(item.getMontantAmende())).append("</MontantAmendeDGI>\n");
            xml.append("      <StatutDelai>").append(item.getStatutDelai()).append("</StatutDelai>\n");
            xml.append("    </Facture>\n");
        }
        xml.append("  </Factures>\n");

        xml.append("</DeclarationDelaisPaiement>\n");

        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // 4. EXPORT CSV NORMALISÉ DGI SIMPL-DÉLAIS DE PAIEMENT
    // =========================================================================

    public byte[] genererCsvSimplDelaisPaiement(int annee, int trimestre) {
        DeclarationDelaisPaiementDTO decl = calculerDeclarationTrimestrielle(annee, trimestre);

        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 pour Excel
        sb.append("\uFEFF");

        // En-têtes CSV DGI
        String[] headers = {
                "Type Piece", "N° Facture", "Date Facture", "Fournisseur", "ICE Fournisseur", "IF Fournisseur",
                "Montant HT", "Montant TVA", "Montant TTC", "Montant Paye", "Montant Restant",
                "Delai Légal (Jours)", "Date Echéance", "Date Règlement Effectif",
                "Jours Retard", "Mois Retard", "Taux Pénalité (%)", "Amende Due DGI (MAD)", "Statut"
        };
        sb.append(String.join(";", headers)).append("\r\n");

        for (DelaisPaiementItemDTO item : decl.getFactures()) {
            sb.append(cleanCsv(item.getTypeFlux())).append(";")
                    .append(cleanCsv(item.getNumeroFacture())).append(";")
                    .append(item.getDateFacture() != null ? item.getDateFacture().toString() : "").append(";")
                    .append(cleanCsv(item.getTiersNom())).append(";")
                    .append(cleanCsv(item.getTiersIce())).append(";")
                    .append(cleanCsv(item.getTiersIdentifiantFiscal())).append(";")
                    .append(fmt(item.getMontantHt())).append(";")
                    .append(fmt(item.getMontantTva())).append(";")
                    .append(fmt(item.getMontantTtc())).append(";")
                    .append(fmt(item.getMontantPaye())).append(";")
                    .append(fmt(item.getMontantRestant())).append(";")
                    .append(item.getDelaiApplicableJours()).append(";")
                    .append(item.getDateEcheanceLegale() != null ? item.getDateEcheanceLegale().toString() : "").append(";")
                    .append(item.getDatePaiementEffective() != null ? item.getDatePaiementEffective().toString() : "").append(";")
                    .append(item.getJoursRetard()).append(";")
                    .append(item.getMoisRetard()).append(";")
                    .append(fmt(item.getTauxAmendePourcentage())).append(";")
                    .append(fmt(item.getMontantAmende())).append(";")
                    .append(cleanCsv(item.getStatutDelai())).append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // MÉTHODES INTERNES DE CALCUL ET MAPPING
    // =========================================================================

    private DelaisPaiementItemDTO analyserFactureAchat(FactureAchat f, LocalDate dateReference, Long tenantId) {
        DelaisPaiementItemDTO dto = new DelaisPaiementItemDTO();
        dto.setTypeFlux("ACHAT");
        dto.setPieceId(f.getId());
        dto.setNumeroFacture(f.getNumeroFacture());
        dto.setDateFacture(f.getDateFacture() != null ? f.getDateFacture().toLocalDate() : LocalDate.now());

        // Fournisseur
        Fournisseur four = f.getFournisseur();
        int delaiJours = DELAI_LEGAL_DEFAUT_JOURS;
        if (four != null) {
            dto.setTiersId(four.getId());
            dto.setTiersNom(four.getNom() != null ? four.getNom() : "");
            dto.setTiersIce(four.getIce() != null ? four.getIce() : "");
            dto.setTiersIdentifiantFiscal(four.getNumeroIdentificationFiscale() != null ? four.getNumeroIdentificationFiscale() : "");

            if (four.getDelaiPaiementJours() != null && four.getDelaiPaiementJours() > 0) {
                // Plafonnement légal à 120 jours (Art. 78-1 Code de Commerce / Loi 69-21)
                delaiJours = Math.min(four.getDelaiPaiementJours(), DELAI_MAX_CONTRACTUEL_JOURS);
            }
        } else {
            dto.setTiersNom("Fournisseur divers");
            dto.setTiersIce("");
            dto.setTiersIdentifiantFiscal("");
        }

        dto.setDelaiApplicableJours(delaiJours);
        LocalDate dateEcheance = dto.getDateFacture().plusDays(delaiJours);
        dto.setDateEcheanceLegale(dateEcheance);

        // Montants facture
        BigDecimal ttc = f.getMontantTtc() != null ? f.getMontantTtc() : BigDecimal.ZERO;
        BigDecimal ht = f.getMontantHt() != null ? f.getMontantHt() : BigDecimal.ZERO;
        BigDecimal tva = f.getMontantTva() != null ? f.getMontantTva() : BigDecimal.ZERO;
        dto.setMontantTtc(ttc);
        dto.setMontantHt(ht);
        dto.setMontantTva(tva);

        // Règlements associés
        List<ReglementFournisseur> reglements = reglementFournisseurRepository.findByFactureAchatIdAndPointDeVenteId(f.getId(), tenantId);
        BigDecimal totalRegle = BigDecimal.ZERO;
        LocalDate derniereDateReglement = null;

        for (ReglementFournisseur r : reglements) {
            BigDecimal m = r.getMontant() != null ? r.getMontant() : BigDecimal.ZERO;
            totalRegle = totalRegle.add(m);
            LocalDate dReg = r.getDateReglement() != null ? r.getDateReglement().toLocalDate() : null;
            if (dReg != null) {
                if (derniereDateReglement == null || dReg.isAfter(derniereDateReglement)) {
                    derniereDateReglement = dReg;
                }
            }
        }

        dto.setMontantPaye(totalRegle);
        BigDecimal restant = ttc.subtract(totalRegle);
        if (restant.compareTo(BigDecimal.ZERO) < 0) restant = BigDecimal.ZERO;
        dto.setMontantRestant(restant);

        // Détermination du retard et pénalités
        boolean estSoldee = restant.compareTo(new BigDecimal("0.01")) <= 0;

        if (estSoldee && derniereDateReglement != null) {
            dto.setDatePaiementEffective(derniereDateReglement);
            if (derniereDateReglement.isAfter(dateEcheance)) {
                // Réglée mais avec retard !
                long jr = ChronoUnit.DAYS.between(dateEcheance, derniereDateReglement);
                int mois = calculerNombreMoisRetard(jr);
                BigDecimal taux = calculerTauxAmende(mois);
                BigDecimal amende = ttc.multiply(taux).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                dto.setJoursRetard(jr);
                dto.setMoisRetard(mois);
                dto.setTauxAmendePourcentage(taux);
                dto.setMontantAmende(amende);
                dto.setStatutDelai("REGLE_EN_RETARD");
            } else {
                dto.setJoursRetard(0);
                dto.setMoisRetard(0);
                dto.setTauxAmendePourcentage(BigDecimal.ZERO);
                dto.setMontantAmende(BigDecimal.ZERO);
                dto.setStatutDelai("DANS_LES_DELAIS");
            }
        } else {
            // Non encore soldée à la date de référence
            if (dateReference.isAfter(dateEcheance)) {
                long jr = ChronoUnit.DAYS.between(dateEcheance, dateReference);
                int mois = calculerNombreMoisRetard(jr);
                BigDecimal taux = calculerTauxAmende(mois);
                // Assiette de l'amende = montant TTC restant dû
                BigDecimal amende = restant.multiply(taux).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                dto.setJoursRetard(jr);
                dto.setMoisRetard(mois);
                dto.setTauxAmendePourcentage(taux);
                dto.setMontantAmende(amende);
                dto.setStatutDelai("EN_RETARD_IMPAYE");
            } else {
                dto.setJoursRetard(0);
                dto.setMoisRetard(0);
                dto.setTauxAmendePourcentage(BigDecimal.ZERO);
                dto.setMontantAmende(BigDecimal.ZERO);

                // Vérifier si risque à 15 jours
                if (dateEcheance.minusDays(15).isBefore(dateReference) || dateEcheance.minusDays(15).isEqual(dateReference)) {
                    dto.setStatutDelai("A_RISQUE_15J");
                } else {
                    dto.setStatutDelai("DANS_LES_DELAIS");
                }
            }
        }

        return dto;
    }

    private int calculerNombreMoisRetard(long joursRetard) {
        if (joursRetard <= 0) return 0;
        // La Loi 69-21 stipule : 3% au 1er mois, puis 0.85% par mois supplémentaire entamé (fraction de mois = mois plein)
        return (int) Math.ceil((double) joursRetard / 30.0);
    }

    private BigDecimal calculerTauxAmende(int moisRetard) {
        if (moisRetard <= 0) return BigDecimal.ZERO;
        if (moisRetard == 1) return TAUX_PREMIER_MOIS;
        // 3% pour le 1er mois + 0.85% * (moisRetard - 1)
        BigDecimal sup = TAUX_MOIS_SUPPLEMENTAIRE.multiply(new BigDecimal(moisRetard - 1));
        return TAUX_PREMIER_MOIS.add(sup);
    }

    private void ventilerTranche(DeclarationDelaisPaiementDTO decl, DelaisPaiementItemDTO item) {
        long jr = item.getJoursRetard();
        BigDecimal assiette = item.getMontantRestant().compareTo(BigDecimal.ZERO) > 0 ? item.getMontantRestant() : item.getMontantTtc();
        BigDecimal amende = item.getMontantAmende();

        if (jr <= 30) {
            decl.setNbFacturesTranche1(decl.getNbFacturesTranche1() + 1);
            decl.setMontantTranche1(decl.getMontantTranche1().add(assiette));
            decl.setAmendesTranche1(decl.getAmendesTranche1().add(amende));
        } else if (jr <= 60) {
            decl.setNbFacturesTranche2(decl.getNbFacturesTranche2() + 1);
            decl.setMontantTranche2(decl.getMontantTranche2().add(assiette));
            decl.setAmendesTranche2(decl.getAmendesTranche2().add(amende));
        } else if (jr <= 90) {
            decl.setNbFacturesTranche3(decl.getNbFacturesTranche3() + 1);
            decl.setMontantTranche3(decl.getMontantTranche3().add(assiette));
            decl.setAmendesTranche3(decl.getAmendesTranche3().add(amende));
        } else {
            decl.setNbFacturesTranche4(decl.getNbFacturesTranche4() + 1);
            decl.setMontantTranche4(decl.getMontantTranche4().add(assiette));
            decl.setAmendesTranche4(decl.getAmendesTranche4().add(amende));
        }
    }

    private LocalDate getDebutTrimestre(int annee, int trimestre) {
        return switch (trimestre) {
            case 1 -> LocalDate.of(annee, 1, 1);
            case 2 -> LocalDate.of(annee, 4, 1);
            case 3 -> LocalDate.of(annee, 7, 1);
            case 4 -> LocalDate.of(annee, 10, 1);
            default -> throw new IllegalArgumentException("Trimestre invalide : " + trimestre);
        };
    }

    private LocalDate getFinTrimestre(int annee, int trimestre) {
        return switch (trimestre) {
            case 1 -> LocalDate.of(annee, 3, 31);
            case 2 -> LocalDate.of(annee, 6, 30);
            case 3 -> LocalDate.of(annee, 9, 30);
            case 4 -> LocalDate.of(annee, 12, 31);
            default -> throw new IllegalArgumentException("Trimestre invalide : " + trimestre);
        };
    }

    private String fmt(BigDecimal m) {
        return (m != null ? m : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cleanCsv(String s) {
        if (s == null) return "";
        return s.replace(";", " ").replace("\r", " ").replace("\n", " ");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
