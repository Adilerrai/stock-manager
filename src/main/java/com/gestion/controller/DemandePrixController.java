package com.gestion.controller;

import com.gestion.persistent.dto.ComparateurOffresDTO;
import com.gestion.persistent.dto.DemandePrixDTO;
import com.gestion.persistent.dto.FusionOffresFournisseurDTO;
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

    @PutMapping("/{id}")
    public ResponseEntity<DemandePrixDTO> updateDemandePrix(
            @PathVariable("id") Long id,
            @RequestBody DemandePrixDTO dto) {
        DemandePrixDTO updated = demandePrixService.updateDemandePrix(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDemandePrix(@PathVariable("id") Long id) {
        demandePrixService.deleteDemandePrix(id);
        return ResponseEntity.noContent().build();
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
            @PathVariable("offreId") Long offreId,
            @RequestParam(value = "commandeId", required = false) Long commandeId) {
        Commande commande = demandePrixService.validerEtTransformerEnCommande(id, offreId, commandeId);
        return ResponseEntity.ok(commande);
    }

    @PostMapping("/offres/ajouter-a-commande-brouillon")
    public ResponseEntity<Commande> ajouterOffresACommandeBrouillon(
            @RequestParam("commandeId") Long commandeId,
            @RequestBody List<Long> offreIds) {
        Commande commande = demandePrixService.ajouterOffresACommandeBrouillon(commandeId, offreIds);
        return ResponseEntity.ok(commande);
    }

    @PostMapping("/offres/fusionner-vers-commande")
    public ResponseEntity<Commande> fusionnerOffresVersCommande(
            @RequestBody FusionOffresFournisseurDTO dto) {
        Commande commande = demandePrixService.fusionnerOffresVersCommande(dto);
        return ResponseEntity.ok(commande);
    }

    @GetMapping("/offres/fournisseur/{fournisseurId}")
    public ResponseEntity<List<OffreFournisseurDTO>> getOffresParFournisseur(
            @PathVariable("fournisseurId") Long fournisseurId) {
        return ResponseEntity.ok(demandePrixService.getOffresParFournisseur(fournisseurId));
    }
}
