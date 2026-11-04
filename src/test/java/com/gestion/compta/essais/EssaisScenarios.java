package com.gestion.compta.essais;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * EssaisScenarios du schéma arbitré : les six scénarios demandés et les points propres à l'arbitrage,
 * joués sur le vrai schéma commercial (lignes insérées en SQL) et le DDL final.
 * Usage : EssaisScenarios url
 */
public class EssaisScenarios {
    static final BigDecimal Z = BigDecimal.ZERO;
    static BigDecimal m(String v) { return new BigDecimal(v); }
    static int ok = 0, ko = 0;
    static void vrai(String nom, boolean c, String detail) {
        if (c) { ok++; System.out.println("OK   " + nom + (detail == null ? "" : " -> " + detail)); }
        else { ko++; System.out.println("KO   " + nom + (detail == null ? "" : " -> " + detail)); }
    }
    static void egal(String nom, Object attendu, Object obtenu) { vrai(nom, String.valueOf(attendu).equals(String.valueOf(obtenu)), "attendu " + attendu + ", obtenu " + obtenu); }
    interface Tx { void run(Connection c) throws Exception; }
    static void refuse(Connection c, String nom, String fragment, Tx t) throws Exception {
        Savepoint sp = c.setSavepoint();
        try { t.run(c); c.rollback(sp); vrai(nom, false, "accepté à tort"); }
        catch (SQLException e) {
            c.rollback(sp);
            String msg = e.getMessage().split("\n")[0];
            vrai(nom, msg.toLowerCase().contains(fragment.toLowerCase()), msg);
        }
    }
    static long l1(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.l1(c, q, p); }
    static String s1(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.s1(c, q, p); }
    static int ex(Connection c, String q, Object... p) throws SQLException { return EssaisInvariants.ex(c, q, p); }

    // ---------- socle commun
    static long livre, pdv, exo, userId;
    static long periodeDe(Connection c, long e, LocalDate d) throws SQLException {
        return l1(c, "select id from compta_periodes where exercice_id=? and type_periode='COURANTE' and ? between date_debut and date_fin", e, d);
    }
    static void ouvrir(Connection c, long e, int n) throws SQLException {
        ex(c, "update compta_periodes set statut='OUVERTE' where exercice_id=? and numero=? and statut='A_VENIR'", e, n);
    }
    static long ecr(Connection c, long l, long e, String j, String date, String ref, Long contre, Object[][] lignes) throws SQLException {
        LocalDate d = LocalDate.parse(date);
        EssaisInvariants.ref = ref;
        return EssaisInvariants.ecrire(c, l, e, periodeDe(c, e, d), j, d, "COURANTE", contre, lignes);
    }
    static long tiers(Connection c, long l, String type, long src, String nom) throws SQLException {
        return l1(c, "insert into compta_tiers(livre_id, type_tiers, source_type, source_id, code_auxiliaire, nom) values (?,?,?,?,null,?) returning id", l, type, type, src, nom);
    }
    static String solde(Connection c, long l, String compte) throws SQLException {
        return s1(c, "select coalesce(sum(debit-credit),0)::numeric(15,2) from compta_lignes where livre_id=? and compte_numero=?", l, compte);
    }
    static String soldeTiers(Connection c, long l, String compte, long t) throws SQLException {
        return s1(c, "select coalesce(sum(debit-credit),0)::numeric(15,2) from compta_lignes where livre_id=? and compte_numero=? and tiers_id=?", l, compte, t);
    }
    /** Le lien d'une pièce : une version de plus, jamais d'écriture active en double. */
    static void lier(Connection c, long l, String type, long id, String ev, String empreinte, long ecriture) throws SQLException {
        long v = l1(c, "select coalesce(max(version),0)+1 from compta_liens_source where livre_id=? and source_type=? and source_id=? and evenement=?", l, type, id, ev);
        ex(c, "insert into compta_liens_source(livre_id, source_type, source_id, evenement, version, point_de_vente_id, empreinte, instantane, ecriture_id, cree_par) values (?,?,?,?,?,?,?,'{}'::jsonb,?,'t')",
           l, type, id, ev, v, pdv, empreinte, ecriture);
    }
    /** Écriture active d'un événement : celle du dernier lien, si elle n'a ni contre-passation ni correction. */
    static Long actif(Connection c, long l, String type, long id, String ev) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("""
            select lien.ecriture_id from compta_liens_source lien
             where lien.livre_id=? and lien.source_type=? and lien.source_id=? and lien.evenement=?
               and not exists (select 1 from compta_ecritures x where x.livre_id=lien.livre_id
                                and (x.contrepassation_de_id=lien.ecriture_id or x.corrige_ecriture_id=lien.ecriture_id))
             order by lien.version desc limit 1""")) {
            s.setLong(1, l); s.setString(2, type); s.setLong(3, id); s.setString(4, ev);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getLong(1) : null; }
        }
    }
    /** Contre-passation miroir d'une écriture, lignes relues en base. */
    static long contrepasser(Connection c, long l, long e, long ecriture, String date) throws SQLException {
        List<Object[]> lignes = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("select compte_numero, tiers_id, debit, credit from compta_lignes where ecriture_id=? order by numero_ligne")) {
            s.setLong(1, ecriture);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) lignes.add(new Object[]{r.getString(1), r.getObject(2), r.getBigDecimal(4), r.getBigDecimal(3)});
            }
        }
        String j = s1(c, "select j.code from compta_ecritures e join compta_journaux j on j.id=e.journal_id where e.id=?", ecriture);
        String ref = s1(c, "select ref_piece from compta_ecritures where id=?", ecriture);
        return ecr(c, l, e, j, date, ref, ecriture, lignes.toArray(new Object[0][]));
    }
    static long ligne(Connection c, long ecriture, String compte) throws SQLException {
        return l1(c, "select id from compta_lignes where ecriture_id=? and compte_numero=? order by numero_ligne limit 1", ecriture, compte);
    }
    static long lettrer(Connection c, long l, String compte, long t, String statut, String origine, long... lignes) throws SQLException {
        long g = l1(c, "insert into compta_lettrages(livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par) values (?,?,?,0,'',?,?,'2026-03-01','t') returning id", l, compte, t, statut, origine);
        for (long x : lignes) ex(c, "insert into compta_lettrage_lignes(ligne_id, lettrage_id, livre_id, compte_numero, tiers_id) values (?,?,?,?,?)", x, g, l, compte, t);
        return g;
    }

    /** État d'une facture, calculé à la lecture : le modèle de requête de la file. */
    static final String ETAT_FACTURE = """
        WITH pdv AS (SELECT point_de_vente_id AS id, date_rattachement FROM compta_livre_points_de_vente WHERE livre_id = ?),
        pieces AS (
          SELECT f.id, f.statut, COALESCE(f.annulee, FALSE) AS annulee, f.date_facture, f.client_id, f.montant_ht, f.montant_tva,
                 f.montant_ttc, f.remise_globale, f.montant_final, f.numero_facture
            FROM factures f JOIN pdv ON pdv.id = f.point_de_vente_id AND f.date_facture >= pdv.date_rattachement
           WHERE f.id = ? AND f.statut <> 'BROUILLON'
        ),
        lignes AS (
          SELECT l.facture_id, string_agg(concat_ws(':', l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva), ','
                                          ORDER BY l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva) AS detail
            FROM lignes_facture l JOIN pieces p ON p.id = l.facture_id
           GROUP BY l.facture_id
        ),
        calcul AS (
          SELECT p.*, md5(concat_ws('|', p.date_facture, p.client_id, p.montant_ht, p.montant_tva, p.montant_ttc,
                     COALESCE(p.remise_globale, 0), p.montant_final, p.numero_facture, l.detail)) AS empreinte
            FROM pieces p LEFT JOIN lignes l ON l.facture_id = p.id
        ),
        dernier AS (
          SELECT DISTINCT ON (s.source_id) s.source_id, s.empreinte, s.ecriture_id
            FROM compta_liens_source s
           WHERE s.livre_id = ? AND s.source_type = 'FACTURE' AND s.evenement = 'COMPTABILISATION'
           ORDER BY s.source_id, s.version DESC
        )
        SELECT CASE WHEN x.id IS NOT NULL THEN 'ECARTEE'
                    WHEN d.source_id IS NULL OR EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.contrepassation_de_id = d.ecriture_id OR c.corrige_ecriture_id = d.ecriture_id)
                         THEN CASE WHEN p.annulee AND p.statut = 'ANNULEE' THEN 'SANS_OBJET'
                                   WHEN p.annulee THEN 'ANOMALIE'
                                   ELSE 'A_COMPTABILISER' END
                    WHEN p.annulee OR p.statut = 'ANNULEE' THEN 'ANNULEE'
                    WHEN d.empreinte <> p.empreinte THEN 'MODIFIEE'
                    ELSE 'COMPTABILISEE' END AS etat, p.empreinte
          FROM calcul p
          LEFT JOIN dernier d ON d.source_id = p.id
          LEFT JOIN compta_pieces_ecartees x ON x.livre_id = ? AND x.source_type = 'FACTURE' AND x.source_id = p.id
        """;
    static String[] etat(Connection c, long l, long facture) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(ETAT_FACTURE)) {
            s.setLong(1, l); s.setLong(2, facture); s.setLong(3, l); s.setLong(4, l);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return new String[]{"HORS_FILE", null};
                return new String[]{r.getString(1), r.getString(2)};
            }
        }
    }
    static long facture(Connection c, long p, long client, String numero, String date, String ht, String tva, String remise, String fin) throws SQLException {
        long id = l1(c, "insert into factures(numero_facture, date_facture, client_id, emise_par_user_id, montant_ht, montant_tva, montant_ttc, remise_globale, montant_final, statut, point_de_vente_id) values (?,?::date,?,?,?::numeric,?::numeric,?::numeric + ?::numeric,?::numeric,?::numeric,'EN_ATTENTE',?) returning id",
                numero, date, client, userId, ht, tva, ht, tva, remise, fin, p);
        ex(c, "insert into lignes_facture(facture_id, produit_id, quantite, prix_unitaire_ht, taux_tva, montant_ht, montant_tva, montant_ttc) values (?,(select min(id) from produits where reference like 'P-ATT-%'),1,?::numeric,20,?::numeric,?::numeric,?::numeric + ?::numeric)", id, ht, ht, tva, ht, tva);
        return id;
    }

    // ---------- résolution du livre courant : la règle de LivreCourantService
    /** @return id du livre, ou null si à activer. Reprend mot pour mot la règle de la section 5. */
    static Long livreCourant(Connection c, long h, Long pdvUtilisateur, Long tenantUtilisateur) throws SQLException {
        boolean proprePdv = Objects.equals(h, pdvUtilisateur) || Objects.equals(h, tenantUtilisateur);
        if (!proprePdv && l1(c, "select count(*) from societes where id=?", h) > 0) {
            return unLong(c, "select id from compta_livres where societe_id=?", h);
        }
        return unLong(c, "select livre_id from compta_livre_points_de_vente where point_de_vente_id=?", h);
    }
    static Long unLong(Connection c, String q, Object... p) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(q)) {
            for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getLong(1) : null; }
        }
    }

    public static void main(String[] a) throws Exception {
        EssaisInvariants.url = a[0];
        try (Connection c = EssaisInvariants.cx()) {
            ex(c, "set lc_messages='C'"); c.commit();
            userId = l1(c, "insert into users(email, password, tenant_id) values ('att" + System.nanoTime() + "@test', 'x', 1) returning id");
            livre = EssaisInvariants.activer(c, "ATT");
            pdv = l1(c, "select point_de_vente_id from compta_livre_points_de_vente where livre_id=?", livre);
            ex(c, "insert into produits(reference, description, point_de_vente_id, type_produit) values ('P-ATT-1','P',?,'MARCHANDISE')", pdv);
            exo = EssaisInvariants.creerExercice(c, livre, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null);
            ouvrir(c, exo, 1); ouvrir(c, exo, 2);
            c.commit();

            scenarioFactureAnnuleeNumeroRecycle(c);
            scenarioChequePartielRejete(c);
            scenarioMemeNumeroAchatVente(c);
            scenarioTvaTrimestrielleCredit(c);
            scenarioBasculeDossier(c);
            scenarioClotureDeuxFois(c);
            pointsArbitrage(c);
        }
        System.out.println("----\nRéussis " + ok + ", échecs " + ko);
    }

    // B. Facture annulée, numéro recyclé
    static void scenarioFactureAnnuleeNumeroRecycle(Connection c) throws Exception {
        System.out.println("== Facture annulée, numéro recyclé");
        long cl = l1(c, "insert into clients(nom, point_de_vente_id) values ('Client B', ?) returning id", pdv);
        long t = tiers(c, livre, "CLIENT", cl, "Client B");
        long a12 = facture(c, pdv, cl, "FA-12", "2026-01-10", "1000", "200", "0", "1200");
        String[] e0 = etat(c, livre, a12);
        egal("FA-12 entre dans la file", "A_COMPTABILISER", e0[0]);
        long v1 = ecr(c, livre, exo, "VT", "2026-01-10", "FA-12", null, new Object[][]{{"3421", t, m("1200.00"), Z},{"7111", null, Z, m("1000.00")},{"4455", null, Z, m("200.00")}});
        lier(c, livre, "FACTURE", a12, "COMPTABILISATION", e0[1], v1);
        egal("FA-12 comptabilisée", "COMPTABILISEE", etat(c, livre, a12)[0]);
        refuse(c, "deuxième écriture active pour la même facture", "compta_lien_deja_actif", x -> lier(x, livre, "FACTURE", a12, "COMPTABILISATION", e0[1], v1));
        // Annulation commerciale : le numéro est libéré, l'ancienne ligne reste.
        ex(c, "update factures set annulee=true, statut='ANNULEE', date_annulation=now(), numero_facture='FA-12-ANN' where id=?", a12);
        egal("facture annulée après comptabilisation", "ANNULEE", etat(c, livre, a12)[0]);
        long miroir = contrepasser(c, livre, exo, v1, "2026-01-20");
        lettrer(c, livre, "3421", t, "TOTAL", "CONTREPASSATION", ligne(c, v1, "3421"), ligne(c, miroir, "3421"));
        egal("après contre-passation : plus rien à faire", "SANS_OBJET", etat(c, livre, a12)[0]);
        egal("3421 du tiers soldé", "0.00", soldeTiers(c, livre, "3421", t));
        // La nouvelle FA-12 : autre id, même numéro. Elle n'est pas vue comme déjà comptabilisée.
        long b12 = facture(c, pdv, cl, "FA-12", "2026-01-21", "500", "100", "0", "600");
        String[] e1 = etat(c, livre, b12);
        egal("la nouvelle FA-12 entre dans la file", "A_COMPTABILISER", e1[0]);
        long v2 = ecr(c, livre, exo, "VT", "2026-01-21", "FA-12", null, new Object[][]{{"3421", t, m("600.00"), Z},{"7111", null, Z, m("500.00")},{"4455", null, Z, m("100.00")}});
        lier(c, livre, "FACTURE", b12, "COMPTABILISATION", e1[1], v2);
        egal("recherche par pièce FA-12 : origine, contre-passation, nouvelle", 3L, l1(c, "select count(*) from compta_ecritures where livre_id=? and ref_piece='FA-12'", livre));
        egal("3421 du tiers = la seule nouvelle facture", "600.00", soldeTiers(c, livre, "3421", t));
        // Annulée puis revalidée par POST /valider : annulee reste vrai, statut VALIDEE. Rien n'est posté en silence.
        ex(c, "update factures set statut='VALIDEE' where id=?", a12);
        egal("annulée puis revalidée : anomalie, pas de comptabilisation", "ANOMALIE", etat(c, livre, a12)[0]);
        // Modifiée après comptabilisation
        ex(c, "update lignes_facture set montant_ht=600 where facture_id=?", b12);
        egal("modification des lignes détectée par l'empreinte", "MODIFIEE", etat(c, livre, b12)[0]);
        ex(c, "update lignes_facture set montant_ht=500 where facture_id=?", b12);
        ex(c, "update factures set statut='PAYEE_PARTIELLEMENT', montant_paye=100, montant_restant=500 where id=?", b12);
        egal("un règlement ne change pas l'empreinte", "COMPTABILISEE", etat(c, livre, b12)[0]);
        c.commit();
    }

    // C. Paiement partiel par chèque, rejeté
    static void scenarioChequePartielRejete(Connection c) throws Exception {
        System.out.println("== Paiement partiel par chèque, rejeté après remise");
        long cl = l1(c, "insert into clients(nom, point_de_vente_id) values ('Client C', ?) returning id", pdv);
        long t = tiers(c, livre, "CLIENT", cl, "Client C");
        long f = facture(c, pdv, cl, "FA-30", "2026-02-02", "1000", "200", "0", "1200");
        String[] e = etat(c, livre, f);
        long ef = ecr(c, livre, exo, "VT", "2026-02-02", "FA-30", null, new Object[][]{{"3421", t, m("1200.00"), Z},{"7111", null, Z, m("1000.00")},{"4455", null, Z, m("200.00")}});
        lier(c, livre, "FACTURE", f, "COMPTABILISATION", e[1], ef);
        // Le commercial enregistre 600 par chèque, affectés à la facture.
        long p = l1(c, "insert into paiements(numero_paiement, date_paiement, montant, mode_paiement, encaisse_par_user_id, client_id, point_de_vente_id) values ('PAY-30','2026-02-05',600,'CHEQUE',?,?,?) returning id", userId, cl, pdv);
        ex(c, "insert into paiement_affectations(paiement_id, facture_id, montant_affecte) values (?,?,600)", p, f);
        long q = l1(c, "insert into cheques_effets(numero_piece, type_effet, sens, statut, montant, date_emission, reference_paiement, client_id, point_de_vente_id) values ('CH-30','CHEQUE','ENCAISSEMENT_CLIENT','EN_PORTEFEUILLE',600,'2026-02-05','PAY-30',?,?) returning id", cl, pdv);
        ex(c, "update factures set statut='PAYEE_PARTIELLEMENT', montant_paye=600, montant_restant=600 where id=?", f);
        egal("la facture reste comptabilisée après le règlement", "COMPTABILISEE", etat(c, livre, f)[0]);
        long ep = ecr(c, livre, exo, "BQ", "2026-02-05", "PAY-30", null, new Object[][]{{"51111", null, m("600.00"), Z},{"3421", t, Z, m("600.00")}});
        lier(c, livre, "PAIEMENT", p, "COMPTABILISATION", "e", ep);
        long gp = lettrer(c, livre, "3421", t, "PARTIEL", "AFFECTATION", ligne(c, ef, "3421"), ligne(c, ep, "3421"));
        // Remise
        ex(c, "update cheques_effets set statut='REMIS_A_L_ENCAISSEMENT', date_remise='2026-02-10' where id=?", q);
        long er = ecr(c, livre, exo, "BQ", "2026-02-10", "CH-30", null, new Object[][]{{"51112", null, m("600.00"), Z},{"51111", null, Z, m("600.00")}});
        lier(c, livre, "EFFET", q, "REMISE", "e", er);
        // Rejet : le commercial annule le paiement et rouvre la facture.
        ex(c, "update cheques_effets set statut='IMPAYE_REJETE', motif_rejet='Provision' where id=?", q);
        ex(c, "update paiements set annule=true, date_annulation=now() where id=?", p);
        ex(c, "update factures set statut='EN_ATTENTE', montant_paye=0, montant_restant=1200 where id=?", f);
        egal("la facture reste comptabilisée après le rejet", "COMPTABILISEE", etat(c, livre, f)[0]);
        // Position lue sur les liens actifs : remise passée, pas d'encaissement.
        boolean remise = actif(c, livre, "EFFET", q, "REMISE") != null, encaisse = actif(c, livre, "EFFET", q, "ENCAISSEMENT") != null;
        String aCrediter = encaisse ? "5141" : remise ? "51112" : "51111";
        egal("position du chèque au rejet", "51112", aCrediter);
        // Règle R05 : paiement annulé dont l'effet est rejeté : jamais de contre-passation du paiement.
        egal("le paiement reste actif : le rejet porte l'écriture", ep, actif(c, livre, "PAIEMENT", p, "COMPTABILISATION"));
        long ej = ecr(c, livre, exo, "BQ", "2026-02-15", "CH-30", null, new Object[][]{{"3421", t, m("600.00"), Z},{aCrediter, null, Z, m("600.00")}});
        lier(c, livre, "EFFET", q, "REJET", "e", ej);
        refuse(c, "un deuxième rejet du même effet", "compta_lien_deja_actif", x -> lier(x, livre, "EFFET", q, "REJET", "e", ej));
        // Lettrage : le groupe partiel saute, le paiement se lettre avec son rejet.
        ex(c, "delete from compta_lettrages where id=?", gp);
        lettrer(c, livre, "3421", t, "TOTAL", "REJET", ligne(c, ep, "3421"), ligne(c, ej, "3421"));
        c.commit();
        egal("le client doit de nouveau tout", "1200.00", soldeTiers(c, livre, "3421", t));
        egal("51111 soldé", "0.00", solde(c, livre, "51111"));
        egal("51112 soldé", "0.00", solde(c, livre, "51112"));
        egal("banque intacte", "0.00", solde(c, livre, "5141"));
        egal("une seule ligne ouverte au tiers : la facture", 1L, l1(c, "select count(*) from compta_lignes l where l.livre_id=? and l.tiers_id=? and not exists (select 1 from compta_lettrage_lignes x where x.ligne_id=l.id)", livre, t));
        // Variante : rejet après encaissement, avec TVA déjà déclarée.
        long p2 = l1(c, "insert into paiements(numero_paiement, date_paiement, montant, mode_paiement, encaisse_par_user_id, client_id, point_de_vente_id) values ('PAY-31','2026-02-16',120,'CHEQUE',?,?,?) returning id", userId, cl, pdv);
        long ep2 = ecr(c, livre, exo, "BQ", "2026-02-16", "PAY-31", null, new Object[][]{{"51111", null, m("120.00"), Z},{"3421", t, Z, m("120.00")}});
        lier(c, livre, "PAIEMENT", p2, "COMPTABILISATION", "e", ep2);
        long q2 = l1(c, "insert into cheques_effets(numero_piece, type_effet, sens, statut, montant, date_emission, date_encaissement, reference_paiement, client_id, point_de_vente_id) values ('CH-31','CHEQUE','ENCAISSEMENT_CLIENT','ENCAISSE_COMPTABILISE',120,'2026-02-16','2026-02-20','PAY-31',?,?) returning id", cl, pdv);
        long ee = ecr(c, livre, exo, "BQ", "2026-02-20", "CH-31", null, new Object[][]{{"5141", null, m("120.00"), Z},{"51111", null, Z, m("120.00")}});
        lier(c, livre, "EFFET", q2, "ENCAISSEMENT", "e", ee);
        long d = l1(c, "insert into compta_tva_declarations(livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite, statut) values (?,2026,2,'2026-02-01','2026-02-28','MENSUELLE',1,'ENCAISSEMENT','2026-03-31','BROUILLON') returning id", livre);
        String ins = "insert into compta_tva_operations_collectees(livre_id, declaration_id, source_type, source_id, facture_id, point_de_vente_id, empreinte_source, date_generateur, taux, ttc, ht, tva) values (?,?,?,?,?,?,'00000000000000000000000000000000','2026-02-20',20,?::numeric,?::numeric,?::numeric)";
        ex(c, ins, livre, d, "PAIEMENT", p2, f, pdv, "120.00", "100.00", "20.00");
        refuse(c, "même encaissement déclaré deux fois", "uq_compta_tvc_unique", x -> ex(x, ins, livre, d, "PAIEMENT", p2, f, pdv, "120.00", "100.00", "20.00"));
        ex(c, ins, livre, d, "REGULARISATION", q2, f, pdv, "-120.00", "-100.00", "-20.00");
        ex(c, "update cheques_effets set statut='IMPAYE_REJETE' where id=?", q2);
        boolean encaisse2 = actif(c, livre, "EFFET", q2, "ENCAISSEMENT") != null;
        egal("rejet après encaissement : position banque", true, encaisse2);
        long ej2 = ecr(c, livre, exo, "BQ", "2026-02-21", "CH-31", null, new Object[][]{{"3421", t, m("120.00"), Z},{"5141", null, Z, m("120.00")}});
        lier(c, livre, "EFFET", q2, "REJET", "e", ej2);
        c.commit();
        egal("rejet après encaissement : 5141 revenu à zéro", "0.00", solde(c, livre, "5141"));
        egal("régularisation TVA : somme des opérations nulle", "0.00", s1(c, "select sum(tva) from compta_tva_operations_collectees where declaration_id=?", d));
    }

    // D. Numéro de facture fournisseur égal à un numéro de facture de vente
    static void scenarioMemeNumeroAchatVente(Connection c) throws Exception {
        System.out.println("== Facture fournisseur et facture de vente de même numéro et de même id");
        long cl = l1(c, "insert into clients(nom, point_de_vente_id) values ('Client D', ?) returning id", pdv);
        long fo = l1(c, "insert into fournisseurs(raison_social, point_de_vente_id) values ('Fournisseur D', ?) returning id", pdv);
        long tc = tiers(c, livre, "CLIENT", 9001, "Client D");
        long tf = tiers(c, livre, "FOURNISSEUR", 9001, "Fournisseur D");
        vrai("même source_id, tiers distincts", tc != tf, tc + " et " + tf);
        long fv = facture(c, pdv, cl, "2026-077", "2026-02-03", "1000", "200", "0", "1200");
        ex(c, "insert into factures_achat(id, numero_facture, date_facture, fournisseur_id, montant_ht, montant_tva, montant_ttc, statut, point_de_vente_id) values (?, '2026-077', '2026-02-03', ?, 500, 100, 600, 'EN_ATTENTE', ?)", fv, fo, pdv);
        long ev = ecr(c, livre, exo, "VT", "2026-02-03", "2026-077", null, new Object[][]{{"3421", tc, m("1200.00"), Z},{"7111", null, Z, m("1000.00")},{"4455", null, Z, m("200.00")}});
        lier(c, livre, "FACTURE", fv, "COMPTABILISATION", "e", ev);
        long ea = ecr(c, livre, exo, "AC", "2026-02-03", "2026-077", null, new Object[][]{{"6111", null, m("500.00"), Z},{"34552", null, m("100.00"), Z},{"4411", tf, Z, m("600.00")}});
        lier(c, livre, "FACTURE_ACHAT", fv, "COMPTABILISATION", "e", ea);
        egal("les deux pièces sont comptabilisées", 2L, l1(c, "select count(*) from compta_liens_source where livre_id=? and source_id=? and source_type in ('FACTURE','FACTURE_ACHAT')", livre, fv));
        egal("recherche par numéro : les deux écritures", 2L, l1(c, "select count(*) from compta_ecritures where livre_id=? and ref_piece='2026-077'", livre));
        egal("journaux distincts, chacun numéroté", 2L, l1(c, "select count(distinct journal_id) from compta_ecritures where livre_id=? and ref_piece='2026-077'", livre));
        refuse(c, "lettrage d'une ligne client avec une ligne fournisseur", "fk_compta_lel_ligne", x -> lettrer(x, livre, "3421", tc, "PARTIEL", "MANUEL", ligne(x, ev, "3421"), ligne(x, ea, "4411")));
        c.commit();
    }

    // E. Déclarant trimestriel avec un crédit
    static void scenarioTvaTrimestrielleCredit(Connection c) throws Exception {
        System.out.println("== TVA : déclarant trimestriel avec un crédit");
        long l = EssaisInvariants.activer(c, "TVAQ");
        long e = EssaisInvariants.creerExercice(c, l, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null);
        ouvrir(c, e, 1); ouvrir(c, e, 2);
        // Pièces de T1 : TVA facturée 100, TVA récupérable 300.
        long t = tiers(c, l, "FOURNISSEUR", 1, "F");
        ecr(c, l, e, "VT", "2026-01-10", "V1", null, new Object[][]{{"5141", null, m("600.00"), Z},{"7111", null, Z, m("500.00")},{"4455", null, Z, m("100.00")}});
        ecr(c, l, e, "AC", "2026-01-11", "A1", null, new Object[][]{{"6111", null, m("1500.00"), Z},{"34552", null, m("300.00"), Z},{"4411", t, Z, m("1800.00")}});
        String insDecl = "insert into compta_tva_declarations(livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite, statut, credit_anterieur, tva_exigible, tva_deductible, tva_due, credit_periode) values (?,?,?,?::date,?::date,'TRIMESTRIELLE',2,'ENCAISSEMENT',?::date,'BROUILLON',?::numeric,?::numeric,?::numeric,?::numeric,?::numeric) returning id";
        long q1 = l1(c, insDecl, l, 2026, 1, "2026-01-01", "2026-03-31", "2026-04-30", "0", "100.00", "300.00", "0", "200.00");
        refuse(c, "une période ne porte pas à la fois un dû et un crédit", "ck_compta_tvd_resultat", x -> ex(x, "update compta_tva_declarations set tva_due=10 where id=?", q1));
        ex(c, "update compta_tva_declarations set statut='VALIDEE' where id=?", q1);
        ex(c, "update compta_tva_declarations set statut='DEPOSEE' where id=?", q1);
        refuse(c, "montant d'une déclaration déposée", "compta_tva_figee", x -> ex(x, "update compta_tva_declarations set credit_periode=999 where id=?", q1));
        // Liquidation de T1 : 4456 créditeur 100, débiteur 300, viré en 3456.
        long li1 = ecr(c, l, e, "OD", "2026-02-20", "TVA T1", null, new Object[][]{
            {"4455", null, m("100.00"), Z},{"4456", null, Z, m("100.00")},
            {"4456", null, m("300.00"), Z},{"34552", null, Z, m("300.00")},
            {"3456", null, m("200.00"), Z},{"4456", null, Z, m("200.00")}});
        egal("après T1 : 4456 soldé", "0.00", solde(c, l, "4456"));
        egal("après T1 : crédit en 3456", "200.00", solde(c, l, "3456"));
        // T2 lit le crédit de la période qui se termine la veille, quel que soit le format de période.
        String anterieur = s1(c, "select credit_periode from compta_tva_declarations where livre_id=? and periode_fin = ?::date - 1 and statut in ('VALIDEE','EDI_ENVOYE','EDI_TRAITE','DEPOSEE','PAYEE') order by version desc limit 1", l, "2026-04-01");
        egal("T2 reprend le crédit de T1", "200.00", anterieur);
        long q2 = l1(c, insDecl, l, 2026, 2, "2026-04-01", "2026-06-30", "2026-07-31", anterieur, "500.00", "100.00", "200.00", "0");
        ex(c, "update compta_tva_declarations set statut='VALIDEE' where id=?", q2);
        ex(c, "update compta_tva_declarations set statut='DEPOSEE' where id=?", q2);
        ecr(c, l, e, "VT", "2026-02-21", "V2", null, new Object[][]{{"5141", null, m("3000.00"), Z},{"7111", null, Z, m("2500.00")},{"4455", null, Z, m("500.00")}});
        ecr(c, l, e, "AC", "2026-02-21", "A2", null, new Object[][]{{"6111", null, m("500.00"), Z},{"34552", null, m("100.00"), Z},{"4411", t, Z, m("600.00")}});
        long li2 = ecr(c, l, e, "OD", "2026-02-22", "TVA T2", null, new Object[][]{
            {"4455", null, m("500.00"), Z},{"4456", null, Z, m("500.00")},
            {"4456", null, m("100.00"), Z},{"34552", null, Z, m("100.00")},
            {"4456", null, m("200.00"), Z},{"3456", null, Z, m("200.00")}});
        ecr(c, l, e, "BQ", "2026-02-23", "TVA T2 pmt", null, new Object[][]{{"4456", null, m("200.00"), Z},{"5141", null, Z, m("200.00")}});
        c.commit();
        egal("après T2 : crédit consommé", "0.00", solde(c, l, "3456"));
        egal("après T2 : 4456 soldé", "0.00", solde(c, l, "4456"));
        egal("après T2 : 4455 soldé", "0.00", solde(c, l, "4455"));
        egal("après T2 : 34552 soldé", "0.00", solde(c, l, "34552"));
        // Passage au mensuel au 1er janvier : le crédit de T4 passe à janvier, le filtre est sur les dates.
        long q4 = l1(c, insDecl, l, 2026, 4, "2026-10-01", "2026-12-31", "2027-01-31", "0", "0", "50.00", "0", "50.00");
        ex(c, "update compta_tva_declarations set statut='VALIDEE' where id=?", q4);
        egal("janvier mensuel reprend le crédit du T4 trimestriel", "50.00", s1(c, "select credit_periode from compta_tva_declarations where livre_id=? and periode_fin = ?::date - 1 and statut <> 'BROUILLON' order by version desc limit 1", l, "2027-01-01"));
        long q3 = l1(c, insDecl, l, 2026, 3, "2026-07-01", "2026-09-30", "2026-10-31", "0", "0", "0", "0", "0");
        refuse(c, "compte 3455 en déduction", "ck_compta_tvo_compte", x -> ex(x, "insert into compta_tva_operations_deductibles(livre_id, declaration_id, source_type, source_id, point_de_vente_id, empreinte_source, numero_facture, date_facture, fournisseur_nom, designation, rubrique_code, compte_tva, ht, taux, tva, ttc, tva_deductible, mp_id, date_paiement) values (?,?,'MANUEL',1,1,'00000000000000000000000000000000','F','2026-01-01','F','D','ACHATS','3455',100,20,20,120,20,2,'2026-01-02')", l, q3));
        c.commit();
    }

    // F. Société basculée par le sélecteur fiduciaire
    static void scenarioBasculeDossier(Connection c) throws Exception {
        System.out.println("== Bascule de dossier par le sélecteur fiduciaire");
        // Collision d'identifiants : la société S porte le même id numérique qu'un point de vente P7 d'un autre client, qui a son livre.
        long p5 = l1(c, "insert into point_de_vente(nom_point_de_vente, tenant_id) values ('Cabinet" + System.nanoTime() + "', 1) returning id");
        long p7 = l1(c, "insert into point_de_vente(nom_point_de_vente, tenant_id) values ('Autre client" + System.nanoTime() + "', 1) returning id");
        long livreP7 = l1(c, "insert into compta_livres(date_demarrage, raison_sociale, active_par) values ('2026-01-01','Autre client','t') returning id");
        ex(c, "insert into compta_livre_points_de_vente(point_de_vente_id, livre_id, date_rattachement) values (?,?,'2026-01-01')", p7, livreP7);
        ex(c, "insert into societes(id, code, raison_sociale, tenant_id) values (?, ?, 'Dossier S', ?)", p7, "S" + p7, p5);
        egal("avant activation du dossier : A_ACTIVER, jamais le livre du point de vente homonyme", null, livreCourant(c, p7, p5, p5));
        long livreS = l1(c, "insert into compta_livres(societe_id, date_demarrage, raison_sociale, active_par) values (?, '2026-01-01','Dossier S','t') returning id", p7);
        egal("utilisateur du cabinet, bascule vers S : le livre de S", livreS, livreCourant(c, p7, p5, p5));
        egal("utilisateur du point de vente P7, sans bascule : son propre livre", livreP7, livreCourant(c, p7, p7, p7));
        egal("utilisateur du cabinet, sans bascule : pas de livre, à activer", null, livreCourant(c, p5, p5, p5));
        egal("le dossier sans commercial n'a aucun point de vente à lire", 0L, l1(c, "select count(*) from compta_livre_points_de_vente where livre_id=?", livreS));
        refuse(c, "deux livres pour un même dossier", "uk_compta_livres_societe", x -> ex(x, "insert into compta_livres(societe_id, date_demarrage, raison_sociale, active_par) values (?, '2026-01-01','Doublon','t')", p7));
        refuse(c, "un point de vente dans deux livres", "compta_livre_points_de_vente_pkey", x -> ex(x, "insert into compta_livre_points_de_vente(point_de_vente_id, livre_id, date_rattachement) values (?,?,'2026-01-01')", p7, livreS));
        c.commit();
    }

    // A. Clôture deux fois de suite, puis lecture du bilan
    static void scenarioClotureDeuxFois(Connection c) throws Exception {
        System.out.println("== Clôture deux fois de suite, lecture du bilan");
        Scenarios.Ctx x = new Scenarios.Ctx(); x.c = c;
        x.soc = EssaisInvariants.activer(c, "CLO2");
        x.ex.put("2025", EssaisInvariants.creerExercice(c, x.soc, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null));
        Scenarios.ouvrir(x, x.ex.get("2025"), 1); Scenarios.ouvrir(x, x.ex.get("2025"), 2);
        EssaisInvariants.ref = "C1";
        x.ecr("2025", "OD", "2025-01-10", new Object[][]{{"5141", null, m("100000.00"), Z},{"1111", null, Z, m("100000.00")}});
        x.ecr("2025", "VT", "2025-02-10", new Object[][]{{"5141", null, m("50000.00"), Z},{"7111", null, Z, m("50000.00")}});
        x.ecr("2025", "OD", "2025-02-20", new Object[][]{{"6111", null, m("20000.00"), Z},{"5141", null, Z, m("20000.00")}});
        Scenarios.avancerPeriodes(x, "2025", 12);
        String avant = Scenarios.jsonMesures(x, "2025", LocalDate.of(2025,12,31));
        Scenarios.cloturer(x, "2025", "2026", LocalDate.of(2026,1,1));
        c.commit();
        String apres = Scenarios.jsonMesures(x, "2025", LocalDate.of(2025,12,31));
        egal("mesures de 2025 identiques avant et après clôture", avant, apres);
        Savepoint sp = c.setSavepoint();
        try { Scenarios.cloturer(x, "2025", "2026", LocalDate.of(2026,1,1)); vrai("deuxième clôture", false, "acceptée à tort"); }
        catch (SQLException e) { vrai("deuxième clôture refusée", e.getMessage().contains("compta_exercice"), e.getMessage().split("\n")[0]); }
        c.rollback(sp);
        egal("mesures de 2025 inchangées après la tentative", avant, Scenarios.jsonMesures(x, "2025", LocalDate.of(2025,12,31)));
        egal("un seul à-nouveau sur 2026", 1L, l1(c, "select count(*) from compta_ecritures where livre_id=? and exercice_id=? and nature='A_NOUVEAU'", x.soc, x.ex.get("2026")));
        egal("une seule écriture de clôture sur 2025", 1L, l1(c, "select count(*) from compta_ecritures where livre_id=? and exercice_id=? and nature='CLOTURE'", x.soc, x.ex.get("2025")));
        egal("banque à l'ouverture de 2026 : 130 000, pas 260 000", "130000.00", s1(c, "select sum(debit-credit) from compta_lignes where livre_id=? and exercice_id=? and compte_numero='5141' and nature_ecriture='A_NOUVEAU'", x.soc, x.ex.get("2026")));
        egal("119x à l'ouverture de 2026 : 30 000 au crédit", "-30000.00", s1(c, "select sum(debit-credit) from compta_lignes where livre_id=? and exercice_id=? and compte_numero like '119%' and nature_ecriture='A_NOUVEAU'", x.soc, x.ex.get("2026")));
        c.commit();
    }

    // Points propres à l'arbitrage
    static void pointsArbitrage(Connection c) throws Exception {
        System.out.println("== Points propres à l'arbitrage");
        long l = EssaisInvariants.activer(c, "ARB");
        long e = EssaisInvariants.creerExercice(c, l, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null);
        ouvrir(c, e, 1); ouvrir(c, e, 2);
        // Chronologie à la maille du mois
        ecr(c, l, e, "OD", "2026-01-20", "A", null, new Object[][]{{"6111", null, m("10.00"), Z},{"5141", null, Z, m("10.00")}});
        ecr(c, l, e, "OD", "2026-01-05", "B", null, new Object[][]{{"6111", null, m("10.00"), Z},{"5141", null, Z, m("10.00")}});
        vrai("deux dates dans le désordre, même mois : accepté, numéro croissant", true, null);
        ecr(c, l, e, "OD", "2026-02-02", "C", null, new Object[][]{{"6111", null, m("10.00"), Z},{"5141", null, Z, m("10.00")}});
        refuse(c, "mois de janvier rouvert dans un journal déjà en février", "compta_chronologie_journal", x -> ecr(x, l, e, "OD", "2026-01-25", "D", null, new Object[][]{{"6111", null, m("10.00"), Z},{"5141", null, Z, m("10.00")}}));
        ecr(c, l, e, "AC", "2026-01-25", "D", null, new Object[][]{{"6111", null, m("10.00"), Z},{"5141", null, Z, m("10.00")}});
        vrai("janvier reste ouvert dans un autre journal", true, null);
        // Exercice à cheval en cours de mois : 13 mois civils touchés
        long l2 = EssaisInvariants.activer(c, "ARB2");
        long id = l1(c, "insert into compta_exercices(livre_id, code, date_debut, date_fin) values (?,'X','2026-03-15','2027-03-14') returning id", l2);
        LocalDate d = LocalDate.of(2026,3,15), au = LocalDate.of(2027,3,14); int n = 1;
        while (!d.isAfter(au)) {
            LocalDate f = d.withDayOfMonth(d.lengthOfMonth()); if (f.isAfter(au)) f = au;
            ex(c, "insert into compta_periodes(livre_id, exercice_id, numero, type_periode, date_debut, date_fin) values (?,?,?,'COURANTE',?,?)", l2, id, n, d, f);
            d = f.plusDays(1); n++;
        }
        egal("clôture en cours de mois : 13 périodes courantes", 14, n);
        refuse(c, "exercice de plus de 12 mois", "ck_compta_exercices_duree", x -> ex(x, "insert into compta_exercices(livre_id, code, date_debut, date_fin, precedent_id) values (?,'Y','2027-03-15','2028-03-15',?)", l2, id));
        // Extourne : miroir entre deux exercices, sans marque d'expurgation
        Scenarios.Ctx x = new Scenarios.Ctx(); x.c = c;
        x.soc = EssaisInvariants.activer(c, "EXT");
        x.ex.put("2025", EssaisInvariants.creerExercice(c, x.soc, "2025", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), null));
        Scenarios.ouvrir(x, x.ex.get("2025"), 1); Scenarios.ouvrir(x, x.ex.get("2025"), 2);
        EssaisInvariants.ref = "E1";
        long ev = x.ecr("2025", "VT", "2025-01-10", new Object[][]{{"5141", null, m("100.00"), Z},{"6111", null, Z, m("100.00")}});
        Scenarios.avancerPeriodes(x, "2025", 12);
        long p99 = l1(c, "select id from compta_periodes where exercice_id=? and numero=99", x.ex.get("2025"));
        EssaisInvariants.aExtourner = true;
        long inv = EssaisInvariants.ecrire(c, x.soc, x.ex.get("2025"), p99, "OD", LocalDate.of(2025,12,31), "INVENTAIRE", null, new Object[][]{{"3491", null, m("40.00"), Z},{"6111", null, Z, m("40.00")}});
        EssaisInvariants.aExtourner = false;
        Scenarios.cloturer(x, "2025", "2026", LocalDate.of(2026,1,1));
        Scenarios.ouvrir(x, x.ex.get("2026"), 1);
        long pe = x.periodeDe("2026", LocalDate.of(2026,1,1));
        long ext = EssaisInvariants.ecrire(c, x.soc, x.ex.get("2026"), pe, "OD", LocalDate.of(2026,1,1), "COURANTE", inv, new Object[][]{{"3491", null, Z, m("40.00")},{"6111", null, m("40.00"), Z}});
        c.commit();
        egal("extourne : miroir entre deux exercices, sans marque d'expurgation", "f", s1(c, "select est_contrepassation from compta_ecritures where id=?", ext));
        egal("extourne : 6111 reçoit un vrai débit en 2026", "40.00", s1(c, "select sum(debit) from compta_lignes where livre_id=? and exercice_id=? and compte_numero='6111' and not est_contrepassation", x.soc, x.ex.get("2026")));
        egal("extourne : 3491 ressort à zéro", "0.00", s1(c, "select (sum(debit-credit))::numeric(15,2) from compta_lignes where livre_id=? and exercice_id=? and compte_numero='3491'", x.soc, x.ex.get("2026")));
        refuse(c, "miroir d'une écriture courante d'un exercice clos", "compta_contrepassation", xx -> EssaisInvariants.ecrire(xx, x.soc, x.ex.get("2026"), pe, "VT", LocalDate.of(2026,1,2), "COURANTE", ev, new Object[][]{{"5141", null, Z, m("100.00")},{"6111", null, m("100.00"), Z}}));
        // Dotation et sortie : annulables seulement par contre-passation
        long l3 = EssaisInvariants.activer(c, "IMM");
        long e3 = EssaisInvariants.creerExercice(c, l3, "2026", LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), null);
        ouvrir(c, e3, 1); ouvrir(c, e3, 2);
        String c2 = s1(c, "select numero from compta_comptes where livre_id=? and numero like '2332%' and imputable order by numero limit 1", l3);
        String c28 = s1(c, "select numero from compta_comptes where livre_id=? and numero like '2833%' and imputable order by numero limit 1", l3);
        long acq = ecr(c, l3, e3, "OD", "2026-01-05", "ACQ", null, new Object[][]{{c2, null, m("1000.00"), Z},{"5141", null, Z, m("1000.00")}});
        long imm = l1(c, "insert into compta_immobilisations(livre_id, code, designation, compte_immo, compte_amort, compte_dotation, date_acquisition, valeur_brute, cumul_reprise, mode_amortissement, duree_mois, ecriture_acquisition_id, cree_par) values (?,'I1','Machine',?,?,'61933','2026-01-05',1000,400,'LINEAIRE',60,?,'t') returning id", l3, c2, c28, acq);
        long dot = ecr(c, l3, e3, "OD", "2026-01-31", "DOT", null, new Object[][]{{"61933", null, m("100.00"), Z},{c28, null, Z, m("100.00")}});
        String insAmo = "insert into compta_amortissements(livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) values (?,?,?,?,'2026-01-01','2026-01-31',?::numeric,?)";
        long amo = l1(c, insAmo + " returning id", l3, imm, e3, "ANNUELLE", "100", dot);
        refuse(c, "deuxième dotation active sur l'exercice", "uq_compta_amo_active", xx -> ex(xx, insAmo, l3, imm, e3, "ANNUELLE", "1", dot));
        refuse(c, "cumul avec la reprise au-delà de la valeur brute", "compta_immo_suramortie", xx -> ex(xx, insAmo, l3, imm, e3, "COMPLEMENT", "501", dot));
        refuse(c, "annuler une dotation sans contre-passation", "compta_immuable", xx -> ex(xx, "update compta_amortissements set contrepassee=true where id=?", amo));
        contrepasser(c, l3, e3, dot, "2026-01-31");
        ex(c, "update compta_amortissements set contrepassee=true where id=?", amo);
        long dot2 = ecr(c, l3, e3, "OD", "2026-01-31", "DOT2", null, new Object[][]{{"61933", null, m("90.00"), Z},{c28, null, Z, m("90.00")}});
        ex(c, insAmo, l3, imm, e3, "ANNUELLE", "90", dot2);
        vrai("dotation contre-passée puis repassée", true, null);
        long so = ecr(c, l3, e3, "OD", "2026-02-10", "SOR", null, new Object[][]{{c28, null, m("490.00"), Z},{"6513", null, m("510.00"), Z},{c2, null, Z, m("1000.00")}});
        String insSor = "insert into compta_immobilisation_sorties(immobilisation_id, livre_id, nature, date_sortie, amortissements_cumules, valeur_nette, plus_ou_moins_value, ecriture_sortie_id, cree_par) values (?,?,'RETRAIT','2026-02-10',490,510,-510,?,'t')";
        ex(c, insSor, imm, l3, so);
        refuse(c, "deuxième sortie active", "uq_compta_sor_active", xx -> ex(xx, insSor, imm, l3, so));
        refuse(c, "dotation après la sortie", "compta_immo_sortie", xx -> ex(xx, insAmo, l3, imm, e3, "COMPLEMENT", "1", dot2));
        refuse(c, "annuler une sortie sans contre-passation", "compta_immuable", xx -> ex(xx, "update compta_immobilisation_sorties set annulee=true, motif_annulation='x' where immobilisation_id=?", imm));
        contrepasser(c, l3, e3, so, "2026-02-11");
        ex(c, "update compta_immobilisation_sorties set annulee=true, motif_annulation='Erreur de saisie' where immobilisation_id=?", imm);
        ex(c, insSor, imm, l3, so);
        vrai("sortie annulée par contre-passation, puis nouvelle sortie possible", true, null);
        c.commit();
    }
}
