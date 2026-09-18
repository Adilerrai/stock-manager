package com.gestion.controller;

import com.gestion.persistent.dto.ModulesAbonnementDTO;
import com.gestion.persistent.dto.PieceCommercialeEnAttenteDTO;
import com.gestion.persistent.dto.RapportDeversementDTO;
import com.gestion.persistent.dto.StatutPasserelleDTO;
import com.gestion.service.PasserelleComptableService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/passerelle-comptable")
public class PasserelleComptableController {

    private final PasserelleComptableService passerelleService;

    public PasserelleComptableController(PasserelleComptableService passerelleService) {
        this.passerelleService = passerelleService;
    }

    @GetMapping("/statut")
    public ResponseEntity<StatutPasserelleDTO> getStatutPasserelle(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(passerelleService.getStatutPasserelle(dateDebut, dateFin));
    }

    @GetMapping("/pieces-en-attente")
    public ResponseEntity<List<PieceCommercialeEnAttenteDTO>> getPiecesEnAttente(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false, defaultValue = "TOUT") String type) {
        return ResponseEntity.ok(passerelleService.getPiecesEnAttente(dateDebut, dateFin, type));
    }

    @PostMapping("/deverser-tout")
    public ResponseEntity<RapportDeversementDTO> deverserTout(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(passerelleService.deverserTout(dateDebut, dateFin));
    }

    @PostMapping("/deverser-ventes")
    public ResponseEntity<RapportDeversementDTO> deverserVentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestBody(required = false) List<Long> factureIds) {
        return ResponseEntity.ok(passerelleService.deverserVentes(dateDebut, dateFin, factureIds));
    }

    @PostMapping("/deverser-achats")
    public ResponseEntity<RapportDeversementDTO> deverserAchats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestBody(required = false) List<Long> factureAchatIds) {
        return ResponseEntity.ok(passerelleService.deverserAchats(dateDebut, dateFin, factureAchatIds));
    }

    @PostMapping("/deverser-paiements-clients")
    public ResponseEntity<RapportDeversementDTO> deverserPaiementsClients(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestBody(required = false) List<Long> paiementIds) {
        return ResponseEntity.ok(passerelleService.deverserPaiementsClients(dateDebut, dateFin, paiementIds));
    }

    @PostMapping("/deverser-reglements-fournisseurs")
    public ResponseEntity<RapportDeversementDTO> deverserReglementsFournisseurs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestBody(required = false) List<Long> reglementIds) {
        return ResponseEntity.ok(passerelleService.deverserReglementsFournisseurs(dateDebut, dateFin, reglementIds));
    }

    @GetMapping("/modules")
    public ResponseEntity<ModulesAbonnementDTO> getModulesAbonnement() {
        return ResponseEntity.ok(passerelleService.getModulesAbonnement());
    }

    @PutMapping("/modules")
    public ResponseEntity<ModulesAbonnementDTO> configurerModules(@RequestBody ModulesAbonnementDTO dto) {
        return ResponseEntity.ok(passerelleService.configurerModules(dto));
    }
}
