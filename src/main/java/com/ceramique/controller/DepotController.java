package com.ceramique.controller;

import com.ceramique.mapper.DepotMapper;
import com.ceramique.persistent.dto.DepotDTO;
import com.ceramique.persistent.model.Depot;
import com.ceramique.service.DepotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/depots")
public class DepotController {

    private final DepotService depotService;
    private final DepotMapper depotMapper;

    public DepotController(DepotService depotService, DepotMapper depotMapper) {
        this.depotService = depotService;
        this.depotMapper = depotMapper;
    }

    @PostMapping("/add")
    public ResponseEntity<DepotDTO> createDepot(@RequestBody DepotDTO depotDTO) {
        Depot depot = depotService.createDepot(depotDTO);
        return ResponseEntity.ok(depotMapper.toDto(depot));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DepotDTO>> getAllDepots() {
        List<Depot> depots = depotService.getAllDepotsActifs();
        List<DepotDTO> depotDTOs = depots.stream()
                .map(depotMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(depotDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepotDTO> getDepotById(@PathVariable("id") Long id) {
        Depot depot = depotService.getDepotById(id);
        return ResponseEntity.ok(depotMapper.toDto(depot));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepotDTO> updateDepot(@PathVariable("id") Long id, @RequestBody DepotDTO depotDTO) {
        Depot depot = depotService.updateDepot(id, depotDTO);
        return ResponseEntity.ok(depotMapper.toDto(depot));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepot(@PathVariable("id") Long id) {
        depotService.deleteDepot(id);
        return ResponseEntity.noContent().build();
    }
}