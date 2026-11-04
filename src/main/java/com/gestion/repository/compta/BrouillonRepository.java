package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonLigneRequest;
import com.gestion.persistent.dto.compta.EcritureDtos.BrouillonRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BrouillonRepository {

    /** En-tête d'un brouillon. */
    public record LigneBrouillon(Long id, Long journalId, LocalDate dateEcriture, LocalDate datePiece, String refPiece, String libelle,
                                 String nature, String motif, boolean aExtourner, LocalDateTime creeLe, String creePar, LocalDateTime modifieLe) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public BrouillonRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long creer(Long livreId, BrouillonRequest b, String auteur) {
        return jdbc.queryForObject("""
                INSERT INTO compta_brouillons (livre_id, journal_id, date_ecriture, date_piece, ref_piece, libelle, nature, motif, a_extourner, cree_par)
                VALUES (:livreId, :journalId, :dateEcriture, :datePiece, :refPiece, :libelle, :nature, :motif, :aExtourner, :auteur)
                RETURNING id
                """, entete(livreId, b).addValue("auteur", auteur), Long.class);
    }

    public void modifier(Long livreId, Long id, BrouillonRequest b) {
        jdbc.update("""
                UPDATE compta_brouillons SET journal_id = :journalId, date_ecriture = :dateEcriture, date_piece = :datePiece,
                    ref_piece = :refPiece, libelle = :libelle, nature = :nature, motif = :motif, a_extourner = :aExtourner, modifie_le = CURRENT_TIMESTAMP
                 WHERE livre_id = :livreId AND id = :id
                """, entete(livreId, b).addValue("id", id));
    }

    public void remplacerLignes(Long livreId, Long id, List<BrouillonLigneRequest> lignes) {
        jdbc.update("DELETE FROM compta_brouillon_lignes WHERE livre_id = :livreId AND brouillon_id = :id", Map.of("livreId", livreId, "id", id));
        List<SqlParameterSource> lots = new ArrayList<>();
        int numero = 1;
        for (BrouillonLigneRequest l : lignes) {
            lots.add(new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("numero", numero++)
                    .addValue("compte", vide(l.compte())).addValue("tiersId", l.tiersId(), Types.BIGINT)
                    .addValue("debit", l.debit()).addValue("credit", l.credit()).addValue("libelle", vide(l.libelle()))
                    .addValue("echeance", date(l.dateEcheance()), Types.DATE).addValue("mouvement", vide(l.natureMouvement()))
                    .addValue("horsFlux", Boolean.TRUE.equals(l.horsFlux())).addValue("devise", vide(l.devise()))
                    .addValue("montantDevise", l.montantDevise(), Types.NUMERIC).addValue("taux", l.tauxTva(), Types.NUMERIC)
                    .addValue("base", l.baseTva(), Types.NUMERIC));
        }
        jdbc.batchUpdate("""
                INSERT INTO compta_brouillon_lignes (livre_id, brouillon_id, numero_ligne, compte_numero, tiers_id, debit, credit, libelle,
                    date_echeance, nature_mouvement, hors_flux, devise, montant_devise, taux_tva, base_tva)
                VALUES (:livreId, :id, :numero, :compte, :tiersId, :debit, :credit, :libelle, :echeance, :mouvement, :horsFlux,
                    :devise, :montantDevise, :taux, :base)
                """, lots.toArray(SqlParameterSource[]::new));
    }

    public Optional<LigneBrouillon> lire(Long livreId, Long id) {
        return jdbc.query("""
                SELECT id, journal_id, date_ecriture, date_piece, ref_piece, libelle, nature, motif, a_extourner, cree_le, cree_par, modifie_le
                  FROM compta_brouillons WHERE livre_id = :livreId AND id = :id
                """, Map.of("livreId", livreId, "id", id), (rs, i) -> ligne(rs)).stream().findFirst();
    }

    public List<LigneBrouillon> lister(Long livreId, Long journalId, LocalDate du, LocalDate au) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, journal_id, date_ecriture, date_piece, ref_piece, libelle, nature, motif, a_extourner, cree_le, cree_par, modifie_le
                  FROM compta_brouillons WHERE livre_id = :livreId
                """);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId);
        if (journalId != null) {
            sql.append(" AND journal_id = :journalId");
            p.addValue("journalId", journalId);
        }
        if (du != null) {
            sql.append(" AND date_ecriture BETWEEN :du AND :au");
            p.addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au));
        }
        return jdbc.query(sql.append(" ORDER BY date_ecriture, id").toString(), p, (rs, i) -> ligne(rs));
    }

    public List<BrouillonLigneDTO> lignes(Long livreId, Long id) {
        return jdbc.query("""
                SELECT numero_ligne, compte_numero, tiers_id, debit, credit, libelle, date_echeance, nature_mouvement, hors_flux,
                       devise, montant_devise, taux_tva, base_tva
                  FROM compta_brouillon_lignes WHERE livre_id = :livreId AND brouillon_id = :id ORDER BY numero_ligne
                """, Map.of("livreId", livreId, "id", id), (rs, i) -> new BrouillonLigneDTO(rs.getInt("numero_ligne"), rs.getString("compte_numero"),
                (Long) rs.getObject("tiers_id"), rs.getBigDecimal("debit"), rs.getBigDecimal("credit"), rs.getString("libelle"),
                rs.getObject("date_echeance", LocalDate.class), rs.getString("nature_mouvement"), rs.getBoolean("hors_flux"),
                rs.getString("devise"), rs.getObject("montant_devise", BigDecimal.class), rs.getObject("taux_tva", BigDecimal.class),
                rs.getObject("base_tva", BigDecimal.class)));
    }

    /** Les brouillards qui ne sont pas dans les états : leur présence rend la situation provisoire. */
    public long compter(Long livreId, LocalDate du, LocalDate au) {
        Long nombre = jdbc.queryForObject("""
                SELECT COUNT(*) FROM compta_brouillons WHERE livre_id = :livreId AND date_ecriture BETWEEN :du AND :au
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", java.sql.Date.valueOf(du)).addValue("au", java.sql.Date.valueOf(au)), Long.class);
        return nombre == null ? 0 : nombre;
    }

    public void supprimer(Long livreId, Long id) {
        jdbc.update("DELETE FROM compta_brouillons WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id));
    }

    private static LigneBrouillon ligne(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LigneBrouillon(rs.getLong("id"), rs.getLong("journal_id"), rs.getObject("date_ecriture", LocalDate.class),
                rs.getObject("date_piece", LocalDate.class), rs.getString("ref_piece"), rs.getString("libelle"), rs.getString("nature"),
                rs.getString("motif"), rs.getBoolean("a_extourner"), rs.getObject("cree_le", LocalDateTime.class), rs.getString("cree_par"),
                rs.getObject("modifie_le", LocalDateTime.class));
    }

    private static MapSqlParameterSource entete(Long livreId, BrouillonRequest b) {
        return new MapSqlParameterSource().addValue("livreId", livreId).addValue("journalId", b.journalId())
                .addValue("dateEcriture", Date.valueOf(b.dateEcriture())).addValue("datePiece", date(b.datePiece()), Types.DATE)
                .addValue("refPiece", vide(b.refPiece())).addValue("libelle", vide(b.libelle()))
                .addValue("nature", b.nature()).addValue("motif", vide(b.motif())).addValue("aExtourner", Boolean.TRUE.equals(b.aExtourner()));
    }

    private static Date date(LocalDate d) {
        return d != null ? Date.valueOf(d) : null;
    }

    private static String vide(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }
}
