package com.gestion.controller;

import com.gestion.persistent.dto.ComparateurOffresDTO;
import com.gestion.persistent.dto.DemandePrixDTO;
import com.gestion.persistent.dto.OffreFournisseurDTO;
import com.gestion.persistent.model.Commande;
import com.gestion.service.DemandePrixService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/demandes-prix")
public class DemandePrixController {

    private final DemandePrixService demandePrixService;

    public DemandePrixController(DemandePrixService demandePrixService) {
        this.demandePrixService = demandePrixService;
    }

    @PostMapping
    public ResponseEntity<DemandePrixDTO> createDemandePrix(@RequestBody DemandePrixDTO dto) {
        DemandePrixDTO created = demandePrixService.createDemandePrix(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DemandePrixDTO>> getAllDemandesPrix() {
        return ResponseEntity.ok(demandePrixService.getAllDemandesPrix());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandePrixDTO> getDemandePrixById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demandePrixService.getDemandePrixById(id));
    }

    @PostMapping("/{id}/offres")
    public ResponseEntity<OffreFournisseurDTO> enregistrerOffre(
            @PathVariable("id") Long id,
            @RequestBody OffreFournisseurDTO offreDTO) {
        OffreFournisseurDTO saved = demandePrixService.enregistrerOffreFournisseur(id, offreDTO);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/comparateur")
    public ResponseEntity<ComparateurOffresDTO> getComparateur(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demandePrixService.getComparateur(id));
    }

    @PostMapping("/{id}/offres/{offreId}/valider-commande")
    public ResponseEntity<Commande> validerEtCommander(
            @PathVariable("id") Long id,
            @PathVariable("offreId") Long offreId) {
        Commande commande = demandePrixService.validerEtTransformerEnCommande(id, offreId);
        return ResponseEntity.ok(commande);
    }
}
