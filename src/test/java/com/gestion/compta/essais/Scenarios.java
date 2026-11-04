package com.gestion.compta.essais;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Prototype de la clôture et de la mesure canonique, joué sur les scénarios de l'audit (S1, S2, S3, S5, S6).
 * Écrit les mesures en JSON pour évaluation par etats_cgnc.json (eval_etats.py).
 */
public class Scenarios {
    static final BigDecimal Z = BigDecimal.ZERO;
    static BigDecimal m(String v) { return new BigDecimal(v); }

    /** La mesure canonique : une seule requête, par exercice, sens jugé par tiers. */
    static final String MESURES = """
        WITH par_tiers AS (
          SELECT l.compte_numero, l.tiers_id,
                 COALESCE(SUM(l.debit - l.credit) FILTER (WHERE l.nature_ecriture = 'A_NOUVEAU'), 0) AS ouverture,
                 COALESCE(SUM(CASE WHEN l.est_contrepassation THEN -l.credit ELSE l.debit END)
                          FILTER (WHERE l.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND l.date_ecriture <= ?), 0) AS md,
                 COALESCE(SUM(CASE WHEN l.est_contrepassation THEN -l.debit ELSE l.credit END)
                          FILTER (WHERE l.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND l.date_ecriture <= ?), 0) AS mc
            FROM compta_lignes l
           WHERE l.livre_id = ? AND l.exercice_id = ? AND l.nature_ecriture <> 'CLOTURE'
           GROUP BY l.compte_numero, l.tiers_id
        )
        SELECT p.compte_numero,
               SUM(GREATEST(p.ouverture, 0))                  AS od,
               SUM(GREATEST(-p.ouverture, 0))                 AS oc,
               SUM(p.md)                                      AS md,
               SUM(p.mc)                                      AS mc,
               SUM(GREATEST(p.ouverture + p.md - p.mc, 0))    AS sd,
               SUM(GREATEST(-(p.ouverture + p.md - p.mc), 0)) AS sc
          FROM par_tiers p
         GROUP BY p.compte_numero
         ORDER BY p.compte_numero
        """;

    static long l1(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.l1(c, q, p); }
    static int ex(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.ex(c, q, p); }

    static class Ctx {
        long soc; Map<String, Long> ex = new LinkedHashMap<>();
        Connection c;
        String cpt(String n) { return n; }
        long per(String code, int n) throws SQLException { return l1(c, "select id from compta_periodes where exercice_id=? and numero=?", ex.get(code), n); }
        long periodeDe(String code, LocalDate d) throws SQLException {
            return l1(c, "select id from compta_periodes where exercice_id=? and type_periode='COURANTE' and ? between date_debut and date_fin", ex.get(code), d);
        }
        long ecr(String code, String j, String date, Object[][] lignes) throws SQLException {
            LocalDate d = LocalDate.parse(date);
            return EssaisInvariants.ecrire(c, soc, ex.get(code), periodeDe(code, d), j, d, "COURANTE", null, lignes);
        }
    }

    /** Ouvre les deux premières périodes et avance la fenêtre : clôture k, ouvre k+2 (ou la période 13). */
    static void avancerPeriodes(Ctx x, String code, int jusqua) throws SQLException {
        long e = x.ex.get(code);
        for (int k = 1; k <= jusqua; k++) {
            ouvrir(x, e, k);
            if (k < 12) ouvrir(x, e, k + 1);
            ex(x.c, "update compta_periodes set statut='CLOTUREE', cloturee_le=now(), cloturee_par='t' where exercice_id=? and numero=?", e, k);
            if (k == 12) ouvrir(x, e, 13);
        }
    }
    static void ouvrir(Ctx x, long e, int k) throws SQLException {
        if (k > 12) { ex(x.c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=99 and statut='A_VENIR'", e); return; }
        ex(x.c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=? and statut='A_VENIR'", e, k);
    }

    /** Clôture définitive, prototype de ClotureService. */
    static void cloturer(Ctx x, String code, String suivant, LocalDate dSuiv) throws Exception {
        Connection c = x.c;
        long e = x.ex.get(code);
        LocalDate fin, deb;
        try (PreparedStatement s = c.prepareStatement("select date_debut, date_fin from compta_exercices where id=?")) {
            s.setLong(1, e); ResultSet r = s.executeQuery(); r.next(); deb = r.getObject(1, LocalDate.class); fin = r.getObject(2, LocalDate.class);
        }
        ex(c, "update compta_exercices set statut='ARRETE', arrete_le=now(), arrete_par='t' where id=?", e);
        // 1. écriture de clôture : un seul exercice, un seul en-tête
        Map<String, BigDecimal> dGroupe = new TreeMap<>(), cGroupe = new TreeMap<>();
        List<Object[]> lignes = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("""
            select co.numero, co.numero, sum(l.debit - l.credit) solde from compta_lignes l join compta_comptes co on co.livre_id = l.livre_id AND co.numero = l.compte_numero
             where l.livre_id=? and l.exercice_id=? and co.classe in (6,7) and l.nature_ecriture <> 'CLOTURE'
             group by co.numero having sum(l.debit - l.credit) <> 0 order by co.numero""")) {
            s.setLong(1, x.soc); s.setLong(2, e);
            ResultSet r = s.executeQuery();
            while (r.next()) {
                String id = r.getString(1); String n = r.getString(2); BigDecimal sol = r.getBigDecimal(3);
                String g = n.startsWith("61") || n.startsWith("71") ? "8100" : n.startsWith("63") || n.startsWith("73") ? "8300" : n.startsWith("65") || n.startsWith("75") ? "8500" : "8800";
                if (sol.signum() > 0) { lignes.add(new Object[]{id, null, Z, sol}); dGroupe.merge(g, sol, BigDecimal::add); }
                else { lignes.add(new Object[]{id, null, sol.negate(), Z}); cGroupe.merge(g, sol.negate(), BigDecimal::add); }
            }
        }
        for (String g : List.of("8100", "8300", "8500", "8800")) {
            if (dGroupe.containsKey(g)) lignes.add(new Object[]{x.cpt(g), null, dGroupe.get(g), Z});
            if (cGroupe.containsKey(g)) lignes.add(new Object[]{x.cpt(g), null, Z, cGroupe.get(g)});
        }
        BigDecimal r81 = net(dGroupe, cGroupe, "8100"), r83 = net(dGroupe, cGroupe, "8300"), r85 = net(dGroupe, cGroupe, "8500"), r88 = net(dGroupe, cGroupe, "8800");
        solder(x, lignes, "8100", "8400", r81);
        solder(x, lignes, "8300", "8400", r83);
        BigDecimal r84 = r81.add(r83);
        solder(x, lignes, "8400", "8600", r84);
        solder(x, lignes, "8500", "8600", r85);
        BigDecimal r86 = r84.add(r85);
        solder(x, lignes, "8600", "8800", r86);
        BigDecimal r = r86.add(r88); // débit net = perte
        if (r.signum() < 0) { lignes.add(new Object[]{x.cpt("8800"), null, r.negate(), Z}); lignes.add(new Object[]{x.cpt("1191"), null, Z, r.negate()}); }
        if (r.signum() > 0) { lignes.add(new Object[]{x.cpt("1199"), null, r, Z}); lignes.add(new Object[]{x.cpt("8800"), null, Z, r}); }
        long p13 = x.per(code, 99);
        if (!lignes.isEmpty()) EssaisInvariants.ecrire(c, x.soc, e, p13, "CL", fin, "CLOTURE", null, lignes.toArray(new Object[0][]));
        // 2. à-nouveaux dans l'exercice suivant : lignes ouvertes pour les comptes lettrables, solde pour les autres
        long eSuiv = x.ex.containsKey(suivant) ? x.ex.get(suivant) : EssaisInvariants.creerExercice(c, x.soc, suivant, dSuiv, dSuiv.plusYears(1).minusDays(1), e);
        x.ex.put(suivant, eSuiv);
        List<Object[]> an = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("""
            select l.compte_numero, l.tiers_id, sum(l.debit - l.credit) solde from compta_lignes l join compta_comptes co on co.livre_id = l.livre_id AND co.numero = l.compte_numero
             where l.livre_id=? and l.exercice_id=? and co.classe <= 5 and not co.lettrable
             group by l.compte_numero, l.tiers_id having sum(l.debit - l.credit) <> 0 order by l.compte_numero""")) {
            s.setLong(1, x.soc); s.setLong(2, e);
            ResultSet r2 = s.executeQuery();
            while (r2.next()) {
                BigDecimal sol = r2.getBigDecimal(3);
                an.add(new Object[]{r2.getString(1), r2.getObject(2), sol.signum() > 0 ? sol : Z, sol.signum() > 0 ? Z : sol.negate()});
            }
        }
        try (PreparedStatement s = c.prepareStatement("""
            select l.compte_numero, l.tiers_id, l.debit, l.credit, l.id from compta_lignes l join compta_comptes co on co.livre_id = l.livre_id AND co.numero = l.compte_numero
             where l.livre_id=? and l.exercice_id=? and co.lettrable
               and not exists (select 1 from compta_lettrage_lignes x join compta_lettrages g on g.id=x.lettrage_id
                                where x.ligne_id=l.id and g.statut='TOTAL'
                                  and not exists (select 1 from compta_lettrage_lignes y join compta_lignes m on m.id=y.ligne_id
                                                   where y.lettrage_id=g.id and m.exercice_id<>l.exercice_id))
             order by l.date_ecriture, l.id""")) {
            s.setLong(1, x.soc); s.setLong(2, e);
            ResultSet r2 = s.executeQuery();
            while (r2.next()) an.add(new Object[]{r2.getString(1), r2.getObject(2), r2.getBigDecimal(3), r2.getBigDecimal(4), r2.getLong(5)});
        }
        if (!an.isEmpty()) EssaisInvariants.ecrire(c, x.soc, eSuiv, x.per(suivant, 0), "AN", dSuiv, "A_NOUVEAU", null, an.toArray(new Object[0][]));
        // Lettrages à cheval sur deux exercices : la ligne de N cède sa place à sa ligne d'à-nouveau.
        ex(c, """
            with moves as (
              select x.ligne_id ancien, a.id nouveau from compta_lettrage_lignes x
                join compta_lignes o on o.id = x.ligne_id and o.exercice_id = ?
                join compta_lignes a on a.ligne_origine_id = o.id and a.exercice_id = ?
            ), suppr as (delete from compta_lettrage_lignes x using moves m where x.ligne_id = m.ancien
                         returning x.lettrage_id, x.livre_id, x.compte_numero, x.tiers_id, m.nouveau)
            insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id)
            select nouveau, lettrage_id, livre_id, compte_numero, tiers_id from suppr""", e, eSuiv);
        ex(c, "update compta_periodes set statut='CLOTUREE', cloturee_le=now(), cloturee_par='t' where exercice_id=? and numero=0", eSuiv);
        ex(c, "update compta_periodes set statut='CLOTUREE', cloturee_le=now(), cloturee_par='t' where id=?", p13);
        ex(c, "update compta_exercices set statut='CLOTURE', cloture_le=now(), cloture_par='t' where id=?", e);
        // 3. contrôle après coup : ouverture N+1 = clôture N, compte par compte et tiers par tiers
        long ecarts = l1(c, """
            select count(*) from (
              select compte_numero, tiers_id, sum(debit - credit) s from compta_lignes l join compta_comptes co on co.livre_id = l.livre_id AND co.numero = l.compte_numero
               where l.livre_id=? and l.exercice_id=? and co.classe <= 5 group by 1,2 having sum(debit-credit)<>0
              except
              select compte_numero, tiers_id, sum(debit - credit) s from compta_lignes
               where livre_id=? and exercice_id=? and nature_ecriture='A_NOUVEAU' group by 1,2) x""", x.soc, e, x.soc, eSuiv);
        if (ecarts != 0) throw new SQLException("AN_EGAL_CLOTURE_N1 : " + ecarts + " écarts");
    }
    static BigDecimal net(Map<String, BigDecimal> d, Map<String, BigDecimal> c, String g) {
        return d.getOrDefault(g, Z).subtract(c.getOrDefault(g, Z));
    }
    /** Solde le compte de résultat g (débit net si n>0) dans le suivant. */
    static void solder(Ctx x, List<Object[]> lignes, String g, String suivant, BigDecimal n) throws SQLException {
        if (n.signum() > 0) { lignes.add(new Object[]{x.cpt(g), null, Z, n}); lignes.add(new Object[]{x.cpt(suivant), null, n, Z}); }
        if (n.signum() < 0) { lignes.add(new Object[]{x.cpt(g), null, n.negate(), Z}); lignes.add(new Object[]{x.cpt(suivant), null, Z, n.negate()}); }
    }

    static String jsonMesures(Ctx x, String code, LocalDate arrete) throws SQLException {
        StringBuilder b = new StringBuilder("{");
        try (PreparedStatement s = x.c.prepareStatement(MESURES)) {
            s.setObject(1, arrete); s.setObject(2, arrete); s.setLong(3, x.soc); s.setLong(4, x.ex.get(code));
            ResultSet r = s.executeQuery(); boolean first = true;
            while (r.next()) {
                if (!first) b.append(','); first = false;
                b.append('"').append(r.getString(1)).append("\":{\"od\":").append(r.getBigDecimal(2)).append(",\"oc\":").append(r.getBigDecimal(3))
                 .append(",\"md\":").append(r.getBigDecimal(4)).append(",\"mc\":").append(r.getBigDecimal(5))
                 .append(",\"sd\":").append(r.getBigDecimal(6)).append(",\"sc\":").append(r.getBigDecimal(7)).append('}');
            }
        }
        return b.append('}').toString();
    }

    public static void main(String[] a) throws Exception {
        EssaisInvariants.url = a[0];
        Path sortie = Paths.get(a[1]);
        Files.createDirectories(sortie);
        // S1 : chaîne de clôture sur trois exercices
        try (Connection c = EssaisInvariants.cx()) {
            Ctx x = new Ctx(); x.c = c;
            x.soc = EssaisInvariants.activer(c, "S1");
            x.ex.put("2025", EssaisInvariants.creerExercice(c, x.soc, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null));
            ouvrir(x, x.ex.get("2025"), 1); ouvrir(x, x.ex.get("2025"), 2);
            String c5141 = x.cpt("5141"), c1111 = x.cpt("1111"), c7111 = x.cpt("7111"), c6111 = x.cpt("6111"), c4455 = x.cpt("4455"), c3421 = x.cpt("3421"), c7119 = x.cpt("7119");
            x.ecr("2025", "OD", "2025-01-10", new Object[][]{{c5141, null, m("100000.00"), Z},{c1111, null, Z, m("100000.00")}});
            x.ecr("2025", "VT", "2025-02-10", new Object[][]{{c5141, null, m("50000.00"), Z},{c7111, null, Z, m("50000.00")}});
            x.ecr("2025", "OD", "2025-02-20", new Object[][]{{c6111, null, m("20000.00"), Z},{c5141, null, Z, m("20000.00")}});
            avancerPeriodes(x, "2025", 12);
            Files.writeString(sortie.resolve("S1_2025_avant.json"), jsonMesures(x, "2025", LocalDate.of(2025,12,31)));
            cloturer(x, "2025", "2026", LocalDate.of(2026,1,1));
            c.commit();
            Files.writeString(sortie.resolve("S1_2025_apres.json"), jsonMesures(x, "2025", LocalDate.of(2025,12,31)));
            Files.writeString(sortie.resolve("S1_2026_vide.json"), jsonMesures(x, "2026", LocalDate.of(2026,12,31)));
            ouvrir(x, x.ex.get("2026"), 1); ouvrir(x, x.ex.get("2026"), 2);
            x.ecr("2026", "VT", "2026-02-10", new Object[][]{{c5141, null, m("10000.00"), Z},{c7111, null, Z, m("10000.00")}});
            avancerPeriodes(x, "2026", 12);
            cloturer(x, "2026", "2027", LocalDate.of(2027,1,1));
            c.commit();
            Files.writeString(sortie.resolve("S1_2026.json"), jsonMesures(x, "2026", LocalDate.of(2026,12,31)));
            Files.writeString(sortie.resolve("S1_2027.json"), jsonMesures(x, "2027", LocalDate.of(2027,12,31)));
            // Une relance de clôture est refusée
            try { ex(c, "update compta_exercices set statut='ARRETE' where id=?", x.ex.get("2025")); System.out.println("KO reprise clôture"); }
            catch (SQLException e) { System.out.println("OK clôture rejouée refusée : " + e.getMessage().split("\n")[0]); c.rollback(); }
            try { EssaisInvariants.ecrire(c, x.soc, x.ex.get("2027"), x.per("2027", 0), "AN", LocalDate.of(2027,1,1), "A_NOUVEAU", null, new Object[][]{{c5141, null, m("1.00"), Z},{c1111, null, Z, m("1.00")}}); c.commit(); System.out.println("KO deuxième AN"); }
            catch (SQLException e) { System.out.println("OK deuxième à-nouveau refusé : " + e.getMessage().split("\n")[0]); c.rollback(); }
            // Clôture de 2028 avant 2027 : chronologie
            long e28 = EssaisInvariants.creerExercice(c, x.soc, "2028", LocalDate.of(2028,1,1), LocalDate.of(2028,12,31), x.ex.get("2027"));
            c.commit();
            try { ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero between 1 and 12", e28); c.rollback(); } catch (SQLException e) { c.rollback(); }
            try { ex(c, "update compta_exercices set statut='ARRETE' where id=?", e28); System.out.println("KO chronologie"); c.rollback(); }
            catch (SQLException e) { System.out.println("OK arrêté non chronologique refusé : " + e.getMessage().split("\n")[0]); c.rollback(); }
            System.out.println("S1 : écritures de clôture " + l1(c, "select count(*) from compta_ecritures where livre_id=? and nature='CLOTURE'", x.soc) + ", à-nouveaux " + l1(c, "select count(*) from compta_ecritures where livre_id=? and nature='A_NOUVEAU'", x.soc));
        }
        // Lettrage à cheval : facture de décembre N lettrée avec un règlement de janvier N+1 avant la clôture de N
        try (Connection c = EssaisInvariants.cx()) {
            Ctx x = new Ctx(); x.c = c;
            x.soc = EssaisInvariants.activer(c, "LET");
            x.ex.put("2025", EssaisInvariants.creerExercice(c, x.soc, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null));
            x.ex.put("2026", EssaisInvariants.creerExercice(c, x.soc, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), x.ex.get("2025")));
            long e25 = x.ex.get("2025"), e26 = x.ex.get("2026");
            String c5141 = x.cpt("5141"), c7111 = x.cpt("7111"), c3421 = x.cpt("3421");
            long tA = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',21, null, 'A') returning id", x.soc);
            for (int k = 1; k <= 11; k++) { ouvrir(x, e25, k); if (k < 11) ouvrir(x, e25, k + 1); ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero=?", e25, k); }
            ouvrir(x, e25, 12); ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=1", e26);
            long f1 = x.ecr("2025", "VT", "2025-12-10", new Object[][]{{c3421, tA, m("1000.00"), Z},{c7111, null, Z, m("1000.00")}});
            long f2 = x.ecr("2025", "VT", "2025-12-11", new Object[][]{{c3421, tA, m("300.00"), Z},{c7111, null, Z, m("300.00")}});
            long r1 = x.ecr("2026", "BQ", "2026-01-10", new Object[][]{{c5141, null, m("1000.00"), Z},{c3421, tA, Z, m("1000.00")}});
            long lf1 = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", f1, c3421), lr1 = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", r1, c3421);
            long g = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','TOTAL','MANUEL','2026-01-10','t') returning id", x.soc, c3421, tA);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", lf1, g, x.soc, c3421, tA);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", lr1, g, x.soc, c3421, tA);
            c.commit();
            ex(c, "update compta_periodes set statut='CLOTUREE' where exercice_id=? and numero=12", e25);
            ouvrir(x, e25, 13);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=2", e26);
            cloturer(x, "2025", "2026", LocalDate.of(2026,1,1));
            c.commit();
            long ouverts = l1(c, """
                select count(*) from compta_lignes l where l.livre_id=? and l.exercice_id=? and l.compte_numero=?
                   and not exists (select 1 from compta_lettrage_lignes x where x.ligne_id=l.id)""", x.soc, e26, c3421);
            String solde = EssaisInvariants.s1(c, "select sum(debit-credit) from compta_lignes where livre_id=? and exercice_id=? and tiers_id=?", x.soc, e26, tA);
            String od = EssaisInvariants.s1(c, "select sum(debit-credit) from compta_lignes where livre_id=? and exercice_id=? and tiers_id=? and nature_ecriture='A_NOUVEAU'", x.soc, e26, tA);
            long dansGroupe = l1(c, "select count(*) from compta_lettrage_lignes x join compta_lignes l on l.id=x.ligne_id where x.lettrage_id=? and l.exercice_id=?", g, e26);
            System.out.println("Lettrage à cheval : ouverture 2026 du tiers " + od + ", solde " + solde + ", lignes ouvertes " + ouverts + ", lignes du groupe sur 2026 : " + dansGroupe);
            if (!od.equals("1300.00") || !solde.equals("300.00") || ouverts != 1 || dansGroupe != 2) throw new SQLException("lettrage à cheval faux");
            System.out.println("OK lettrage à cheval reporté sur la ligne d'à-nouveau, groupe équilibré au commit");
            // un groupe entièrement sur 2025, gelé après la clôture : il n'y en a pas ici, le groupe a migré ; la ligne de 2025 n'est plus lettrée
            long gelee = l1(c, "select count(*) from compta_lettrage_lignes where ligne_id=?", lf1);
            if (gelee != 0) throw new SQLException("ligne de N encore lettrée");
        }
        // S2 : découvert, S6 : remise globale, S5 : chèque, S3-bis : tiers créditeur
        try (Connection c = EssaisInvariants.cx()) {
            Ctx x = new Ctx(); x.c = c;
            x.soc = EssaisInvariants.activer(c, "S2");
            x.ex.put("2026", EssaisInvariants.creerExercice(c, x.soc, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null));
            ouvrir(x, x.ex.get("2026"), 1); ouvrir(x, x.ex.get("2026"), 2);
            String c5141 = x.cpt("5141"), c1111 = x.cpt("1111"), c7111 = x.cpt("7111"), c6111 = x.cpt("6111"), c4455 = x.cpt("4455"), c3421 = x.cpt("3421"), c7119 = x.cpt("7119"), c51111 = x.cpt("51111"), c51112 = x.cpt("51112");
            long tA = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',11, null, 'A') returning id", x.soc);
            long tB = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',12, null, 'B') returning id", x.soc);
            // S2 : capital 10 000, achats 30 000 : banque créditrice de 20 000
            x.ecr("2026", "OD", "2026-01-05", new Object[][]{{c5141, null, m("10000.00"), Z},{c1111, null, Z, m("10000.00")}});
            x.ecr("2026", "OD", "2026-01-06", new Object[][]{{c6111, null, m("30000.00"), Z},{c5141, null, Z, m("30000.00")}});
            // S6 : facture 1000 HT + 200 TVA, remise globale 200, paiement 1000
            x.ecr("2026", "VT", "2026-01-10", new Object[][]{{c3421, tA, m("1000.00"), Z},{c7111, null, Z, m("800.00")},{c4455, null, Z, m("200.00")}});
            x.ecr("2026", "BQ", "2026-01-20", new Object[][]{{c5141, null, m("1000.00"), Z},{c3421, tA, Z, m("1000.00")}});
            // S5 : chèque 1000 : portefeuille, remise, encaissement
            x.ecr("2026", "VT", "2026-01-11", new Object[][]{{c3421, tB, m("1000.00"), Z},{c7111, null, Z, m("1000.00")}});
            x.ecr("2026", "BQ", "2026-02-01", new Object[][]{{c51111, null, m("1000.00"), Z},{c3421, tB, Z, m("1000.00")}});
            x.ecr("2026", "BQ", "2026-02-05", new Object[][]{{c51112, null, m("1000.00"), Z},{c51111, null, Z, m("1000.00")}});
            x.ecr("2026", "BQ", "2026-02-10", new Object[][]{{c5141, null, m("1000.00"), Z},{c51112, null, Z, m("1000.00")}});
            // Tiers de sens contraire : A trop payé de 500, B doit 800 sur le même collectif
            x.ecr("2026", "BQ", "2026-02-11", new Object[][]{{c5141, null, m("500.00"), Z},{c3421, tA, Z, m("500.00")}});
            x.ecr("2026", "VT", "2026-02-12", new Object[][]{{c3421, tB, m("800.00"), Z},{c7111, null, Z, m("800.00")}});
            c.commit();
            Files.writeString(sortie.resolve("S2.json"), jsonMesures(x, "2026", LocalDate.of(2026,12,31)));
            // Soldes détaillés
            for (String n : List.of("5141", "3421", "51111", "51112")) {
                try (PreparedStatement s = c.prepareStatement("select coalesce(sum(debit-credit),0) from compta_lignes l join compta_comptes co on co.livre_id = l.livre_id AND co.numero = l.compte_numero where l.livre_id=? and co.numero=?")) {
                    s.setLong(1, x.soc); s.setString(2, n); ResultSet r = s.executeQuery(); r.next();
                    System.out.println("S2 solde " + n + " = " + r.getBigDecimal(1));
                }
            }
        }
    }
}
