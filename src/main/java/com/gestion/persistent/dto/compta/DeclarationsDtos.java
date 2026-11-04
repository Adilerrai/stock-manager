package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Délais de paiement, retenue à la source sur la TVA, échéancier. Aucun fichier officiel : le cahier EDI des délais n'a pas été lu. */
public final class DeclarationsDtos {

    private DeclarationsDtos() {
    }

    // --- Délais de paiement

    public record DpTrimestreDTO(int annee, int trimestre, LocalDate du, LocalDate au, LocalDate dateLimite, @Schema(nullable = true) Long declarationId, @Schema(nullable = true) String statut,
                                 @MontantJson BigDecimal totalAmende, int lignes) {
    }

    public record DpLigneDTO(Long factureAchatId, String numeroFacture, Long tiersId, String fournisseur, @MontantJson BigDecimal montantTtc, LocalDate dateEmission, LocalDate dateEcheance,
                             @MontantJson BigDecimal montantNonPaye, @MontantJson BigDecimal montantHorsDelai, @Schema(nullable = true) LocalDate datePaiement, int moisRetard,
                             @MontantJson BigDecimal tauxApplique, @MontantJson BigDecimal amende) {
    }

    public record DpDeclarationDTO(boolean calculee, @Schema(nullable = true) Long id, int annee, int trimestre, LocalDate du, LocalDate au, LocalDate dateLimite, @MontantJson BigDecimal chiffreAffaires,
                                   @MontantJson BigDecimal totalNonPayees, @MontantJson BigDecimal totalHorsDelai, @MontantJson BigDecimal totalAmende, @MontantJson BigDecimal versement,
                                   @MontantJson BigDecimal totalLitiges, String statut, @Schema(nullable = true) LocalDateTime valideLe, @Schema(nullable = true) String validePar,
                                   @Schema(nullable = true) LocalDate deposeLe, @Schema(nullable = true) Long ecritureAmendeId, List<DpLigneDTO> lignes) {
    }

    /** Vide : le chiffre d'affaires vient du dernier exercice du livre. */
    public record DpCalculRequest(@MontantJson @Schema(nullable = true) BigDecimal chiffreAffaires) {
    }

    public record DpValidationRequest(@NotNull LocalDate dateEcriture) {
    }

    public record DpDepotRequest(@NotNull LocalDate deposeLe) {
    }

    public record DpFactureDTO(Long factureAchatId, String numero, String fournisseur, @MontantJson BigDecimal montantTtc, LocalDate dateEmission, @Schema(nullable = true) LocalDate dateLivraison,
                               @Schema(nullable = true) Integer delaiConvenuJours, int delaiRetenu, LocalDate echeance, @MontantJson @Schema(nullable = true) BigDecimal litigeMontant,
                               @Schema(nullable = true) LocalDate litigeDate, @Schema(nullable = true) LocalDate litigeJugementDate) {
    }

    public record DpFactureRequest(@Schema(nullable = true) LocalDate dateEmission, @Schema(nullable = true) LocalDate dateLivraison, @Schema(nullable = true) Integer delaiConvenuJours,
                                   @MontantJson @Schema(nullable = true) BigDecimal litigeMontant, @Schema(nullable = true) LocalDate litigeDate, @Schema(nullable = true) LocalDate litigeJugementDate) {
    }

    // --- Retenue à la source

    public record RasMoisResumeDTO(int annee, int mois, LocalDate dateLimite, int reglements, int aQualifier, int aRetenir, @MontantJson BigDecimal totalRetenu) {
    }

    /** {@code etat} : A_QUALIFIER, SANS_FACTURE, SANS_RETENUE, A_RETENIR ou RETENUE. {@code attestation} : VALIDE, PERIMEE, NON_VERIFIEE ou ABSENTE. */
    public record RasLigneDTO(Long reglementId, String numeroReglement, LocalDate datePaiement, @MontantJson BigDecimal montantPaye, @Schema(nullable = true) Long factureAchatId,
                              @Schema(nullable = true) String numeroFacture, @Schema(nullable = true) Long tiersId, @Schema(nullable = true) String fournisseur, @Schema(nullable = true) Boolean personnePhysique,
                              String attestation, @Schema(nullable = true) String nature, @MontantJson BigDecimal montantBrut, @MontantJson BigDecimal cumulServices, @MontantJson BigDecimal montantHt,
                              @MontantJson BigDecimal tauxTva, @MontantJson BigDecimal montantTva, @MontantJson BigDecimal tauxRetenue, @MontantJson BigDecimal montantRetenu,
                              @Schema(nullable = true) String article, String raison, String etat, @Schema(nullable = true) Long operationId, @Schema(nullable = true) Long ecritureId) {
    }

    public record RasMoisDTO(int annee, int mois, LocalDate dateLimite, @MontantJson BigDecimal totalRetenu, @MontantJson BigDecimal versement, List<RasLigneDTO> lignes) {
    }

    public record RasNatureRequest(@NotBlank String nature) {
    }

    /** {@code montantBrut} : le montant du règlement avant retenue, si le commercial a enregistré le net payé. Vide : le montant du règlement. */
    public record RasRetenueRequest(@NotNull Long factureAchatId, @MontantJson @Schema(nullable = true) BigDecimal montantBrut) {
    }

    public record AttestationDTO(Long id, LocalDate dateDelivrance, @Schema(nullable = true) String reference, boolean verifieeSurDgi, LocalDateTime saisieLe, String saisiePar) {
    }

    public record AttestationRequest(@NotNull LocalDate dateDelivrance, @Schema(nullable = true) String reference, boolean verifieeSurDgi) {
    }

    public record TiersFiscalDTO(Long tiersId, String nom, @Schema(nullable = true) Boolean personnePhysique, List<AttestationDTO> attestations) {
    }

    public record TiersFormeRequest(@NotNull Boolean personnePhysique) {
    }

    // --- Échéancier

    public record EcheanceDTO(Long id, String type, String libelle, String periode, LocalDate dateLimite, String statut, String article, @Schema(nullable = true) LocalDate faitLe,
                              @Schema(nullable = true) String reference, boolean enRetard) {
    }

    public record EcheancesDTO(int annee, List<EcheanceDTO> lignes, List<String> avertissements, int joursFeries) {
    }

    public record EcheanceRequest(@NotBlank String statut, @Schema(nullable = true) LocalDate faitLe, @Schema(nullable = true) String reference) {
    }

    public record JourFerieDTO(LocalDate jour, String libelle) {
    }
}
