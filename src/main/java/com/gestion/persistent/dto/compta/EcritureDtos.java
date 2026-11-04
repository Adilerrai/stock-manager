package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contrat des brouillons et des écritures. */
public final class EcritureDtos {

    private EcritureDtos() {
    }

    public record BrouillonRequest(
            @NotNull Long journalId,
            @NotNull LocalDate dateEcriture,
            @Schema(nullable = true) LocalDate datePiece,
            @Schema(nullable = true) String refPiece,
            @Schema(nullable = true) String libelle,
            @NotBlank String nature,
            @Schema(nullable = true) String motif,
            @Schema(nullable = true) Boolean aExtourner,
            @NotNull @Valid List<BrouillonLigneRequest> lignes) {
    }

    public record BrouillonLigneRequest(
            @Schema(nullable = true) String compte,
            @Schema(nullable = true) Long tiersId,
            @NotNull @MontantJson BigDecimal debit,
            @NotNull @MontantJson BigDecimal credit,
            @Schema(nullable = true) String libelle,
            @Schema(nullable = true) LocalDate dateEcheance,
            @Schema(nullable = true) String natureMouvement,
            @Schema(nullable = true) Boolean horsFlux,
            @Schema(nullable = true) String devise,
            @MontantJson @Schema(nullable = true) BigDecimal montantDevise,
            @MontantJson @Schema(nullable = true) BigDecimal tauxTva,
            @MontantJson @Schema(nullable = true) BigDecimal baseTva) {
    }

    public record BrouillonDTO(
            Long id, Long journalId, LocalDate dateEcriture,
            @Schema(nullable = true) LocalDate datePiece,
            @Schema(nullable = true) String refPiece,
            @Schema(nullable = true) String libelle,
            String nature,
            @Schema(nullable = true) String motif,
            boolean aExtourner,
            List<BrouillonLigneDTO> lignes,
            LocalDateTime creeLe, String creePar,
            @Schema(nullable = true) LocalDateTime modifieLe,
            @MontantJson BigDecimal totalDebit,
            @MontantJson BigDecimal totalCredit,
            @MontantJson BigDecimal ecart,
            List<AnomalieEcritureDTO> anomalies) {
    }

    public record BrouillonLigneDTO(
            int numeroLigne,
            @Schema(nullable = true) String compte,
            @Schema(nullable = true) Long tiersId,
            @MontantJson BigDecimal debit,
            @MontantJson BigDecimal credit,
            @Schema(nullable = true) String libelle,
            @Schema(nullable = true) LocalDate dateEcheance,
            @Schema(nullable = true) String natureMouvement,
            boolean horsFlux,
            @Schema(nullable = true) String devise,
            @MontantJson @Schema(nullable = true) BigDecimal montantDevise,
            @MontantJson @Schema(nullable = true) BigDecimal tauxTva,
            @MontantJson @Schema(nullable = true) BigDecimal baseTva) {
    }

    /** Ce qui empêche la validation. ligne est nulle pour une anomalie de l'en-tête ou de l'écriture entière. */
    public record AnomalieEcritureDTO(@Schema(nullable = true) Integer ligne, String code, String message) {
    }

    public record ReferenceJournalDTO(Long id, String code, String libelle) {
    }

    public record ReferenceCompteDTO(String numero, String libelle) {
    }

    public record ReferenceTiersDTO(Long id, String codeAuxiliaire, String nom) {
    }

    public record SourceEcritureDTO(String sourceType, Long sourceId, String evenement, int version,
                                    @Schema(nullable = true) String numero) {
    }

    public record PieceJointeDTO(Long documentId, String nom) {
    }

    public record EcritureDTO(
            Long id, Long exerciceId, Long periodeId, ReferenceJournalDTO journal, long numero, String numeroComplet,
            LocalDate dateEcriture, LocalDate datePiece, String refPiece, String libelle, String nature, String origine,
            boolean estContrepassation,
            @Schema(nullable = true) Long contrepassationDeId,
            @Schema(nullable = true) Long contrepasseeParId,
            @Schema(nullable = true) Long corrigeEcritureId,
            @Schema(nullable = true) String motif,
            boolean aExtourner,
            @MontantJson BigDecimal totalDebit,
            @MontantJson BigDecimal totalCredit,
            LocalDateTime saisieLe, String saisiePar, LocalDateTime valideLe, String validePar,
            List<SourceEcritureDTO> sources,
            List<PieceJointeDTO> piecesJointes,
            List<LigneEcritureComptaDTO> lignes) {
    }

    public record LigneEcritureComptaDTO(
            Long id, int numeroLigne, ReferenceCompteDTO compte,
            @Schema(nullable = true) ReferenceTiersDTO tiers,
            @MontantJson BigDecimal debit,
            @MontantJson BigDecimal credit,
            @Schema(nullable = true) String libelle,
            String refPiece, LocalDate datePiece,
            @Schema(nullable = true) LocalDate dateEcheance,
            @Schema(nullable = true) String lettrage,
            @Schema(nullable = true) String natureMouvement,
            boolean horsFlux,
            @Schema(nullable = true) String devise,
            @MontantJson @Schema(nullable = true) BigDecimal montantDevise,
            @MontantJson @Schema(nullable = true) BigDecimal tauxTva,
            @MontantJson @Schema(nullable = true) BigDecimal baseTva) {
    }

    public record ContrepassationRequest(@Schema(nullable = true) LocalDate dateEcriture, @NotBlank String motif) {
    }

    public record PieceJointeRequest(@NotNull Long documentId) {
    }
}
