package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneInventaireDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.StockOuvertureDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** L'inventaire de clôture. La lecture du commercial ne sert qu'à proposer : les lignes se figent côté comptabilité, en quantités et en valeurs. */
@Repository
public class InventaireComptaRepository {

    public record Inventaire(Long id, String statut, String methode, boolean aucunStock, Long ecritureInitial, Long ecritureFinal, LocalDateTime valideLe, String validePar) {
    }

    public record LigneProposee(Long produitId, String typeProduit, String designation, BigDecimal quantite, BigDecimal prixUnitaire, String origine) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public InventaireComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Inventaire> lire(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT id, statut, methode_valorisation, aucun_stock, ecriture_initial_id, ecriture_final_id, valide_le, valide_par FROM compta_inventaires WHERE livre_id = :livreId AND exercice_id = :exercice
                """, Map.of("livreId", livreId, "exercice", exerciceId), (rs, i) -> new Inventaire(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), (Long) rs.getObject(5), (Long) rs.getObject(6), rs.getObject(7, LocalDateTime.class), rs.getString(8)))
                .stream().findFirst();
    }

    public List<LigneInventaireDTO> lignes(Long livreId, Long inventaireId) {
        return jdbc.query("""
                SELECT id, compte_stock, compte_variation, produit_id, designation, quantite, prix_unitaire, valeur, origine FROM compta_inventaire_lignes WHERE livre_id = :livreId AND inventaire_id = :id ORDER BY compte_stock, id
                """, Map.of("livreId", livreId, "id", inventaireId), (rs, i) -> new LigneInventaireDTO(rs.getLong(1), rs.getString(2), rs.getString(3), (Long) rs.getObject(4), rs.getString(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getString(9)));
    }

    public Long enregistrer(Long livreId, Long exerciceId, String methode, boolean aucunStock) {
        return jdbc.queryForObject("""
                INSERT INTO compta_inventaires (livre_id, exercice_id, methode_valorisation, aucun_stock) VALUES (:livreId, :exercice, :methode, :aucun)
                ON CONFLICT (livre_id, exercice_id) DO UPDATE SET methode_valorisation = EXCLUDED.methode_valorisation, aucun_stock = EXCLUDED.aucun_stock RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("methode", methode).addValue("aucun", aucunStock), Long.class);
    }

    public void viderLignes(Long livreId, Long inventaireId) {
        jdbc.update("DELETE FROM compta_inventaire_lignes WHERE livre_id = :livreId AND inventaire_id = :id", Map.of("livreId", livreId, "id", inventaireId));
    }

    public void ajouterLigne(Long livreId, Long inventaireId, LigneInventaireDTO l, BigDecimal valeur, String compteVariation) {
        jdbc.update("""
                INSERT INTO compta_inventaire_lignes (livre_id, inventaire_id, compte_stock, compte_variation, produit_id, designation, quantite, prix_unitaire, valeur, origine)
                VALUES (:livreId, :id, :stock, :variation, :produit, :designation, :quantite, :prix, :valeur, :origine)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", inventaireId).addValue("stock", l.compteStock()).addValue("variation", compteVariation).addValue("produit", l.produitId(), Types.BIGINT)
                .addValue("designation", l.designation()).addValue("quantite", l.quantite()).addValue("prix", l.prixUnitaire()).addValue("valeur", valeur).addValue("origine", l.origine(), Types.VARCHAR));
    }

    public void valider(Long livreId, Long inventaireId, Long ecritureInitial, Long ecritureFinal, String auteur) {
        jdbc.update("""
                UPDATE compta_inventaires SET statut = 'VALIDE', ecriture_initial_id = :initial, ecriture_final_id = :final, valide_le = CURRENT_TIMESTAMP, valide_par = :auteur WHERE livre_id = :livreId AND id = :id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", inventaireId).addValue("initial", ecritureInitial, Types.BIGINT).addValue("final", ecritureFinal, Types.BIGINT).addValue("auteur", auteur));
    }

    public void rouvrir(Long livreId, Long inventaireId) {
        jdbc.update("""
                UPDATE compta_inventaires SET statut = 'BROUILLON', ecriture_initial_id = NULL, ecriture_final_id = NULL, valide_le = NULL, valide_par = NULL WHERE livre_id = :livreId AND id = :id
                """, Map.of("livreId", livreId, "id", inventaireId));
    }

    /** Le dernier inventaire validé de chaque dépôt de chaque point de vente du livre, dans l'exercice. */
    public List<LigneProposee> proposition(Long livreId, LocalDate du, LocalDate au) {
        return jdbc.query("""
                WITH derniers AS (
                  SELECT DISTINCT ON (i.point_de_vente_id, i.depot_id) i.id, i.reference FROM inventaires i
                    JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = i.point_de_vente_id
                   WHERE i.statut = 'VALIDE' AND i.date_inventaire BETWEEN :du AND :au
                   ORDER BY i.point_de_vente_id, i.depot_id, i.date_inventaire DESC, i.id DESC
                )
                SELECT l.produit_id, pr.type_produit, COALESCE(pr.designation, pr.description, pr.reference, 'Article ' || l.produit_id), l.quantite_reelle, l.prix_unitaire, d.reference
                  FROM lignes_inventaire l JOIN derniers d ON d.id = l.inventaire_id LEFT JOIN produits pr ON pr.id = l.produit_id
                 WHERE COALESCE(l.quantite_reelle, 0) > 0 ORDER BY 3
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au)),
                (rs, i) -> new LigneProposee((Long) rs.getObject(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getString(6)));
    }

    /** Le stock d'ouverture, lu sur les à-nouveaux de l'exercice : il se solde dans 6114 et assimilés à la clôture. */
    public List<StockOuvertureDTO> ouverture(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT compte_numero, SUM(debit - credit) FROM compta_lignes
                 WHERE livre_id = :livreId AND exercice_id = :exercice AND nature_ecriture = 'A_NOUVEAU' AND compte_numero LIKE '31%' GROUP BY compte_numero HAVING SUM(debit - credit) <> 0 ORDER BY compte_numero
                """, Map.of("livreId", livreId, "exercice", exerciceId), (rs, i) -> new StockOuvertureDTO(rs.getString(1), null, rs.getBigDecimal(2)));
    }
}
