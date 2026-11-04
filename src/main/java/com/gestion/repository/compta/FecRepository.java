package com.gestion.repository.compta;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Map;

/** Parcourt le journal d'un exercice ligne à ligne, sur un curseur du serveur : rien ne s'accumule en mémoire. */
@Repository
public class FecRepository {

    private static final int LIGNES_PAR_LOT = 1000;

    private final NamedParameterJdbcTemplate jdbc;

    public FecRepository(DataSource source) {
        JdbcTemplate modele = new JdbcTemplate(source);
        modele.setFetchSize(LIGNES_PAR_LOT);
        this.jdbc = new NamedParameterJdbcTemplate(modele);
    }

    /** À appeler dans une transaction : sans elle, le pilote PostgreSQL ignore le curseur et lit tout. */
    public void parcourir(Long livreId, Long exerciceId, RowCallbackHandler lecteur) {
        jdbc.query("""
                SELECT j.code AS code_journal, j.libelle AS libelle_journal,
                       j.code || '-' || x.code || '-' || LPAD(e.numero::text, 6, '0') AS num_ecriture, e.date_ecriture,
                       l.compte_numero, c.libelle AS libelle_compte, t.code_auxiliaire, t.nom AS libelle_auxiliaire,
                       l.ref_piece, l.date_piece, COALESCE(l.libelle, e.libelle) AS libelle_ecriture, l.debit, l.credit,
                       lt.code AS lettrage, lt.date_lettrage, e.valide_le::date AS date_validation, l.montant_devise, l.devise
                  FROM compta_lignes l
                  JOIN compta_ecritures e ON e.livre_id = l.livre_id AND e.id = l.ecriture_id
                  JOIN compta_journaux j ON j.livre_id = l.livre_id AND j.id = l.journal_id
                  JOIN compta_exercices x ON x.livre_id = l.livre_id AND x.id = l.exercice_id
                  JOIN compta_comptes c ON c.livre_id = l.livre_id AND c.numero = l.compte_numero
                  LEFT JOIN compta_tiers t ON t.livre_id = l.livre_id AND t.id = l.tiers_id
                  LEFT JOIN compta_lettrage_lignes ll ON ll.ligne_id = l.id
                  LEFT JOIN compta_lettrages lt ON lt.id = ll.lettrage_id
                 WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId
                 ORDER BY j.code, e.numero, l.numero_ligne
                """, Map.of("livreId", livreId, "exerciceId", exerciceId), lecteur);
    }
}
