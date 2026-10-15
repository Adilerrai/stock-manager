package com.gestion.controller;

import com.gestion.persistent.dto.PrestataireDTO;
import com.gestion.persistent.dto.PrestataireSearchCriteria;
import com.gestion.persistent.enums.TypePrestation;
import com.gestion.service.PrestataireService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/prestataires", "/api/v1/prestataires"})
@CrossOrigin(origins = "*")
public class PrestataireController {

    private final PrestataireService prestataireService;

    public PrestataireController(PrestataireService prestataireService) {
        this.prestataireService = prestataireService;
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<PrestataireDTO> creerPrestataire(@RequestBody PrestataireDTO dto) {
        PrestataireDTO created = prestataireService.creerPrestataire(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping({"/{id}", "/update"})
    public ResponseEntity<PrestataireDTO> modifierPrestataire(
            @PathVariable(required = false) Long id,
            @RequestBody PrestataireDTO dto) {
        Long targetId = id != null ? id : dto.getId();
        if (targetId == null) {
            throw new IllegalArgumentException("L'identifiant du prestataire est obligatoire pour la mise à jour.");
        }
        return ResponseEntity.ok(prestataireService.modifierPrestataire(targetId, dto));
    }

    @GetMapping({"", "/all"})
    public ResponseEntity<List<PrestataireDTO>> getAllPrestataires(
            @RequestParam(required = false, defaultValue = "false") boolean actifSeulement) {
        return ResponseEntity.ok(prestataireService.getAllPrestataires(actifSeulement));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestataireDTO> getPrestataireById(@PathVariable Long id) {
        return ResponseEntity.ok(prestataireService.getPrestataireById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerPrestataire(@PathVariable Long id) {
        prestataireService.supprimerPrestataire(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<Page<PrestataireDTO>> searchPrestataires(
            @RequestBody(required = false) PrestataireSearchCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(prestataireService.searchPrestataires(criteria, pageable));
    }

    @GetMapping("/types-prestation")
    public ResponseEntity<List<Map<String, String>>> getTypesPrestation() {
        List<Map<String, String>> list = new ArrayList<>();
        for (TypePrestation tp : TypePrestation.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("valeur", tp.name());
            m.put("libelle", tp.getLibelle());
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }
}
