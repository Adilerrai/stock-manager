package com.gestion.controller;

import com.gestion.persistent.dto.*;
import com.gestion.service.RapprochementBancaireService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tresorerie/rapprochement")
@CrossOrigin(origins = "*")
public class RapprochementBancaireController {

    private final RapprochementBancaireService rapprochementService;

    public RapprochementBancaireController(RapprochementBancaireService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }

    @PostMapping("/import-ocr/{compteId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<ReleveBancaireDTO> importerReleveOcr(
            @PathVariable Long compteId,
            @RequestParam("file") MultipartFile file) throws IOException {
        ReleveBancaireDTO dto = rapprochementService.importerReleveOcr(
                compteId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PostMapping("/preview-ocr")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<List<LigneReleveBancaireDTO>> previewOcr(
            @RequestParam("file") MultipartFile file) throws IOException {
        List<LigneReleveBancaireDTO> list = rapprochementService.previewOcr(
                file.getBytes(), file.getOriginalFilename(), file.getContentType());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/comparatif-5141/{compteId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<RapprochementComparatif5141DTO> getComparatif5141(
            @PathVariable Long compteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateArrete) {
        LocalDate fin = (dateFin != null) ? dateFin : dateArrete;
        return ResponseEntity.ok(rapprochementService.getComparatif5141(compteId, dateDebut, fin));
    }

    @PostMapping("/creer-ecriture")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<ItemComparatifRapprochementDTO> creerEcriturePourLigne(
            @RequestBody CreerEcritureReleveRequest request) {
        return ResponseEntity.ok(rapprochementService.creerEcriturePourLigne(request));
    }

    @PostMapping("/auto-5141/{releveId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<Map<String, Object>> autoRapprocher5141(@PathVariable Long releveId) {
        return ResponseEntity.ok(rapprochementService.autoRapprocher5141(releveId));
    }

    @PostMapping("/import/{compteId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<ReleveBancaireDTO> importerReleve(
            @PathVariable Long compteId,
            @RequestParam("file") MultipartFile file) throws IOException {
        ReleveBancaireDTO dto = rapprochementService.importerReleveCsv(compteId, file.getOriginalFilename(), file.getBytes());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }


    @GetMapping("/releves")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<List<ReleveBancaireDTO>> getReleves(@RequestParam(required = false) Long compteId) {
        return ResponseEntity.ok(rapprochementService.getReleves(compteId));
    }

    @GetMapping("/releves/{id}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<ReleveBancaireDTO> getReleveDetail(@PathVariable Long id) {
        return ResponseEntity.ok(rapprochementService.getReleveDetail(id));
    }

    @GetMapping("/etat/{compteId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<RapprochementEtatDTO> getRapprochementEtat(
            @PathVariable Long compteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateArrete) {
        return ResponseEntity.ok(rapprochementService.getRapprochementEtat(compteId, dateArrete));
    }

    @PostMapping("/pointer")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<LigneReleveBancaireDTO> rapprocher(@RequestBody RapprochementPointageRequest request) {
        return ResponseEntity.ok(rapprochementService.rapprocher(request));
    }

    @PostMapping("/depointer/{ligneId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<LigneReleveBancaireDTO> derapprocher(@PathVariable Long ligneId) {
        return ResponseEntity.ok(rapprochementService.derapprocher(ligneId));
    }

    @PostMapping("/auto/{releveId}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<Map<String, Object>> autoRapprochement(@PathVariable Long releveId) {
        return ResponseEntity.ok(rapprochementService.autoRapprochement(releveId));
    }

    @DeleteMapping("/releves/{id}")
    @PreAuthorize("hasAuthority('TRESORERIE_GESTION')")
    public ResponseEntity<Void> supprimerReleve(@PathVariable Long id) {
        rapprochementService.supprimerReleve(id);
        return ResponseEntity.noContent().build();
    }
}
