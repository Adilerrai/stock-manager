package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.LivreDtos.ActivationLivreRequest;
import com.gestion.persistent.dto.compta.LivreDtos.IdentiteLivreDTO;
import com.gestion.persistent.dto.compta.LivreDtos.PeriodeOuverteDTO;
import com.gestion.persistent.dto.compta.LivreDtos.PointDeVenteLivreDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LivreRepository {

    /** Comptes de tiers du plan : ils exigent un tiers à la saisie et se lettrent. */
    private static final List<String> NATURES_TIERS = List.of("COLLECTIF_CLIENT", "CLIENT_DOUTEUX", "CLIENT_EFFET_A_RECEVOIR",
            "CLIENT_A_FACTURER", "CLIENT_CREDITEUR", "COLLECTIF_FOURNISSEUR", "FOURNISSEUR_EFFET_A_PAYER", "FOURNISSEUR_FNP",
            "FOURNISSEUR_DEBITEUR");

    /** Identité et réglages lus sur compta_livres. */
    public record LigneLivre(Long id, Long societeId, String raisonSociale, String identifiantFiscal, String ice, String modele,
                             LocalDate dateDemarrage, String remiseGlobaleMode) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public LivreRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LigneLivre lire(Long livreId) {
        return jdbc.queryForObject("""
                SELECT id, societe_id, raison_sociale, identifiant_fiscal, ice, modele, date_demarrage, remise_globale_mode
                  FROM compta_livres WHERE id = :livreId
                """, Map.of("livreId", livreId), (rs, i) -> new LigneLivre(rs.getLong("id"), (Long) rs.getObject("societe_id"),
                rs.getString("raison_sociale"), rs.getString("identifiant_fiscal"), rs.getString("ice"), rs.getString("modele"),
                rs.getObject("date_demarrage", LocalDate.class), rs.getString("remise_globale_mode")));
    }

    public List<PointDeVenteLivreDTO> pointsDeVente(Long livreId) {
        return jdbc.query("""
                SELECT l.point_de_vente_id, COALESCE(p.nom, p.nom_point_de_vente) AS nom, l.date_rattachement
                  FROM compta_livre_points_de_vente l JOIN point_de_vente p ON p.id = l.point_de_vente_id
                 WHERE l.livre_id = :livreId ORDER BY l.date_rattachement, l.point_de_vente_id
                """, Map.of("livreId", livreId), (rs, i) -> new PointDeVenteLivreDTO(rs.getLong(1), rs.getString(2),
                rs.getObject(3, LocalDate.class)));
    }

    public List<PeriodeOuverteDTO> periodesOuvertes(Long livreId) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin
                  FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = 'COURANTE' AND statut = 'OUVERTE'
                 ORDER BY date_debut
                """, Map.of("livreId", livreId), (rs, i) -> periodeOuverte(rs));
    }

    /** La période 99 de l'exercice le plus ancien où elle est ouverte : les écritures d'inventaire s'y passent. */
    public Optional<PeriodeOuverteDTO> periodeInventaire(Long livreId) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin
                  FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = 'CLOTURE' AND statut = 'OUVERTE'
                 ORDER BY date_debut LIMIT 1
                """, Map.of("livreId", livreId), (rs, i) -> periodeOuverte(rs)).stream().findFirst();
    }

    public static PeriodeOuverteDTO periodeOuverte(ResultSet rs) throws SQLException {
        return new PeriodeOuverteDTO(rs.getLong("id"), rs.getLong("exercice_id"), rs.getInt("numero"),
                rs.getString("type_periode"), rs.getObject("date_debut", LocalDate.class), rs.getObject("date_fin", LocalDate.class));
    }

    /** L'exercice de la première période ouverte, sinon le plus récent. */
    public Optional<Long> exerciceCourant(Long livreId) {
        List<Long> ouverts = jdbc.queryForList("""
                SELECT exercice_id FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = 'COURANTE' AND statut = 'OUVERTE'
                 ORDER BY date_debut LIMIT 1
                """, Map.of("livreId", livreId), Long.class);
        if (!ouverts.isEmpty()) {
            return Optional.of(ouverts.get(0));
        }
        return jdbc.queryForList("SELECT id FROM compta_exercices WHERE livre_id = :livreId ORDER BY date_debut DESC LIMIT 1",
                Map.of("livreId", livreId), Long.class).stream().findFirst();
    }

    public Long creer(Long societeId, ActivationLivreRequest demande, String auteur) {
        IdentiteLivreDTO identite = demande.identite();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("societeId", societeId)
                .addValue("dateDemarrage", Date.valueOf(demande.dateDemarrage()))
                .addValue("activePar", auteur);
        parametresIdentite(p, identite);
        return jdbc.queryForObject("""
                INSERT INTO compta_livres (societe_id, date_demarrage, raison_sociale, forme_juridique, identifiant_fiscal, ice,
                    registre_commerce, taxe_professionnelle, adresse, ville, activite_principale, date_constitution,
                    date_debut_exploitation, categorie_is, active_par)
                VALUES (:societeId, :dateDemarrage, :raisonSociale, :formeJuridique, :identifiantFiscal, :ice,
                    :registreCommerce, :taxeProfessionnelle, :adresse, :ville, :activitePrincipale, :dateConstitution,
                    :dateDebutExploitation, :categorieIs, :activePar)
                RETURNING id
                """, p, Long.class);
    }

    /** L'identité complète du livre : l'IS et la liasse en lisent les dates et la catégorie. */
    public IdentiteLivreDTO identite(Long livreId) {
        return jdbc.queryForObject("""
                SELECT raison_sociale, forme_juridique, identifiant_fiscal, ice, registre_commerce, taxe_professionnelle, adresse, ville, activite_principale, date_constitution, date_debut_exploitation, categorie_is
                  FROM compta_livres WHERE id = :livreId
                """, Map.of("livreId", livreId), (rs, i) -> new IdentiteLivreDTO(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getObject(10, LocalDate.class), rs.getObject(11, LocalDate.class), rs.getString(12)));
    }

    /** T01 et T31 se corrigent depuis la déclaration : seuls ces deux champs bougent. */
    public void modifierIdentifiants(Long livreId, String identifiantFiscal, String ice) {
        jdbc.update("UPDATE compta_livres SET identifiant_fiscal = :fiscal, ice = :ice WHERE id = :livreId",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("fiscal", identifiantFiscal, java.sql.Types.VARCHAR).addValue("ice", ice, java.sql.Types.VARCHAR));
    }

    public void modifierIdentite(Long livreId, IdentiteLivreDTO identite) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId);
        parametresIdentite(p, identite);
        jdbc.update("""
                UPDATE compta_livres SET raison_sociale = :raisonSociale, forme_juridique = :formeJuridique,
                    identifiant_fiscal = :identifiantFiscal, ice = :ice, registre_commerce = :registreCommerce,
                    taxe_professionnelle = :taxeProfessionnelle, adresse = :adresse, ville = :ville,
                    activite_principale = :activitePrincipale, date_constitution = :dateConstitution,
                    date_debut_exploitation = :dateDebutExploitation, categorie_is = :categorieIs
                 WHERE id = :livreId
                """, p);
    }

    public void rattacher(Long livreId, Long pointDeVenteId, LocalDate dateRattachement) {
        jdbc.update("""
                INSERT INTO compta_livre_points_de_vente (point_de_vente_id, livre_id, date_rattachement)
                VALUES (:pointDeVenteId, :livreId, :dateRattachement)
                """, Map.of("pointDeVenteId", pointDeVenteId, "livreId", livreId, "dateRattachement", Date.valueOf(dateRattachement)));
    }

    /** Copie le référentiel : 723 comptes de 4 chiffres et plus. Rubriques et postes restent des titres du référentiel. */
    public int copierPlan(Long livreId) {
        return jdbc.update("""
                INSERT INTO compta_comptes (livre_id, numero, libelle, sens_normal, nature, imputable, exige_tiers, lettrable, origine)
                SELECT :livreId, numero, libelle, sens_normal, nature, imputable_defaut,
                       nature IN (:naturesTiers), nature IN (:naturesTiers), origine
                  FROM compta_referentiel_comptes
                 WHERE modele = 'NORMAL' AND length(numero) >= 4
                """, Map.of("livreId", livreId, "naturesTiers", NATURES_TIERS));
    }

    public Optional<IdentiteLivreDTO> identiteDuProfil(Long pointDeVenteId) {
        return jdbc.query("""
                SELECT p.nom_entreprise, p.numero_identification_fiscale, p.ice, p.registre_commerce, p.adresse, p.ville, p.activite
                  FROM entreprise_profiles p WHERE p.point_de_vente_id = :id
                """, Map.of("id", pointDeVenteId), (rs, i) -> new IdentiteLivreDTO(rs.getString(1), null, rs.getString(2), rs.getString(3),
                rs.getString(4), null, rs.getString(5), rs.getString(6), rs.getString(7), null, null, "GENERAL"))
                .stream().findFirst();
    }

    public Optional<IdentiteLivreDTO> identiteDuPointDeVente(Long pointDeVenteId) {
        return jdbc.query("SELECT COALESCE(nom, nom_point_de_vente), adresse FROM point_de_vente WHERE id = :id",
                Map.of("id", pointDeVenteId), (rs, i) -> new IdentiteLivreDTO(rs.getString(1), null, null, null, null, null,
                        rs.getString(2), null, null, null, null, "GENERAL")).stream().findFirst();
    }

    public Optional<IdentiteLivreDTO> identiteDuDossier(Long societeId) {
        return jdbc.query("""
                SELECT raison_sociale, forme_juridique, identifiant_fiscal, ice, rc, patente, adresse, ville
                  FROM societes WHERE id = :id
                """, Map.of("id", societeId), (rs, i) -> new IdentiteLivreDTO(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), null, null, null, "GENERAL"))
                .stream().findFirst();
    }

    private static void parametresIdentite(MapSqlParameterSource p, IdentiteLivreDTO identite) {
        p.addValue("raisonSociale", identite.raisonSociale())
                .addValue("formeJuridique", identite.formeJuridique())
                .addValue("identifiantFiscal", identite.identifiantFiscal())
                .addValue("ice", identite.ice())
                .addValue("registreCommerce", identite.registreCommerce())
                .addValue("taxeProfessionnelle", identite.taxeProfessionnelle())
                .addValue("adresse", identite.adresse())
                .addValue("ville", identite.ville())
                .addValue("activitePrincipale", identite.activitePrincipale())
                .addValue("dateConstitution", identite.dateConstitution() != null ? Date.valueOf(identite.dateConstitution()) : null, java.sql.Types.DATE)
                .addValue("dateDebutExploitation", identite.dateDebutExploitation() != null ? Date.valueOf(identite.dateDebutExploitation()) : null, java.sql.Types.DATE)
                .addValue("categorieIs", identite.categorieIs());
    }
}
