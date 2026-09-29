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

    @GetMapping({"/exercices/actif", "/exercice/actif"})
    public ResponseEntity<ExerciceComptableDTO> getExerciceActif() {
        return ResponseEntity.ok(clotureService.getExerciceActif());
    }

    @PostMapping({"/exercices/actif", "/exercice/actif"})
    public ResponseEntity<java.util.Map<String, Object>> definirExerciceActif(
            @RequestParam(required = false) Long exerciceId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String code,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            @RequestHeader(value = "X-Exercice-Id", required = false) Long headerId,
            @RequestHeader(value = "X-Exercice-Year", required = false) Integer headerYear) {

        Long targetId = exerciceId != null ? exerciceId : headerId;
        Integer targetYear = annee != null ? annee : headerYear;
        String targetCode = code;

        if (body != null) {
            if (targetId == null && body.get("exerciceId") != null) {
                try {
                    targetId = Long.valueOf(body.get("exerciceId").toString());
                } catch (Exception ignored) {}
            }
            if (targetYear == null && body.get("annee") != null) {
                try {
                    targetYear = Integer.valueOf(body.get("annee").toString());
                } catch (Exception ignored) {}
            }
            if (targetCode == null && body.get("code") != null) {
                targetCode = body.get("code").toString();
            }
        }

        ExerciceComptableDTO dto = clotureService.definirExerciceActif(targetId, targetYear, targetCode);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Exercice actif mis à jour avec succès");
        response.put("annee", dto != null && dto.getDateDebut() != null ? dto.getDateDebut().getYear() : (targetYear != null ? targetYear : 2025));
        response.put("exerciceId", dto != null ? dto.getId() : targetId);
        response.put("exercice", dto);

        return ResponseEntity.ok(response);
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
