package com.acommon.controller;

import com.acommon.persistant.dto.PointDeVenteCreationRequest;
import com.acommon.persistant.dto.PointDeVenteResponse;
import com.acommon.service.PointDeVenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points-vente")
public class PointDeVenteController {

    @Autowired
    private  PointDeVenteService pointDeVenteService;



    @PostMapping("/create")
    public ResponseEntity<PointDeVenteResponse> createPointDeVente(@Valid @RequestBody PointDeVenteCreationRequest request) {
        PointDeVenteResponse response = pointDeVenteService.createPointDeVente(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<PointDeVenteResponse>> getAllPointsDeVente() {
        List<PointDeVenteResponse> pointsDeVente = pointDeVenteService.getAllPointsDeVente();
        return ResponseEntity.ok(pointsDeVente);
    }
}