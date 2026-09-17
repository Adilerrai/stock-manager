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

    @PostMapping("/import/{compteId}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<ReleveBancaireDTO> importerReleve(
            @PathVariable Long compteId,
            @RequestParam("file") MultipartFile file) throws IOException {
        ReleveBancaireDTO dto = rapprochementService.importerReleveCsv(compteId, file.getOriginalFilename(), file.getBytes());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/releves")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<ReleveBancaireDTO>> getReleves(@RequestParam(required = false) Long compteId) {
        return ResponseEntity.ok(rapprochementService.getReleves(compteId));
    }

    @GetMapping("/releves/{id}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<ReleveBancaireDTO> getReleveDetail(@PathVariable Long id) {
        return ResponseEntity.ok(rapprochementService.getReleveDetail(id));
    }

    @GetMapping("/etat/{compteId}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<RapprochementEtatDTO> getRapprochementEtat(
            @PathVariable Long compteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateArrete) {
        return ResponseEntity.ok(rapprochementService.getRapprochementEtat(compteId, dateArrete));
    }

    @PostMapping("/pointer")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<LigneReleveBancaireDTO> rapprocher(@RequestBody RapprochementPointageRequest request) {
        return ResponseEntity.ok(rapprochementService.rapprocher(request));
    }

    @PostMapping("/depointer/{ligneId}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<LigneReleveBancaireDTO> derapprocher(@PathVariable Long ligneId) {
        return ResponseEntity.ok(rapprochementService.derapprocher(ligneId));
    }

    @PostMapping("/auto/{releveId}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<Map<String, Object>> autoRapprochement(@PathVariable Long releveId) {
        return ResponseEntity.ok(rapprochementService.autoRapprochement(releveId));
    }

    @DeleteMapping("/releves/{id}")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE')")
    public ResponseEntity<Void> supprimerReleve(@PathVariable Long id) {
        rapprochementService.supprimerReleve(id);
        return ResponseEntity.noContent().build();
    }
}
