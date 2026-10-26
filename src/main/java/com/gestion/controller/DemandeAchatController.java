package com.gestion.controller;

import com.gestion.persistent.dto.DemandeAchatDTO;
import com.gestion.persistent.dto.DemandePrixDTO;
import com.gestion.persistent.enums.Incoterm;
import com.gestion.service.DemandeAchatService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/demandes-achat")
@CrossOrigin(origins = "*")
public class DemandeAchatController {

    private final DemandeAchatService demandeAchatService;

    public DemandeAchatController(DemandeAchatService demandeAchatService) {
        this.demandeAchatService = demandeAchatService;
    }

    @PostMapping
    public ResponseEntity<DemandeAchatDTO> createDemandeAchat(@RequestBody DemandeAchatDTO dto) {
        return new ResponseEntity<>(demandeAchatService.createDemandeAchat(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandeAchatDTO> updateDemandeAchat(@PathVariable("id") Long id, @RequestBody DemandeAchatDTO dto) {
        return ResponseEntity.ok(demandeAchatService.updateDemandeAchat(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<DemandeAchatDTO>> getAllDemandesAchat() {
        return ResponseEntity.ok(demandeAchatService.getAllDemandesAchat());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeAchatDTO> getDemandeAchatById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demandeAchatService.getDemandeAchatById(id));
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<DemandeAchatDTO> validerDemandeAchat(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demandeAchatService.validerDemandeAchat(id));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<DemandeAchatDTO> annulerDemandeAchat(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demandeAchatService.annulerDemandeAchat(id));
    }

    public static class ConversionNationaleRequest {
        public List<Long> ligneIds;
        public List<Long> fournisseurIds;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        public LocalDateTime dateLimiteReponse;
    }

    public static class ConversionInternationaleRequest {
        public List<Long> ligneIds;
        public String devise;
        public Incoterm incoterm;
        public String lieuLivraison;
        public List<Long> fournisseurIds;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        public LocalDateTime dateLimiteReponse;
    }

    @PostMapping("/{id}/convertir-ddp-nationale")
    public ResponseEntity<DemandePrixDTO> convertirEnDdpNationale(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ConversionNationaleRequest request) {
        List<Long> ligneIds = (request != null) ? request.ligneIds : null;
        List<Long> frnIds = (request != null) ? request.fournisseurIds : null;
        LocalDateTime dateLimite = (request != null) ? request.dateLimiteReponse : null;
        DemandePrixDTO ddp = demandeAchatService.transformerEnDemandePrixNationale(id, ligneIds, frnIds, dateLimite);
        return ResponseEntity.ok(ddp);
    }

    @PostMapping("/{id}/convertir-ddp-internationale")
    public ResponseEntity<DemandePrixDTO> convertirEnDdpInternationale(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ConversionInternationaleRequest request) {
        List<Long> ligneIds = (request != null) ? request.ligneIds : null;
        String devise = (request != null) ? request.devise : "USD";
        Incoterm incoterm = (request != null) ? request.incoterm : null;
        String lieu = (request != null) ? request.lieuLivraison : null;
        List<Long> frnIds = (request != null) ? request.fournisseurIds : null;
        LocalDateTime dateLimite = (request != null) ? request.dateLimiteReponse : null;

        DemandePrixDTO ddp = demandeAchatService.transformerEnDemandePrixInternationale(id, ligneIds, devise, incoterm, lieu, frnIds, dateLimite);
        return ResponseEntity.ok(ddp);
    }
}
