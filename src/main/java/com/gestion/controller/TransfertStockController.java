package com.gestion.controller;

import com.gestion.persistent.dto.TransfertStockDTO;
import com.gestion.persistent.enums.StatutTransfert;
import com.gestion.service.TransfertStockService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<List<TransfertStockDTO>> getAllTransferts(
            @RequestParam(required = false) StatutTransfert statut) {
        return ResponseEntity.ok(transfertService.getAllTransferts(statut));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransfertStockDTO> getTransfertById(@PathVariable Long id) {
        return ResponseEntity.ok(transfertService.getTransfertById(id));
    }

    @PostMapping
    public ResponseEntity<TransfertStockDTO> creerTransfert(
            @RequestBody TransfertStockDTO dto,
            @RequestParam(required = false) Long userId) {
        return new ResponseEntity<>(transfertService.creerTransfert(dto, userId), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransfertStockDTO> modifierTransfert(
            @PathVariable Long id,
            @RequestBody TransfertStockDTO dto,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.modifierTransfert(id, dto, userId));
    }

    @PostMapping("/{id}/expedier")
    public ResponseEntity<TransfertStockDTO> expedierTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.expedierTransfert(id, userId));
    }

    @PostMapping("/{id}/recevoir")
    public ResponseEntity<TransfertStockDTO> recevoirTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.recevoirTransfert(id, userId));
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<TransfertStockDTO> validerTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.validerTransfertDirect(id, userId));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<TransfertStockDTO> annulerTransfert(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(transfertService.annulerTransfert(id, userId));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTransfertPdf(@PathVariable Long id) {
        TransfertStockDTO t = transfertService.getTransfertById(id);
        String code = (t != null && t.getNumeroTransfert() != null) ? t.getNumeroTransfert() : "TRF-" + id;
        String fileName = code + ".pdf";

        byte[] pdf = transfertService.genererTransfertPdf(id);

        String cleanAscii = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String encodedFilename = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + cleanAscii + "\"; filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("X-Filename", fileName)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }
}
