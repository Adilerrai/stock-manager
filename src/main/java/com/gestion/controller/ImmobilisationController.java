package com.gestion.controller;

import com.gestion.persistent.dto.EcritureComptableDTO;
import com.gestion.persistent.dto.ImmobilisationDTO;
import com.gestion.persistent.dto.LiasseTableauT10DTO;
import com.gestion.persistent.dto.LiasseTableauT11DTO;
import com.gestion.persistent.dto.LiasseTableauT12PlusMoinsValuesDTO;
import com.gestion.service.ImmobilisationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/immobilisations")
public class ImmobilisationController {

    private final ImmobilisationService immobilisationService;

    public ImmobilisationController(ImmobilisationService immobilisationService) {
        this.immobilisationService = immobilisationService;
    }

    @PostMapping
    public ResponseEntity<ImmobilisationDTO> creerImmobilisation(@RequestBody ImmobilisationDTO dto) {
        return new ResponseEntity<>(immobilisationService.creerImmobilisation(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImmobilisationDTO> getImmobilisation(@PathVariable Long id) {
        return ResponseEntity.ok(immobilisationService.getImmobilisation(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImmobilisationDTO> modifierImmobilisation(
            @PathVariable Long id, @RequestBody ImmobilisationDTO dto) {
        return ResponseEntity.ok(immobilisationService.modifierImmobilisation(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<ImmobilisationDTO>> getToutesImmobilisations() {
        return ResponseEntity.ok(immobilisationService.getToutesImmobilisations());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerImmobilisation(@PathVariable Long id) {
        immobilisationService.supprimerImmobilisation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cession")
    public ResponseEntity<ImmobilisationDTO> enregistrerCession(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateCession,
            @RequestParam(required = false, defaultValue = "0") BigDecimal prixCession) {
        return ResponseEntity.ok(immobilisationService.enregistrerCession(id, dateCession, prixCession));
    }

    @PostMapping("/dotations/generer")
    public ResponseEntity<EcritureComptableDTO> genererDotationsInventaire(
            @RequestParam(required = false) Integer annee) {
        int anneeEffective = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(immobilisationService.genererDotationsInventaire(anneeEffective));
    }

    @GetMapping("/etats/t10-immobilisations")
    public ResponseEntity<LiasseTableauT10DTO> getTableauT10(
            @RequestParam(required = false) Integer annee) {
        int anneeEffective = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(immobilisationService.getTableauT10(anneeEffective));
    }

    @GetMapping("/etats/t11-amortissements")
    public ResponseEntity<LiasseTableauT11DTO> getTableauT11(
            @RequestParam(required = false) Integer annee) {
        int anneeEffective = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(immobilisationService.getTableauT11(anneeEffective));
    }

    @GetMapping("/etats/t12-cessions")
    public ResponseEntity<LiasseTableauT12PlusMoinsValuesDTO> getTableauT12PlusMoinsValues(
            @RequestParam(required = false) Integer annee) {
        int anneeEffective = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(immobilisationService.getTableauT12PlusMoinsValues(anneeEffective));
    }
}
