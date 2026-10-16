package com.gestion.controller;

import com.gestion.persistent.dto.ConsommableDTO;
import com.gestion.persistent.dto.ConsommableSearchCriteria;
import com.gestion.persistent.dto.SortieConsommableRequest;
import com.gestion.persistent.enums.CategorieConsommable;
import com.gestion.persistent.enums.UniteConsommable;
import com.gestion.service.ConsommableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping({"/api/consommables", "/api/v1/consommables"})
@CrossOrigin(origins = "*")
public class ConsommableController {

    private final ConsommableService consommableService;

    public ConsommableController(ConsommableService consommableService) {
        this.consommableService = consommableService;
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<ConsommableDTO> creerConsommable(@RequestBody ConsommableDTO dto) {
        ConsommableDTO created = consommableService.creerConsommable(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping({"/{id}", "/update"})
    public ResponseEntity<ConsommableDTO> modifierConsommable(
            @PathVariable(required = false) Long id,
            @RequestBody ConsommableDTO dto) {
        Long targetId = id != null ? id : dto.getId();
        if (targetId == null) {
            throw new IllegalArgumentException("L'identifiant du consommable est obligatoire pour la modification.");
        }
        return ResponseEntity.ok(consommableService.modifierConsommable(targetId, dto));
    }

    @GetMapping({"", "/all"})
    public ResponseEntity<List<ConsommableDTO>> getAllConsommables(
            @RequestParam(required = false, defaultValue = "false") boolean actifSeulement) {
        return ResponseEntity.ok(consommableService.getAllConsommables(actifSeulement));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsommableDTO> getConsommableById(@PathVariable Long id) {
        return ResponseEntity.ok(consommableService.getConsommableById(id));
    }

    @GetMapping("/alertes-stock")
    public ResponseEntity<List<ConsommableDTO>> getAlertesStock() {
        return ResponseEntity.ok(consommableService.getAlertesStock());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerConsommable(@PathVariable Long id) {
        consommableService.supprimerConsommable(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ConsommableDTO>> searchConsommables(
            @RequestBody(required = false) ConsommableSearchCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(consommableService.searchConsommables(criteria, pageable));
    }

    @PostMapping("/{id}/ajuster-stock")
    public ResponseEntity<ConsommableDTO> ajusterStock(
            @PathVariable Long id,
            @RequestParam BigDecimal quantite,
            @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(consommableService.ajusterStock(id, quantite, motif));
    }

    @PostMapping("/{id}/sortie")
    public ResponseEntity<ConsommableDTO> enregistrerSortie(
            @PathVariable Long id,
            @RequestBody SortieConsommableRequest request) {
        return ResponseEntity.ok(consommableService.enregistrerSortie(id, request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, String>>> getCategories() {
        List<Map<String, String>> list = new ArrayList<>();
        for (CategorieConsommable c : CategorieConsommable.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("valeur", c.name());
            m.put("libelle", c.getLibelle());
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unites")
    public ResponseEntity<List<Map<String, String>>> getUnites() {
        List<Map<String, String>> list = new ArrayList<>();
        for (UniteConsommable u : UniteConsommable.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("valeur", u.name());
            m.put("libelle", u.getLibelle());
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }
}
