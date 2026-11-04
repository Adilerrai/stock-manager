package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Le registre des immobilisations et l'inventaire de clôture. Le cumul se lit dans les dotations passées, jamais recopié sur la fiche. */
public final class ImmobilisationDtos {

    private ImmobilisationDtos() {
    }

    /**
     * @param contrepartie FOURNISSEUR, BANQUE ou CAISSE quand le service passe l'écriture d'acquisition
     * @param ecritureAcquisitionId une écriture déjà au journal : facture d'achat ventilée ou à-nouveau d'une reprise
     */
    public record ImmobilisationRequest(@NotBlank String designation, @NotBlank String compteImmo, @Schema(nullable = true) String compteAmort, @Schema(nullable = true) String compteDotation,
                                        @NotNull LocalDate dateAcquisition, @Schema(nullable = true) LocalDate dateMiseEnService, @MontantJson @NotNull BigDecimal valeurBrute,
                                        @MontantJson @Schema(nullable = true) BigDecimal tvaDeduite, @MontantJson @Schema(nullable = true) BigDecimal cumulReprise, @NotBlank String modeAmortissement,
                                        @Schema(nullable = true) Integer dureeMois, @Schema(nullable = true) Long tiersId, @Schema(nullable = true) String contrepartie, @Schema(nullable = true) Long ecritureAcquisitionId,
                                        @Schema(nullable = true) String refPiece, @Schema(nullable = true) LocalDate datePiece, @Schema(nullable = true) String sourceType, @Schema(nullable = true) Long sourceId) {
    }

    public record ComptesLiesDTO(String compteImmo, @Schema(nullable = true) String compteAmort, @Schema(nullable = true) String compteDotation) {
    }

    public record LigneAEnregistrerDTO(Long ecritureId, String numeroComplet, LocalDate date, String refPiece, String libelle, String compte, @MontantJson BigDecimal montant) {
    }

    public record ImmobilisationResumeDTO(Long id, String code, String designation, String compteImmo, LocalDate dateAcquisition, @Schema(nullable = true) LocalDate dateMiseEnService, @MontantJson BigDecimal valeurBrute,
                                          @MontantJson BigDecimal cumulAmortissements, @MontantJson BigDecimal valeurNette, String modeAmortissement, @Schema(nullable = true) Integer dureeMois, String statut) {
    }

    public record AnnuiteDTO(LocalDate du, LocalDate au, @MontantJson BigDecimal montant) {
    }

    public record DotationPasseeDTO(Long id, String exercice, String nature, LocalDate du, LocalDate au, @MontantJson BigDecimal montant, Long ecritureId, boolean contrepassee) {
    }

    public record SortieDTO(Long id, String nature, LocalDate dateSortie, @MontantJson BigDecimal prixCessionHt, @MontantJson BigDecimal tvaCession, @MontantJson BigDecimal amortissementsCumules, @MontantJson BigDecimal valeurNette,
                            @MontantJson BigDecimal plusOuMoinsValue, @MontantJson BigDecimal reversementTva, Long ecritureSortieId, @Schema(nullable = true) Long ecriturePrixId, boolean annulee, @Schema(nullable = true) String motifAnnulation) {
    }

    /** @param planPrevisionnel une prévision : aucun état ne la lit */
    public record ImmobilisationDTO(Long id, String code, String designation, String compteImmo, @Schema(nullable = true) String compteAmort, @Schema(nullable = true) String compteDotation, LocalDate dateAcquisition,
                                    @Schema(nullable = true) LocalDate dateMiseEnService, @MontantJson BigDecimal valeurBrute, @MontantJson BigDecimal cumulReprise, @MontantJson BigDecimal tvaDeduite, String modeAmortissement,
                                    @Schema(nullable = true) Integer dureeMois, @MontantJson @Schema(nullable = true) BigDecimal coefficientDegressif, String statut, @Schema(nullable = true) Long tiersId, Long ecritureAcquisitionId,
                                    @MontantJson BigDecimal cumulAmortissements, @MontantJson BigDecimal valeurNette, List<AnnuiteDTO> planPrevisionnel, List<DotationPasseeDTO> dotationsPassees, List<SortieDTO> sorties, LocalDateTime creeLe) {
    }

    public record DotationsRequest(boolean apercu) {
    }

    public record LigneDotationDTO(Long immobilisationId, String code, String designation, LocalDate du, LocalDate au, @MontantJson BigDecimal montant, String compteDotation, String compteAmortissement) {
    }

    public record DotationsDTO(List<LigneDotationDTO> lignes, @MontantJson BigDecimal total, @Schema(nullable = true) Long ecritureId) {
    }

    public record AnnulationRequest(@NotBlank String motif) {
    }

    public record SortieRequest(@NotBlank String nature, @NotNull LocalDate dateSortie, @MontantJson @Schema(nullable = true) BigDecimal prixCessionHt, @MontantJson @Schema(nullable = true) BigDecimal tvaCession, @Schema(nullable = true) Long tiersId) {
    }

    // --- Inventaire

    public record LigneInventaireDTO(@Schema(nullable = true) Long id, @NotBlank String compteStock, @Schema(nullable = true) String compteVariation, @Schema(nullable = true) Long produitId, @NotBlank String designation,
                                     @MontantJson @NotNull BigDecimal quantite, @MontantJson @NotNull BigDecimal prixUnitaire, @MontantJson @Schema(nullable = true) BigDecimal valeur, @Schema(nullable = true) String origine) {
    }

    public record StockOuvertureDTO(String compteStock, String compteVariation, @MontantJson BigDecimal montant) {
    }

    public record InventaireDTO(@Schema(nullable = true) Long id, Long exerciceId, String exerciceCode, LocalDate dateInventaire, String statut, @Schema(nullable = true) String methodeValorisation, boolean aucunStock,
                                @Schema(nullable = true) Long ecritureInitialId, @Schema(nullable = true) Long ecritureFinalId, @Schema(nullable = true) LocalDateTime valideLe, @Schema(nullable = true) String validePar,
                                @MontantJson BigDecimal stockInitial, @MontantJson BigDecimal stockFinal, List<StockOuvertureDTO> ouverture, List<LigneInventaireDTO> lignes, boolean periodeOuverte) {
    }

    public record InventaireRequest(@Schema(nullable = true) String methodeValorisation, @NotNull Boolean aucunStock, @Valid @NotNull List<LigneInventaireDTO> lignes) {
    }
}
