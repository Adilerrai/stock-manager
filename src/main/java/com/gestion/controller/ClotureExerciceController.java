package com.gestion.controller;

import com.gestion.persistent.dto.CloturePreviewDTO;
import com.gestion.persistent.dto.ExerciceComptableDTO;
import com.gestion.service.ClotureExerciceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/comptabilite")
@CrossOrigin(origins = "*")
public class ClotureExerciceController {

    private final ClotureExerciceService clotureService;

    public ClotureExerciceController(ClotureExerciceService clotureService) {
        this.clotureService = clotureService;
    }

    @GetMapping("/exercices")
    public ResponseEntity<List<ExerciceComptableDTO>> getExercices() {
        return ResponseEntity.ok(clotureService.getExercices());
    }

    @PostMapping("/exercices")
    public ResponseEntity<ExerciceComptableDTO> creerExercice(@RequestBody ExerciceComptableDTO dto) {
        return new ResponseEntity<>(clotureService.creerExercice(dto), HttpStatus.CREATED);
    }

    @GetMapping("/cloture/preparer/{exerciceId}")
    public ResponseEntity<CloturePreviewDTO> preparerCloture(@PathVariable Long exerciceId) {
        return ResponseEntity.ok(clotureService.preparerCloture(exerciceId));
    }

    @PostMapping("/cloture/executer/{exerciceId}")
    public ResponseEntity<ExerciceComptableDTO> executerCloture(
            @PathVariable Long exerciceId,
            Principal principal) {
        String auteur = principal != null ? principal.getName() : "Administrateur";
        return ResponseEntity.ok(clotureService.executerCloture(exerciceId, auteur));
    }
}
