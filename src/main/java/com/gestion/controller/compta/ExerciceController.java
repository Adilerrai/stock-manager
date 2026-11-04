package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.CloturePeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ControlePeriodeDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.service.compta.ExerciceService;
import com.gestion.service.compta.PeriodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("comptaExerciceController")
@RequestMapping(value = "/api/v1/compta", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : exercices et périodes")
public class ExerciceController {

    private final ExerciceService exercices;
    private final PeriodeService periodes;

    public ExerciceController(ExerciceService exercices, PeriodeService periodes) {
        this.exercices = exercices;
        this.periodes = periodes;
    }

    @GetMapping("/exercices")
    @Operation(operationId = "comptaExerciceLister", summary = "Exercices du livre")
    public ResponseEntity<List<ExerciceDTO>> lister() {
        return ResponseEntity.ok(exercices.lister());
    }

    @GetMapping("/exercices/{id}/periodes")
    @Operation(operationId = "comptaExercicePeriodes", summary = "Périodes d'un exercice")
    public ResponseEntity<List<PeriodeDTO>> periodes(@PathVariable Long id) {
        return ResponseEntity.ok(exercices.periodes(id));
    }

    @PostMapping("/exercices/{id}/suivant")
    @Operation(operationId = "comptaExerciceSuivant", summary = "Crée l'exercice suivant")
    public ResponseEntity<ExerciceDTO> suivant(@PathVariable Long id, @Valid @RequestBody ExerciceDatesRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exercices.creerSuivant(id, demande));
    }

    @GetMapping("/periodes/{id}/controle")
    @Operation(operationId = "comptaExerciceControle", summary = "Ce qui bloquerait la clôture de la période")
    public ResponseEntity<ControlePeriodeDTO> controle(@PathVariable Long id) {
        return ResponseEntity.ok(periodes.controler(id));
    }

    @PostMapping("/periodes/{id}/cloturer")
    @Operation(operationId = "comptaExerciceCloturer", summary = "Ferme une période et ouvre la suivante")
    public ResponseEntity<CloturePeriodeDTO> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(periodes.cloturer(id));
    }
}
