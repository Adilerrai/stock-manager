package com.gestion.controller;

import com.acommon.persistant.model.User;
import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmTacheDTO;
import com.gestion.persistent.model.CrmTache;
import com.gestion.service.CrmTacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/taches")
@CrossOrigin(origins = "*")
public class CrmTacheController {

    private final CrmTacheService tacheService;
    private final CrmMapper crmMapper;

    public CrmTacheController(CrmTacheService tacheService, CrmMapper crmMapper) {
        this.tacheService = tacheService;
        this.crmMapper = crmMapper;
    }

    @PostMapping
    public ResponseEntity<CrmTacheDTO> creer(
            @RequestBody CrmTacheDTO dto,
            @AuthenticationPrincipal User userConnecte) {
        CrmTache entity = crmMapper.toEntity(dto);
        if (userConnecte != null && entity.getCreeParUserId() == null) {
            entity.setCreeParUserId(userConnecte.getId());
        }
        CrmTache saved = tacheService.creerTache(entity);
        return new ResponseEntity<>(crmMapper.toDto(saved), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/terminer")
    public ResponseEntity<CrmTacheDTO> terminer(@PathVariable Long id) {
        CrmTache updated = tacheService.marquerTerminee(id);
        return ResponseEntity.ok(crmMapper.toDto(updated));
    }

    @GetMapping("/mes-taches")
    public ResponseEntity<List<CrmTacheDTO>> getMesTaches(@AuthenticationPrincipal User userConnecte) {
        Long userId = userConnecte != null ? userConnecte.getId() : 1L;
        List<CrmTacheDTO> dtos = tacheService.getMesTaches(userId).stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/en-retard")
    public ResponseEntity<List<CrmTacheDTO>> getEnRetard() {
        List<CrmTacheDTO> dtos = tacheService.getTachesEnRetard().stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
