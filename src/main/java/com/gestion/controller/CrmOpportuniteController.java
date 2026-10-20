package com.gestion.controller;

import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmActiviteDTO;
import com.gestion.persistent.dto.CrmOpportuniteDTO;
import com.gestion.persistent.dto.CrmTransitionEtapeRequest;
import com.gestion.persistent.model.CrmOpportunite;
import com.gestion.service.CrmActiviteService;
import com.gestion.service.CrmOpportuniteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/opportunites")
@CrossOrigin(origins = "*")
public class CrmOpportuniteController {

    private final CrmOpportuniteService opportuniteService;
    private final CrmActiviteService activiteService;
    private final CrmMapper crmMapper;

    public CrmOpportuniteController(CrmOpportuniteService opportuniteService,
                                   CrmActiviteService activiteService,
                                   CrmMapper crmMapper) {
        this.opportuniteService = opportuniteService;
        this.activiteService = activiteService;
        this.crmMapper = crmMapper;
    }

    @PostMapping
    public ResponseEntity<CrmOpportuniteDTO> creer(@RequestBody CrmOpportuniteDTO dto) {
        CrmOpportunite entity = crmMapper.toEntity(dto);
        CrmOpportunite saved = opportuniteService.creerOpportunite(entity);
        return new ResponseEntity<>(crmMapper.toDto(saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrmOpportuniteDTO> modifier(@PathVariable Long id, @RequestBody CrmOpportuniteDTO dto) {
        CrmOpportunite entity = crmMapper.toEntity(dto);
        CrmOpportunite updated = opportuniteService.modifierOpportunite(id, entity);
        return ResponseEntity.ok(crmMapper.toDto(updated));
    }

    @PutMapping("/{id}/etape")
    public ResponseEntity<CrmOpportuniteDTO> changerEtape(
            @PathVariable Long id,
            @RequestBody CrmTransitionEtapeRequest request) {
        CrmOpportunite updated = opportuniteService.changerEtape(
                id,
                request.getNouvelleEtape(),
                request.getMotifPerte(),
                request.getNotes()
        );
        return ResponseEntity.ok(crmMapper.toDto(updated));
    }

    @GetMapping
    public ResponseEntity<List<CrmOpportuniteDTO>> getAll() {
        List<CrmOpportuniteDTO> dtos = opportuniteService.getAllOpportunites().stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmOpportuniteDTO> getById(@PathVariable Long id) {
        CrmOpportunite entity = opportuniteService.getOpportuniteById(id);
        return ResponseEntity.ok(crmMapper.toDto(entity));
    }

    @GetMapping("/commercial/{commercialId}")
    public ResponseEntity<List<CrmOpportuniteDTO>> getByCommercial(@PathVariable Long commercialId) {
        List<CrmOpportuniteDTO> dtos = opportuniteService.getOpportunitesByCommercial(commercialId).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/activites")
    public ResponseEntity<List<CrmActiviteDTO>> getActivites(@PathVariable Long id) {
        List<CrmActiviteDTO> dtos = activiteService.getHistoriqueOpportunite(id).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
