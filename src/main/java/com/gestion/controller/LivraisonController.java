package com.gestion.controller;

import com.gestion.persistent.dto.LivraisonDTO;
import com.gestion.persistent.dto.LivraisonSearchCriteria;
import com.gestion.persistent.model.Livraison;
import com.gestion.service.LivraisonService;
import com.gestion.service.LivraisonRetardService;
import com.gestion.persistent.dto.LivraisonRetardDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.gestion.mapper.LivraisonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/livraisons")
public class LivraisonController {

    private static final Logger log = LoggerFactory.getLogger(LivraisonController.class);

    private final LivraisonService livraisonService;
    private final LivraisonMapper livraisonMapper;
    private final LivraisonRetardService livraisonRetardService;

    public LivraisonController(LivraisonService livraisonService, LivraisonMapper livraisonMapper, LivraisonRetardService livraisonRetardService) {
        this.livraisonService = livraisonService;
        this.livraisonMapper = livraisonMapper;
        this.livraisonRetardService = livraisonRetardService;
    }

    @PostMapping("/add")
    public ResponseEntity<LivraisonDTO> createLivraison(@RequestBody LivraisonDTO livraisonDTO) {
        // Validation minimale côté controller pour éviter 500 internes quand des champs obligatoires manquent
        if (livraisonDTO == null) {
            log.warn("createLivraison called with null payload");
            return ResponseEntity.badRequest().build();
        }
        // Le fournisseur est récupérable depuis la commande après mapping, donc on exige la commandeId
        if (livraisonDTO.getCommandeId() == null) {
            log.warn("createLivraison missing commandeId: {}", livraisonDTO);
            return ResponseEntity.badRequest().build();
        }
        if (livraisonDTO.getLignesLivraison() == null || livraisonDTO.getLignesLivraison().isEmpty()) {
            log.warn("createLivraison missing lignesLivraison: {}", livraisonDTO);
            return ResponseEntity.badRequest().build();
        }

        Livraison livraison = livraisonMapper.toEntity(livraisonDTO);
        Livraison livraisonSaved = livraisonService.creerLivraison(livraison);
        return ResponseEntity.ok(livraisonMapper.toDto(livraisonSaved));
    }

    @GetMapping("/all")
    public ResponseEntity<List<LivraisonDTO>> getAllLivraisons() {
        List<Livraison> livraisons = livraisonService.getAllLivraisons();
        List<LivraisonDTO> livraisonDTOs = livraisons.stream()
                .map(livraisonMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(livraisonDTOs);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<LivraisonDTO>> searchLivraisons(
            @RequestBody LivraisonSearchCriteria criteria, Pageable pageable) {
        Page<Livraison> page = livraisonService.searchLivraisons(criteria, pageable);
        return ResponseEntity.ok(page.map(livraisonMapper::toDto));
    }

    @PutMapping("/update")
    public ResponseEntity<LivraisonDTO> updateLivraison(@RequestBody LivraisonDTO livraisonDTO) {
        Livraison livraison = livraisonService.updateLivraison(livraisonDTO);
        return ResponseEntity.ok(livraisonMapper.toDto(livraison));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLivraison(@PathVariable("id") Long id) {
        livraisonService.deleteLivraison(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<LivraisonDTO> validerLivraison(@PathVariable("id") Long id) {
        Livraison livraison = livraisonService.validerLivraison(id);
        return ResponseEntity.ok(livraisonMapper.toDto(livraison));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<LivraisonDTO> annulerLivraison(@PathVariable("id") Long id) {
        Livraison livraison = livraisonService.annulerLivraison(id);
        return ResponseEntity.ok(livraisonMapper.toDto(livraison));
    }



    @GetMapping("/{id}/details")
    public ResponseEntity<LivraisonDTO> getLivraisonWithDetails(@PathVariable("id") Long id) {
        Livraison livraison = livraisonService.getLivraisonWithDetails(id);
        return ResponseEntity.ok(livraisonMapper.toDto(livraison));
    }

    @GetMapping("/retards")
    public ResponseEntity<List<LivraisonRetardDTO>> getRetardsFournisseurs(
            @RequestParam(value = "pointDeVenteId", required = false) Long pointDeVenteId) {
        return ResponseEntity.ok(livraisonRetardService.getRetardsFournisseurs(pointDeVenteId));
    }

    @GetMapping("/retards/export-excel")
    public ResponseEntity<byte[]> exporterExcelRetardsFournisseurs(
            @RequestParam(value = "pointDeVenteId", required = false) Long pointDeVenteId) {
        byte[] bytes = livraisonRetardService.genererExcelRetardsFournisseurs(pointDeVenteId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Livraisons_Fournisseurs_En_Retard.xlsx")
                .body(bytes);
    }

}
