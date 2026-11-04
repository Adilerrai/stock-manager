package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.FiscalDtos.AcompteDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.CelluleSaisieDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DeficitDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Les tables de l'IS et de la liasse. Les registres T13, T18 et T19 portent des données personnelles : rien de leur contenu ne se journalise. */
@Repository
public class FiscalComptaRepository {

    public record CalculIs(Long id, String categorie, BigDecimal resultatComptable, BigDecimal reintegrations, BigDecimal deductions, BigDecimal deficits, BigDecimal resultatFiscal, BigDecimal baseArrondie, BigDecimal tauxIs, BigDecimal isTheorique,
                           BigDecimal baseCm, BigDecimal cm, boolean cmExoneree, BigDecimal impotDu, BigDecimal acomptes, BigDecimal excedentAnterieur, BigDecimal reliquat, BigDecimal excedent, BigDecimal css, String parametres, String statut,
                           Long ecritureCharge, Long ecritureLiquidation, LocalDateTime valideLe, String validePar) {
    }

    public record Registre(int numero, String donnees) {
    }

    public record Liasse(Long id, int version, String statut, boolean nonValideEdi, String sha256, byte[] xml, LocalDate deposeLe, String numeroRecu) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public FiscalComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- IS

    public Optional<CalculIs> calculIs(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT id, categorie_taux, resultat_comptable, reintegrations, deductions, deficits_imputes, resultat_fiscal, base_arrondie, taux_is, is_theorique, base_cm, cm, cm_exoneree, impot_du, acomptes_verses, excedent_anterieur, reliquat, excedent, css,
                       CAST(parametres AS text), statut, ecriture_charge_id, ecriture_liquidation_id, valide_le, valide_par
                  FROM compta_is_calculs WHERE livre_id = :livreId AND exercice_id = :exercice
                """, Map.of("livreId", livreId, "exercice", exerciceId), (rs, i) -> new CalculIs(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9), rs.getBigDecimal(10),
                rs.getBigDecimal(11), rs.getBigDecimal(12), rs.getBoolean(13), rs.getBigDecimal(14), rs.getBigDecimal(15), rs.getBigDecimal(16), rs.getBigDecimal(17), rs.getBigDecimal(18), rs.getBigDecimal(19), rs.getString(20), rs.getString(21), (Long) rs.getObject(22), (Long) rs.getObject(23), rs.getObject(24, LocalDateTime.class), rs.getString(25)))
                .stream().findFirst();
    }

    public void ecrireCalcul(Long livreId, Long exerciceId, CalculIs c) {
        jdbc.update("""
                INSERT INTO compta_is_calculs (livre_id, exercice_id, categorie_taux, resultat_comptable, reintegrations, deductions, deficits_imputes, resultat_fiscal, base_arrondie, taux_is, is_theorique, base_cm, cm, cm_exoneree, impot_du, acomptes_verses, excedent_anterieur, reliquat, excedent, css, parametres)
                VALUES (:livreId, :exercice, :categorie, :comptable, :reint, :deduc, :deficits, :fiscal, :base, :taux, :is, :baseCm, :cm, :exo, :impot, :acomptes, :anterieur, :reliquat, :excedent, :css, CAST(:parametres AS jsonb))
                ON CONFLICT (livre_id, exercice_id) DO UPDATE SET categorie_taux = EXCLUDED.categorie_taux, resultat_comptable = EXCLUDED.resultat_comptable, reintegrations = EXCLUDED.reintegrations, deductions = EXCLUDED.deductions, deficits_imputes = EXCLUDED.deficits_imputes,
                    resultat_fiscal = EXCLUDED.resultat_fiscal, base_arrondie = EXCLUDED.base_arrondie, taux_is = EXCLUDED.taux_is, is_theorique = EXCLUDED.is_theorique, base_cm = EXCLUDED.base_cm, cm = EXCLUDED.cm, cm_exoneree = EXCLUDED.cm_exoneree, impot_du = EXCLUDED.impot_du,
                    acomptes_verses = EXCLUDED.acomptes_verses, excedent_anterieur = EXCLUDED.excedent_anterieur, reliquat = EXCLUDED.reliquat, excedent = EXCLUDED.excedent, css = EXCLUDED.css, parametres = EXCLUDED.parametres
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("categorie", c.categorie()).addValue("comptable", c.resultatComptable()).addValue("reint", c.reintegrations()).addValue("deduc", c.deductions()).addValue("deficits", c.deficits())
                .addValue("fiscal", c.resultatFiscal()).addValue("base", c.baseArrondie()).addValue("taux", c.tauxIs()).addValue("is", c.isTheorique()).addValue("baseCm", c.baseCm()).addValue("cm", c.cm()).addValue("exo", c.cmExoneree()).addValue("impot", c.impotDu()).addValue("acomptes", c.acomptes())
                .addValue("anterieur", c.excedentAnterieur()).addValue("reliquat", c.reliquat()).addValue("excedent", c.excedent()).addValue("css", c.css()).addValue("parametres", c.parametres()));
    }

    public void validerIs(Long livreId, Long exerciceId, Long ecriture, String auteur) {
        jdbc.update("UPDATE compta_is_calculs SET statut = 'VALIDE', ecriture_charge_id = :ecriture, valide_le = CURRENT_TIMESTAMP, valide_par = :auteur WHERE livre_id = :livreId AND exercice_id = :exercice",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("ecriture", ecriture, Types.BIGINT).addValue("auteur", auteur));
    }

    public void rouvrirIs(Long livreId, Long exerciceId) {
        jdbc.update("UPDATE compta_is_calculs SET statut = 'BROUILLON', ecriture_charge_id = NULL, valide_le = NULL, valide_par = NULL WHERE livre_id = :livreId AND exercice_id = :exercice", Map.of("livreId", livreId, "exercice", exerciceId));
    }

    public void lierLiquidation(Long livreId, Long exerciceId, Long ecriture) {
        jdbc.update("UPDATE compta_is_calculs SET ecriture_liquidation_id = :ecriture WHERE livre_id = :livreId AND exercice_id = :exercice", Map.of("livreId", livreId, "exercice", exerciceId, "ecriture", ecriture));
    }

    /** La base de la cotisation minimale : produits d'exploitation et financiers, subventions. Les comptes retenus sont listés dans le service. */
    public BigDecimal produits(Long livreId, Long exerciceId, Collection<String> prefixes) {
        String condition = String.join(" OR ", prefixes.stream().map(p -> "compte_numero LIKE '" + p.replaceAll("[^0-9]", "") + "%'").toList());
        BigDecimal v = jdbc.queryForObject("SELECT COALESCE(SUM(credit - debit), 0) FROM compta_lignes WHERE livre_id = :livreId AND exercice_id = :exercice AND nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND (" + condition + ")",
                Map.of("livreId", livreId, "exercice", exerciceId), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Le solde du compte hors écriture de clôture : la charge d'impôt se relit après la clôture. */
    public BigDecimal soldeHorsCloture(Long livreId, Long exerciceId, String prefixe) {
        BigDecimal v = jdbc.queryForObject("SELECT COALESCE(SUM(debit - credit), 0) FROM compta_lignes WHERE livre_id = :livreId AND exercice_id = :exercice AND nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND compte_numero LIKE :prefixe",
                Map.of("livreId", livreId, "exercice", exerciceId, "prefixe", prefixe + "%"), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    // --- Acomptes

    public List<AcompteDTO> acomptes(Long livreId, Long exerciceId) {
        return jdbc.query("SELECT numero, echeance, montant_du, montant_paye, date_paiement, reference, ecriture_id FROM compta_is_acomptes WHERE livre_id = :livreId AND exercice_id = :exercice ORDER BY numero",
                Map.of("livreId", livreId, "exercice", exerciceId), (rs, i) -> new AcompteDTO(rs.getInt(1), rs.getObject(2, LocalDate.class), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getObject(5, LocalDate.class), rs.getString(6), (Long) rs.getObject(7)));
    }

    public void ecrireAcompte(Long livreId, Long exerciceId, int numero, LocalDate echeance, BigDecimal montant) {
        jdbc.update("""
                INSERT INTO compta_is_acomptes (livre_id, exercice_id, numero, echeance, montant_du) VALUES (:livreId, :exercice, :numero, :echeance, :montant)
                ON CONFLICT (livre_id, exercice_id, numero) DO UPDATE SET echeance = EXCLUDED.echeance, montant_du = EXCLUDED.montant_du WHERE compta_is_acomptes.ecriture_id IS NULL
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("numero", numero).addValue("echeance", Date.valueOf(echeance)).addValue("montant", montant));
    }

    public void payerAcompte(Long livreId, Long exerciceId, int numero, BigDecimal montant, LocalDate date, String reference, Long ecriture) {
        jdbc.update("UPDATE compta_is_acomptes SET montant_paye = :montant, date_paiement = :date, reference = :reference, ecriture_id = :ecriture WHERE livre_id = :livreId AND exercice_id = :exercice AND numero = :numero",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("numero", numero).addValue("montant", montant).addValue("date", Date.valueOf(date)).addValue("reference", reference, Types.VARCHAR).addValue("ecriture", ecriture));
    }

    // --- Déficits

    public List<DeficitDTO> deficits(Long livreId) {
        return jdbc.query("SELECT id, annee_origine, nature, montant_initial, montant_restant, saisi FROM compta_deficits WHERE livre_id = :livreId ORDER BY annee_origine, nature", Map.of("livreId", livreId),
                (rs, i) -> new DeficitDTO(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBoolean(6)));
    }

    public void ecrireDeficit(Long livreId, Long exerciceOrigine, int annee, String nature, BigDecimal montant, boolean saisi) {
        jdbc.update("""
                INSERT INTO compta_deficits (livre_id, exercice_origine_id, annee_origine, nature, montant_initial, montant_restant, saisi) VALUES (:livreId, :exercice, :annee, :nature, :montant, :montant, :saisi)
                ON CONFLICT (livre_id, annee_origine, nature) DO UPDATE SET montant_initial = EXCLUDED.montant_initial, montant_restant = EXCLUDED.montant_restant, saisi = EXCLUDED.saisi
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceOrigine, Types.BIGINT).addValue("annee", annee).addValue("nature", nature).addValue("montant", montant).addValue("saisi", saisi));
    }

    public void modifierRestant(Long livreId, Long id, BigDecimal restant) {
        jdbc.update("UPDATE compta_deficits SET montant_restant = :restant WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id, "restant", restant));
    }

    public void retirerDeficit(Long livreId, int annee, String nature) {
        jdbc.update("DELETE FROM compta_deficits WHERE livre_id = :livreId AND annee_origine = :annee AND nature = :nature", Map.of("livreId", livreId, "annee", annee, "nature", nature));
    }

    // --- Saisies et registres de la liasse

    public List<CelluleSaisieDTO> saisies(Long livreId, Long exerciceId) {
        return jdbc.query("SELECT code_ligne, colonne, valeur_num, valeur_txt FROM compta_liasse_saisies WHERE livre_id = :livreId AND exercice_id = :exercice ORDER BY code_ligne, colonne",
                Map.of("livreId", livreId, "exercice", exerciceId), (rs, i) -> new CelluleSaisieDTO(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)));
    }

    public List<CelluleSaisieDTO> saisiesDuTableau(Long livreId, Long exerciceId, String tableau) {
        return jdbc.query("SELECT code_ligne, colonne, valeur_num, valeur_txt FROM compta_liasse_saisies WHERE livre_id = :livreId AND exercice_id = :exercice AND tableau = :tableau ORDER BY code_ligne, colonne",
                Map.of("livreId", livreId, "exercice", exerciceId, "tableau", tableau), (rs, i) -> new CelluleSaisieDTO(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)));
    }

    public void ecrireSaisie(Long livreId, Long exerciceId, String tableau, CelluleSaisieDTO c, String auteur) {
        jdbc.update("""
                INSERT INTO compta_liasse_saisies (livre_id, exercice_id, tableau, code_ligne, colonne, valeur_num, valeur_txt, modifie_par) VALUES (:livreId, :exercice, :tableau, :ligne, :colonne, :num, :txt, :auteur)
                ON CONFLICT (livre_id, exercice_id, tableau, code_ligne, colonne) DO UPDATE SET valeur_num = EXCLUDED.valeur_num, valeur_txt = EXCLUDED.valeur_txt, modifie_par = EXCLUDED.modifie_par, modifie_le = CURRENT_TIMESTAMP
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("tableau", tableau).addValue("ligne", c.codeLigne()).addValue("colonne", c.colonne()).addValue("num", c.valeurNum(), Types.NUMERIC).addValue("txt", c.valeurTxt(), Types.VARCHAR).addValue("auteur", auteur));
    }

    public void retirerSaisie(Long livreId, Long exerciceId, String tableau, String ligne, String colonne) {
        jdbc.update("DELETE FROM compta_liasse_saisies WHERE livre_id = :livreId AND exercice_id = :exercice AND tableau = :tableau AND code_ligne = :ligne AND colonne = :colonne",
                Map.of("livreId", livreId, "exercice", exerciceId, "tableau", tableau, "ligne", ligne, "colonne", colonne));
    }

    public void retirerSaisiesDeLigne(Long livreId, Long exerciceId, String tableau, String prefixeLigne) {
        jdbc.update("DELETE FROM compta_liasse_saisies WHERE livre_id = :livreId AND exercice_id = :exercice AND tableau = :tableau AND code_ligne LIKE :prefixe",
                Map.of("livreId", livreId, "exercice", exerciceId, "tableau", tableau, "prefixe", prefixeLigne));
    }

    public List<Registre> registre(Long livreId, Long exerciceId, String registre) {
        return jdbc.query("SELECT numero, CAST(donnees AS text) FROM compta_liasse_registres WHERE livre_id = :livreId AND exercice_id = :exercice AND registre = :registre ORDER BY numero",
                Map.of("livreId", livreId, "exercice", exerciceId, "registre", registre), (rs, i) -> new Registre(rs.getInt(1), rs.getString(2)));
    }

    public Map<String, Long> nombresRegistres(Long livreId, Long exerciceId) {
        Map<String, Long> n = new java.util.HashMap<>();
        jdbc.query("SELECT registre, COUNT(*) FROM compta_liasse_registres WHERE livre_id = :livreId AND exercice_id = :exercice GROUP BY registre", Map.of("livreId", livreId, "exercice", exerciceId), rs -> {
            n.put(rs.getString(1), rs.getLong(2));
        });
        return n;
    }

    public void remplacerRegistre(Long livreId, Long exerciceId, String registre, List<Registre> lignes, String auteur) {
        jdbc.update("DELETE FROM compta_liasse_registres WHERE livre_id = :livreId AND exercice_id = :exercice AND registre = :registre", Map.of("livreId", livreId, "exercice", exerciceId, "registre", registre));
        for (Registre l : lignes) {
            jdbc.update("INSERT INTO compta_liasse_registres (livre_id, exercice_id, registre, numero, donnees, modifie_par) VALUES (:livreId, :exercice, :registre, :numero, CAST(:donnees AS jsonb), :auteur)",
                    new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("registre", registre).addValue("numero", l.numero()).addValue("donnees", l.donnees()).addValue("auteur", auteur));
        }
    }

    // --- La liasse

    public Optional<Liasse> liasse(Long livreId, Long exerciceId) {
        return jdbc.query("SELECT id, version, statut, non_valide_edi, xml_sha256, xml_contenu, depose_le, numero_recu FROM compta_liasses WHERE livre_id = :livreId AND exercice_id = :exercice ORDER BY version DESC LIMIT 1",
                Map.of("livreId", livreId, "exercice", exerciceId), (rs, i) -> new Liasse(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getBoolean(4), rs.getString(5), rs.getBytes(6), rs.getObject(7, LocalDate.class), rs.getString(8))).stream().findFirst();
    }

    public void ecrireXml(Long livreId, Long exerciceId, int figeage, String modele, String sha256, byte[] xml) {
        int n = jdbc.update("UPDATE compta_liasses SET xml_sha256 = :sha, xml_contenu = :xml, figeage = :figeage WHERE livre_id = :livreId AND exercice_id = :exercice AND version = (SELECT MAX(version) FROM compta_liasses WHERE livre_id = :livreId AND exercice_id = :exercice) AND statut = 'BROUILLON'",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("sha", sha256).addValue("xml", xml, Types.BINARY).addValue("figeage", figeage));
        if (n == 0) {
            jdbc.update("INSERT INTO compta_liasses (livre_id, exercice_id, modele_edi, figeage, xml_sha256, xml_contenu) VALUES (:livreId, :exercice, :modele, :figeage, :sha, :xml)",
                    new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercice", exerciceId).addValue("modele", modele).addValue("figeage", figeage).addValue("sha", sha256).addValue("xml", xml, Types.BINARY));
        }
    }

    public void deposerLiasse(Long livreId, Long id, LocalDate le, String recu, String auteur) {
        jdbc.update("UPDATE compta_liasses SET statut = 'DEPOSEE', depose_le = :le, numero_recu = :recu, valide_le = CURRENT_TIMESTAMP, valide_par = :auteur WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("le", Date.valueOf(le)).addValue("recu", recu, Types.VARCHAR).addValue("auteur", auteur));
    }

    // --- Les registres de l'application, pour T10 et T16

    public List<Map<String, Object>> sorties(Long livreId, Long exerciceId) {
        return jdbc.queryForList("""
                SELECT s.date_sortie, i.compte_immo, i.designation, i.valeur_brute, s.amortissements_cumules, s.valeur_nette, s.prix_cession_ht, s.plus_ou_moins_value
                  FROM compta_immobilisation_sorties s JOIN compta_immobilisations i ON i.livre_id = s.livre_id AND i.id = s.immobilisation_id JOIN compta_exercices e ON e.livre_id = s.livre_id AND e.id = :exercice
                 WHERE s.livre_id = :livreId AND NOT s.annulee AND s.date_sortie BETWEEN e.date_debut AND e.date_fin ORDER BY s.date_sortie, s.id
                """, Map.of("livreId", livreId, "exercice", exerciceId));
    }

    public List<Map<String, Object>> dotationsParFiche(Long livreId, Long exerciceId) {
        return jdbc.queryForList("""
                SELECT i.designation, i.date_acquisition, i.valeur_brute, i.cumul_reprise, i.duree_mois, i.mode_amortissement,
                       COALESCE((SELECT SUM(a.montant) FROM compta_amortissements a JOIN compta_exercices x ON x.livre_id = a.livre_id AND x.id = a.exercice_id JOIN compta_exercices e ON e.livre_id = a.livre_id AND e.id = :exercice
                                  WHERE a.livre_id = i.livre_id AND a.immobilisation_id = i.id AND NOT a.contrepassee AND x.date_fin < e.date_debut), 0) AS anterieures,
                       COALESCE((SELECT SUM(a.montant) FROM compta_amortissements a WHERE a.livre_id = i.livre_id AND a.immobilisation_id = i.id AND NOT a.contrepassee AND a.exercice_id = :exercice), 0) AS exercice
                  FROM compta_immobilisations i WHERE i.livre_id = :livreId AND i.mode_amortissement <> 'NON_AMORTISSABLE' ORDER BY i.code
                """, Map.of("livreId", livreId, "exercice", exerciceId));
    }
}
