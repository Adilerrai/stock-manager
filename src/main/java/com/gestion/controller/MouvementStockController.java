package com.gestion.controller;

import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.MouvementStockMapper;
import com.gestion.persistent.dto.MouvementStockDTO;
import com.gestion.persistent.model.MouvementStock;
import com.gestion.repository.MouvementStockRepository;
import com.gestion.service.MouvementStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/mouvements-stock")
public class MouvementStockController {

    private final MouvementStockService mouvementStockService;
    private final MouvementStockRepository mouvementStockRepository;
    private final MouvementStockMapper mouvementStockMapper;

    public MouvementStockController(MouvementStockService mouvementStockService,
                                    MouvementStockRepository mouvementStockRepository,
                                    MouvementStockMapper mouvementStockMapper) {
        this.mouvementStockService = mouvementStockService;
        this.mouvementStockRepository = mouvementStockRepository;
        this.mouvementStockMapper = mouvementStockMapper;
    }

    @GetMapping
    public ResponseEntity<List<MouvementStockDTO>> getAllMouvements() {
        Long tenantId = TenantContext.getCurrentTenant();
        List<MouvementStock> mouvements = mouvementStockRepository.findOrderByDateMouvementDesc(tenantId != null ? tenantId : 1L);
        List<MouvementStockDTO> dtos = mouvements.stream()
                .map(mouvementStockMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<MouvementStockDTO>> getMouvementsByProduit(@PathVariable Long produitId) {
        List<MouvementStock> mouvements = mouvementStockService.getHistoriqueProduit(produitId);
        List<MouvementStockDTO> dtos = mouvements.stream()
                .map(mouvementStockMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}

