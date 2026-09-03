package com.gestion.controller;

import com.gestion.persistent.dto.BarometreEntrepriseDTO;
import com.gestion.service.BarometreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barometre")
public class BarometreController {

    private final BarometreService barometreService;

    public BarometreController(BarometreService barometreService) {
        this.barometreService = barometreService;
    }

    @GetMapping
    public ResponseEntity<BarometreEntrepriseDTO> getBarometre() {
        return ResponseEntity.ok(barometreService.calculerBarometre());
    }
}
