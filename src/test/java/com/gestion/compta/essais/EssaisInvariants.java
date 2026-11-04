package com.gestion.compta.essais;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Prototype des tests de schéma proposés : chaque invariant est essayé contre PostgreSQL embarqué.
 * Usage : EssaisInvariants jdbc:postgresql://localhost:54329/t1
 */
public class EssaisInvariants {
    static String url;
    static int ok = 0, ko = 0;

    interface Tx { void run(Connection c) throws Exception; }

    static Connection cx() throws SQLException {
        Connection c = DriverManager.getConnection(url, "postgres", "postgres");
        c.setAutoCommit(false);
        return c;
    }

    /** La transaction doit réussir au commit. */
    static void passe(String nom, Tx t) throws Exception {
        try (Connection c = cx()) {
            t.run(c);
            c.commit();
            System.out.println("OK   " + nom);
            ok++;
        } catch (SQLException e) {
            System.out.println("KO   " + nom + " : attendu succès, obtenu " + e.getMessage().split("\n")[0]);
            ko++;
        }
    }

    /** La transaction doit échouer (exécution ou commit) avec ce fragment de message. */
    static void refuse(String nom, String fragment, Tx t) throws Exception {
        try (Connection c = cx()) {
            t.run(c);
            c.commit();
            System.out.println("KO   " + nom + " : accepté à tort");
            ko++;
        } catch (SQLException e) {
            String m = e.getMessage().split("\n")[0];
            if (e.getMessage().toLowerCase().contains(fragment.toLowerCase())) {
                System.out.println("OK   " + nom + " -> " + m);
                ok++;
            } else {
                System.out.println("KO   " + nom + " : attendu [" + fragment + "], obtenu " + e.getMessage());
                ko++;
            }
        }
    }

    static long l1(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
            try (ResultSet r = s.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }
    static String s1(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
            try (ResultSet r = s.executeQuery()) { r.next(); return r.getString(1); }
        }
    }
    static int ex(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
            return s.executeUpdate();
        }
    }

    // ---- données de base
    static long soc1, soc2, pdv1, pdv2;
    static Map<String, Long> j1 = new HashMap<>(), j2 = new HashMap<>();
    static long ex26, ex27, ex26b;
    static long p(long ex, int n, Connection c) throws SQLException { return l1(c, "select id from compta_periodes where exercice_id=? and numero=?", ex, n); }
    static String cpt(long soc, String num, Connection c) throws SQLException { return num; }
    static long tiersA, tiersB, tiersAutre; static boolean aExtourner = false; static String ref = "P1";

    static long activer(Connection c, String nom) throws SQLException {
        long pdv = l1(c, "insert into point_de_vente(nom_point_de_vente, tenant_id) values (?, 1) returning id", nom + System.nanoTime());
        long soc = l1(c, "insert into compta_livres(date_demarrage, raison_sociale, active_par) values ('2026-01-01', ?, 'test') returning id", nom);
        ex(c, "insert into compta_livre_points_de_vente(point_de_vente_id, livre_id, date_rattachement) values (?, ?, '2026-01-01')", pdv, soc);
        ex(c, "insert into compta_comptes(livre_id, numero, libelle, sens_normal, nature, imputable, exige_tiers, lettrable, origine) " +
                "select ?, numero, libelle, sens_normal, nature, imputable_defaut, " +
                " nature in ('COLLECTIF_CLIENT','CLIENT_DOUTEUX','CLIENT_EFFET_A_RECEVOIR','CLIENT_A_FACTURER','CLIENT_CREDITEUR','COLLECTIF_FOURNISSEUR','FOURNISSEUR_EFFET_A_PAYER','FOURNISSEUR_FNP','FOURNISSEUR_DEBITEUR')," +
                " nature in ('COLLECTIF_CLIENT','CLIENT_DOUTEUX','CLIENT_EFFET_A_RECEVOIR','CLIENT_A_FACTURER','CLIENT_CREDITEUR','COLLECTIF_FOURNISSEUR','FOURNISSEUR_EFFET_A_PAYER','FOURNISSEUR_FNP','FOURNISSEUR_DEBITEUR')," +
                " origine from compta_referentiel_comptes where modele='NORMAL' and length(numero)>=4", soc);
        for (String[] j : new String[][]{{"VT","Ventes","VENTES"},{"AC","Achats","ACHATS"},{"BQ","Banque","BANQUE"},{"CA","Caisse","CAISSE"},{"OD","Opérations diverses","OD"},{"AN","À-nouveaux","A_NOUVEAUX"},{"CL","Clôture","CLOTURE"}}) {
            ex(c, "insert into compta_journaux(livre_id, code, libelle, type_journal) values (?,?,?,?)", soc, j[0], j[1], j[2]);
        }
        return soc;
    }

    static long creerExercice(Connection c, long soc, String code, LocalDate du, LocalDate au, Long prec) throws SQLException {
        long id = l1(c, "insert into compta_exercices(livre_id, code, date_debut, date_fin, precedent_id) values (?,?,?,?,?) returning id", soc, code, du, au, prec);
        ex(c, "insert into compta_periodes(livre_id, exercice_id, numero, type_periode, date_debut, date_fin, statut) values (?,?,0,'OUVERTURE',?,?,'OUVERTE')", soc, id, du, du);
        LocalDate d = du; int n = 1;
        while (!d.isAfter(au)) {
            LocalDate f = d.withDayOfMonth(d.lengthOfMonth()); if (f.isAfter(au)) f = au;
            ex(c, "insert into compta_periodes(livre_id, exercice_id, numero, type_periode, date_debut, date_fin) values (?,?,?,'COURANTE',?,?)", soc, id, n, d, f);
            d = f.plusDays(1); n++;
        }
        ex(c, "insert into compta_periodes(livre_id, exercice_id, numero, type_periode, date_debut, date_fin) values (?,?,99,'CLOTURE',?,?)", soc, id, au, au);
        return id;
    }

    /** Insère une écriture par le seul chemin prévu. lignes : {compte, tiers(null), debit, credit} */
    static long ecrire(Connection c, long soc, long ex, long per, String jcode, LocalDate date, String nature, Long contreDe, Object[][] lignes) throws SQLException {
        return ecrireBrut(c, soc, ex, per, jcode, date, nature, contreDe, lignes, null);
    }
    static long ecrireBrut(Connection c, long soc, long ex, long per, String jcode, LocalDate date, String nature, Long contreDe, Object[][] lignes, Integer nbForce) throws SQLException {
        BigDecimal d = BigDecimal.ZERO, cr = BigDecimal.ZERO;
        for (Object[] l : lignes) { d = d.add((BigDecimal) l[2]); cr = cr.add((BigDecimal) l[3]); }
        long jid; String jtype;
        try (PreparedStatement s = c.prepareStatement("select id, type_journal from compta_journaux where livre_id=? and code=?")) {
            s.setLong(1, soc); s.setString(2, jcode);
            try (ResultSet r = s.executeQuery()) { r.next(); jid = r.getLong(1); jtype = r.getString(2); }
        }
        boolean cp = contreDe != null;
        long id = l1(c, "insert into compta_ecritures(livre_id, exercice_id, periode_id, journal_id, type_journal, numero, date_ecriture, date_piece, ref_piece, libelle, nature, origine, est_contrepassation, contrepassation_de_id, a_extourner, nb_lignes, total_debit, total_credit, saisie_le, saisie_par, valide_le, valide_par) " +
                "values (?,?,?,?,?,0,?,?,'" + ref + "','Libellé',?,'SAISIE',?,?,?,?,?,?,now(),'test',now(),'test') returning id",
                soc, ex, per, jid, jtype, date, date, nature, cp, contreDe, aExtourner, nbForce != null ? nbForce : lignes.length, d, cr);
        cp = "t".equals(s1(c, "select est_contrepassation from compta_ecritures where id=?", id));
        int n = 1;
        try (PreparedStatement s = c.prepareStatement("insert into compta_lignes(livre_id, ecriture_id, numero_ligne, exercice_id, journal_id, date_ecriture, nature_ecriture, est_contrepassation, compte_numero, tiers_id, debit, credit, ref_piece, date_piece, ligne_origine_id) values (?,?,?,?,?,?,?,?,?,?,?,?,'" + ref + "',?,?)")) {
            for (Object[] l : lignes) {
                s.setLong(1, soc); s.setLong(2, id); s.setInt(3, n++); s.setLong(4, ex); s.setLong(5, jid); s.setObject(6, date); s.setString(7, nature); s.setBoolean(8, cp);
                s.setString(9, (String) l[0]); s.setObject(10, l[1]); s.setBigDecimal(11, (BigDecimal) l[2]); s.setBigDecimal(12, (BigDecimal) l[3]); s.setObject(13, date); s.setObject(14, l.length > 4 ? l[4] : null);
                s.executeUpdate();
            }
        }
        return id;
    }
    static BigDecimal m(String v) { return new BigDecimal(v); }
    static final BigDecimal Z = BigDecimal.ZERO;

    public static void main(String[] a) throws Exception {
        url = a[0];
        LocalDate j15 = LocalDate.of(2026, 1, 15);
        try (Connection c = cx()) {
            soc1 = activer(c, "SOC1"); soc2 = activer(c, "SOC2");
            ex26 = creerExercice(c, soc1, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null);
            ex26b = creerExercice(c, soc2, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=1", ex26);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=2", ex26);
            ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=1", ex26b);
            tiersA = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',1, null, 'Client A') returning id", soc1);
            tiersB = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',2, null, 'Client B') returning id", soc1);
            tiersAutre = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'CLIENT','CLIENT',1, null, 'Autre société') returning id", soc2);
            c.commit();
            System.out.println("plan copié : " + l1(c, "select count(*) from compta_comptes where livre_id=?", soc1) + " comptes, imputables " + l1(c, "select count(*) from compta_comptes where livre_id=? and imputable", soc1));
            System.out.println("code auxiliaire : " + s1(c, "select code_auxiliaire from compta_tiers where id=?", tiersA));
        }
        String c3421, c7111, c4455, c5141, c6111, c34552, c3455, c1111, c1191, c8100, c2321;
        try (Connection c = cx()) {
            c3421 = cpt(soc1,"3421",c); c7111 = cpt(soc1,"7111",c); c4455 = cpt(soc1,"4455",c); c5141 = cpt(soc1,"5141",c);
            c6111 = cpt(soc1,"6111",c); c34552 = cpt(soc1,"34552",c); c3455 = cpt(soc1,"3455",c); c1111 = cpt(soc1,"1111",c); c1191 = cpt(soc1,"1191",c);
            c8100 = cpt(soc1,"8100",c); c2321 = cpt(soc1,"2321",c);
        }
        long p0, p1, p2, p3, p13;
        try (Connection c = cx()) { p0 = p(ex26,0,c); p1 = p(ex26,1,c); p2 = p(ex26,2,c); p3 = p(ex26,3,c); p13 = p(ex26,99,c); }
        final long fp0=p0, fp1=p1, fp2=p2, fp3=p3, fp13=p13;
        final String f3421=c3421,f7111=c7111,f4455=c4455,f5141=c5141,f6111=c6111,f34552=c34552,f3455=c3455,f1111=c1111,f1191=c1191,f8100=c8100,f2321=c2321;

        Object[][] vente = {{f3421, tiersA, m("1200.00"), Z},{f7111, null, Z, m("1000.00")},{f4455, null, Z, m("200.00")}};
        final long[] e1 = new long[1];
        passe("écriture équilibrée, numéro attribué par la base", c -> {
            e1[0] = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, vente);
            if (l1(c, "select numero from compta_ecritures where id=?", e1[0]) != 1) throw new SQLException("numéro attendu 1");
        });
        passe("numéro suivant = 2", c -> {
            long id = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, vente);
            if (l1(c, "select numero from compta_ecritures where id=?", id) != 2) throw new SQLException("numéro attendu 2");
        });
        try (Connection c = cx()) {
            ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, vente);
            c.rollback();
        }
        passe("un rollback rend le numéro : pas de trou", c -> {
            long id = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, vente);
            if (l1(c, "select numero from compta_ecritures where id=?", id) != 3) throw new SQLException("numéro attendu 3, obtenu " + l1(c, "select numero from compta_ecritures where id=?", id));
        });
        refuse("en-tête déséquilibré", "ck_compta_ecr_equilibre", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), Z},{f7111, null, Z, m("9.00")}}));
        refuse("écriture à zéro", "ck_compta_lig_sens", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), Z},{f7111, null, Z, m("10.00")},{f6111, null, Z, Z}}));
        refuse("montant négatif", "ck_compta_ecr_equilibre", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f5141, null, m("-10.00"), Z},{f7111, null, Z, m("-10.00")}}));
        refuse("débit et crédit sur la même ligne", "ck_compta_lig_sens", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), m("5.00")},{f7111, null, m("5.00"), m("10.00")}}));
        refuse("ligne annoncée manquante : refus au commit", "COMPTA_ECRITURE_INCOHERENTE", c -> ecrireBrut(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), Z},{f7111, null, Z, m("10.00")}}, 3));
        refuse("une seule ligne", "ck_compta_ecr", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), m("0.00")}}));
        refuse("ligne ajoutée après scellement", "COMPTA_ECRITURE_SCELLEE", c -> {
            long id = e1[0];
            long jid = l1(c, "select journal_id from compta_ecritures where id=?", id);
            ex(c, "insert into compta_lignes(livre_id, ecriture_id, numero_ligne, exercice_id, journal_id, date_ecriture, nature_ecriture, est_contrepassation, compte_numero, debit, credit, ref_piece, date_piece) values (?,?,4,?,?,?,'COURANTE',false,?,5,0,'P','2026-01-15')", soc1, id, ex26, jid, j15, f5141);
        });
        refuse("ligne remplaçant un numéro existant", "uk_compta_lig_numero", c -> {
            long id = e1[0];
            long jid = l1(c, "select journal_id from compta_ecritures where id=?", id);
            ex(c, "insert into compta_lignes(livre_id, ecriture_id, numero_ligne, exercice_id, journal_id, date_ecriture, nature_ecriture, est_contrepassation, compte_numero, debit, credit, ref_piece, date_piece) values (?,?,1,?,?,?,'COURANTE',false,?,5,0,'P','2026-01-15')", soc1, id, ex26, jid, j15, f5141);
        });
        refuse("colonnes dénormalisées incohérentes (date)", "fk_compta_lig_ecriture", c -> {
            long id = ecrireBrut(c, soc1, ex26, fp1, "OD", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), Z},{f7111, null, Z, m("10.00")}}, 3);
            ex(c, "insert into compta_lignes(livre_id, ecriture_id, numero_ligne, exercice_id, journal_id, date_ecriture, nature_ecriture, est_contrepassation, compte_numero, debit, credit, ref_piece, date_piece) values (?,?,3,?,?,?,'COURANTE',false,?,5,0,'P','2026-01-15')", soc1, id, ex26, l1(c,"select journal_id from compta_ecritures where id=?",id), LocalDate.of(2026,1,16), f5141);
        });
        refuse("UPDATE sur une écriture validée", "COMPTA_IMMUABLE", c -> ex(c, "update compta_ecritures set libelle='x' where id=?", e1[0]));
        refuse("DELETE sur une écriture validée", "COMPTA_IMMUABLE", c -> ex(c, "delete from compta_ecritures where id=?", e1[0]));
        refuse("UPDATE sur une ligne validée", "COMPTA_IMMUABLE", c -> ex(c, "update compta_lignes set debit=1 where ecriture_id=?", e1[0]));
        refuse("DELETE sur une ligne validée", "COMPTA_IMMUABLE", c -> ex(c, "delete from compta_lignes where ecriture_id=?", e1[0]));
        // Le refus se lit dans le catalogue : aucun test de la comptabilité ne lance un vidage, même attendu refusé.
        passe("vidage du journal : déclencheurs actifs", c -> {
            long actifs = l1(c, "select count(*) from pg_trigger where tgname in ('trg_compta_ecritures_truncate', 'trg_compta_lignes_truncate') and tgenabled <> 'D'");
            if (actifs != 2) throw new SQLException("déclencheurs actifs : " + actifs);
        });
        refuse("compte d'une autre société", "fk_compta_lig_compte", c -> {
            ex(c, "insert into compta_comptes(livre_id, numero, libelle, sens_normal, nature, imputable, origine) values (?, '514199', 'Sous-compte autre livre', 'DEBIT', 'TRESORERIE_BANQUE', true, 'SOCIETE')", soc2);
            String cAutre = "514199";
            ecrire(c, soc1, ex26, fp1, "OD", j15, "COURANTE", null, new Object[][]{{cAutre, null, m("10.00"), Z},{f7111, null, Z, m("10.00")}});
        });
        refuse("tiers d'une autre société", "fk_compta_lig_tiers", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f3421, tiersAutre, m("10.00"), Z},{f7111, null, Z, m("10.00")}}));
        refuse("collectif sans tiers", "COMPTA_TIERS_OBLIGATOIRE", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f3421, null, m("10.00"), Z},{f7111, null, Z, m("10.00")}}));
        refuse("tiers sur un compte de charge", "ck_compta_lig_tiers", c -> ecrire(c, soc1, ex26, fp1, "OD", j15, "COURANTE", null, new Object[][]{{f6111, tiersA, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        refuse("3455 sans division (art. 112-I-8°)", "COMPTA_COMPTE_NON_IMPUTABLE", c -> ecrire(c, soc1, ex26, fp1, "AC", j15, "COURANTE", null, new Object[][]{{f3455, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        refuse("classe 8 hors clôture", "ck_compta_lig_classe_8", c -> ecrire(c, soc1, ex26, fp1, "OD", j15, "COURANTE", null, new Object[][]{{f8100, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        refuse("période à venir", "COMPTA_PERIODE_FERMEE", c -> ecrire(c, soc1, ex26, fp3, "OD", LocalDate.of(2026,3,15), "COURANTE", null, new Object[][]{{f6111, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        refuse("date hors période", "COMPTA_DATE_HORS_PERIODE", c -> ecrire(c, soc1, ex26, fp1, "OD", LocalDate.of(2026,2,15), "COURANTE", null, new Object[][]{{f6111, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        refuse("à-nouveau hors du journal AN", "ck_compta_ecr_nature_an", c -> ecrire(c, soc1, ex26, fp0, "OD", LocalDate.of(2026,1,1), "A_NOUVEAU", null, new Object[][]{{f5141, null, m("10.00"), Z},{f1111, null, Z, m("10.00")}}));
        refuse("écriture courante dans le journal AN", "ck_compta_ecr_nature_an", c -> ecrire(c, soc1, ex26, fp1, "AN", j15, "COURANTE", null, new Object[][]{{f5141, null, m("10.00"), Z},{f1111, null, Z, m("10.00")}}));
        refuse("à-nouveau sur un compte de charge", "ck_compta_lig_an_classe", c -> ecrire(c, soc1, ex26, fp0, "AN", LocalDate.of(2026,1,1), "A_NOUVEAU", null, new Object[][]{{f6111, null, m("10.00"), Z},{f1111, null, Z, m("10.00")}}));
        passe("à-nouveau de reprise", c -> ecrire(c, soc1, ex26, fp0, "AN", LocalDate.of(2026,1,1), "A_NOUVEAU", null, new Object[][]{{f5141, null, m("100000.00"), Z},{f1111, null, Z, m("100000.00")}}));
        refuse("deuxième à-nouveau sur le même exercice", "uq_compta_ecr_an", c -> ecrire(c, soc1, ex26, fp0, "AN", LocalDate.of(2026,1,1), "A_NOUVEAU", null, new Object[][]{{f5141, null, m("10.00"), Z},{f1111, null, Z, m("10.00")}}));
        refuse("clôture sur un exercice non arrêté", "COMPTA_NATURE_PERIODE", c -> {
            ecrire(c, soc1, ex26, fp1, "CL", j15, "CLOTURE", null, new Object[][]{{f8100, null, m("10.00"), Z},{f7111, null, Z, m("10.00")}});
        });
        // contre-passation
        final long[] e2 = new long[1];
        passe("écriture à contre-passer", c -> e2[0] = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, vente));
        refuse("contre-passation non miroir", "COMPTA_CONTREPASSATION_NON_MIROIR", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", e2[0], new Object[][]{{f3421, tiersB, Z, m("1200.00")},{f7111, null, m("1000.00"), Z},{f4455, null, m("200.00"), Z}}));
        refuse("contre-passation dans un autre journal", "même journal", c -> ecrire(c, soc1, ex26, fp1, "OD", j15, "COURANTE", e2[0], new Object[][]{{f3421, tiersA, Z, m("1200.00")},{f7111, null, m("1000.00"), Z},{f4455, null, m("200.00"), Z}}));
        final long[] e3 = new long[1];
        passe("contre-passation miroir", c -> e3[0] = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", e2[0], new Object[][]{{f3421, tiersA, Z, m("1200.00")},{f7111, null, m("1000.00"), Z},{f4455, null, m("200.00"), Z}}));
        refuse("deuxième contre-passation", "uk_compta_ecr_contrepassation", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", e2[0], new Object[][]{{f3421, tiersA, Z, m("1200.00")},{f7111, null, m("1000.00"), Z},{f4455, null, m("200.00"), Z}}));
        refuse("contre-passation d'une contre-passation", "ne se contre-passe pas", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", e3[0], new Object[][]{{f3421, tiersA, m("1200.00"), Z},{f7111, null, Z, m("1000.00")},{f4455, null, Z, m("200.00")}}));

        // lettrage
        final long[] fa = new long[1], fb = new long[1], pa = new long[1], pb = new long[1];
        passe("factures et règlements de deux clients, même montant", c -> {
            long eA = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f3421, tiersA, m("1200.00"), Z},{f7111, null, Z, m("1200.00")}});
            long eB = ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", null, new Object[][]{{f3421, tiersB, m("1200.00"), Z},{f7111, null, Z, m("1200.00")}});
            long rA = ecrire(c, soc1, ex26, fp1, "BQ", j15, "COURANTE", null, new Object[][]{{f5141, null, m("1200.00"), Z},{f3421, tiersA, Z, m("1200.00")}});
            long rB = ecrire(c, soc1, ex26, fp1, "BQ", j15, "COURANTE", null, new Object[][]{{f5141, null, m("1200.00"), Z},{f3421, tiersB, Z, m("1200.00")}});
            fa[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", eA, f3421);
            fb[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", eB, f3421);
            pa[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", rA, f3421);
            pb[0] = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", rB, f3421);
        });
        refuse("lettrage entre deux clients (S3)", "fk_compta_lel_ligne", c -> {
            long g = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','TOTAL','MANUEL',?,'t') returning id", soc1, f3421, tiersA, j15);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", fa[0], g, soc1, f3421, tiersA);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", pb[0], g, soc1, f3421, tiersA);
        });
        refuse("lettrage total déséquilibré, refus au commit", "COMPTA_LETTRAGE_DESEQUILIBRE", c -> {
            long g = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','TOTAL','MANUEL',?,'t') returning id", soc1, f3421, tiersA, j15);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", fa[0], g, soc1, f3421, tiersA);
            long e4 = ecrire(c, soc1, ex26, fp1, "BQ", j15, "COURANTE", null, new Object[][]{{f5141, null, m("1000.00"), Z},{f3421, tiersA, Z, m("1000.00")}});
            long l = l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=?", e4, f3421);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", l, g, soc1, f3421, tiersA);
        });
        final long[] gA = new long[1];
        passe("lettrage total du client A", c -> {
            gA[0] = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','TOTAL','MANUEL',?,'t') returning id", soc1, f3421, tiersA, j15);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", fa[0], gA[0], soc1, f3421, tiersA);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", pa[0], gA[0], soc1, f3421, tiersA);
            System.out.println("     code attribué : " + s1(c, "select code from compta_lettrages where id=?", gA[0]));
        });
        refuse("une ligne déjà lettrée", "compta_lettrage_lignes_pkey", c -> {
            long g = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','PARTIEL','MANUEL',?,'t') returning id", soc1, f3421, tiersA, j15);
            ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", fa[0], g, soc1, f3421, tiersA);
        });
        // AAA : 700 lettrages partiels sur un autre compte pour franchir ZZ
        passe("codes de lettrage après ZZ (S4)", c -> {
            String c4411 = cpt(soc1,"4411",c);
            long tf = l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?, 'FOURNISSEUR','FOURNISSEUR',1, null, 'F') returning id", soc1);
            ex(c, "insert into compta_compteurs_lettrage(livre_id, compte_numero, dernier_numero) values (?,?,675)", soc1, c4411);
            long e4 = ecrire(c, soc1, ex26, fp1, "AC", j15, "COURANTE", null, new Object[][]{{f5141, null, m("6.00"), Z},{c4411, tf, Z, m("1.00")},{c4411, tf, Z, m("1.00")},{c4411, tf, Z, m("1.00")},{c4411, tf, Z, m("1.00")},{c4411, tf, Z, m("1.00")},{c4411, tf, Z, m("1.00")}});
            try (PreparedStatement ps = c.prepareStatement("select id from compta_lignes where ecriture_id=? and compte_numero=? order by id")) {
                ps.setLong(1, e4); ps.setString(2, c4411);
                ResultSet r = ps.executeQuery(); List<Long> ids = new ArrayList<>(); while (r.next()) ids.add(r.getLong(1));
                List<String> codes = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    long g = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'','PARTIEL','MANUEL',?,'t') returning id", soc1, c4411, tf, j15);
                    ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", ids.get(i*2), g, soc1, c4411, tf);
                    ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", ids.get(i*2+1), g, soc1, c4411, tf);
                    codes.add(s1(c, "select code from compta_lettrages where id=?", g));
                }
                System.out.println("     codes : " + codes);
                if (!codes.equals(List.of("ZZ","AAA","AAB"))) throw new SQLException("codes faux " + codes);
            }
        });

        // périodes et exercices
        refuse("troisième période courante ouverte", "COMPTA_DEUX_PERIODES_MAX", c -> ex(c, "update compta_periodes set statut='OUVERTE' where id=?", fp3));
        refuse("clôture de février avant janvier", "COMPTA_PERIODE_NON_CHRONOLOGIQUE", c -> ex(c, "update compta_periodes set statut='CLOTUREE' where id=?", fp2));
        passe("brouillon daté de janvier", c -> ex(c, "insert into compta_brouillons(livre_id, journal_id, date_ecriture, cree_par) values (?,?,?,'t')", soc1, l1(c,"select id from compta_journaux where livre_id=? and code='OD'",soc1), j15));
        refuse("clôture de janvier avec un brouillon", "COMPTA_BROUILLONS_RESTANTS", c -> ex(c, "update compta_periodes set statut='CLOTUREE' where id=?", fp1));
        passe("suppression du brouillon", c -> ex(c, "delete from compta_brouillons where livre_id=?", soc1));
        passe("clôture de janvier, ouverture de mars", c -> { ex(c, "update compta_periodes set statut='CLOTUREE' where id=?", fp1); ex(c, "update compta_periodes set statut='OUVERTE' where id=?", fp3); });
        refuse("réouverture de janvier", "COMPTA_PERIODE_TRANSITION", c -> ex(c, "update compta_periodes set statut='OUVERTE' where id=?", fp1));
        refuse("écriture dans janvier clos", "COMPTA_PERIODE_FERMEE", c -> ecrire(c, soc1, ex26, fp1, "OD", j15, "COURANTE", null, new Object[][]{{f6111, null, m("10.00"), Z},{f5141, null, Z, m("10.00")}}));
        refuse("contre-passation dans une période close", "COMPTA_PERIODE_FERMEE", c -> ecrire(c, soc1, ex26, fp1, "VT", j15, "COURANTE", e1[0], new Object[][]{{f3421, tiersA, Z, m("1200.00")},{f7111, null, m("1000.00"), Z},{f4455, null, m("200.00"), Z}}));
        refuse("exercice de plus de 12 mois", "ck_compta_exercices_duree", c -> l1(c, "insert into compta_exercices(livre_id, code, date_debut, date_fin, precedent_id) values (?,'X','2027-01-01','2028-01-01',?) returning id", soc1, ex26));
        refuse("exercice non contigu", "COMPTA_EXERCICE_NON_CONTIGU", c -> l1(c, "insert into compta_exercices(livre_id, code, date_debut, date_fin, precedent_id) values (?,'X','2027-02-01','2027-12-31',?) returning id", soc1, ex26));
        refuse("deuxième exercice initial", "uq_compta_exercice_premier", c -> l1(c, "insert into compta_exercices(livre_id, code, date_debut, date_fin) values (?,'X','2025-01-01','2025-12-31') returning id", soc1));
        refuse("arrêté avec des périodes ouvertes", "COMPTA_PERIODES_OUVERTES", c -> ex(c, "update compta_exercices set statut='ARRETE' where id=?", ex26));
        refuse("clôture sans arrêté", "COMPTA_EXERCICE_TRANSITION", c -> ex(c, "update compta_exercices set statut='CLOTURE' where id=?", ex26));
        refuse("dates d'exercice modifiées", "COMPTA_EXERCICE_DATES_FIGEES", c -> ex(c, "update compta_exercices set date_fin='2026-11-30' where id=?", ex26));
        refuse("suppression d'un compte mouvementé (avis 2.6)", "fk_compta_lig_compte", c -> ex(c, "delete from compta_comptes where livre_id=? and numero=?", soc1, f5141));
        refuse("changement de numéro de compte", "COMPTA_COMPTE_FIGE", c -> ex(c, "update compta_comptes set numero='51419' where livre_id=? and numero=?", soc1, f5141));

        // concurrence
        int nThreads = 8, parThread = 150;
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        AtomicInteger valides = new AtomicInteger(), annules = new AtomicInteger();
        long t0 = System.currentTimeMillis();
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < nThreads; t++) {
            final int seed = t;
            fs.add(pool.submit(() -> {
                Random rnd = new Random(seed);
                try (Connection c = cx()) {
                    for (int i = 0; i < parThread; i++) {
                        ecrire(c, soc2, ex26b, l1(c, "select id from compta_periodes where exercice_id=? and numero=1", ex26b), "OD", j15, "COURANTE", null,
                                new Object[][]{{cpt(soc2,"6111",c), null, m("10.00"), Z},{cpt(soc2,"5141",c), null, Z, m("10.00")}});
                        if (rnd.nextInt(5) == 0) { c.rollback(); annules.incrementAndGet(); }
                        else { c.commit(); valides.incrementAndGet(); }
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
                return null;
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        try (Connection c = cx()) {
            long jod = l1(c, "select id from compta_journaux where livre_id=? and code='OD'", soc2);
            long n = l1(c, "select count(*) from compta_ecritures where livre_id=? and journal_id=?", soc2, jod);
            long mx = l1(c, "select max(numero) from compta_ecritures where livre_id=? and journal_id=?", soc2, jod);
            long ruptures = l1(c, "select count(*) from (select numero, valide_le, lag(numero) over w pn, lag(valide_le) over w pv from compta_ecritures where livre_id=? and journal_id=? window w as (order by numero)) x where numero<>pn+1 or valide_le<pv", soc2, jod);
            System.out.printf("concurrence : %d threads, %d validées, %d annulées, %d lignes en journal, max numéro %d, ruptures %d, %d ms%n", nThreads, valides.get(), annules.get(), n, mx, ruptures, System.currentTimeMillis()-t0);
            if (n == valides.get() && mx == n && ruptures == 0) { ok++; System.out.println("OK   numérotation sans trou ni doublon sous concurrence, horodatage croissant"); }
            else { ko++; System.out.println("KO   numérotation sous concurrence"); }
        }
        System.out.println("----\nRéussis " + ok + ", échecs " + ko);
    }
}
