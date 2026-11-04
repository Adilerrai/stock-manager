package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.LigneEcritureComptaDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.PieceJointeDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceCompteDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceJournalDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceTiersDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.SourceEcritureDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * L'unique chemin d'écriture vers le journal. Le journal est en ajout seul et n'a pas d'entité :
 * aucun save ne peut le modifier. Le déclencheur attribue le numéro, l'horodatage et est_contrepassation.
 */
@Repository
public class JournalRepository {

    public record NouvelleEcriture(Long exerciceId, Long periodeId, Long journalId, String typeJournal, LocalDate dateEcriture,
                                   LocalDate datePiece, String refPiece, String libelle, String nature, String origine,
                                   Long contrepassationDeId, Long corrigeEcritureId, String motif, boolean aExtourner,
                                   BigDecimal totalDebit, BigDecimal totalCredit, LocalDateTime saisieLe, String saisiePar,
                                   String validePar, List<NouvelleLigne> lignes) {
    }

    public record NouvelleLigne(String compte, Long tiersId, BigDecimal debit, BigDecimal credit, String libelle, LocalDate dateEcheance,
                                String natureMouvement, boolean horsFlux, String devise, BigDecimal montantDevise, BigDecimal tauxTva,
                                BigDecimal baseTva, Long ligneOrigineId, Long pointDeVenteId) {
    }

    public record EcritureEnregistree(Long id, long numero, LocalDateTime valideLe, boolean estContrepassation) {
    }

    public record ANouveauEcrit(EcritureEnregistree ecriture, long nbLignes) {
    }

    /** Une ligne relue pour construire un miroir ou lettrer. */
    public record LigneJournal(Long id, int numeroLigne, String compte, Long tiersId, BigDecimal debit, BigDecimal credit, String libelle,
                               LocalDate dateEcheance, String natureMouvement, boolean horsFlux, String devise, BigDecimal montantDevise,
                               BigDecimal tauxTva, BigDecimal baseTva, Long pointDeVenteId) {
    }

    private static final String INSERER_LIGNE = """
            INSERT INTO compta_lignes (livre_id, ecriture_id, numero_ligne, exercice_id, journal_id, date_ecriture, nature_ecriture,
                est_contrepassation, compte_numero, tiers_id, debit, credit, libelle, ref_piece, date_piece, date_echeance,
                nature_mouvement, hors_flux, devise, montant_devise, taux_tva, base_tva, ligne_origine_id, point_de_vente_id)
            VALUES (:livreId, :ecritureId, :numeroLigne, :exerciceId, :journalId, :dateEcriture, :nature, :estContrepassation, :compte,
                :tiersId, :debit, :credit, :libelle, :refPiece, :datePiece, :echeance, :mouvement, :horsFlux, :devise, :montantDevise,
                :taux, :base, :origine, :pointDeVente)
            """;

    /**
     * Ce qui se reprend, classes 1 à 5, écriture de clôture incluse puisqu'elle alimente 119x. Compte ordinaire : une ligne par compte et tiers, au solde net.
     * Compte de tiers lettrable : une ligne par pièce encore ouverte, c'est-à-dire qui n'est pas dans un groupe total entièrement sur l'exercice.
     * C'est ce qui garde le lettrage et la balance âgée d'une année sur l'autre.
     */
    private static final String LIGNES_A_NOUVEAU = """
            WITH soldes AS (
              SELECT l.compte_numero, l.tiers_id, SUM(l.debit - l.credit) AS solde
                FROM compta_lignes l JOIN compta_comptes c ON c.livre_id = l.livre_id AND c.numero = l.compte_numero
               WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceClosId AND l.compte_numero ~ '^[1-5]' AND NOT (c.lettrable AND c.exige_tiers)
               GROUP BY l.compte_numero, l.tiers_id HAVING SUM(l.debit - l.credit) <> 0
            ), pieces AS (
              SELECT l.id, l.compte_numero, l.tiers_id, l.debit, l.credit, l.libelle, l.ref_piece, l.date_piece, l.date_echeance, l.point_de_vente_id
                FROM compta_lignes l JOIN compta_comptes c ON c.livre_id = l.livre_id AND c.numero = l.compte_numero
               WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceClosId AND l.compte_numero ~ '^[1-5]' AND c.lettrable AND c.exige_tiers
                 AND NOT EXISTS (
                   SELECT 1 FROM compta_lettrage_lignes x JOIN compta_lettrages g ON g.id = x.lettrage_id
                    WHERE x.ligne_id = l.id AND g.statut = 'TOTAL'
                      AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes y JOIN compta_lignes m ON m.id = y.ligne_id
                                       WHERE y.lettrage_id = g.id AND m.exercice_id <> l.exercice_id))
            ), toutes AS (
              SELECT compte_numero, tiers_id, GREATEST(solde, 0) AS debit, GREATEST(-solde, 0) AS credit, CAST(NULL AS varchar) AS libelle,
                     CAST(NULL AS varchar) AS ref_piece, CAST(NULL AS date) AS date_piece, CAST(NULL AS date) AS date_echeance,
                     CAST(NULL AS bigint) AS origine, CAST(NULL AS bigint) AS point_de_vente
                FROM soldes
              UNION ALL
              SELECT compte_numero, tiers_id, debit, credit, libelle, ref_piece, date_piece, date_echeance, id, point_de_vente_id FROM pieces
            )
            """;

    private static final String SELECTION_ECRITURE = """
            SELECT e.*, j.code AS journal_code, j.libelle AS journal_libelle, x.code AS exercice_code,
                   (SELECT c.id FROM compta_ecritures c WHERE c.contrepassation_de_id = e.id) AS contrepassee_par
              FROM compta_ecritures e
              JOIN compta_journaux j ON j.id = e.journal_id
              JOIN compta_exercices x ON x.id = e.exercice_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JournalRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public EcritureEnregistree ajouter(Long livreId, NouvelleEcriture e) {
        EcritureEnregistree enregistree = entete(livreId, e, e.lignes().size(), e.totalDebit(), e.totalCredit());
        List<SqlParameterSource> lignes = new ArrayList<>();
        int numero = 1;
        for (NouvelleLigne l : e.lignes()) {
            lignes.add(new MapSqlParameterSource().addValue("livreId", livreId).addValue("ecritureId", enregistree.id())
                    .addValue("numeroLigne", numero++).addValue("exerciceId", e.exerciceId()).addValue("journalId", e.journalId())
                    .addValue("dateEcriture", Date.valueOf(e.dateEcriture())).addValue("nature", e.nature())
                    .addValue("estContrepassation", enregistree.estContrepassation()).addValue("compte", l.compte())
                    .addValue("tiersId", l.tiersId(), Types.BIGINT).addValue("debit", l.debit()).addValue("credit", l.credit())
                    .addValue("libelle", l.libelle(), Types.VARCHAR).addValue("refPiece", e.refPiece()).addValue("datePiece", Date.valueOf(e.datePiece()))
                    .addValue("echeance", l.dateEcheance() != null ? Date.valueOf(l.dateEcheance()) : null, Types.DATE)
                    .addValue("mouvement", l.natureMouvement(), Types.VARCHAR).addValue("horsFlux", l.horsFlux())
                    .addValue("devise", l.devise(), Types.CHAR).addValue("montantDevise", l.montantDevise(), Types.NUMERIC)
                    .addValue("taux", l.tauxTva(), Types.NUMERIC).addValue("base", l.baseTva(), Types.NUMERIC)
                    .addValue("origine", l.ligneOrigineId(), Types.BIGINT).addValue("pointDeVente", l.pointDeVenteId(), Types.BIGINT));
        }
        jdbc.batchUpdate(INSERER_LIGNE, lignes.toArray(SqlParameterSource[]::new));
        // Lève une incohérence ici plutôt qu'au commit, tout en permettant une autre écriture dans la même transaction.
        jdbc.getJdbcTemplate().execute("SET CONSTRAINTS trg_compta_ecritures_scellement IMMEDIATE");
        jdbc.getJdbcTemplate().execute("SET CONSTRAINTS trg_compta_ecritures_scellement DEFERRED");
        return enregistree;
    }

    private EcritureEnregistree entete(Long livreId, NouvelleEcriture e, int nbLignes, BigDecimal totalDebit, BigDecimal totalCredit) {
        MapSqlParameterSource entete = new MapSqlParameterSource()
                .addValue("livreId", livreId).addValue("exerciceId", e.exerciceId()).addValue("periodeId", e.periodeId())
                .addValue("journalId", e.journalId()).addValue("typeJournal", e.typeJournal())
                .addValue("dateEcriture", Date.valueOf(e.dateEcriture())).addValue("datePiece", Date.valueOf(e.datePiece()))
                .addValue("refPiece", e.refPiece()).addValue("libelle", e.libelle()).addValue("nature", e.nature())
                .addValue("origine", e.origine()).addValue("contrepassationDeId", e.contrepassationDeId(), Types.BIGINT)
                .addValue("corrigeEcritureId", e.corrigeEcritureId(), Types.BIGINT).addValue("motif", e.motif(), Types.VARCHAR)
                .addValue("aExtourner", e.aExtourner()).addValue("nbLignes", nbLignes)
                .addValue("totalDebit", totalDebit).addValue("totalCredit", totalCredit)
                .addValue("saisieLe", Timestamp.valueOf(e.saisieLe())).addValue("saisiePar", e.saisiePar()).addValue("validePar", e.validePar());
        return jdbc.queryForObject("""
                INSERT INTO compta_ecritures (livre_id, exercice_id, periode_id, journal_id, type_journal, numero,
                    date_ecriture, date_piece, ref_piece, libelle, nature, origine,
                    contrepassation_de_id, corrige_ecriture_id, motif, a_extourner, nb_lignes, total_debit, total_credit,
                    saisie_le, saisie_par, valide_le, valide_par)
                VALUES (:livreId, :exerciceId, :periodeId, :journalId, :typeJournal, 0,
                    :dateEcriture, :datePiece, :refPiece, :libelle, :nature, :origine,
                    :contrepassationDeId, :corrigeEcritureId, :motif, :aExtourner, :nbLignes, :totalDebit, :totalCredit,
                    :saisieLe, :saisiePar, now(), :validePar)
                RETURNING id, numero, valide_le, est_contrepassation
                """, entete, (rs, i) -> new EcritureEnregistree(rs.getLong(1), rs.getLong(2), rs.getObject(3, LocalDateTime.class), rs.getBoolean(4)));
    }

    /**
     * L'à-nouveau de l'exercice suivant, écrit par la base : aucune ligne ne passe par Java, quel que soit le volume.
     * Sans ligne à reprendre, il n'y a pas d'écriture. Une somme qui ne s'équilibre pas interrompt la clôture.
     */
    public Optional<ANouveauEcrit> ajouterANouveau(Long livreId, Long exerciceClosId, NouvelleEcriture e) {
        MapSqlParameterSource selection = new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceClosId", exerciceClosId);
        Object[] totaux = jdbc.queryForObject(LIGNES_A_NOUVEAU + " SELECT COUNT(*), COALESCE(SUM(debit), 0), COALESCE(SUM(credit), 0) FROM toutes", selection,
                (rs, i) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3)});
        long nombre = (Long) totaux[0];
        if (nombre == 0) {
            return Optional.empty();
        }
        BigDecimal debit = (BigDecimal) totaux[1];
        BigDecimal credit = (BigDecimal) totaux[2];
        if (debit.compareTo(credit) != 0) {
            throw new IllegalStateException("À-nouveaux déséquilibrés : débit " + debit + ", crédit " + credit);
        }
        EcritureEnregistree enregistree = entete(livreId, e, (int) nombre, debit, credit);
        jdbc.update(LIGNES_A_NOUVEAU + """
                INSERT INTO compta_lignes (livre_id, ecriture_id, numero_ligne, exercice_id, journal_id, date_ecriture, nature_ecriture,
                    est_contrepassation, compte_numero, tiers_id, debit, credit, libelle, ref_piece, date_piece, date_echeance,
                    nature_mouvement, hors_flux, devise, montant_devise, taux_tva, base_tva, ligne_origine_id, point_de_vente_id)
                SELECT :livreId, :ecritureId, CAST(ROW_NUMBER() OVER (ORDER BY compte_numero, tiers_id NULLS FIRST, origine NULLS FIRST) AS integer),
                       :exerciceId, :journalId, :dateEcriture, 'A_NOUVEAU', FALSE, compte_numero, tiers_id, debit, credit, libelle,
                       COALESCE(ref_piece, :refPiece), COALESCE(date_piece, :dateEcriture), date_echeance, NULL, FALSE, NULL, NULL, NULL, NULL, origine, point_de_vente
                  FROM toutes
                """, selection.addValue("ecritureId", enregistree.id()).addValue("exerciceId", e.exerciceId()).addValue("journalId", e.journalId())
                .addValue("dateEcriture", Date.valueOf(e.dateEcriture())).addValue("refPiece", e.refPiece()));
        jdbc.getJdbcTemplate().execute("SET CONSTRAINTS trg_compta_ecritures_scellement IMMEDIATE");
        jdbc.getJdbcTemplate().execute("SET CONSTRAINTS trg_compta_ecritures_scellement DEFERRED");
        return Optional.of(new ANouveauEcrit(enregistree, nombre));
    }

    public Optional<LocalDate> derniereDate(Long livreId, Long exerciceId, Long journalId) {
        return jdbc.query("""
                SELECT derniere_date FROM compta_sequences_journal
                 WHERE livre_id = :livreId AND exercice_id = :exerciceId AND journal_id = :journalId
                """, Map.of("livreId", livreId, "exerciceId", exerciceId, "journalId", journalId),
                (rs, i) -> rs.getObject(1, LocalDate.class)).stream().findFirst();
    }

    public Optional<EcritureDTO> lire(Long livreId, Long id) {
        List<EcritureDTO> resultat = jdbc.query(SELECTION_ECRITURE + " WHERE e.livre_id = :livreId AND e.id = :id",
                Map.of("livreId", livreId, "id", id), (rs, i) -> enTete(rs, List.of(), List.of(), lignes(livreId, rs.getLong("id"))));
        return resultat.stream().findFirst().map(e -> new EcritureDTO(e.id(), e.exerciceId(), e.periodeId(), e.journal(), e.numero(),
                e.numeroComplet(), e.dateEcriture(), e.datePiece(), e.refPiece(), e.libelle(), e.nature(), e.origine(), e.estContrepassation(),
                e.contrepassationDeId(), e.contrepasseeParId(), e.corrigeEcritureId(), e.motif(), e.aExtourner(), e.totalDebit(), e.totalCredit(),
                e.saisieLe(), e.saisiePar(), e.valideLe(), e.validePar(), sources(livreId, id), piecesJointes(livreId, id), e.lignes()));
    }

    /** Une page d'écritures avec leurs lignes, lues en une requête par lot. */
    public List<EcritureDTO> page(Long livreId, Long exerciceId, Long journalId, Long periodeId, Long apresJournal, Long apresNumero, int taille) {
        StringBuilder sql = new StringBuilder(SELECTION_ECRITURE).append(" WHERE e.livre_id = :livreId AND e.exercice_id = :exerciceId");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId).addValue("taille", taille);
        if (journalId != null) {
            sql.append(" AND e.journal_id = :journalId");
            p.addValue("journalId", journalId);
        }
        if (periodeId != null) {
            sql.append(" AND e.periode_id = :periodeId");
            p.addValue("periodeId", periodeId);
        }
        if (apresNumero != null) {
            sql.append(" AND (e.journal_id, e.numero) > (:apresJournal, :apresNumero)");
            p.addValue("apresJournal", apresJournal).addValue("apresNumero", apresNumero);
        }
        sql.append(" ORDER BY e.journal_id, e.numero LIMIT :taille");
        List<EcritureDTO> entetes = jdbc.query(sql.toString(), p, (rs, i) -> enTete(rs, List.of(), List.of(), List.of()));
        if (entetes.isEmpty()) {
            return entetes;
        }
        Map<Long, List<LigneEcritureComptaDTO>> parEcriture = lignesParEcriture(livreId, entetes.stream().map(EcritureDTO::id).toList());
        return entetes.stream().map(e -> new EcritureDTO(e.id(), e.exerciceId(), e.periodeId(), e.journal(), e.numero(), e.numeroComplet(),
                e.dateEcriture(), e.datePiece(), e.refPiece(), e.libelle(), e.nature(), e.origine(), e.estContrepassation(), e.contrepassationDeId(),
                e.contrepasseeParId(), e.corrigeEcritureId(), e.motif(), e.aExtourner(), e.totalDebit(), e.totalCredit(), e.saisieLe(), e.saisiePar(),
                e.valideLe(), e.validePar(), List.of(), List.of(), parEcriture.getOrDefault(e.id(), List.of()))).toList();
    }

    public BigDecimal[] totaux(Long livreId, Long exerciceId, Long journalId, Long periodeId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(sum(total_debit), 0), COALESCE(sum(total_credit), 0) FROM compta_ecritures
                 WHERE livre_id = :livreId AND exercice_id = :exerciceId
                """);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId);
        if (journalId != null) {
            sql.append(" AND journal_id = :journalId");
            p.addValue("journalId", journalId);
        }
        if (periodeId != null) {
            sql.append(" AND periode_id = :periodeId");
            p.addValue("periodeId", periodeId);
        }
        return jdbc.queryForObject(sql.toString(), p, (rs, i) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2)});
    }

    /** Les totaux de chaque journal, période par période : le journal centralisateur. */
    public List<com.gestion.persistent.dto.compta.ConsultationDtos.CentralisateurLigneDTO> centralisateur(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT j.id, j.code, j.libelle, p.id, p.numero, p.type_periode, p.date_debut, p.date_fin, count(*), SUM(e.total_debit), SUM(e.total_credit)
                  FROM compta_ecritures e
                  JOIN compta_journaux j ON j.livre_id = e.livre_id AND j.id = e.journal_id
                  JOIN compta_periodes p ON p.livre_id = e.livre_id AND p.id = e.periode_id
                 WHERE e.livre_id = :livreId AND e.exercice_id = :exercice
                 GROUP BY j.id, j.code, j.libelle, p.id, p.numero, p.type_periode, p.date_debut, p.date_fin
                 ORDER BY p.numero, j.code
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId),
                (rs, i) -> new com.gestion.persistent.dto.compta.ConsultationDtos.CentralisateurLigneDTO(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getInt(5), rs.getString(6),
                        rs.getObject(7, LocalDate.class), rs.getObject(8, LocalDate.class), rs.getLong(9), rs.getBigDecimal(10), rs.getBigDecimal(11)));
    }

    /** Un montant se cherche sur chaque ligne, pas seulement sur le total ; un compte cherche aussi ses sous-comptes. */
    public List<EcritureDTO> rechercher(Long livreId, String refPiece, LocalDate datePiece, BigDecimal montant, String libelle, String compte, LocalDate du, LocalDate au) {
        StringBuilder sql = new StringBuilder(SELECTION_ECRITURE).append(" WHERE e.livre_id = :livreId");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId);
        if (refPiece != null && !refPiece.isBlank()) {
            sql.append(" AND e.ref_piece ILIKE :ref");
            p.addValue("ref", "%" + refPiece.trim() + "%");
        }
        if (datePiece != null) {
            sql.append(" AND e.date_piece = :datePiece");
            p.addValue("datePiece", Date.valueOf(datePiece));
        }
        if (montant != null) {
            sql.append(" AND (e.total_debit = :montant OR EXISTS (SELECT 1 FROM compta_lignes lm WHERE lm.livre_id = e.livre_id AND lm.ecriture_id = e.id AND (lm.debit = :montant OR lm.credit = :montant)))");
            p.addValue("montant", montant);
        }
        if (libelle != null && !libelle.isBlank()) {
            sql.append(" AND e.libelle ILIKE :libelle");
            p.addValue("libelle", "%" + libelle.trim() + "%");
        }
        if (compte != null && !compte.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM compta_lignes lc WHERE lc.livre_id = e.livre_id AND lc.ecriture_id = e.id AND lc.compte_numero LIKE :compte)");
            p.addValue("compte", compte.trim() + "%");
        }
        if (du != null) {
            sql.append(" AND e.date_ecriture >= :du");
            p.addValue("du", Date.valueOf(du));
        }
        if (au != null) {
            sql.append(" AND e.date_ecriture <= :au");
            p.addValue("au", Date.valueOf(au));
        }
        return jdbc.query(sql.append(" ORDER BY e.date_ecriture DESC, e.id DESC LIMIT 200").toString(), p, (rs, i) -> enTete(rs, List.of(), List.of(), List.of()));
    }

    public List<LigneJournal> lignesBrutes(Long livreId, Long ecritureId) {
        return jdbc.query("""
                SELECT id, numero_ligne, compte_numero, tiers_id, debit, credit, libelle, date_echeance, nature_mouvement, hors_flux,
                       devise, montant_devise, taux_tva, base_tva, point_de_vente_id
                  FROM compta_lignes WHERE livre_id = :livreId AND ecriture_id = :id ORDER BY numero_ligne
                """, Map.of("livreId", livreId, "id", ecritureId), (rs, i) -> new LigneJournal(rs.getLong("id"), rs.getInt("numero_ligne"),
                rs.getString("compte_numero"), (Long) rs.getObject("tiers_id"), rs.getBigDecimal("debit"), rs.getBigDecimal("credit"),
                rs.getString("libelle"), rs.getObject("date_echeance", LocalDate.class), rs.getString("nature_mouvement"), rs.getBoolean("hors_flux"),
                rs.getString("devise"), rs.getObject("montant_devise", BigDecimal.class), rs.getObject("taux_tva", BigDecimal.class),
                rs.getObject("base_tva", BigDecimal.class), (Long) rs.getObject("point_de_vente_id")));
    }

    public void lierPieceJointe(Long livreId, Long ecritureId, Long documentId, String auteur) {
        jdbc.update("""
                INSERT INTO compta_pieces_jointes (livre_id, ecriture_id, document_id, lie_par)
                VALUES (:livreId, :ecritureId, :documentId, :auteur) ON CONFLICT DO NOTHING
                """, Map.of("livreId", livreId, "ecritureId", ecritureId, "documentId", documentId, "auteur", auteur));
    }

    private List<LigneEcritureComptaDTO> lignes(Long livreId, Long ecritureId) {
        return lignesParEcriture(livreId, List.of(ecritureId)).getOrDefault(ecritureId, List.of());
    }

    private Map<Long, List<LigneEcritureComptaDTO>> lignesParEcriture(Long livreId, Collection<Long> ecritures) {
        Map<Long, List<LigneEcritureComptaDTO>> resultat = new java.util.HashMap<>();
        jdbc.query("""
                SELECT l.*, c.libelle AS compte_libelle, t.code_auxiliaire, t.nom AS tiers_nom, g.code AS lettrage
                  FROM compta_lignes l
                  JOIN compta_comptes c ON c.livre_id = l.livre_id AND c.numero = l.compte_numero
                  LEFT JOIN compta_tiers t ON t.id = l.tiers_id
                  LEFT JOIN compta_lettrage_lignes x ON x.ligne_id = l.id
                  LEFT JOIN compta_lettrages g ON g.id = x.lettrage_id
                 WHERE l.livre_id = :livreId AND l.ecriture_id IN (:ecritures)
                 ORDER BY l.ecriture_id, l.numero_ligne
                """, Map.of("livreId", livreId, "ecritures", ecritures), rs -> {
            Long tiers = (Long) rs.getObject("tiers_id");
            resultat.computeIfAbsent(rs.getLong("ecriture_id"), cle -> new ArrayList<>()).add(new LigneEcritureComptaDTO(
                    rs.getLong("id"), rs.getInt("numero_ligne"), new ReferenceCompteDTO(rs.getString("compte_numero"), rs.getString("compte_libelle")),
                    tiers != null ? new ReferenceTiersDTO(tiers, rs.getString("code_auxiliaire"), rs.getString("tiers_nom")) : null,
                    rs.getBigDecimal("debit"), rs.getBigDecimal("credit"), rs.getString("libelle"), rs.getString("ref_piece"),
                    rs.getObject("date_piece", LocalDate.class), rs.getObject("date_echeance", LocalDate.class), rs.getString("lettrage"),
                    rs.getString("nature_mouvement"), rs.getBoolean("hors_flux"), rs.getString("devise"),
                    rs.getObject("montant_devise", BigDecimal.class), rs.getObject("taux_tva", BigDecimal.class), rs.getObject("base_tva", BigDecimal.class)));
        });
        return resultat;
    }

    /** Les liens vers le commercial arrivent avec la passerelle : compta_liens_source n'existe qu'à partir de là. */
    private List<SourceEcritureDTO> sources(Long livreId, Long ecritureId) {
        Boolean presente = jdbc.queryForObject("SELECT to_regclass('compta_liens_source') IS NOT NULL", Map.of(), Boolean.class);
        if (!Boolean.TRUE.equals(presente)) {
            return List.of();
        }
        return jdbc.query("""
                SELECT source_type, source_id, evenement, version, instantane->>'numero' FROM compta_liens_source
                 WHERE livre_id = :livreId AND ecriture_id = :ecritureId ORDER BY source_type, source_id
                """, Map.of("livreId", livreId, "ecritureId", ecritureId), (rs, i) -> new SourceEcritureDTO(rs.getString(1), rs.getLong(2),
                rs.getString(3), rs.getInt(4), rs.getString(5)));
    }

    private List<PieceJointeDTO> piecesJointes(Long livreId, Long ecritureId) {
        return jdbc.query("""
                SELECT p.document_id, d.nom_original FROM compta_pieces_jointes p JOIN documents_comptables d ON d.id = p.document_id
                 WHERE p.livre_id = :livreId AND p.ecriture_id = :ecritureId ORDER BY p.lie_le
                """, Map.of("livreId", livreId, "ecritureId", ecritureId), (rs, i) -> new PieceJointeDTO(rs.getLong(1), rs.getString(2)));
    }

    private static EcritureDTO enTete(ResultSet rs, List<SourceEcritureDTO> sources, List<PieceJointeDTO> pieces, List<LigneEcritureComptaDTO> lignes) throws SQLException {
        String journal = rs.getString("journal_code");
        long numero = rs.getLong("numero");
        return new EcritureDTO(rs.getLong("id"), rs.getLong("exercice_id"), rs.getLong("periode_id"),
                new ReferenceJournalDTO(rs.getLong("journal_id"), journal, rs.getString("journal_libelle")), numero,
                String.format("%s-%s-%06d", journal, rs.getString("exercice_code"), numero),
                rs.getObject("date_ecriture", LocalDate.class), rs.getObject("date_piece", LocalDate.class), rs.getString("ref_piece"), rs.getString("libelle"),
                rs.getString("nature"), rs.getString("origine"), rs.getBoolean("est_contrepassation"), (Long) rs.getObject("contrepassation_de_id"),
                (Long) rs.getObject("contrepassee_par"), (Long) rs.getObject("corrige_ecriture_id"), rs.getString("motif"), rs.getBoolean("a_extourner"),
                rs.getBigDecimal("total_debit"), rs.getBigDecimal("total_credit"), rs.getObject("saisie_le", LocalDateTime.class), rs.getString("saisie_par"),
                rs.getObject("valide_le", LocalDateTime.class), rs.getString("valide_par"), sources, pieces, lignes);
    }
}
