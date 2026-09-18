package com.gestion.persistent.dto;

import java.time.LocalDate;

public class LiasseFiscaleCompleteDTO {

    // Identification de la société & Période
    private Integer anneeFiscale;
    private LocalDate dateDebutExercice;
    private LocalDate dateFinExercice;
    private Long tenantId;
    private String raisonSociale = "ENTREPRISE COMMERCIALE & INDUSTRIELLE";
    private String identifiantFiscal = "00000000";
    private String ice = "000000000000000";
    private String registreCommerce = "00000";
    private String numeroCnss = "0000000";
    private String activitePrincipale = "Commerce et Services";

    // Les Tableaux Réglementaires DGI (Modèle Normal Marocain)
    private BilanOfficielDTO tableau1BilanActif;
    private BilanOfficielDTO tableau2BilanPassif;
    private CpcOfficielDTO tableau3Cpc;
    private LiasseTableauT4TfrDTO tableau4EsgTfr;
    private LiasseTableauT5CafDTO tableau5EsgCaf;
    private LiasseTableauT6FinancementDTO tableau6Financement;
    private LiasseTableauT7ProvisionsDTO tableau7Provisions;
    private LiasseTableauT8CreancesDettesDTO tableau8CreancesDettes;
    private LiasseTableauT9TitresDTO tableau9TitresParticipation;
    private LiasseTableauT10DTO tableau10Immobilisations;
    private LiasseTableauT11DTO tableau11Amortissements;
    private LiasseTableauT12PlusMoinsValuesDTO tableau12PlusMoinsValuesCessions;
    private LiasseTableauT13CapitalSocialDTO tableau13CapitalSocial;
    private LiasseTableauT14AffectationDTO tableau14AffectationResultat;
    private LiasseTableauT15DetailCpcDTO tableau15DetailCpc;
    private LiasseTableauT16ResultatFiscalDTO tableau16DeterminationResultatFiscal;
    private LiasseTableauT17CalculIsDTO tableau17CalculIsEtCm;
    private LiasseTableauT18CreditBailDTO tableau18BiensCreditBail;
    private LiasseTableauT19DerogationsDTO tableaux19Et20DerogationsEtMethodes;

    public LiasseFiscaleCompleteDTO() {}

    public Integer getAnneeFiscale() { return anneeFiscale; }
    public void setAnneeFiscale(Integer anneeFiscale) { this.anneeFiscale = anneeFiscale; }

    public LocalDate getDateDebutExercice() { return dateDebutExercice; }
    public void setDateDebutExercice(LocalDate dateDebutExercice) { this.dateDebutExercice = dateDebutExercice; }

    public LocalDate getDateFinExercice() { return dateFinExercice; }
    public void setDateFinExercice(LocalDate dateFinExercice) { this.dateFinExercice = dateFinExercice; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getRegistreCommerce() { return registreCommerce; }
    public void setRegistreCommerce(String registreCommerce) { this.registreCommerce = registreCommerce; }

    public String getNumeroCnss() { return numeroCnss; }
    public void setNumeroCnss(String numeroCnss) { this.numeroCnss = numeroCnss; }

    public String getActivitePrincipale() { return activitePrincipale; }
    public void setActivitePrincipale(String activitePrincipale) { this.activitePrincipale = activitePrincipale; }

    public BilanOfficielDTO getTableau1BilanActif() { return tableau1BilanActif; }
    public void setTableau1BilanActif(BilanOfficielDTO tableau1BilanActif) { this.tableau1BilanActif = tableau1BilanActif; }

    public BilanOfficielDTO getTableau2BilanPassif() { return tableau2BilanPassif; }
    public void setTableau2BilanPassif(BilanOfficielDTO tableau2BilanPassif) { this.tableau2BilanPassif = tableau2BilanPassif; }

    public CpcOfficielDTO getTableau3Cpc() { return tableau3Cpc; }
    public void setTableau3Cpc(CpcOfficielDTO tableau3Cpc) { this.tableau3Cpc = tableau3Cpc; }

    public LiasseTableauT4TfrDTO getTableau4EsgTfr() { return tableau4EsgTfr; }
    public void setTableau4EsgTfr(LiasseTableauT4TfrDTO tableau4EsgTfr) { this.tableau4EsgTfr = tableau4EsgTfr; }

    public LiasseTableauT5CafDTO getTableau5EsgCaf() { return tableau5EsgCaf; }
    public void setTableau5EsgCaf(LiasseTableauT5CafDTO tableau5EsgCaf) { this.tableau5EsgCaf = tableau5EsgCaf; }

    public LiasseTableauT6FinancementDTO getTableau6Financement() { return tableau6Financement; }
    public void setTableau6Financement(LiasseTableauT6FinancementDTO tableau6Financement) { this.tableau6Financement = tableau6Financement; }

    public LiasseTableauT7ProvisionsDTO getTableau7Provisions() { return tableau7Provisions; }
    public void setTableau7Provisions(LiasseTableauT7ProvisionsDTO tableau7Provisions) { this.tableau7Provisions = tableau7Provisions; }

    public LiasseTableauT8CreancesDettesDTO getTableau8CreancesDettes() { return tableau8CreancesDettes; }
    public void setTableau8CreancesDettes(LiasseTableauT8CreancesDettesDTO tableau8CreancesDettes) { this.tableau8CreancesDettes = tableau8CreancesDettes; }

    public LiasseTableauT9TitresDTO getTableau9TitresParticipation() { return tableau9TitresParticipation; }
    public void setTableau9TitresParticipation(LiasseTableauT9TitresDTO tableau9TitresParticipation) { this.tableau9TitresParticipation = tableau9TitresParticipation; }

    public LiasseTableauT10DTO getTableau10Immobilisations() { return tableau10Immobilisations; }
    public void setTableau10Immobilisations(LiasseTableauT10DTO tableau10Immobilisations) { this.tableau10Immobilisations = tableau10Immobilisations; }

    public LiasseTableauT11DTO getTableau11Amortissements() { return tableau11Amortissements; }
    public void setTableau11Amortissements(LiasseTableauT11DTO tableau11Amortissements) { this.tableau11Amortissements = tableau11Amortissements; }

    public LiasseTableauT12PlusMoinsValuesDTO getTableau12PlusMoinsValuesCessions() { return tableau12PlusMoinsValuesCessions; }
    public void setTableau12PlusMoinsValuesCessions(LiasseTableauT12PlusMoinsValuesDTO tableau12PlusMoinsValuesCessions) { this.tableau12PlusMoinsValuesCessions = tableau12PlusMoinsValuesCessions; }

    public LiasseTableauT13CapitalSocialDTO getTableau13CapitalSocial() { return tableau13CapitalSocial; }
    public void setTableau13CapitalSocial(LiasseTableauT13CapitalSocialDTO tableau13CapitalSocial) { this.tableau13CapitalSocial = tableau13CapitalSocial; }

    public LiasseTableauT14AffectationDTO getTableau14AffectationResultat() { return tableau14AffectationResultat; }
    public void setTableau14AffectationResultat(LiasseTableauT14AffectationDTO tableau14AffectationResultat) { this.tableau14AffectationResultat = tableau14AffectationResultat; }

    public LiasseTableauT15DetailCpcDTO getTableau15DetailCpc() { return tableau15DetailCpc; }
    public void setTableau15DetailCpc(LiasseTableauT15DetailCpcDTO tableau15DetailCpc) { this.tableau15DetailCpc = tableau15DetailCpc; }

    public LiasseTableauT16ResultatFiscalDTO getTableau16DeterminationResultatFiscal() { return tableau16DeterminationResultatFiscal; }
    public void setTableau16DeterminationResultatFiscal(LiasseTableauT16ResultatFiscalDTO tableau16DeterminationResultatFiscal) { this.tableau16DeterminationResultatFiscal = tableau16DeterminationResultatFiscal; }

    public LiasseTableauT17CalculIsDTO getTableau17CalculIsEtCm() { return tableau17CalculIsEtCm; }
    public void setTableau17CalculIsEtCm(LiasseTableauT17CalculIsDTO tableau17CalculIsEtCm) { this.tableau17CalculIsEtCm = tableau17CalculIsEtCm; }

    public LiasseTableauT18CreditBailDTO getTableau18BiensCreditBail() { return tableau18BiensCreditBail; }
    public void setTableau18BiensCreditBail(LiasseTableauT18CreditBailDTO tableau18BiensCreditBail) { this.tableau18BiensCreditBail = tableau18BiensCreditBail; }

    public LiasseTableauT19DerogationsDTO getTableaux19Et20DerogationsEtMethodes() { return tableaux19Et20DerogationsEtMethodes; }
    public void setTableaux19Et20DerogationsEtMethodes(LiasseTableauT19DerogationsDTO tableaux19Et20DerogationsEtMethodes) { this.tableaux19Et20DerogationsEtMethodes = tableaux19Et20DerogationsEtMethodes; }

    // Alias getters directs T1 à T20
    public BilanOfficielDTO getTableau1() { return tableau1BilanActif; }
    public BilanOfficielDTO getTableau2() { return tableau2BilanPassif; }
    public CpcOfficielDTO getTableau3() { return tableau3Cpc; }
    public LiasseTableauT4TfrDTO getTableau4() { return tableau4EsgTfr; }
    public LiasseTableauT5CafDTO getTableau5() { return tableau5EsgCaf; }
    public LiasseTableauT6FinancementDTO getTableau6() { return tableau6Financement; }
    public LiasseTableauT7ProvisionsDTO getTableau7() { return tableau7Provisions; }
    public LiasseTableauT8CreancesDettesDTO getTableau8() { return tableau8CreancesDettes; }
    public LiasseTableauT9TitresDTO getTableau9() { return tableau9TitresParticipation; }
    public LiasseTableauT10DTO getTableau10() { return tableau10Immobilisations; }
    public LiasseTableauT11DTO getTableau11() { return tableau11Amortissements; }
    public LiasseTableauT12PlusMoinsValuesDTO getTableau12() { return tableau12PlusMoinsValuesCessions; }
    public LiasseTableauT13CapitalSocialDTO getTableau13() { return tableau13CapitalSocial; }
    public LiasseTableauT14AffectationDTO getTableau14() { return tableau14AffectationResultat; }
    public LiasseTableauT15DetailCpcDTO getTableau15() { return tableau15DetailCpc; }
    public LiasseTableauT16ResultatFiscalDTO getTableau16() { return tableau16DeterminationResultatFiscal; }
    public LiasseTableauT17CalculIsDTO getTableau17() { return tableau17CalculIsEtCm; }
    public LiasseTableauT18CreditBailDTO getTableau18() { return tableau18BiensCreditBail; }
    public LiasseTableauT19DerogationsDTO getTableau19() { return tableaux19Et20DerogationsEtMethodes; }
    public LiasseTableauT19DerogationsDTO getTableau20() { return tableaux19Et20DerogationsEtMethodes; }
}
