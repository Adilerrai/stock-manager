package com.gestion.controller;

import com.gestion.persistent.dto.DepenseDTO;
import com.gestion.persistent.dto.DocumentComptableDTO;
import com.gestion.persistent.dto.ResultatEntrepriseDTO;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.service.DepenseService;
import com.gestion.service.GedService;
import org.springframework.context.annotation.Lazy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/depenses", "/api/v1/depenses"})
@CrossOrigin(origins = "*")
public class DepenseController {

    private final DepenseService depenseService;
    private final GedService gedService;

    public DepenseController(DepenseService depenseService, @Lazy GedService gedService) {
        this.depenseService = depenseService;
        this.gedService = gedService;
    }

    @PostMapping
    public ResponseEntity<DepenseDTO> creerDepense(@RequestBody DepenseDTO dto,
                                                   @RequestParam(required = false) Long userId) {
        DepenseDTO cree = depenseService.creerDepense(dto, userId);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PostMapping("/search")
    public ResponseEntity<org.springframework.data.domain.Page<DepenseDTO>> searchDepenses(
            @RequestBody(required = false) com.gestion.persistent.dto.DepenseSearchCriteria criteria,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(depenseService.searchDepenses(criteria, pageable));
    }

    @GetMapping
    public ResponseEntity<List<DepenseDTO>> getDepenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (dateDebut != null || dateFin != null) {
            return ResponseEntity.ok(depenseService.getDepensesByPeriode(dateDebut, dateFin));
        }
        return ResponseEntity.ok(depenseService.getAllDepenses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepenseDTO> getDepense(@PathVariable Long id) {
        return ResponseEntity.ok(depenseService.getDepenseById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerDepense(@PathVariable Long id) {
        depenseService.supprimerDepense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, BigDecimal>> getStatistiques(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(depenseService.getStatistiquesParCategorie(dateDebut, dateFin));
    }

    @GetMapping("/resultat-net")
    public ResponseEntity<ResultatEntrepriseDTO> getResultatNet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        ResultatEntrepriseDTO resultat = depenseService.calculerResultatNet(dateDebut, dateFin);
        return ResponseEntity.ok(resultat);
    }

    // =========================================================================
    // GESTION DES PIÈCES JOINTES (GED) ATTACHÉES À LA DÉPENSE
    // =========================================================================

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentComptableDTO>> getDocumentsDepense(@PathVariable Long id) {
        return ResponseEntity.ok(gedService.getDocumentsParDepense(id));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentComptableDTO> attacherDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "typePiece", required = false, defaultValue = "JUSTIFICATIF_DEPENSE") String typePiece,
            @RequestParam(value = "description", required = false) String description) throws IOException {
        
        // Vérifier que la dépense existe
        depenseService.getDepenseById(id);

        DocumentComptableDTO doc = gedService.stockerDocument(
                file, typePiece, description, null, null, null, null, null, null, null, null, id
        );
        return new ResponseEntity<>(doc, HttpStatus.CREATED);
    }

    // =========================================================================
    // DÉCLARATION PAYÉE
    // =========================================================================

    @PutMapping("/{id}")
    public ResponseEntity<DepenseDTO> modifierDepense(
            @PathVariable Long id,
            @RequestBody DepenseDTO dto) {
        return ResponseEntity.ok(depenseService.modifierDepense(id, dto));
    }

    @PutMapping("/{id}/declarer-payee")
    public ResponseEntity<DepenseDTO> declarerPayee(
            @PathVariable Long id,
            @RequestParam ModePaiement modePaiement,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePaiement,
            @RequestParam(required = false) String referencePaiement) {
        return ResponseEntity.ok(depenseService.declarerPayee(id, modePaiement, datePaiement, referencePaiement));
    }
}
