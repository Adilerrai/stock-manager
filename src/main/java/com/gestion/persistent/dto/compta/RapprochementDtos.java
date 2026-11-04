package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Le pointage relie une ligne d'écriture à une ligne de relevé, avec un montant. Le solde comptable vient du grand livre, jamais de comptes_financiers.solde_actuel. */
public final class RapprochementDtos {

    private RapprochementDtos() {
    }

    /** @param partageAvec les autres comptes financiers portés par le même compte comptable : leur état ne se valide pas tant qu'ils ne sont pas séparés */
    public record CompteRapprochementDTO(Long id, String code, String nom, String compte, List<String> partageAvec, @Schema(nullable = true) LocalDate dernierEtat) {
    }

    public record EcritureNonPointeeDTO(Long ligneId, LocalDate date, String refPiece, String libelle, @MontantJson BigDecimal debit, @MontantJson BigDecimal credit, @MontantJson BigDecimal reste) {
    }

    public record ReleveNonPointeDTO(Long ligneReleveId, LocalDate date, @Schema(nullable = true) String reference, String libelle, @MontantJson BigDecimal debit, @MontantJson BigDecimal credit, @MontantJson BigDecimal reste) {
    }

    public record EtatRapprochementDTO(CompteRapprochementDTO compte, LocalDate arrete, boolean releveConnu, @MontantJson BigDecimal soldeReleve, @MontantJson BigDecimal ecrituresDebitNonPointees, @MontantJson BigDecimal ecrituresCreditNonPointees,
                                       @MontantJson BigDecimal soldeBanqueRectifie, @MontantJson BigDecimal soldeComptable, @MontantJson BigDecimal creditsReleveNonPointes, @MontantJson BigDecimal debitsReleveNonPointes,
                                       @MontantJson BigDecimal soldeComptableRectifie, @MontantJson BigDecimal ecart, boolean valide, List<EcritureNonPointeeDTO> ecrituresNonPointees, List<ReleveNonPointeDTO> relevesNonPointes) {
    }

    public record PointageRequest(@NotNull Long ligneReleveId, @NotNull Long ligneId, @MontantJson @NotNull BigDecimal montant, @NotNull LocalDate datePointage, @Schema(nullable = true) String origine) {
    }

    public record PointageDTO(Long id, Long ligneReleveId, Long ligneId, @MontantJson BigDecimal montant, LocalDate datePointage, String origine, LocalDateTime pointeLe, String pointePar, @Schema(nullable = true) String libelleReleve, String refPiece) {
    }

    /** Une proposition rapproche sur le montant et la date, et attend une confirmation. */
    public record PropositionPointageDTO(Long ligneReleveId, Long ligneId, @MontantJson BigDecimal montant, LocalDate dateReleve, LocalDate dateEcriture, String libelleReleve, String libelleEcriture, int ecartJours) {
    }

    /** Frais depuis une ligne de relevé : l'écriture et le pointage passent ensemble. Aucune détection par mots-clés. */
    public record ComptabiliserReleveRequest(@NotBlank String compte, @MontantJson @Schema(nullable = true) BigDecimal montantTva, @MontantJson @Schema(nullable = true) BigDecimal tauxTva) {
    }

    public record ValiderEtatRequest(@NotNull LocalDate arrete) {
    }

    public record EtatValideDTO(Long id, LocalDate arrete, @MontantJson BigDecimal soldeComptable, @MontantJson BigDecimal soldeReleve, @MontantJson BigDecimal soldeRectifie, LocalDateTime valideLe, String validePar) {
    }

    // --- Le pointage vu de la TVA

    public record LigneClasseeDTO(Long ligneReleveId, LocalDate date, @Schema(nullable = true) String reference, String libelle, @MontantJson BigDecimal debit, @MontantJson BigDecimal credit, @MontantJson BigDecimal pointe,
                                  @Schema(nullable = true) String classement, @Schema(nullable = true) String rubriqueCode) {
    }

    public record ClassementRequest(@NotBlank String classement, @Schema(nullable = true) String rubriqueCode, @Schema(nullable = true) String libelle) {
    }
}
