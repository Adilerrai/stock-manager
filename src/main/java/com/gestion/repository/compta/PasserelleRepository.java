package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.PasserelleDtos.CorrespondanceDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Les liens, décisions et compléments de la passerelle. Le commercial n'y est jamais écrit : l'état d'une pièce se lit ici. */
@Repository
public class PasserelleRepository {

    /** @param actif son écriture n'a ni contre-passation ni correction */
    public record Lien(String sourceType, Long sourceId, String evenement, int version, String empreinte, Long ecritureId, boolean actif, Long pointDeVenteId, String instantane) {
    }

    public record Ventilation(String compte, BigDecimal montantHt, BigDecimal tauxTva, BigDecimal montantTva, boolean tvaDeductible, String motifNonDeductible,
                              boolean estImmobilisation, String numeroFactureFournisseur, LocalDate dateFactureFournisseur, String rubriqueTva) {
    }

    public record Complement(Long compteFinancierId, LocalDate dateEffet, String referenceDocument) {
    }

    public record ResultatLu(String sourceType, Long sourceId, String evenement, boolean reussi, Long ecritureId, String numeroComplet, String codeErreur, String message) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public PasserelleRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Le dernier lien de chaque pièce et événement, pour un type ou pour une pièce. */
    public List<Lien> derniersLiens(Long livreId, String type, Long sourceId) {
        return jdbc.query("""
                SELECT DISTINCT ON (s.source_id, s.evenement) s.source_type, s.source_id, s.evenement, s.version, s.empreinte, s.ecriture_id, s.point_de_vente_id, s.instantane::text,
                       NOT EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.livre_id = s.livre_id AND (c.contrepassation_de_id = s.ecriture_id OR c.corrige_ecriture_id = s.ecriture_id)) AS actif
                  FROM compta_liens_source s
                 WHERE s.livre_id = :livreId AND s.source_type = :type AND (CAST(:id AS bigint) IS NULL OR s.source_id = :id)
                 ORDER BY s.source_id, s.evenement, s.version DESC
                """, cle(livreId, type, sourceId), (rs, i) -> new Lien(rs.getString(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getLong(6),
                rs.getBoolean(9), (Long) rs.getObject(7), rs.getString(8)));
    }

    public Optional<Lien> dernierLien(Long livreId, String type, Long sourceId, String evenement) {
        return jdbc.query("""
                SELECT s.source_type, s.source_id, s.evenement, s.version, s.empreinte, s.ecriture_id, s.point_de_vente_id, s.instantane::text,
                       NOT EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.livre_id = s.livre_id AND (c.contrepassation_de_id = s.ecriture_id OR c.corrige_ecriture_id = s.ecriture_id)) AS actif
                  FROM compta_liens_source s
                 WHERE s.livre_id = :livreId AND s.source_type = :type AND s.source_id = :id AND s.evenement = :evenement
                 ORDER BY s.version DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("id", sourceId).addValue("evenement", evenement),
                (rs, i) -> new Lien(rs.getString(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getLong(6), rs.getBoolean(9), (Long) rs.getObject(7), rs.getString(8)))
                .stream().findFirst();
    }

    public void ecrireLien(Long livreId, String type, Long sourceId, String evenement, int version, Long pointDeVenteId, String empreinte, String instantane, Long ecritureId, String auteur) {
        jdbc.update("""
                INSERT INTO compta_liens_source (livre_id, source_type, source_id, evenement, version, point_de_vente_id, empreinte, instantane, ecriture_id, cree_par)
                VALUES (:livreId, :type, :id, :evenement, :version, :pdv, :empreinte, CAST(:instantane AS jsonb), :ecritureId, :auteur)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("id", sourceId).addValue("evenement", evenement)
                .addValue("version", version).addValue("pdv", pointDeVenteId).addValue("empreinte", empreinte).addValue("instantane", instantane)
                .addValue("ecritureId", ecritureId).addValue("auteur", auteur));
    }

    public List<Long> piecesEcartees(Long livreId, String type, Long sourceId) {
        return jdbc.queryForList("SELECT source_id FROM compta_pieces_ecartees WHERE livre_id = :livreId AND source_type = :type AND (CAST(:id AS bigint) IS NULL OR source_id = :id)",
                cle(livreId, type, sourceId), Long.class);
    }

    private static MapSqlParameterSource cle(Long livreId, String type, Long sourceId) {
        return new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("id", sourceId, Types.BIGINT);
    }

    public void ecarter(Long livreId, String type, Long sourceId, String motif, String auteur) {
        jdbc.update("""
                INSERT INTO compta_pieces_ecartees (livre_id, source_type, source_id, motif, ecarte_par) VALUES (:livreId, :type, :id, :motif, :auteur)
                ON CONFLICT (livre_id, source_type, source_id) DO UPDATE SET motif = EXCLUDED.motif, ecarte_le = CURRENT_TIMESTAMP, ecarte_par = EXCLUDED.ecarte_par
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("id", sourceId).addValue("motif", motif).addValue("auteur", auteur));
    }

    public void reprendre(Long livreId, String type, Long sourceId) {
        jdbc.update("DELETE FROM compta_pieces_ecartees WHERE livre_id = :livreId AND source_type = :type AND source_id = :id", Map.of("livreId", livreId, "type", type, "id", sourceId));
    }

    public List<CorrespondanceDTO> correspondances(Long livreId) {
        return jdbc.query("SELECT type_cle, cle, compte_numero FROM compta_correspondances WHERE livre_id = :livreId ORDER BY type_cle, cle", Map.of("livreId", livreId),
                (rs, i) -> new CorrespondanceDTO(rs.getString(1), rs.getString(2), rs.getString(3)));
    }

    public void ecrireCorrespondance(Long livreId, String typeCle, String cle, String compte, String auteur) {
        jdbc.update("""
                INSERT INTO compta_correspondances (livre_id, type_cle, cle, compte_numero, modifie_par) VALUES (:livreId, :type, :cle, :compte, :auteur)
                ON CONFLICT (livre_id, type_cle, cle) DO UPDATE SET compte_numero = EXCLUDED.compte_numero, modifie_le = CURRENT_TIMESTAMP, modifie_par = EXCLUDED.modifie_par
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", typeCle).addValue("cle", cle).addValue("compte", compte).addValue("auteur", auteur));
    }

    public void supprimerCorrespondance(Long livreId, String typeCle, String cle) {
        jdbc.update("DELETE FROM compta_correspondances WHERE livre_id = :livreId AND type_cle = :type AND cle = :cle", Map.of("livreId", livreId, "type", typeCle, "cle", cle));
    }

    public Map<Long, List<Ventilation>> ventilations(Long livreId, String type, Long sourceId) {
        Map<Long, List<Ventilation>> parPiece = new java.util.HashMap<>();
        jdbc.query("""
                SELECT source_id, compte_numero, montant_ht, taux_tva, montant_tva, tva_deductible, motif_non_deductible, est_immobilisation, numero_facture_fournisseur, date_facture_fournisseur, rubrique_tva
                  FROM compta_ventilations WHERE livre_id = :livreId AND source_type = :type AND (CAST(:id AS bigint) IS NULL OR source_id = :id) ORDER BY source_id, numero
                """, cle(livreId, type, sourceId), rs -> {
            parPiece.computeIfAbsent(rs.getLong(1), k -> new java.util.ArrayList<>()).add(new Ventilation(rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5),
                    rs.getBoolean(6), rs.getString(7), rs.getBoolean(8), rs.getString(9), rs.getObject(10, LocalDate.class), rs.getString(11)));
        });
        return parPiece;
    }

    public void remplacerVentilation(Long livreId, String type, Long sourceId, List<Ventilation> lignes, String auteur) {
        jdbc.update("DELETE FROM compta_ventilations WHERE livre_id = :livreId AND source_type = :type AND source_id = :id", Map.of("livreId", livreId, "type", type, "id", sourceId));
        int numero = 1;
        for (Ventilation v : lignes) {
            jdbc.update("""
                    INSERT INTO compta_ventilations (livre_id, source_type, source_id, numero, compte_numero, montant_ht, taux_tva, montant_tva, tva_deductible, motif_non_deductible,
                        est_immobilisation, numero_facture_fournisseur, date_facture_fournisseur, rubrique_tva, saisie_par)
                    VALUES (:livreId, :type, :id, :numero, :compte, :ht, :taux, :tva, :deductible, :motif, :immobilisation, :numeroFacture, :dateFacture, :rubrique, :auteur)
                    """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("id", sourceId).addValue("numero", numero++).addValue("compte", v.compte())
                    .addValue("ht", v.montantHt()).addValue("taux", v.tauxTva()).addValue("tva", v.montantTva()).addValue("deductible", v.tvaDeductible())
                    .addValue("motif", v.motifNonDeductible(), Types.VARCHAR).addValue("immobilisation", v.estImmobilisation())
                    .addValue("numeroFacture", v.numeroFactureFournisseur(), Types.VARCHAR)
                    .addValue("dateFacture", v.dateFactureFournisseur() == null ? null : Date.valueOf(v.dateFactureFournisseur()), Types.DATE)
                    .addValue("rubrique", v.rubriqueTva(), Types.VARCHAR).addValue("auteur", auteur));
        }
    }

    /** Par « id|événement ». */
    public Map<String, Complement> complements(Long livreId, String type, Long sourceId) {
        Map<String, Complement> parCle = new java.util.HashMap<>();
        jdbc.query("""
                SELECT source_id, evenement, compte_financier_id, date_effet, reference_document FROM compta_complements_source
                 WHERE livre_id = :livreId AND source_type = :type AND (CAST(:id AS bigint) IS NULL OR source_id = :id)
                """, cle(livreId, type, sourceId), rs -> {
            parCle.put(rs.getLong(1) + "|" + rs.getString(2), new Complement((Long) rs.getObject(3), rs.getObject(4, LocalDate.class), rs.getString(5)));
        });
        return parCle;
    }

    public void ecrireComplement(Long livreId, String type, Long sourceId, String evenement, Complement c, String auteur) {
        jdbc.update("""
                INSERT INTO compta_complements_source (livre_id, source_type, source_id, evenement, compte_financier_id, date_effet, reference_document, saisi_par)
                VALUES (:livreId, :type, :id, :evenement, :financier, :date, :reference, :auteur)
                ON CONFLICT (livre_id, source_type, source_id, evenement) DO UPDATE
                   SET compte_financier_id = EXCLUDED.compte_financier_id, date_effet = EXCLUDED.date_effet, reference_document = EXCLUDED.reference_document,
                       saisi_le = CURRENT_TIMESTAMP, saisi_par = EXCLUDED.saisi_par
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("id", sourceId).addValue("evenement", evenement)
                .addValue("financier", c.compteFinancierId(), Types.BIGINT).addValue("date", c.dateEffet() == null ? null : Date.valueOf(c.dateEffet()), Types.DATE)
                .addValue("reference", c.referenceDocument(), Types.VARCHAR).addValue("auteur", auteur));
    }

    public Long ouvrirLot(Long livreId, int demandes, String auteur) {
        return jdbc.queryForObject("INSERT INTO compta_lots (livre_id, nb_demandes, lance_par) VALUES (:livreId, :demandes, :auteur) RETURNING id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("demandes", demandes).addValue("auteur", auteur), Long.class);
    }

    public void ecrireResultat(Long livreId, Long lotId, String type, Long sourceId, String evenement, boolean reussi, Long ecritureId, String code, String message) {
        jdbc.update("""
                INSERT INTO compta_lot_resultats (livre_id, lot_id, source_type, source_id, evenement, reussi, ecriture_id, code_erreur, message)
                VALUES (:livreId, :lotId, :type, :id, :evenement, :reussi, :ecritureId, :code, :message)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("lotId", lotId).addValue("type", type).addValue("id", sourceId).addValue("evenement", evenement)
                .addValue("reussi", reussi).addValue("ecritureId", ecritureId, Types.BIGINT).addValue("code", code, Types.VARCHAR).addValue("message", message, Types.VARCHAR));
    }

    public void fermerLot(Long livreId, Long lotId, int reussies, int echecs) {
        jdbc.update("UPDATE compta_lots SET termine_le = CURRENT_TIMESTAMP, nb_reussies = :reussies, nb_echecs = :echecs WHERE livre_id = :livreId AND id = :lotId",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("lotId", lotId).addValue("reussies", reussies).addValue("echecs", echecs));
    }

    public List<ResultatLu> resultats(Long livreId, Long lotId) {
        return jdbc.query("""
                SELECT r.source_type, r.source_id, r.evenement, r.reussi, r.ecriture_id,
                       CASE WHEN e.id IS NULL THEN NULL ELSE j.code || '-' || x.code || '-' || LPAD(e.numero::text, 6, '0') END, r.code_erreur, r.message
                  FROM compta_lot_resultats r
                  LEFT JOIN compta_ecritures e ON e.livre_id = r.livre_id AND e.id = r.ecriture_id
                  LEFT JOIN compta_journaux j ON j.id = e.journal_id
                  LEFT JOIN compta_exercices x ON x.id = e.exercice_id
                 WHERE r.livre_id = :livreId AND r.lot_id = :lotId ORDER BY r.id
                """, Map.of("livreId", livreId, "lotId", lotId), (rs, i) -> new ResultatLu(rs.getString(1), rs.getLong(2), rs.getString(3), rs.getBoolean(4), (Long) rs.getObject(5),
                rs.getString(6), rs.getString(7), rs.getString(8)));
    }

    public boolean lotExiste(Long livreId, Long lotId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM compta_lots WHERE livre_id = :livreId AND id = :lotId)", Map.of("livreId", livreId, "lotId", lotId), Boolean.class));
    }
}
