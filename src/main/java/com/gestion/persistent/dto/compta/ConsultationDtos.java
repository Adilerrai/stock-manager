package com.gestion.persistent.dto.compta;

import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceCompteDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceTiersDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contrat des consultations et du lettrage. Tout total vient du serveur : le front n'additionne jamais un montant. */
public final class ConsultationDtos {

    private ConsultationDtos() {
    }

    public record TotauxDTO(@MontantJson BigDecimal debit, @MontantJson BigDecimal credit) {
    }

    public record PageJournalDTO(List<EcritureDTO> contenu, TotauxDTO totauxPage, TotauxDTO totauxPeriode,
                                 @Schema(nullable = true) String curseurSuivant) {
    }

    public record GrandLivreCompteDTO(
            ReferenceCompteDTO compte,
            @Schema(nullable = true) ReferenceTiersDTO tiers,
            @MontantJson BigDecimal ouverture,
            @MontantJson BigDecimal cumulDebit,
            @MontantJson BigDecimal cumulCredit,
            @MontantJson BigDecimal solde,
            boolean ouvertureProvisoire,
            List<LigneGrandLivreDTO> contenu,
            @Schema(nullable = true) String curseurSuivant) {
    }

    public record LigneGrandLivreDTO(
            Long ligneId, Long ecritureId, String journalCode, String numeroComplet, LocalDate date, String refPiece,
            String libelle,
            @Schema(nullable = true) ReferenceTiersDTO tiers,
            @MontantJson BigDecimal debit,
            @MontantJson BigDecimal credit,
            @MontantJson BigDecimal soldeProgressif,
            @Schema(nullable = true) String lettrage,
            @Schema(nullable = true) LocalDate dateEcheance) {
    }

    public record TotauxBalanceDTO(
            @MontantJson BigDecimal ouvertureDebit, @MontantJson BigDecimal ouvertureCredit,
            @MontantJson BigDecimal mouvementsDebit, @MontantJson BigDecimal mouvementsCredit,
            @MontantJson BigDecimal soldeDebit, @MontantJson BigDecimal soldeCredit) {
    }

    public record LigneBalanceDTO(
            String compte, String libelle,
            @MontantJson BigDecimal ouvertureDebit, @MontantJson BigDecimal ouvertureCredit,
            @MontantJson BigDecimal mouvementsDebit, @MontantJson BigDecimal mouvementsCredit,
            @MontantJson BigDecimal soldeDebit, @MontantJson BigDecimal soldeCredit) {
    }

    public record TotalClasseDTO(int classe, TotauxBalanceDTO totaux) {
    }

    /** Produits et charges de la balance : l'impôt est le compte 67 passé, s'il l'est. */
    public record SyntheseResultatDTO(
            @MontantJson BigDecimal produits, @MontantJson BigDecimal charges, @MontantJson BigDecimal avantImpots,
            @MontantJson BigDecimal impots, @MontantJson BigDecimal net) {
    }

    public record BalanceDTO(LocalDate arrete, boolean ouvertureProvisoire, boolean expurgee, TotauxBalanceDTO totaux,
                             List<TotalClasseDTO> totauxClasses, SyntheseResultatDTO resultat,
                             List<LigneBalanceDTO> contenu, @Schema(nullable = true) String curseurSuivant) {
    }

    public record LigneBalanceAuxiliaireDTO(
            Long tiersId, String codeAuxiliaire, String nom,
            @MontantJson BigDecimal ouvertureDebit, @MontantJson BigDecimal ouvertureCredit,
            @MontantJson BigDecimal mouvementsDebit, @MontantJson BigDecimal mouvementsCredit,
            @MontantJson BigDecimal soldeDebit, @MontantJson BigDecimal soldeCredit) {
    }

    public record BalanceAuxiliaireDTO(LocalDate arrete, boolean ouvertureProvisoire, String compte, TotauxBalanceDTO totaux,
                                       List<LigneBalanceAuxiliaireDTO> contenu, @Schema(nullable = true) String curseurSuivant) {
    }

    public record TranchesAgeesDTO(
            @MontantJson BigDecimal nonEchu, @MontantJson BigDecimal t0_30, @MontantJson BigDecimal t31_60,
            @MontantJson BigDecimal t61_90, @MontantJson BigDecimal t90Plus, @MontantJson BigDecimal total) {
    }

    public record LigneBalanceAgeeDTO(Long tiersId, String codeAuxiliaire, String nom, TranchesAgeesDTO tranches) {
    }

    public record BalanceAgeeDTO(LocalDate arrete, String compte, TranchesAgeesDTO totaux, List<LigneBalanceAgeeDTO> contenu,
                                 @Schema(nullable = true) String curseurSuivant) {
    }

    public record LigneOuverteDTO(
            Long ligneId, LocalDate date, String numeroComplet, String refPiece, String libelle,
            @MontantJson BigDecimal debit,
            @MontantJson BigDecimal credit,
            @Schema(nullable = true) LocalDate dateEcheance,
            @Schema(nullable = true) Long lettrageId,
            @Schema(nullable = true) String lettrage,
            @Schema(nullable = true) String statutLettrage) {
    }

    public record PageLignesLettrageDTO(@MontantJson BigDecimal soldeOuvert, List<LigneOuverteDTO> contenu,
                                        @Schema(nullable = true) String curseurSuivant) {
    }

    public record LettrageRequest(@NotBlank String compte, @NotNull Long tiersId, @NotBlank String statut,
                                  @NotNull LocalDate dateLettrage, @NotEmpty List<Long> lignes) {
    }

    public record LettrageDTO(Long id, String code, String statut, @MontantJson BigDecimal totalDebit,
                              @MontantJson BigDecimal totalCredit, @MontantJson BigDecimal ecart) {
    }

    public record PropositionLettrageDTO(String origine, List<Long> lignes, String motif, @MontantJson BigDecimal ecart) {
    }
}
