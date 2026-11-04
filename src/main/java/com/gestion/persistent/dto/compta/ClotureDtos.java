package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Arrêté, levée, clôture définitive et affectation du résultat. */
public final class ClotureDtos {

    private ClotureDtos() {
    }

    public record EtapeClotureDTO(String code, String etat, @Schema(nullable = true) String detail) {
    }

    /** @param resteAAffecter positif : bénéfice à affecter ; négatif : perte. Nul tant que l'exercice n'est pas clos. */
    public record ClotureExerciceDTO(String statut, List<EtapeClotureDTO> etapes, boolean peutArreter, boolean peutLever, boolean peutCloturer,
                                     @MontantJson @Schema(nullable = true) BigDecimal resultatNet,
                                     @MontantJson @Schema(nullable = true) BigDecimal resteAAffecter,
                                     @Schema(nullable = true) String sha256Fec) {
    }

    /** Tant que l'inventaire et l'IS n'ont pas leur module, l'arrêté ne suppose jamais en silence que ce travail a été fait. */
    public record ArreterRequest(@NotBlank String declarationIs, @io.swagger.v3.oas.annotations.media.Schema(nullable = true) String motifDotations) {
    }

    public record ArreteDTO(String statut, int figeage, @MontantJson BigDecimal resultatNet) {
    }

    public record LeverArreteRequest(@NotBlank String motif) {
    }

    public record StatutExerciceDTO(String statut) {
    }

    public record CloturerRequest(@NotBlank String confirmation) {
    }

    public record ClotureDefinitiveDTO(String statut, @Schema(nullable = true) Long ecritureClotureId, @Schema(nullable = true) Long ecritureOuvertureId,
                                       Long exerciceSuivantId, long lignesAnouveau, int extournes, @Schema(nullable = true) String sha256Fec) {
    }

    public record ArchiveDTO(String sha256Fec) {
    }

    public record AffectationLigneRequest(@NotBlank String compte, @MontantJson @NotNull BigDecimal montant) {
    }

    public record AffectationRequest(@NotNull LocalDate dateDecision, @NotEmpty @Valid List<AffectationLigneRequest> lignes) {
    }
}
