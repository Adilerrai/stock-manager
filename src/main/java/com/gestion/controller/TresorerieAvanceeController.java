package com.gestion.controller;

import com.gestion.persistent.dto.CompteFinancierDTO;
import com.gestion.persistent.dto.MouvementTresorerieDTO;
import com.gestion.persistent.dto.SyntheseTresorerieDTO;
import com.gestion.service.TresorerieAvanceeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tresorerie-avancee")
@CrossOrigin(origins = "*")
public class TresorerieAvanceeController {

    private final TresorerieAvanceeService tresorerieService;

    public TresorerieAvanceeController(TresorerieAvanceeService tresorerieService) {
        this.tresorerieService = tresorerieService;
    }

    /**
     * « Où est mon argent ? »
     * Synthèse complète des caisses physiques et des comptes bancaires
     */
    @GetMapping("/synthese")
    public ResponseEntity<SyntheseTresorerieDTO> getSynthese() {
        return ResponseEntity.ok(tresorerieService.getSynthese());
    }

    @GetMapping("/comptes")
    public ResponseEntity<List<CompteFinancierDTO>> getTousLesComptes() {
        return ResponseEntity.ok(tresorerieService.getTousLesComptes());
    }

    @PostMapping("/comptes")
    public ResponseEntity<CompteFinancierDTO> creerCompte(@RequestBody CompteFinancierDTO dto) {
        return new ResponseEntity<>(tresorerieService.creerCompte(dto), HttpStatus.CREATED);
    }

    /**
     * Enregistrer un mouvement d'espèces / virement interne :
     * Retrait patron, versement caisse -> banque, apport de fonds, etc.
     */
    @PostMapping("/mouvements")
    public ResponseEntity<MouvementTresorerieDTO> enregistrerMouvement(
            @RequestBody MouvementTresorerieDTO dto,
            @RequestParam(required = false) Long userId) {
        return new ResponseEntity<>(tresorerieService.enregistrerMouvement(dto, userId), HttpStatus.CREATED);
    }

    @GetMapping("/mouvements")
    public ResponseEntity<List<MouvementTresorerieDTO>> getHistorique(
            @RequestParam(required = false) Long compteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(tresorerieService.getHistorique(compteId, dateDebut, dateFin));
    }
}
