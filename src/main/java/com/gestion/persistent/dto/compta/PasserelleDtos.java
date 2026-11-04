package com.gestion.persistent.dto.compta;

import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceCompteDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceJournalDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** La passerelle : du commercial au journal, sur action de l'utilisateur. */
public final class PasserelleDtos {

    private PasserelleDtos() {
    }

    public record CompteurSourceDTO(String sourceType, long aComptabiliser, long comptabilisees, long anomalies, long ecartees, long modifiees, long annulees, long disparues, long sansObjet) {
    }

    /** @param bloquante faux : une alerte, qui n'empêche pas de comptabiliser */
    public record AnomaliePieceDTO(String code, String message, boolean bloquante) {
    }

    public record PieceFileDTO(String sourceType, Long sourceId, String evenement, @Schema(nullable = true) String numero, @Schema(nullable = true) LocalDate date,
                               @Schema(nullable = true) Long pointDeVenteId, @Schema(nullable = true) String tiersNom, @MontantJson @Schema(nullable = true) BigDecimal montant,
                               String etat, @Schema(nullable = true) String empreinte, @Schema(nullable = true) Integer version, @Schema(nullable = true) Long ecritureId,
                               List<AnomaliePieceDTO> anomalies, boolean unitaire) {
    }

    public record PageFileDTO(List<PieceFileDTO> contenu, @Schema(nullable = true) String curseurSuivant) {
    }

    /** @param id nul tant que le tiers n'existe pas dans le livre : il naîtra de la première pièce qui le demande */
    public record TiersProposeDTO(@Schema(nullable = true) Long id, @Schema(nullable = true) String codeAuxiliaire, String nom) {
    }

    public record LignePropositionDTO(ReferenceCompteDTO compte, @Schema(nullable = true) TiersProposeDTO tiers, @MontantJson BigDecimal debit, @MontantJson BigDecimal credit,
                                      @MontantJson @Schema(nullable = true) BigDecimal tauxTva, @MontantJson @Schema(nullable = true) BigDecimal baseTva) {
    }

    public record PropositionDTO(String sourceType, Long sourceId, String evenement, @Schema(nullable = true) Integer version, @Schema(nullable = true) String empreinte, String etat,
                                 @Schema(nullable = true) ReferenceJournalDTO journal, @Schema(nullable = true) LocalDate datePiece, @Schema(nullable = true) LocalDate dateEcriture,
                                 @Schema(nullable = true) String refPiece, @Schema(nullable = true) String libelle, @Schema(nullable = true) Long contrepassationDe,
                                 List<LignePropositionDTO> lignes, List<AnomaliePieceDTO> anomalies, List<String> complementsRequis) {
    }

    public record ComplementsRequest(@Schema(nullable = true) Long compteFinancierId, @Schema(nullable = true) LocalDate dateEffet, @Schema(nullable = true) String referenceDocument) {
    }

    public record PieceDemandee(@NotBlank String sourceType, @NotNull Long sourceId, @NotBlank String evenement, @Schema(nullable = true) String empreinteVue,
                                @Schema(nullable = true) @Valid ComplementsRequest complements) {
    }

    public record ComptabiliserRequest(@NotEmpty @Valid List<PieceDemandee> pieces) {
    }

    public record ResultatPieceDTO(String sourceType, Long sourceId, String evenement, boolean reussi, @Schema(nullable = true) Long ecritureId, @Schema(nullable = true) String numeroComplet,
                                   @Schema(nullable = true) String codeErreur, @Schema(nullable = true) String message) {
    }

    public record LotDTO(Long lotId, int reussies, int echecs, List<ResultatPieceDTO> resultats) {
    }

    /** du et au bornent la lecture, comme pour la file. */
    public record ApercuRequest(@Schema(nullable = true) LocalDate du, @Schema(nullable = true) LocalDate au, @NotEmpty @Valid List<PieceDemandee> pieces) {
    }

    public record ApercuCompteDTO(String compte, @Schema(nullable = true) String libelle, @MontantJson BigDecimal debit, @MontantJson BigDecimal credit) {
    }

    public record ApercuAnomalieDTO(String sourceType, Long sourceId, String numero, String code, String message, boolean bloquante) {
    }

    public record ApercuDTO(List<ApercuCompteDTO> parCompte, List<ApercuAnomalieDTO> anomalies, int pieces, @MontantJson BigDecimal totalDebit, @MontantJson BigDecimal totalCredit) {
    }

    public record EcarterRequest(@NotBlank String motif) {
    }

    public record VentilationLigneDTO(@NotBlank String compte, @MontantJson @NotNull BigDecimal montantHt, @MontantJson @Schema(nullable = true) BigDecimal tauxTva,
                                      @MontantJson @Schema(nullable = true) BigDecimal montantTva, @Schema(nullable = true) Boolean tvaDeductible, @Schema(nullable = true) String motifNonDeductible,
                                      @Schema(nullable = true) Boolean estImmobilisation, @Schema(nullable = true) String numeroFactureFournisseur,
                                      @Schema(nullable = true) LocalDate dateFactureFournisseur, @Schema(nullable = true) String rubriqueTva) {
    }

    public record VentilationRequest(@NotNull @Valid List<VentilationLigneDTO> lignes) {
    }

    public record VerificationRequest(@NotNull LocalDate du, @NotNull LocalDate au) {
    }

    public record CorrespondanceDTO(@NotBlank String typeCle, @NotNull String cle, @NotBlank String compte) {
    }

    public record CompteFinancierDTO(Long id, String code, String nom, String type) {
    }
}
