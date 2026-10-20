package com.gestion.controller;

import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmAnnulerVisiteRequest;
import com.gestion.persistent.dto.CrmVisiteDTO;
import com.gestion.persistent.model.CrmRapportVisite;
import com.gestion.persistent.model.CrmVisite;
import com.gestion.repository.CrmRapportVisiteRepository;
import com.gestion.service.CrmVisiteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/visites")
@CrossOrigin(origins = "*")
public class CrmVisiteController {

    private final CrmVisiteService visiteService;
    private final CrmRapportVisiteRepository rapportVisiteRepository;
    private final CrmMapper crmMapper;

    public CrmVisiteController(CrmVisiteService visiteService,
                              CrmRapportVisiteRepository rapportVisiteRepository,
                              CrmMapper crmMapper) {
        this.visiteService = visiteService;
        this.rapportVisiteRepository = rapportVisiteRepository;
        this.crmMapper = crmMapper;
    }

    @PostMapping
    public ResponseEntity<CrmVisiteDTO> planifier(@RequestBody CrmVisiteDTO dto) {
        CrmVisite entity = crmMapper.toEntity(dto);
        CrmVisite saved = visiteService.planifierVisite(entity);
        return new ResponseEntity<>(enrichirRapport(crmMapper.toDto(saved)), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/realiser")
    public ResponseEntity<CrmVisiteDTO> marquerRealisee(@PathVariable Long id) {
        CrmVisite updated = visiteService.marquerRealisee(id);
        return ResponseEntity.ok(enrichirRapport(crmMapper.toDto(updated)));
    }

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<CrmVisiteDTO> cloturer(@PathVariable Long id) {
        CrmVisite updated = visiteService.cloturerVisite(id);
        return ResponseEntity.ok(enrichirRapport(crmMapper.toDto(updated)));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<CrmVisiteDTO> annuler(
            @PathVariable Long id,
            @RequestBody CrmAnnulerVisiteRequest request) {
        CrmVisite updated = visiteService.annulerVisite(id, request.getMotif());
        return ResponseEntity.ok(enrichirRapport(crmMapper.toDto(updated)));
    }

    @GetMapping
    public ResponseEntity<List<CrmVisiteDTO>> getAll() {
        List<CrmVisiteDTO> dtos = visiteService.getAllVisites().stream()
                .map(crmMapper::toDto)
                .map(this::enrichirRapport)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmVisiteDTO> getById(@PathVariable Long id) {
        CrmVisite entity = visiteService.getVisiteById(id);
        return ResponseEntity.ok(enrichirRapport(crmMapper.toDto(entity)));
    }

    @GetMapping("/commercial/{commercialId}")
    public ResponseEntity<List<CrmVisiteDTO>> getByCommercial(@PathVariable Long commercialId) {
        List<CrmVisiteDTO> dtos = visiteService.getVisitesByCommercial(commercialId).stream()
                .map(crmMapper::toDto)
                .map(this::enrichirRapport)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<CrmVisiteDTO>> getByEntreprise(@PathVariable Long entrepriseId) {
        List<CrmVisiteDTO> dtos = visiteService.getVisitesByEntreprise(entrepriseId).stream()
                .map(crmMapper::toDto)
                .map(this::enrichirRapport)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private CrmVisiteDTO enrichirRapport(CrmVisiteDTO dto) {
        if (dto != null && dto.getId() != null) {
            Optional<CrmRapportVisite> rapportOpt = rapportVisiteRepository.findByVisiteId(dto.getId());
            rapportOpt.ifPresent(r -> {
                dto.setRapportId(r.getId());
                dto.setRapportStatut(r.getStatut().name());
            });
        }
        return dto;
    }
}
