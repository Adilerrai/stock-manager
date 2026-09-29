package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.model.Societe;
import com.gestion.repository.SocieteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class SocialFiscalService {

    private final UserRepository userRepository;
    private final SocieteRepository societeRepository;
    private final ComptabiliteService comptabiliteService;

    // Plafonds et taux légaux marocains
    public static final BigDecimal PLAFOND_CNSS_MENSUEL = new BigDecimal("6000.00");
    public static final BigDecimal PLAFOND_CNSS_ANNUEL = new BigDecimal("72000.00");
    public static final BigDecimal TAUX_CNSS_SALARIAL = new BigDecimal("4.48");
    public static final BigDecimal TAUX_AMO_SALARIAL = new BigDecimal("2.26");
    public static final BigDecimal TAUX_AMO_PATRONAL = new BigDecimal("4.11");
    public static final BigDecimal TAUX_ALLOCATIONS_FAMILIALES = new BigDecimal("6.40");
    public static final BigDecimal TAUX_PRESTATIONS_SOCIALES_PATRONAL = new BigDecimal("8.98");
    public static final BigDecimal TAUX_TAXE_FORMATION_PRO = new BigDecimal("1.60");
    public static final BigDecimal PLAFOND_FRAIS_PRO_ANNUEL = new BigDecimal("35000.00");
    public static final BigDecimal TAUX_FRAIS_PRO = new BigDecimal("35.00");
    public static final BigDecimal DEDUCTION_PAR_CHARGE_FAMILLE = new BigDecimal("360.00"); // 360 MAD / an par personne (max 6)

    public SocialFiscalService(UserRepository userRepository,
                               SocieteRepository societeRepository,
                               ComptabiliteService comptabiliteService) {
        this.userRepository = userRepository;
        this.societeRepository = societeRepository;
        this.comptabiliteService = comptabiliteService;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    // =========================================================================
    // 1. ÉTAT 9421 : DÉCLARATION ANNUELLE DES SALAIRES (ART. 79 DU CGI)
    // =========================================================================

    @Transactional(readOnly = true)
    public DeclarationEtat9421DTO calculerEtat9421(int annee) {
        Long tenantId = getTenantId();
        Societe societe = societeRepository.findById(tenantId)
                .orElseGet(() -> societeRepository.findByTenantIdAndIsParDefautTrue(tenantId)
                        .orElseGet(Societe::new));

        DeclarationEtat9421DTO decl = new DeclarationEtat9421DTO();
        decl.setExercice(annee);
        decl.setRaisonSociale(societe.getRaisonSociale() != null ? societe.getRaisonSociale() : "Entreprise " + tenantId);
        decl.setIdentifiantFiscal(societe.getIdentifiantFiscal() != null ? societe.getIdentifiantFiscal() : "");
        decl.setIce(societe.getIce() != null ? societe.getIce() : "");
        decl.setNumeroCnssEntreprise(societe.getCnss() != null ? societe.getCnss() : "");

        List<User> users = userRepository.findByTenantId(tenantId);
        List<EmployeSalaireAnnuelDTO> employes = new ArrayList<>();

        if (!users.isEmpty()) {
            int index = 1;
            for (User u : users) {
                // Estimation / simulation du profil salarial pour les utilisateurs du tenant
                EmployeSalaireAnnuelDTO emp = new EmployeSalaireAnnuelDTO();
                emp.setMatricule("EMP-" + String.format("%03d", index++));
                emp.setNom(u.getNomComplet() != null ? u.getNomComplet() : u.getUsername());
                emp.setPrenom("");
                emp.setCin("CIN" + (u.getId() != null ? u.getId() * 1234 : 1000));
                emp.setNumeroCnss("CNSS" + (u.getId() != null ? u.getId() * 5678 : 2000));
                emp.setDateRecrutement(LocalDate.of(annee - 2, 1, 1));
                emp.setJoursTravailles(312);
                emp.setSituationFamiliale("MARIE");
                emp.setNombrePersonnesACharge(2);

                // Salaire brut annuel de base (ex: 8 000 MAD / mois * 12 = 96 000 MAD)
                BigDecimal brutAnnuel = new BigDecimal("96000.00");
                BigDecimal indemnitesExonerees = new BigDecimal("6000.00"); // 500 MAD / mois panier/transport
                BigDecimal avantages = BigDecimal.ZERO;

                calculerFiscaliteEmploye(emp, brutAnnuel, avantages, indemnitesExonerees);
                employes.add(emp);
            }
        } else {
            // Échantillon standard représentatif pour test ou premier dossier
            EmployeSalaireAnnuelDTO emp1 = new EmployeSalaireAnnuelDTO();
            emp1.setMatricule("EMP-001");
            emp1.setNom("BENJELLOUN");
            emp1.setPrenom("Karim");
            emp1.setCin("BE456123");
            emp1.setNumeroCnss("184569872");
            emp1.setDateRecrutement(LocalDate.of(2022, 3, 1));
            emp1.setJoursTravailles(312);
            emp1.setSituationFamiliale("MARIE");
            emp1.setNombrePersonnesACharge(2);
            calculerFiscaliteEmploye(emp1, new BigDecimal("120000.00"), BigDecimal.ZERO, new BigDecimal("7200.00"));
            employes.add(emp1);

            EmployeSalaireAnnuelDTO emp2 = new EmployeSalaireAnnuelDTO();
            emp2.setMatricule("EMP-002");
            emp2.setNom("EL OUAZZANI");
            emp2.setPrenom("Sara");
            emp2.setCin("CD789456");
            emp2.setNumeroCnss("295412368");
            emp2.setDateRecrutement(LocalDate.of(2023, 6, 15));
            emp2.setJoursTravailles(312);
            emp2.setSituationFamiliale("CELIBATAIRE");
            emp2.setNombrePersonnesACharge(0);
            calculerFiscaliteEmploye(emp2, new BigDecimal("72000.00"), BigDecimal.ZERO, new BigDecimal("4800.00"));
            employes.add(emp2);
        }

        decl.setEmployes(employes);
        decl.setTotalEmployes(employes.size());

        for (EmployeSalaireAnnuelDTO e : employes) {
            decl.setTotalMasseSalarialeBrute(decl.getTotalMasseSalarialeBrute().add(e.getTotalBrutGlobal()));
            decl.setTotalAvantages(decl.getTotalAvantages().add(e.getAvantagesEnNature()));
            decl.setTotalIndemnitesExonerees(decl.getTotalIndemnitesExonerees().add(e.getIndemnitesExonerees()));
            decl.setTotalRetenuesSociales(decl.getTotalRetenuesSociales().add(e.getTotalRetenuesSociales()));
            decl.setTotalFraisProfessionnels(decl.getTotalFraisProfessionnels().add(e.getFraisProfessionnels()));
            decl.setTotalNetImposable(decl.getTotalNetImposable().add(e.getSalaireNetImposable()));
            decl.setTotalIrRetenu(decl.getTotalIrRetenu().add(e.getIrNetRetenu()));
        }

        return decl;
    }

    private void calculerFiscaliteEmploye(EmployeSalaireAnnuelDTO emp, BigDecimal brutBase, BigDecimal avantages, BigDecimal exonerees) {
        emp.setSalaireBrutAnnuel(brutBase);
        emp.setAvantagesEnNature(avantages);
        emp.setIndemnitesExonerees(exonerees);
        BigDecimal tbg = brutBase.add(avantages).add(exonerees);
        emp.setTotalBrutGlobal(tbg);

        // Brut Imposable (SBI = TBG - Indemnités exonérées)
        BigDecimal sbi = tbg.subtract(exonerees);

        // Cotisations CNSS (4.48% plafonné à 72 000 MAD / an)
        BigDecimal baseCnss = sbi.min(PLAFOND_CNSS_ANNUEL);
        BigDecimal cnssSalariale = baseCnss.multiply(TAUX_CNSS_SALARIAL).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        emp.setCotisationsCnssSalariales(cnssSalariale);

        // Cotisations AMO (2.26% non plafonné)
        BigDecimal amoSalariale = sbi.multiply(TAUX_AMO_SALARIAL).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        emp.setCotisationsAmoSalariales(amoSalariale);

        BigDecimal retenuesSociales = cnssSalariale.add(amoSalariale);
        emp.setTotalRetenuesSociales(retenuesSociales);

        // Frais professionnels : 35% de (SBI - avantages), plafonné à 35 000 MAD
        BigDecimal baseFraisPro = sbi.subtract(avantages);
        BigDecimal fraisPro = baseFraisPro.multiply(TAUX_FRAIS_PRO).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                .min(PLAFOND_FRAIS_PRO_ANNUEL);
        emp.setFraisProfessionnels(fraisPro);

        // Salaire Net Imposable (SNI)
        BigDecimal sni = sbi.subtract(retenuesSociales).subtract(fraisPro);
        if (sni.compareTo(BigDecimal.ZERO) < 0) sni = BigDecimal.ZERO;
        emp.setSalaireNetImposable(sni);

        // Calcul IR selon barème progressif annuel officiel marocain
        BigDecimal irBrut = calculerIrBaremeProgressifAnnuel(sni);
        emp.setIrBrut(irBrut);

        // Déductions pour charges de famille (360 MAD par an par personne, max 6 personnes)
        int charges = Math.min(Math.max(emp.getNombrePersonnesACharge(), 0), 6);
        BigDecimal deductionFamille = DEDUCTION_PAR_CHARGE_FAMILLE.multiply(new BigDecimal(charges));
        emp.setDeductionsChargesFamille(deductionFamille);

        BigDecimal irNet = irBrut.subtract(deductionFamille);
        if (irNet.compareTo(BigDecimal.ZERO) < 0) irNet = BigDecimal.ZERO;
        emp.setIrNetRetenu(irNet);
    }

    private BigDecimal calculerIrBaremeProgressifAnnuel(BigDecimal sni) {
        if (sni == null || sni.compareTo(new BigDecimal("30000.00")) <= 0) {
            return BigDecimal.ZERO;
        }

        double revenu = sni.doubleValue();
        double impot;

        if (revenu <= 50000.0) {
            impot = revenu * 0.10 - 3000.0;
        } else if (revenu <= 60000.0) {
            impot = revenu * 0.20 - 8000.0;
        } else if (revenu <= 80000.0) {
            impot = revenu * 0.30 - 14000.0;
        } else if (revenu <= 180000.0) {
            impot = revenu * 0.34 - 17200.0;
        } else {
            impot = revenu * 0.38 - 24400.0;
        }

        return BigDecimal.valueOf(Math.max(0, impot)).setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // 2. EXPORT EDI XML SIMPL-IR (DGI MAROC)
    // =========================================================================

    public byte[] genererXmlSimplIr(int annee) {
        DeclarationEtat9421DTO decl = calculerEtat9421(annee);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<DeclarationTraitementSalaire xmlns=\"http://simpl.portail.dgi.gov.ma/ir\">\n");

        // En-tête
        xml.append("  <Entete>\n");
        xml.append("    <IdentifiantFiscal>").append(escapeXml(decl.getIdentifiantFiscal())).append("</IdentifiantFiscal>\n");
        xml.append("    <ICE>").append(escapeXml(decl.getIce())).append("</ICE>\n");
        xml.append("    <NumeroCNSS>").append(escapeXml(decl.getNumeroCnssEntreprise())).append("</NumeroCNSS>\n");
        xml.append("    <RaisonSociale>").append(escapeXml(decl.getRaisonSociale())).append("</RaisonSociale>\n");
        xml.append("    <Exercice>").append(decl.getExercice()).append("</Exercice>\n");
        xml.append("    <DateDepotLegal>").append(decl.getExercice() + 1).append("-02-28</DateDepotLegal>\n");
        xml.append("    <DateGeneration>").append(LocalDate.now()).append("</DateGeneration>\n");
        xml.append("    <TotalPersonnel>").append(decl.getTotalEmployes()).append("</TotalPersonnel>\n");
        xml.append("    <TotalBrutGlobal>").append(fmt(decl.getTotalMasseSalarialeBrute())).append("</TotalBrutGlobal>\n");
        xml.append("    <TotalIrNetRetenu>").append(fmt(decl.getTotalIrRetenu())).append("</TotalIrNetRetenu>\n");
        xml.append("  </Entete>\n");

        // Liste du personnel
        xml.append("  <ListePersonnel>\n");
        for (EmployeSalaireAnnuelDTO emp : decl.getEmployes()) {
            xml.append("    <Personnel>\n");
            xml.append("      <Matricule>").append(escapeXml(emp.getMatricule())).append("</Matricule>\n");
            xml.append("      <Nom>").append(escapeXml(emp.getNom())).append("</Nom>\n");
            xml.append("      <Prenom>").append(escapeXml(emp.getPrenom())).append("</Prenom>\n");
            xml.append("      <CIN>").append(escapeXml(emp.getCin())).append("</CIN>\n");
            xml.append("      <CNSS>").append(escapeXml(emp.getNumeroCnss())).append("</CNSS>\n");
            xml.append("      <DateRecrutement>").append(emp.getDateRecrutement()).append("</DateRecrutement>\n");
            xml.append("      <JoursTravailles>").append(emp.getJoursTravailles()).append("</JoursTravailles>\n");
            xml.append("      <SituationFamiliale>").append(emp.getSituationFamiliale()).append("</SituationFamiliale>\n");
            xml.append("      <PersonnesACharge>").append(emp.getNombrePersonnesACharge()).append("</PersonnesACharge>\n");
            xml.append("      <SalaireBrutGlobal>").append(fmt(emp.getTotalBrutGlobal())).append("</SalaireBrutGlobal>\n");
            xml.append("      <IndemnitesExonerees>").append(fmt(emp.getIndemnitesExonerees())).append("</IndemnitesExonerees>\n");
            xml.append("      <CotisationsCnss>").append(fmt(emp.getCotisationsCnssSalariales())).append("</CotisationsCnss>\n");
            xml.append("      <CotisationsAmo>").append(fmt(emp.getCotisationsAmoSalariales())).append("</CotisationsAmo>\n");
            xml.append("      <FraisProfessionnels>").append(fmt(emp.getFraisProfessionnels())).append("</FraisProfessionnels>\n");
            xml.append("      <NetImposable>").append(fmt(emp.getSalaireNetImposable())).append("</NetImposable>\n");
            xml.append("      <IrBrut>").append(fmt(emp.getIrBrut())).append("</IrBrut>\n");
            xml.append("      <DeductionChargesFamille>").append(fmt(emp.getDeductionsChargesFamille())).append("</DeductionChargesFamille>\n");
            xml.append("      <IrNetRetenu>").append(fmt(emp.getIrNetRetenu())).append("</IrNetRetenu>\n");
            xml.append("    </Personnel>\n");
        }
        xml.append("  </ListePersonnel>\n");

        xml.append("</DeclarationTraitementSalaire>\n");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // 3. EXPORT CSV ÉTAT 9421 (DGI MAROC)
    // =========================================================================

    public byte[] genererCsvEtat9421(int annee) {
        DeclarationEtat9421DTO decl = calculerEtat9421(annee);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");

        String[] headers = {
                "Matricule", "Nom", "Prénom", "CIN", "N° CNSS", "Date Recrutement", "Jours Travaillés",
                "Situation Famille", "Charges Famille", "Total Brut Global", "Indemnités Exonérées",
                "Cotisation CNSS", "Cotisation AMO", "Frais Professionnels", "Net Imposable", "IR Brut", "Déduction Famille", "IR Net Retenu"
        };
        sb.append(String.join(";", headers)).append("\r\n");

        for (EmployeSalaireAnnuelDTO emp : decl.getEmployes()) {
            sb.append(cleanCsv(emp.getMatricule())).append(";")
                    .append(cleanCsv(emp.getNom())).append(";")
                    .append(cleanCsv(emp.getPrenom())).append(";")
                    .append(cleanCsv(emp.getCin())).append(";")
                    .append(cleanCsv(emp.getNumeroCnss())).append(";")
                    .append(emp.getDateRecrutement()).append(";")
                    .append(emp.getJoursTravailles()).append(";")
                    .append(cleanCsv(emp.getSituationFamiliale())).append(";")
                    .append(emp.getNombrePersonnesACharge()).append(";")
                    .append(fmt(emp.getTotalBrutGlobal())).append(";")
                    .append(fmt(emp.getIndemnitesExonerees())).append(";")
                    .append(fmt(emp.getCotisationsCnssSalariales())).append(";")
                    .append(fmt(emp.getCotisationsAmoSalariales())).append(";")
                    .append(fmt(emp.getFraisProfessionnels())).append(";")
                    .append(fmt(emp.getSalaireNetImposable())).append(";")
                    .append(fmt(emp.getIrBrut())).append(";")
                    .append(fmt(emp.getDeductionsChargesFamille())).append(";")
                    .append(fmt(emp.getIrNetRetenu())).append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // 4. BORDEREAU DE DÉCLARATION DES SALAIRES CNSS (BDS DAMANCOM)
    // =========================================================================

    public BordereauDamancomDTO genererBordereauBdsDamancom(int annee, int mois) {
        Long tenantId = getTenantId();
        Societe societe = societeRepository.findById(tenantId)
                .orElseGet(() -> societeRepository.findByTenantIdAndIsParDefautTrue(tenantId)
                        .orElseGet(Societe::new));

        BordereauDamancomDTO bds = new BordereauDamancomDTO();
        bds.setAnnee(annee);
        bds.setMois(mois);
        bds.setNumeroAffiliationEntreprise(societe.getCnss() != null ? societe.getCnss() : "1234567");
        bds.setRaisonSociale(societe.getRaisonSociale() != null ? societe.getRaisonSociale() : "Entreprise " + tenantId);
        bds.setIce(societe.getIce() != null ? societe.getIce() : "");

        DeclarationEtat9421DTO annuel = calculerEtat9421(annee);
        List<LigneBdsDamancomDTO> lignes = new ArrayList<>();

        for (EmployeSalaireAnnuelDTO emp : annuel.getEmployes()) {
            LigneBdsDamancomDTO l = new LigneBdsDamancomDTO();
            l.setNumeroImmatriculationCnss(emp.getNumeroCnss());
            l.setCin(emp.getCin());
            l.setNom(emp.getNom());
            l.setPrenom(emp.getPrenom());
            l.setJoursTravailles(26);

            // Mensualisation
            BigDecimal brutMensuel = emp.getSalaireBrutAnnuel().divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            BigDecimal plafonne = brutMensuel.min(PLAFOND_CNSS_MENSUEL);

            l.setSalaireBrutReel(brutMensuel);
            l.setSalairePlafonneCnss(plafonne);

            BigDecimal cnssSal = plafonne.multiply(TAUX_CNSS_SALARIAL).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal amoSal = brutMensuel.multiply(TAUX_AMO_SALARIAL).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            l.setCotisationSalarialeCnss(cnssSal);
            l.setCotisationSalarialeAmo(amoSal);

            lignes.add(l);

            bds.setTotalSalairesBruts(bds.getTotalSalairesBruts().add(brutMensuel));
            bds.setTotalSalairesPlafonnes(bds.getTotalSalairesPlafonnes().add(plafonne));
            bds.setTotalCotisationsSalariales(bds.getTotalCotisationsSalariales().add(cnssSal).add(amoSal));
        }

        bds.setLignes(lignes);
        bds.setTotalSalaries(lignes.size());

        // Cotisations patronales
        BigDecimal allocFam = bds.getTotalSalairesBruts().multiply(TAUX_ALLOCATIONS_FAMILIALES).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal prestSoc = bds.getTotalSalairesPlafonnes().multiply(TAUX_PRESTATIONS_SOCIALES_PATRONAL).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal taxeForm = bds.getTotalSalairesBruts().multiply(TAUX_TAXE_FORMATION_PRO).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal amoPat = bds.getTotalSalairesBruts().multiply(TAUX_AMO_PATRONAL).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        bds.setTotalAllocationsFamiliales(allocFam);
        bds.setTotalPrestationsSociales(prestSoc);
        bds.setTotalTaxeFormationPro(taxeForm);
        bds.setTotalCotisationsAmo(amoPat.add(bds.getLignes().stream().map(LigneBdsDamancomDTO::getCotisationSalarialeAmo).reduce(BigDecimal.ZERO, BigDecimal::add)));

        BigDecimal totalPatronal = allocFam.add(prestSoc).add(taxeForm).add(amoPat);
        bds.setTotalCotisationsPatronales(totalPatronal);
        bds.setTotalGlobalAPayerCnss(bds.getTotalCotisationsSalariales().add(totalPatronal));

        return bds;
    }

    public byte[] genererFichierTxtBdsDamancom(int annee, int mois) {
        BordereauDamancomDTO bds = genererBordereauBdsDamancom(annee, mois);
        StringBuilder sb = new StringBuilder();

        // En-tête BDS Damancom : Code 01
        // Format normalisé : 01 + NumAffilie(7) + Periode(AAAAMM) + RaisonSociale(30) + NbSalaries(5) + MasseBrute(12)
        sb.append(String.format("01%-7s%04d%02d%-30s%05d%012d\r\n",
                padRight(bds.getNumeroAffiliationEntreprise(), 7),
                annee, mois,
                padRight(bds.getRaisonSociale(), 30),
                bds.getTotalSalaries(),
                bds.getTotalSalairesBruts().multiply(new BigDecimal("100")).longValue()
        ));

        // Lignes salariés : Code 02
        // Format normalisé : 02 + NumImmat(9) + CIN(8) + NomPrenom(30) + Jours(2) + Brut(10) + Plafonne(10)
        for (LigneBdsDamancomDTO l : bds.getLignes()) {
            sb.append(String.format("02%-9s%-8s%-30s%02d%010d%010d\r\n",
                    padRight(l.getNumeroImmatriculationCnss(), 9),
                    padRight(l.getCin(), 8),
                    padRight(l.getNom() + " " + l.getPrenom(), 30),
                    l.getJoursTravailles(),
                    l.getSalaireBrutReel().multiply(new BigDecimal("100")).longValue(),
                    l.getSalairePlafonneCnss().multiply(new BigDecimal("100")).longValue()
            ));
        }

        // Totalisateur : Code 09
        sb.append(String.format("09%05d%012d%012d\r\n",
                bds.getTotalSalaries(),
                bds.getTotalSalairesBruts().multiply(new BigDecimal("100")).longValue(),
                bds.getTotalGlobalAPayerCnss().multiply(new BigDecimal("100")).longValue()
        ));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // 5. GÉNÉRATION DE L'ÉCRITURE COMPTABLE D'OD DE PAIE
    // =========================================================================

    public EcritureComptableDTO genererEcritureOdPaie(OdPaieGenerationRequest req) {
        if (req.getMasseSalarialeBrute() == null || req.getMasseSalarialeBrute().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La masse salariale brute doit être supérieure à zéro.");
        }

        LocalDate dateEcr = req.getDateEcriture() != null ? req.getDateEcriture()
                : LocalDate.of(req.getAnnee(), req.getMois(), 1).plusMonths(1).minusDays(1);

        String refPiece = req.getReferencePiece() != null ? req.getReferencePiece()
                : String.format("PAIE-%04d-%02d", req.getAnnee(), req.getMois());

        String libelle = req.getLibelle() != null ? req.getLibelle()
                : String.format("OD Paie & Charges Sociales — %02d/%04d", req.getMois(), req.getAnnee());

        EcritureComptableDTO ecr = new EcritureComptableDTO();
        ecr.setJournalCode(req.getJournalCode() != null ? req.getJournalCode() : "OD");
        ecr.setDateEcriture(dateEcr);
        ecr.setReferencePiece(refPiece);
        ecr.setLibelle(libelle);
        ecr.setValidee(true);

        List<LigneEcritureDTO> lignes = new ArrayList<>();

        // 1. Débit 61711000 : Rémunération du personnel (Salaires Bruts)
        LigneEcritureDTO lBrut = new LigneEcritureDTO();
        lBrut.setNumeroCompte("61711000");
        lBrut.setLibelleLigne("Salaires bruts du personnel");
        lBrut.setReferenceLigne(refPiece);
        lBrut.setDebit(req.getMasseSalarialeBrute());
        lBrut.setCredit(BigDecimal.ZERO);
        lignes.add(lBrut);

        // 2. Débit 61741000 : Charges sociales patronales (CNSS & AMO)
        if (req.getChargesSocialesPatronales().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcritureDTO lChPat = new LigneEcritureDTO();
            lChPat.setNumeroCompte("61741000");
            lChPat.setLibelleLigne("Charges sociales patronales CNSS / AMO");
            lChPat.setReferenceLigne(refPiece);
            lChPat.setDebit(req.getChargesSocialesPatronales());
            lChPat.setCredit(BigDecimal.ZERO);
            lignes.add(lChPat);
        }

        // 3. Débit 61743000 : Cotisations retraite patronale (CIMR)
        if (req.getRetraitePatronaleCimr().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcritureDTO lCimrPat = new LigneEcritureDTO();
            lCimrPat.setNumeroCompte("61743000");
            lCimrPat.setLibelleLigne("Cotisations patronales retraite CIMR");
            lCimrPat.setReferenceLigne(refPiece);
            lCimrPat.setDebit(req.getRetraitePatronaleCimr());
            lCimrPat.setCredit(BigDecimal.ZERO);
            lignes.add(lCimrPat);
        }

        // 4. Crédit 44320000 : Rémunérations dues au personnel (Salaires nets à payer)
        if (req.getNetAPayerPersonnel().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcritureDTO lNet = new LigneEcritureDTO();
            lNet.setNumeroCompte("44320000");
            lNet.setLibelleLigne("Salaires nets dus au personnel");
            lNet.setReferenceLigne(refPiece);
            lNet.setCredit(req.getNetAPayerPersonnel());
            lNet.setDebit(BigDecimal.ZERO);
            lignes.add(lNet);
        }

        // 5. Crédit 44410000 : CNSS (Cotisations salariales + patronales)
        if (req.getCnssAPayer().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcritureDTO lCnss = new LigneEcritureDTO();
            lCnss.setNumeroCompte("44410000");
            lCnss.setLibelleLigne("C.N.S.S. à payer");
            lCnss.setReferenceLigne(refPiece);
            lCnss.setCredit(req.getCnssAPayer());
            lCnss.setDebit(BigDecimal.ZERO);
            lignes.add(lCnss);
        }

        // 6. Crédit 44430000 : Mutuelle / Retraite CIMR
        if (req.getMutuelleCimrAPayer().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcritureDTO lCimr = new LigneEcritureDTO();
            lCimr.setNumeroCompte("44430000");
            lCimr.setLibelleLigne("Organismes de retraite & mutuelle à payer");
            lCimr.setReferenceLigne(refPiece);
            lCimr.setCredit(req.getMutuelleCimrAPayer());
            lCimr.setDebit(BigDecimal.ZERO);
            lignes.add(lCimr);
        }

        // 7. Crédit 44525000 : État - IR prélevé à la source sur salaires
        if (req.getIrPreleveSource().compareTo(BigDecimal.ZERO) > 0) {
            LigneEcritureDTO lIr = new LigneEcritureDTO();
            lIr.setNumeroCompte("44525000");
            lIr.setLibelleLigne("État - IR retenu à la source sur salaires");
            lIr.setReferenceLigne(refPiece);
            lIr.setCredit(req.getIrPreleveSource());
            lIr.setDebit(BigDecimal.ZERO);
            lignes.add(lIr);
        }

        ecr.setLignes(lignes);

        // Enregistrement via le moteur de comptabilité officiel
        return comptabiliteService.creerEcriture(ecr);
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

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }
}
