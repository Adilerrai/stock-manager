package com.gestion.controller;

import com.gestion.persistent.dto.ControleTvaDTO;
import com.gestion.persistent.dto.DeclarationTvaEnregistreeDTO;
import com.gestion.persistent.enums.RegimeTva;
import com.gestion.service.TvaAvanceeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tva")
@CrossOrigin(origins = "*")
public class TvaAvanceeController {

    private final TvaAvanceeService tvaAvanceeService;

    public TvaAvanceeController(TvaAvanceeService tvaAvanceeService) {
        this.tvaAvanceeService = tvaAvanceeService;
    }

    @PostMapping("/declarations/generer")
    public ResponseEntity<DeclarationTvaEnregistreeDTO> genererDeclaration(
            @RequestParam String periode,
            @RequestParam(required = false, defaultValue = "ENCAISSEMENT") RegimeTva regime,
            @RequestParam(required = false) BigDecimal prorata,
            @RequestParam(required = false) BigDecimal creditAnterieurManuel,
            @RequestParam(required = false) String notes) {
        DeclarationTvaEnregistreeDTO dto = tvaAvanceeService.genererDeclaration(
                periode, regime, prorata, creditAnterieurManuel, notes);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/declarations")
    public ResponseEntity<List<DeclarationTvaEnregistreeDTO>> getHistoriqueDeclarations() {
        return ResponseEntity.ok(tvaAvanceeService.getHistoriqueDeclarations());
    }

    @GetMapping("/declarations/{id}")
    public ResponseEntity<DeclarationTvaEnregistreeDTO> getDeclaration(@PathVariable Long id) {
        return ResponseEntity.ok(tvaAvanceeService.getDeclaration(id));
    }

    @PatchMapping("/declarations/{id}/valider")
    public ResponseEntity<DeclarationTvaEnregistreeDTO> validerDeclaration(@PathVariable Long id) {
        return ResponseEntity.ok(tvaAvanceeService.validerDeclaration(id));
    }

    @GetMapping("/controle")
    public ResponseEntity<ControleTvaDTO> getControleReconciliation(@RequestParam String periode) {
        return ResponseEntity.ok(tvaAvanceeService.controleReconciliation(periode));
    }
}
