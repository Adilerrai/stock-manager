package com.gestion.persistent.dto.compta;

import com.gestion.persistent.enums.compta.EtatLivre;
import com.gestion.persistent.enums.compta.OrigineLivre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contrat des routes du livre et de l'abonnement. Tous les champs sortent, null compris. */
public final class LivreDtos {

    private LivreDtos() {
    }

    public record LivreDTO(
            @NotNull EtatLivre etat,
            @Schema(nullable = true) Long livreId,
            @Schema(nullable = true) String raisonSociale,
            @Schema(nullable = true) String identifiantFiscal,
            @Schema(nullable = true) String ice,
            @Schema(nullable = true) String modele,
            @Schema(nullable = true) LocalDate dateDemarrage,
            @Schema(nullable = true) String remiseGlobaleMode,
            @Schema(nullable = true) OrigineLivre origine,
            List<PointDeVenteLivreDTO> pointsDeVente,
            @Schema(nullable = true) Long exerciceCourantId,
            List<PeriodeOuverteDTO> periodesOuvertes,
            @Schema(nullable = true) PeriodeOuverteDTO periodeInventaire,
            @Schema(nullable = true) String versionContrat) {
    }

    public record PointDeVenteLivreDTO(Long id, String nom, LocalDate dateRattachement) {
    }

    public record PeriodeOuverteDTO(Long id, Long exerciceId, int numero, String typePeriode, LocalDate du, LocalDate au) {
    }

    public record IdentiteLivreDTO(
            @NotBlank String raisonSociale,
            @Schema(nullable = true) String formeJuridique,
            @Schema(nullable = true) String identifiantFiscal,
            @Schema(nullable = true) String ice,
            @Schema(nullable = true) String registreCommerce,
            @Schema(nullable = true) String taxeProfessionnelle,
            @Schema(nullable = true) String adresse,
            @Schema(nullable = true) String ville,
            @Schema(nullable = true) String activitePrincipale,
            @Schema(nullable = true) LocalDate dateConstitution,
            @Schema(nullable = true) LocalDate dateDebutExploitation,
            @NotBlank String categorieIs) {
    }

    public record ExerciceDatesRequest(@NotBlank String code, @NotNull LocalDate du, @NotNull LocalDate au) {
    }

    public record ActivationLivreRequest(
            @NotNull LocalDate dateDemarrage,
            @NotNull @Valid ExerciceDatesRequest premierExercice,
            @NotNull @Valid IdentiteLivreDTO identite) {
    }

    public record RattachementPointDeVenteRequest(@NotNull Long pointDeVenteId, @NotNull LocalDate dateRattachement) {
    }

    public record AbonnementDTO(
            Long pointDeVenteId,
            boolean actif,
            @Schema(nullable = true) LocalDate souscritLe,
            @Schema(nullable = true) LocalDateTime modifieLe,
            @Schema(nullable = true) String modifiePar) {
    }

    public record AbonnementRequest(@NotNull Boolean actif) {
    }
}
