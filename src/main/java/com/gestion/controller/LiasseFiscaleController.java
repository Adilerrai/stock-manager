package com.gestion.controller;

import com.gestion.persistent.dto.*;
import com.gestion.service.LiasseFiscaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/liasse-fiscale")
public class LiasseFiscaleController {

    private final LiasseFiscaleService liasseFiscaleService;

    public LiasseFiscaleController(LiasseFiscaleService liasseFiscaleService) {
        this.liasseFiscaleService = liasseFiscaleService;
    }

    private int anneeEffective(Integer annee) {
        return annee != null ? annee : LocalDate.now().getYear();
    }

    /**
     * LIASSE COMPLETE (20 TABLEAUX RÉGLEMENTAIRES DGI MODÈLE NORMAL)
     */
    @GetMapping("/complete")
    public ResponseEntity<LiasseFiscaleCompleteDTO> getLiasseFiscaleComplete(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee))
        );
    }

    /**
     * T1 - BILAN ACTIF
     */
    @GetMapping("/t1-bilan-actif")
    public ResponseEntity<BilanOfficielDTO> getTableau1BilanActif(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau1BilanActif()
        );
    }

    /**
     * T2 - BILAN PASSIF
     */
    @GetMapping("/t2-bilan-passif")
    public ResponseEntity<BilanOfficielDTO> getTableau2BilanPassif(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau2BilanPassif()
        );
    }

    /**
     * T3 - COMPTE DE PRODUITS ET CHARGES (CPC)
     */
    @GetMapping("/t3-cpc")
    public ResponseEntity<CpcOfficielDTO> getTableau3Cpc(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau3Cpc()
        );
    }

    /**
     * T4 - ESG / TFR (TABLEAU DE FORMATION DES RÉSULTATS)
     */
    @GetMapping("/t4-esg-tfr")
    public ResponseEntity<LiasseTableauT4TfrDTO> getTableau4EsgTfr(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau4EsgTfr()
        );
    }

    /**
     * T5 - ESG / CAF (CAPACITÉ D'AUTOFINANCEMENT)
     */
    @GetMapping("/t5-esg-caf")
    public ResponseEntity<LiasseTableauT5CafDTO> getTableau5EsgCaf(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau5EsgCaf()
        );
    }

    /**
     * T6 - TABLEAU DE FINANCEMENT DE L'EXERCICE
     */
    @GetMapping("/t6-financement")
    public ResponseEntity<LiasseTableauT6FinancementDTO> getTableau6Financement(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau6Financement()
        );
    }

    /**
     * T7 - PROVISIONS
     */
    @GetMapping("/t7-provisions")
    public ResponseEntity<LiasseTableauT7ProvisionsDTO> getTableau7Provisions(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau7Provisions()
        );
    }

    /**
     * T8 - CRÉANCES ET DETTES
     */
    @GetMapping("/t8-creances-dettes")
    public ResponseEntity<LiasseTableauT8CreancesDettesDTO> getTableau8CreancesDettes(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau8CreancesDettes()
        );
    }

    /**
     * T9 - TITRES DE PARTICIPATION ET AUTRES TITRES IMMOBILISÉS
     */
    @GetMapping({"/t9", "/t9-titres"})
    public ResponseEntity<LiasseTableauT9TitresDTO> getTableau9Titres(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau9TitresParticipation()
        );
    }

    /**
     * T10 - TABLEAU DES IMMOBILISATIONS (A3)
     */
    @GetMapping({"/t10", "/t10-immobilisations"})
    public ResponseEntity<LiasseTableauT10DTO> getTableau10Immobilisations(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau10Immobilisations()
        );
    }

    /**
     * T11 - TABLEAU DES AMORTISSEMENTS (A4)
     */
    @GetMapping({"/t11", "/t11-amortissements"})
    public ResponseEntity<LiasseTableauT11DTO> getTableau11Amortissements(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau11Amortissements()
        );
    }

    /**
     * T12 - PLUS OU MOINS-VALUES SUR CESSIONS D'IMMOBILISATIONS (T23)
     */
    @GetMapping({"/t12", "/t12-cessions"})
    public ResponseEntity<LiasseTableauT12PlusMoinsValuesDTO> getTableau12Cessions(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau12PlusMoinsValuesCessions()
        );
    }

    /**
     * T13 - RÉPARTITION DU CAPITAL SOCIAL
     */
    @GetMapping({"/t13", "/t13-capital-social"})
    public ResponseEntity<LiasseTableauT13CapitalSocialDTO> getTableau13CapitalSocial(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau13CapitalSocial()
        );
    }

    /**
     * T14 - AFFECTATION DES RÉSULTATS
     */
    @GetMapping({"/t14", "/t14-affectation-resultat"})
    public ResponseEntity<LiasseTableauT14AffectationDTO> getTableau14AffectationResultat(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau14AffectationResultat()
        );
    }

    /**
     * T15 - DÉTAIL DES POSTES DU CPC
     */
    @GetMapping("/t15-detail-cpc")
    public ResponseEntity<LiasseTableauT15DetailCpcDTO> getTableau15DetailCpc(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau15DetailCpc()
        );
    }

    /**
     * T16 - DÉTERMINATION DU RÉSULTAT FISCAL (PASSAGE COMPTABLE AU FISCAL)
     */
    @GetMapping("/t16-resultat-fiscal")
    public ResponseEntity<LiasseTableauT16ResultatFiscalDTO> getTableau16ResultatFiscal(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau16DeterminationResultatFiscal()
        );
    }

    /**
     * T17 - CALCUL DE L'IS ET DE LA COTISATION MINIMALE
     */
    @GetMapping("/t17-calcul-is")
    public ResponseEntity<LiasseTableauT17CalculIsDTO> getTableau17CalculIs(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau17CalculIsEtCm()
        );
    }

    /**
     * T18 - BIENS EN CRÉDIT-BAIL (LEASING)
     */
    @GetMapping({"/t18", "/t18-credit-bail"})
    public ResponseEntity<LiasseTableauT18CreditBailDTO> getTableau18CreditBail(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableau18BiensCreditBail()
        );
    }

    /**
     * T19 - DÉROGATIONS AUX PRINCIPES COMPTABLES
     */
    @GetMapping({"/t19", "/t19-derogations"})
    public ResponseEntity<LiasseTableauT19DerogationsDTO> getTableau19Derogations(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableaux19Et20DerogationsEtMethodes()
        );
    }

    /**
     * T20 - CHANGEMENTS DE MÉTHODES COMPTABLES
     */
    @GetMapping({"/t20", "/t20-changements-methodes"})
    public ResponseEntity<LiasseTableauT19DerogationsDTO> getTableau20ChangementsMethodes(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(
                liasseFiscaleService.getLiasseFiscaleComplete(anneeEffective(annee)).getTableaux19Et20DerogationsEtMethodes()
        );
    }
}
