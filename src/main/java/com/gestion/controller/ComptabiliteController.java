package com.gestion.controller;

import com.gestion.persistent.dto.*;
import com.gestion.service.ComptabiliteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comptabilite")
@CrossOrigin(origins = "*")
public class ComptabiliteController {

    private final ComptabiliteService comptabiliteService;
    private final com.gestion.service.TvaAvanceeService tvaAvanceeService;

    public ComptabiliteController(ComptabiliteService comptabiliteService, com.gestion.service.TvaAvanceeService tvaAvanceeService) {
        this.comptabiliteService = comptabiliteService;
        this.tvaAvanceeService = tvaAvanceeService;
    }

    // =========================================================================
    // PLAN COMPTABLE
    // =========================================================================

    @GetMapping("/plan-comptable")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<CompteComptableDTO>> getPlanComptable() {
        return ResponseEntity.ok(comptabiliteService.getPlanComptable());
    }

    @PostMapping("/plan-comptable")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<CompteComptableDTO> creerCompte(@RequestBody CompteComptableDTO dto) {
        return new ResponseEntity<>(comptabiliteService.creerCompte(dto), HttpStatus.CREATED);
    }

    // =========================================================================
    // JOURNAUX
    // =========================================================================

    @GetMapping("/journaux")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<JournalComptableDTO>> getJournaux() {
        return ResponseEntity.ok(comptabiliteService.getJournaux());
    }

    @GetMapping("/journaux/{id}")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<JournalComptableDTO> getJournalById(@PathVariable Long id) {
        return ResponseEntity.ok(comptabiliteService.getJournalById(id));
    }

    @PostMapping("/journaux")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<JournalComptableDTO> creerJournal(@RequestBody JournalComptableDTO dto) {
        return new ResponseEntity<>(comptabiliteService.creerJournal(dto), HttpStatus.CREATED);
    }

    @PutMapping("/journaux/{id}")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<JournalComptableDTO> modifierJournal(@PathVariable Long id, @RequestBody JournalComptableDTO dto) {
        return ResponseEntity.ok(comptabiliteService.modifierJournal(id, dto));
    }

    @PatchMapping("/journaux/{id}/toggle-actif")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<JournalComptableDTO> toggleActifJournal(@PathVariable Long id) {
        return ResponseEntity.ok(comptabiliteService.toggleActifJournal(id));
    }

    @DeleteMapping("/journaux/{id}")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<Void> supprimerJournal(@PathVariable Long id) {
        comptabiliteService.supprimerJournal(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // ÉCRITURES COMPTABLES
    // =========================================================================

    @GetMapping("/ecritures")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<EcritureComptableDTO>> getEcritures(
            @RequestParam(required = false) Long journalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getEcritures(journalId, dateDebut, dateFin));
    }

    @GetMapping("/ecritures/{id}")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<EcritureComptableDTO> getEcritureById(@PathVariable Long id) {
        return ResponseEntity.ok(comptabiliteService.getEcritureById(id));
    }

    @PostMapping("/ecritures")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<EcritureComptableDTO> creerEcriture(@RequestBody EcritureComptableDTO dto) {
        return new ResponseEntity<>(comptabiliteService.creerEcriture(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/ecritures/{id}/valider")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<EcritureComptableDTO> validerEcriture(@PathVariable Long id) {
        return ResponseEntity.ok(comptabiliteService.validerEcriture(id));
    }

    // =========================================================================
    // GRAND LIVRE & BALANCE & TVA
    // =========================================================================

    @GetMapping("/grand-livre")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<GrandLivreDTO>> getGrandLivre(
            @RequestParam(required = false) String numeroCompte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getGrandLivre(numeroCompte, dateDebut, dateFin));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<BalanceCompteDTO>> getBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getBalance(dateDebut, dateFin));
    }

    @GetMapping("/tva")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<DeclarationTvaDTO> getDeclarationTva(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getDeclarationTva(dateDebut, dateFin));
    }

    @GetMapping("/tva/releve-deduction")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<com.gestion.persistent.dto.ReleveDeductionTvaDTO> getReleveDeduction(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false, defaultValue = "false") Boolean seulementRapproches) {
        return ResponseEntity.ok(tvaAvanceeService.getReleveDeduction(dateDebut, dateFin, seulementRapproches));
    }

    @GetMapping("/tva/releve-deduction/export-xlsx")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<byte[]> exporterReleveDeductionXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false, defaultValue = "false") Boolean seulementRapproches) {
        byte[] xlsxBytes = tvaAvanceeService.exporterReleveDeductionXlsx(dateDebut, dateFin, seulementRapproches);
        String filename = "releve_deduction_tva_art112_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(xlsxBytes);
    }

    @GetMapping(value = "/export/csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<byte[]> exporterEcrituresCsv(
            @RequestParam(required = false) Long journalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        byte[] csvBytes = comptabiliteService.exporterEcrituresCsv(journalId, dateDebut, dateFin);
        String filename = "ecritures_comptables_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvBytes);
    }

    // =========================================================================
    // LETTRAGE COMPTABLE
    // =========================================================================

    @GetMapping("/lettrage/lignes-ouvertes")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<LigneLettrageDTO>> getLignesNonLettrees(
            @RequestParam(required = false, defaultValue = "3421") String prefixCompte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getLignesNonLettrees(prefixCompte, dateDebut, dateFin));
    }

    @GetMapping("/lettrage/lignes-lettrees")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<List<LigneLettrageDTO>> getLignesLettrees(
            @RequestParam(required = false, defaultValue = "3421") String prefixCompte) {
        return ResponseEntity.ok(comptabiliteService.getLignesLettrees(prefixCompte));
    }

    @PostMapping("/lettrage/valider")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<Map<String, String>> validerLettrage(@RequestBody LettrageValidationRequest request) {
        String code = comptabiliteService.validerLettrage(request.getLigneIds());
        return ResponseEntity.ok(Map.of("codeLettrage", code, "message", "Lettrage effectué avec succès"));
    }

    @PostMapping("/lettrage/annuler")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<Map<String, String>> annulerLettrage(@RequestParam String codeLettrage) {
        comptabiliteService.annulerLettrage(codeLettrage);
        return ResponseEntity.ok(Map.of("message", "Lettrage " + codeLettrage + " annulé"));
    }

    @PostMapping("/lettrage/auto")
    @PreAuthorize("hasAuthority('COMPTA_WRITE')")
    public ResponseEntity<Map<String, Object>> autoLettrage(@RequestParam(required = false, defaultValue = "3421") String prefixCompte) {
        return ResponseEntity.ok(comptabiliteService.autoLettrage(prefixCompte));
    }

    // =========================================================================
    // BILAN OFFICIEL PCGM (ACTIF / PASSIF)
    // =========================================================================

    @GetMapping("/bilan-officiel")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<BilanOfficielDTO> getBilanOfficiel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateArrete) {
        return ResponseEntity.ok(comptabiliteService.getBilanOfficiel(dateArrete));
    }

    // =========================================================================
    // CPC OFFICIEL PCGM (COMPTE DE PRODUITS ET CHARGES)
    // =========================================================================

    @GetMapping("/cpc-officiel")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<CpcOfficielDTO> getCpcOfficiel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getCpcOfficiel(dateDebut, dateFin));
    }

    // =========================================================================
    // EXPORT FEC DGI (18 COLONNES NORMALISÉES)
    // =========================================================================

    @GetMapping(value = "/export/fec-dgi", produces = "text/plain; charset=UTF-8")
    @PreAuthorize("hasAuthority('COMPTA_READ')")
    public ResponseEntity<byte[]> exporterFecDgi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false, defaultValue = "\t") String separateur) {
        byte[] fecBytes = comptabiliteService.exporterFecDgi(dateDebut, dateFin, separateur);
        String filename = "FEC_DGI_" + (dateFin != null ? dateFin : LocalDate.now()) + ".txt";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .body(fecBytes);
    }
}
