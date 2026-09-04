package com.gestion.controller;

import com.gestion.persistent.dto.TransfertStockDTO;
import com.gestion.persistent.enums.StatutTransfert;
import com.gestion.service.TransfertStockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks/transferts")
@CrossOrigin(origins = "*")
public class TransfertStockController {

    private final TransfertStockService transfertService;

    public TransfertStockController(TransfertStockService transfertService) {
        this.transfertService = transfertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STOCK_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<List<TransfertStockDTO>> getAllTransferts(
            @RequestParam(required = false) StatutTransfert statut) {
        return ResponseEntity.ok(transfertService.getAllTransferts(statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STOCK_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<TransfertStockDTO> getTransfertById(@PathVariable Long id) {
        return ResponseEntity.ok(transfertService.getTransfertById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('STOCK_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<TransfertStockDTO> creerTransfert(
            @RequestBody TransfertStockDTO dto,
            @RequestParam(required = false) Long userId) {
        return new ResponseEntity<>(transfertService.creerTransfert(dto, userId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/expedier")
    @PreAuthorize("hasAnyAuthority('STOCK_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<TransfertStockDTO> expedierTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.expedierTransfert(id, userId));
    }

    @PostMapping("/{id}/recevoir")
    @PreAuthorize("hasAnyAuthority('STOCK_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<TransfertStockDTO> recevoirTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.recevoirTransfert(id, userId));
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyAuthority('STOCK_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE')")
    public ResponseEntity<TransfertStockDTO> annulerTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.annulerTransfert(id, userId));
    }
}
