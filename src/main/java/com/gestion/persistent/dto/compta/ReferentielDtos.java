package com.gestion.persistent.dto.compta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contrat du plan, des tiers, des journaux, des exercices et des périodes. */
public final class ReferentielDtos {

    private ReferentielDtos() {
    }

    public record PageCompteDTO(List<CompteDTO> contenu, @Schema(nullable = true) String curseurSuivant) {
    }

    public record CompteDTO(String numero, String libelle, int classe, String sensNormal, String nature, boolean imputable,
                            boolean exigeTiers, boolean lettrable, boolean actif, String origine, boolean mouvemente) {
    }

    /** Rubriques et postes du plan : des titres, jamais imputés. */
    public record TitrePlanDTO(String numero, String libelle, int classe) {
    }

    public record CompteCreationRequest(@NotBlank String numero, @NotBlank String libelle) {
    }

    public record ComptePatchRequest(@Schema(nullable = true) String libelle, @Schema(nullable = true) Boolean actif,
                                     @Schema(nullable = true) Boolean imputable) {
    }

    public record PageTiersDTO(List<TiersDTO> contenu, @Schema(nullable = true) String curseurSuivant) {
    }

    public record TiersDTO(Long id, String typeTiers, String codeAuxiliaire, String nom,
                           @Schema(nullable = true) String ice, @Schema(nullable = true) String identifiantFiscal,
                           @Schema(nullable = true) String sourceType, @Schema(nullable = true) Long sourceId,
                           @Schema(nullable = true) String compteCollectif, boolean entrepriseLiee, boolean organismePublic,
                           boolean nonResident, boolean actif) {
    }

    public record TiersCreationRequest(@NotBlank String typeTiers, @NotBlank String nom,
                                       @Schema(nullable = true) String ice, @Schema(nullable = true) String identifiantFiscal,
                                       @Schema(nullable = true) String compteCollectif,
                                       @Schema(nullable = true) Boolean entrepriseLiee,
                                       @Schema(nullable = true) Boolean organismePublic,
                                       @Schema(nullable = true) Boolean nonResident) {
    }

    public record TiersPatchRequest(@Schema(nullable = true) String nom, @Schema(nullable = true) String ice,
                                    @Schema(nullable = true) String identifiantFiscal,
                                    @Schema(nullable = true) String compteCollectif,
                                    @Schema(nullable = true) Boolean entrepriseLiee,
                                    @Schema(nullable = true) Boolean organismePublic,
                                    @Schema(nullable = true) Boolean nonResident, @Schema(nullable = true) Boolean actif) {
    }

    public record JournalDTO(Long id, String code, String libelle, String typeJournal,
                             @Schema(nullable = true) String compteTresorerie, boolean actif) {
    }

    public record JournalCreationRequest(@NotBlank String code, @NotBlank String libelle, @NotBlank String typeJournal,
                                         @NotBlank String compteTresorerie) {
    }

    public record ExerciceDTO(Long id, String code, LocalDate du, LocalDate au, String statut,
                              @Schema(nullable = true) Long precedentId,
                              @MontantJson @Schema(nullable = true) BigDecimal resultatNet, boolean ouvertureProvisoire,
                              @Schema(nullable = true) LocalDateTime arreteLe, @Schema(nullable = true) String arretePar,
                              @Schema(nullable = true) LocalDateTime clotureLe, @Schema(nullable = true) String cloturePar) {
    }

    public record PeriodeDTO(Long id, int numero, String typePeriode, LocalDate du, LocalDate au, String statut,
                             @Schema(nullable = true) LocalDateTime clotureeLe, @Schema(nullable = true) String cloturePar,
                             long nbEcritures, long nbBrouillons,
                             @Schema(nullable = true, description = "Null tant que la passerelle n'est pas livrée") Long nbPiecesEnAttente) {
    }

    public record ControlePeriodeDTO(Long periodeId, List<String> blocages, long nbBrouillons,
                                     @Schema(nullable = true) Long piecesEnAttente) {
    }

    public record CloturePeriodeRequest(@Schema(nullable = true) String motifPiecesEnAttente) {
    }

    public record CloturePeriodeDTO(Long periodeId, String statut,
                                    @Schema(nullable = true) LivreDtos.PeriodeOuverteDTO periodeOuverte,
                                    boolean exerciceSuivantRequis) {
    }

    public record CodeRequis(@NotNull String code) {
    }
}
