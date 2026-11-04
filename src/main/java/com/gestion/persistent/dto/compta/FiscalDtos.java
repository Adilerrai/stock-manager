package com.gestion.persistent.dto.compta;

import com.gestion.persistent.dto.compta.EtatDtos.ColonneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.LigneEtatDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** L'IS et la liasse. Rien n'est officiel : le XML reste marqué non validé tant que le cahier de la DGI n'a pas été lu. */
public final class FiscalDtos {

    private FiscalDtos() {
    }

    public record SourceFiscaleDTO(String code, String cle, LocalDate valideDu, @MontantJson BigDecimal valeur, String source, String fiabilite) {
    }

    public record IsDTO(Long exerciceId, String exercice, boolean calcule, String categorieTaux, @MontantJson BigDecimal resultatComptable, @MontantJson BigDecimal reintegrations, @MontantJson BigDecimal deductions, @MontantJson BigDecimal deficitsImputes,
                        @MontantJson BigDecimal resultatFiscal, @MontantJson BigDecimal baseArrondie, @MontantJson BigDecimal tauxIs, @MontantJson BigDecimal isTheorique, @MontantJson BigDecimal baseCm, @MontantJson BigDecimal cm, boolean cmExoneree,
                        @MontantJson BigDecimal impotDu, @MontantJson BigDecimal acomptesVerses, @MontantJson BigDecimal excedentAnterieur, @MontantJson BigDecimal reliquat, @MontantJson BigDecimal excedent, @MontantJson BigDecimal css, String statut,
                        @Schema(nullable = true) Long ecritureChargeId, @Schema(nullable = true) Long ecritureLiquidationId, @Schema(nullable = true) LocalDateTime valideLe, @Schema(nullable = true) String validePar, List<SourceFiscaleDTO> sources, boolean periodeOuverte) {
    }

    public record AcompteDTO(int numero, LocalDate echeance, @MontantJson BigDecimal montantDu, @MontantJson @Schema(nullable = true) BigDecimal montantPaye, @Schema(nullable = true) LocalDate datePaiement, @Schema(nullable = true) String reference, @Schema(nullable = true) Long ecritureId) {
    }

    /** @param raison pourquoi rien ne se calcule : premier exercice sans référence, par exemple */
    public record AcomptesDTO(@Schema(nullable = true) String exerciceReference, @MontantJson @Schema(nullable = true) BigDecimal beneficeReference, @Schema(nullable = true) String raison, List<AcompteDTO> acomptes) {
    }

    public record PaiementAcompteRequest(@NotNull LocalDate datePaiement, @MontantJson @NotNull BigDecimal montant, @Schema(nullable = true) String reference, @Schema(nullable = true) Long compteFinancierId) {
    }

    public record LiquidationIsRequest(@NotNull LocalDate datePaiement, @Schema(nullable = true) Long compteFinancierId) {
    }

    public record DeficitDTO(Long id, int anneeOrigine, String nature, @MontantJson BigDecimal montantInitial, @MontantJson BigDecimal montantRestant, boolean saisi) {
    }

    public record DeficitRequest(@NotNull Integer anneeOrigine, @NotBlank String nature, @MontantJson @NotNull BigDecimal montant) {
    }

    public record MotifRequest(@NotBlank String motif) {
    }

    // --- Liasse

    public record CadrageDTO(String code, @Schema(nullable = true) Boolean reussi, @MontantJson @Schema(nullable = true) BigDecimal gauche, @MontantJson @Schema(nullable = true) BigDecimal droite, @Schema(nullable = true) String raison) {
    }

    public record TableauResumeDTO(String code, String titre, String statut, String categorie, @Schema(nullable = true) String raison, @Schema(nullable = true) String etatEtic, @Schema(nullable = true) Integer tableauEdi, List<CadrageDTO> cadrages) {
    }

    public record LiasseDTO(int version, String statut, boolean nonValideEdi, @Schema(nullable = true) String xmlSha256, @Schema(nullable = true) LocalDate deposeLe, @Schema(nullable = true) String numeroRecu, List<TableauResumeDTO> tableaux) {
    }

    public record CelluleSaisieDTO(@NotBlank String codeLigne, @NotBlank String colonne, @MontantJson @Schema(nullable = true) BigDecimal valeurNum, @Schema(nullable = true) String valeurTxt) {
    }

    public record BlocLibreDTO(String code, String libelle, String sens) {
    }

    public record ColonneRegistreDTO(String code, String libelle, String type) {
    }

    public record ValeurRegistreDTO(@NotBlank String colonne, @Schema(nullable = true) String valeur) {
    }

    public record LigneRegistreDTO(@NotNull Integer numero, @Valid @NotNull List<ValeurRegistreDTO> valeurs) {
    }

    /** @param saisissables les cellules à saisir, sous la forme « ligne|colonne » ; registreLectureSeule : le registre vient de l'application, pas d'une saisie */
    public record TableauDTO(String code, String titre, String statut, String categorie, @Schema(nullable = true) String raison, @Schema(nullable = true) String note, List<ColonneEtatDTO> colonnes, List<LigneEtatDTO> lignes, List<String> saisissables,
                             List<BlocLibreDTO> blocs, List<CelluleSaisieDTO> saisies, List<ColonneRegistreDTO> colonnesRegistre, List<LigneRegistreDTO> registre, boolean registreLectureSeule, List<CadrageDTO> cadrages, boolean modifiable) {
    }

    public record SaisiesRequest(@Valid @NotNull List<CelluleSaisieDTO> cellules) {
    }

    public record RegistreRequest(@Valid @NotNull List<LigneRegistreDTO> lignes) {
    }

    public record DepotLiasseRequest(@NotNull LocalDate deposeLe, @Schema(nullable = true) String numeroRecu) {
    }
}
