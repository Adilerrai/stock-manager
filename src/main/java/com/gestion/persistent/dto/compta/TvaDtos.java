package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** La déclaration de TVA : le calcul part des paiements. L'application prépare ; elle n'envoie rien à la DGI. */
public final class TvaDtos {

    private TvaDtos() {
    }

    public record TvaParametresDTO(int annee, boolean assujetti, String faitGenerateur, String periodicite, @MontantJson BigDecimal prorata, boolean prorataProvisoire,
                                   @Schema(nullable = true) Integer premierePeriode, @MontantJson BigDecimal creditInitial, boolean enregistres) {
    }

    public record TvaParametresRequest(@NotNull Boolean assujetti, @NotBlank String faitGenerateur, @NotBlank String periodicite, @MontantJson @NotNull BigDecimal prorata,
                                       @Schema(nullable = true) Boolean prorataProvisoire, @Schema(nullable = true) Integer premierePeriode, @MontantJson @NotNull BigDecimal creditInitial) {
    }

    /** @param id nul : la période n'a pas encore de déclaration ; statut vaut alors A_CREER */
    public record TvaPeriodeDTO(@Schema(nullable = true) Long id, int annee, int numeroPeriode, LocalDate du, LocalDate au, String periodicite, int version, String statut, LocalDate dateLimite,
                                @MontantJson BigDecimal tvaExigible, @MontantJson BigDecimal tvaDeductible, @MontantJson BigDecimal tvaDue, @MontantJson BigDecimal creditPeriode, boolean enRetard) {
    }

    public record TvaCreationRequest(@NotNull Integer annee, @NotNull Integer numeroPeriode) {
    }

    public record TvaLigneDTO(String code, String section, String libelle, @Schema(nullable = true) String taux, @MontantJson @Schema(nullable = true) BigDecimal baseCalculee,
                              @MontantJson @Schema(nullable = true) BigDecimal taxeCalculee, @MontantJson @Schema(nullable = true) BigDecimal baseRetenue,
                              @MontantJson @Schema(nullable = true) BigDecimal taxeRetenue, @Schema(nullable = true) String motifEcart) {
    }

    public record TvaComptesControlesDTO(long bloquants, long alertes, long nonEffectues) {
    }

    public record TvaDeclarationDTO(Long id, int annee, int numeroPeriode, LocalDate du, LocalDate au, String periodicite, int regimeDgi, String faitGenerateur, int version, String statut,
                                    LocalDate dateLimite, String societe, @Schema(nullable = true) String identifiantFiscal, @Schema(nullable = true) String ice, String modeleFormulaire,
                                    @MontantJson BigDecimal creditAnterieur, @MontantJson BigDecimal tvaExigible, @MontantJson BigDecimal tvaDeductible,
                                    @MontantJson BigDecimal tvaDue, @MontantJson BigDecimal creditPeriode, @Schema(nullable = true) LocalDateTime calculeLe, @Schema(nullable = true) LocalDateTime valideLe,
                                    @Schema(nullable = true) String valideePar, @Schema(nullable = true) LocalDate deposeLe, @Schema(nullable = true) String numeroRecu, @Schema(nullable = true) LocalDate payeLe,
                                    @Schema(nullable = true) Long ecritureLiquidationId, @Schema(nullable = true) Long ecriturePaiementId, List<TvaLigneDTO> lignes, TvaComptesControlesDTO controles) {
    }

    public record TvaEncaissementDTO(Long id, String sourceType, Long sourceId, @Schema(nullable = true) Long factureId, @Schema(nullable = true) String factureNumero, LocalDate date,
                                     @Schema(nullable = true) String mode, @Schema(nullable = true) String reference, @Schema(nullable = true) String tiersNom, @MontantJson BigDecimal taux,
                                     @MontantJson BigDecimal ttc, @MontantJson BigDecimal ht, @MontantJson BigDecimal tva, String traitement, @Schema(nullable = true) String ligneCode,
                                     @Schema(nullable = true) String motifSurcharge) {
    }

    public record TvaDeductionDTO(Long id, String sourceType, Long sourceId, int ordre, String numeroFacture, LocalDate dateFacture, String fournisseurNom, @Schema(nullable = true) String fournisseurIf,
                                  @Schema(nullable = true) String fournisseurIce, String designation, String rubriqueCode, @Schema(nullable = true) String ligneCode, String compteTva,
                                  @MontantJson BigDecimal ht, @MontantJson BigDecimal taux, @MontantJson BigDecimal tva, @MontantJson BigDecimal ttc, @MontantJson BigDecimal prorata,
                                  @MontantJson BigDecimal tvaDeductible, int mpId, @Schema(nullable = true) String referencePaiement, LocalDate datePaiement, @Schema(nullable = true) String motifSurcharge) {
    }

    public record TvaRubriqueDTO(String code, String libelle, String compteTva, List<String> taux) {
    }

    /** @param type COLLECTEE ou DEDUCTIBLE sur l'opération visée ; seuls les champs renseignés changent */
    public record TvaOperationRequest(@NotBlank String motif, @Schema(nullable = true) String rubriqueCode, @Schema(nullable = true) String designation, @Schema(nullable = true) String traitement,
                                      @MontantJson @Schema(nullable = true) BigDecimal taux, @Schema(nullable = true) String numeroFacture, @Schema(nullable = true) String fournisseurIf,
                                      @Schema(nullable = true) String fournisseurIce, @Schema(nullable = true) String referencePaiement) {
    }

    public record TvaRattrapageDTO(String sourceType, Long sourceId, String numero, LocalDate date, @MontantJson BigDecimal montant, String mode, @Schema(nullable = true) String tiersNom, boolean plusDeDouzeMois) {
    }

    public record TvaRattraperRequest(@NotEmpty List<TvaPieceVisee> pieces) {
    }

    public record TvaPieceVisee(@NotBlank String sourceType, @NotNull Long sourceId) {
    }

    public record TvaControleDTO(Long id, String code, String gravite, String message, @Schema(nullable = true) String objetType, @Schema(nullable = true) Long objetId, String etat, @Schema(nullable = true) String motif) {
    }

    public record TvaIdentiteRequest(@Schema(nullable = true) String identifiantFiscal, @Schema(nullable = true) String ice, @NotBlank String motif) {
    }

    public record TvaAccepterRequest(@NotBlank String motif) {
    }

    public record TvaAccepterCodeRequest(@NotBlank String code, @NotBlank String motif) {
    }

    public record TvaFichierDTO(String type, String nomFichier, String sha256, LocalDateTime genereLe, String etatDgi, @Schema(nullable = true) String commentaireDgi) {
    }

    public record TvaEtatFichierRequest(@NotBlank String etat, @Schema(nullable = true) String commentaire) {
    }

    public record TvaDepotRequest(@NotNull LocalDate deposeLe, @Schema(nullable = true) String numeroRecu) {
    }

    public record TvaPaiementRequest(@NotNull LocalDate payeLe, @NotBlank String modePaiement, @Schema(nullable = true) Long compteFinancierId) {
    }

    /** Ligne saisie ou corrigée à la main, toujours avec un motif. Sans valeur : la correction est retirée et le calcul reprend. */
    public record TvaCorrectionLigneRequest(@NotBlank String code, @MontantJson @Schema(nullable = true) BigDecimal baseRetenue, @MontantJson @Schema(nullable = true) BigDecimal taxeRetenue, @NotBlank String motif) {
    }
}
