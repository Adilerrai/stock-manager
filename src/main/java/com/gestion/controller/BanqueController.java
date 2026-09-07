package com.gestion.controller;

import com.gestion.persistent.dto.BanqueDTO;
import com.gestion.persistent.dto.BanqueStatDTO;
import com.gestion.service.BanqueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banques")
@CrossOrigin(origins = "*")
public class BanqueController {

    private final BanqueService banqueService;

    public BanqueController(BanqueService banqueService) {
        this.banqueService = banqueService;
    }

    @GetMapping
    public ResponseEntity<List<BanqueDTO>> getBanquesActives() {
        return ResponseEntity.ok(banqueService.getBanquesActives());
    }

    @GetMapping("/toutes")
    public ResponseEntity<List<BanqueDTO>> getAllBanques() {
        return ResponseEntity.ok(banqueService.getAllBanques());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BanqueDTO> getBanqueById(@PathVariable Long id) {
        return ResponseEntity.ok(banqueService.getBanqueById(id));
    }

    @PostMapping
    public ResponseEntity<BanqueDTO> creerBanque(@RequestBody BanqueDTO dto) {
        return new ResponseEntity<>(banqueService.creerBanque(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BanqueDTO> modifierBanque(@PathVariable Long id, @RequestBody BanqueDTO dto) {
        return ResponseEntity.ok(banqueService.modifierBanque(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerBanque(@PathVariable Long id) {
        banqueService.supprimerBanque(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<BanqueStatDTO>> getStatistiquesBanques() {
        return ResponseEntity.ok(banqueService.getStatistiquesBanquesEtAgences());
    }
}
