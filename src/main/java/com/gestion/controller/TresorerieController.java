package com.gestion.controller;

import com.gestion.persistent.dto.BalanceAgeeDTO;
import com.gestion.persistent.dto.EcheancierDTO;
import com.gestion.persistent.dto.ReleveClientDTO;
import com.gestion.persistent.enums.StatutRemise;
import com.gestion.persistent.model.BordereauRemise;
import com.gestion.service.TresorerieService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tresorerie")
@CrossOrigin(origins = "*")
public class TresorerieController {

    private final TresorerieService tresorerieService;

    public TresorerieController(TresorerieService tresorerieService) {
        this.tresorerieService = tresorerieService;
    }

    @GetMapping("/releve-client/{clientId}")
    public ResponseEntity<ReleveClientDTO> getReleveClient(
            @PathVariable Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(tresorerieService.genererReleveClient(clientId, dateDebut, dateFin));
    }

    @GetMapping("/balance-agee-clients")
    public ResponseEntity<BalanceAgeeDTO> getBalanceAgeeClients() {
        return ResponseEntity.ok(tresorerieService.calculerBalanceAgeeClients());
    }

    @GetMapping("/balance-agee-fournisseurs")
    public ResponseEntity<BalanceAgeeDTO> getBalanceAgeeFournisseurs() {
        return ResponseEntity.ok(tresorerieService.calculerBalanceAgeeFournisseurs());
    }

    @GetMapping("/echeancier")
    public ResponseEntity<EcheancierDTO> getEcheancier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(tresorerieService.genererEcheancier(dateDebut, dateFin));
    }

    @PostMapping("/remises")
    public ResponseEntity<BordereauRemise> creerBordereauRemise(@RequestBody BordereauRemise bordereau) {
        BordereauRemise cree = tresorerieService.creerBordereauRemise(bordereau);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PatchMapping("/remises/{id}/statut")
    public ResponseEntity<BordereauRemise> changerStatutRemise(@PathVariable Long id,
                                                               @RequestParam StatutRemise statut) {
        return ResponseEntity.ok(tresorerieService.changerStatutRemise(id, statut));
    }

    @GetMapping("/remises")
    public ResponseEntity<List<BordereauRemise>> getAllBordereaux() {
        return ResponseEntity.ok(tresorerieService.getAllBordereaux());
    }
}
