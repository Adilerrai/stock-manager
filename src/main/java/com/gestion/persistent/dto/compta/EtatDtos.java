package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** États de synthèse, contrôles rejouables et balance de reprise. */
public final class EtatDtos {

    private EtatDtos() {
    }

    public record ColonneEtatDTO(String code, String libelle) {
    }

    /** Les six mentions qu'un état doit porter, à l'écran comme sur papier. */
    public record MentionsEtatDTO(String entite, String typeEtat, String exercice, String periode, LocalDateTime produitLe, String logiciel) {
    }

    /**
     * @param valeurs par code de colonne, chaînes décimales. La seule carte de l'API : ces colonnes ne se connaissent qu'à l'exécution.
     *                Une colonne sans valeur est nulle ou absente, jamais « 0.00 ».
     */
    public record LigneEtatDTO(String code, String libelle, String type, int niveau, @Schema(nullable = true) String masse, boolean horsTotal,
                               Map<String, String> valeurs, List<String> alertes) {
    }

    /** @param reussi nul quand le contrôle n'a pas pu tourner : un contrôle non effectué ne s'affiche jamais réussi */
    public record ControleEtatDTO(String code, String gravite, @Schema(nullable = true) Boolean reussi,
                                  @MontantJson @Schema(nullable = true) BigDecimal gauche, @MontantJson @Schema(nullable = true) BigDecimal droite,
                                  @MontantJson @Schema(nullable = true) BigDecimal ecart, @Schema(nullable = true) String message) {
    }

    public record EtatDTO(String code, String titre, String versionDefinition, String origine, @Schema(nullable = true) Integer figeage,
                          boolean ouvertureProvisoire, long nbBrouillons, LocalDate arrete, MentionsEtatDTO mentions, List<ColonneEtatDTO> colonnes,
                          List<LigneEtatDTO> lignes, List<ControleEtatDTO> controles) {
    }

    public record CompteCelluleDTO(String compte, String libelle, String mesure, @MontantJson BigDecimal montant, boolean reclassement) {
    }

    public record DetailCelluleDTO(@Schema(nullable = true) String formule, List<CompteCelluleDTO> comptes) {
    }

    /** @param reussi nul : non effectué */
    public record ResultatControleDTO(String code, String gravite, @Schema(nullable = true) Boolean reussi,
                                      @MontantJson @Schema(nullable = true) BigDecimal valeurA, @MontantJson @Schema(nullable = true) BigDecimal valeurB,
                                      @Schema(nullable = true) String message, @Schema(nullable = true) String lien) {
    }

    public record RapportControlesDTO(@Schema(nullable = true) String execution, @Schema(nullable = true) LocalDateTime executeLe,
                                      @Schema(nullable = true) String executePar, long bloquants, List<ResultatControleDTO> resultats) {
    }

    public record RepriseLigneDTO(@NotBlank String compte, @MontantJson @NotNull BigDecimal soldeDebit, @MontantJson @NotNull BigDecimal soldeCredit) {
    }

    public record RepriseSoldesDTO(LocalDate du, LocalDate au, boolean possible, @MontantJson BigDecimal totalDebit, @MontantJson BigDecimal totalCredit,
                                   List<RepriseLigneDTO> lignes) {
    }

    public record RepriseSoldesRequest(@NotNull @Valid List<RepriseLigneDTO> lignes) {
    }

    public record ParametreFiscalDTO(String code, String cle, LocalDate valideDu, @Schema(nullable = true) LocalDate valideAu,
                                     @MontantJson BigDecimal valeur, @MontantJson @Schema(nullable = true) BigDecimal borneMin,
                                     @MontantJson @Schema(nullable = true) BigDecimal borneMax, String source, String fiabilite) {
    }
}
