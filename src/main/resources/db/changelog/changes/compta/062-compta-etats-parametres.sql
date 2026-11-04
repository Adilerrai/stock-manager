-- Changeset 062 : états figés, reprises N-1, archives, contrôles, paramètres fiscaux datés.

-- Un exercice arrêté ou clos relit ses états ici, jamais un recalcul (CGNC : mention N-1).
CREATE TABLE compta_etats_figes (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT       NOT NULL,
    exercice_id        BIGINT       NOT NULL,
    figeage            INTEGER      NOT NULL,
    code_etat          VARCHAR(30)  NOT NULL,
    version_definition VARCHAR(20)  NOT NULL,
    contenu            JSONB        NOT NULL,
    sha256             CHAR(64)     NOT NULL,
    fige_le            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fige_par           VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_fig_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_fig UNIQUE (livre_id, exercice_id, figeage, code_etat)
);
CREATE TRIGGER trg_compta_etats_figes_immuables BEFORE UPDATE OR DELETE ON compta_etats_figes
    FOR EACH ROW EXECUTE FUNCTION compta_interdire_modification();

-- Balance d'un exercice absent de l'application : colonnes N-1 et N-2 calculées par les mêmes formules, jamais estimées.
CREATE TABLE compta_reprises_soldes (
    id             BIGSERIAL      PRIMARY KEY,
    livre_id       BIGINT         NOT NULL,
    date_debut     DATE           NOT NULL,
    date_fin       DATE           NOT NULL,
    compte_numero  VARCHAR(12)    NOT NULL,
    solde_debit    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    solde_credit   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    saisie_le      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saisie_par     VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_rep_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_rep UNIQUE (livre_id, date_fin, compte_numero),
    CONSTRAINT ck_compta_rep_soldes CHECK (solde_debit >= 0 AND solde_credit >= 0),
    CONSTRAINT ck_compta_rep_dates CHECK (date_fin >= date_debut)
);

-- Archive de clôture (avis n° 24, annexe 2.9). Le journal étant immuable, le FEC se régénère à l'identique :
-- on garde l'empreinte, et le contenu seulement pour ce qui ne se régénère pas.
CREATE TABLE compta_archives_cloture (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT       NOT NULL,
    exercice_id  BIGINT       NOT NULL,
    type_archive VARCHAR(20)  NOT NULL,
    nom_fichier  VARCHAR(255) NOT NULL,
    sha256       CHAR(64)     NOT NULL,
    nb_lignes    BIGINT,
    contenu      BYTEA,
    genere_le    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    genere_par   VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_arc_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_arc UNIQUE (livre_id, exercice_id, type_archive),
    CONSTRAINT ck_compta_arc_type CHECK (type_archive IN ('FEC', 'JOURNAL', 'GRAND_LIVRE', 'BALANCE', 'ETATS', 'LIASSE'))
);
CREATE TRIGGER trg_compta_archives_immuables BEFORE UPDATE OR DELETE ON compta_archives_cloture
    FOR EACH ROW EXECUTE FUNCTION compta_interdire_modification();

-- Rapport de contrôle rejouable (avis n° 24, annexe 2.5 et 2.2 b). Un contrôle non exécuté n'a pas de ligne.
CREATE TABLE compta_controles (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT       NOT NULL,
    exercice_id  BIGINT       NOT NULL,
    execution    UUID         NOT NULL,
    code         VARCHAR(40)  NOT NULL,
    gravite      VARCHAR(20)  NOT NULL,
    reussi       BOOLEAN      NOT NULL,
    valeur_a     NUMERIC(18, 2),
    valeur_b     NUMERIC(18, 2),
    message      VARCHAR(500),
    execute_le   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execute_par  VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_ctl_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_ctl_gravite CHECK (gravite IN ('BLOQUANT', 'BLOQUE_CLOTURE', 'AVERTISSEMENT'))
);
CREATE INDEX idx_compta_ctl_exercice ON compta_controles(livre_id, exercice_id, execute_le DESC);

-- Paramètres dépendant de l'année : valeur, date d'effet, source. Aucun taux dans le code.
CREATE TABLE compta_parametres_fiscaux (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40)    NOT NULL,
    cle           VARCHAR(40)    NOT NULL DEFAULT '',
    rattachement  VARCHAR(20)    NOT NULL,
    valide_du     DATE           NOT NULL,
    valide_au     DATE,
    valeur        NUMERIC(18, 6) NOT NULL,
    borne_min     NUMERIC(18, 2),
    borne_max     NUMERIC(18, 2),
    source        VARCHAR(255)   NOT NULL,
    fiabilite     VARCHAR(10)    NOT NULL DEFAULT 'VERIFIE',
    CONSTRAINT uk_compta_par UNIQUE (code, cle, valide_du),
    CONSTRAINT ck_compta_par_dates CHECK (valide_au IS NULL OR valide_au >= valide_du),
    CONSTRAINT ck_compta_par_rattachement CHECK (rattachement IN ('OUVERTURE_EXERCICE', 'DATE_OPERATION', 'DATE_EMISSION', 'DATE_PAIEMENT', 'DEBUT_RETARD', 'ANNEE')),
    CONSTRAINT ck_compta_par_fiabilite CHECK (fiabilite IN ('VERIFIE', 'SECONDAIRE', 'A_VERIFIER'))
);
CREATE FUNCTION compta_parametres_sans_chevauchement() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM compta_parametres_fiscaux p
                WHERE p.code = NEW.code AND p.cle = NEW.cle AND p.id <> NEW.id
                  AND daterange(p.valide_du, p.valide_au, '[]') && daterange(NEW.valide_du, NEW.valide_au, '[]')) THEN
        PERFORM compta_refus('compta_parametre_chevauchement', format('%s, %s, %s', NEW.code, NEW.cle, NEW.valide_du));
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER trg_compta_parametres_chevauchement AFTER INSERT OR UPDATE ON compta_parametres_fiscaux
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION compta_parametres_sans_chevauchement();

INSERT INTO compta_parametres_fiscaux (code, cle, rattachement, valide_du, valide_au, valeur, borne_min, borne_max, source, fiabilite)
SELECT 'IS_TAUX', v.cle, 'OUVERTURE_EXERCICE', d.du, d.au, v.t[d.i], v.bmin, v.bmax,
       'CGI art. 247-XXXVII-A, art. 19-I ; NC 733 p. 13', 'VERIFIE'
  FROM (VALUES
        ('BN_JUSQUA_300K',       ARRAY[12.50, 15.00, 17.50, 20.00], 0::NUMERIC,    300000::NUMERIC),
        ('BN_300K_A_1M',         ARRAY[20.00, 20.00, 20.00, 20.00], 300000.01,     1000000),
        ('BN_1M_A_100M',         ARRAY[28.25, 25.50, 22.75, 20.00], 1000000.01,    99999999.99),
        ('BN_100M_ET_PLUS',      ARRAY[32.00, 33.00, 34.00, 35.00], 100000000,     NULL),
        ('INDUSTRIE_MOINS_100M', ARRAY[24.50, 23.00, 21.50, 20.00], 0,             99999999.99),
        ('INDUSTRIE_100M_PLUS',  ARRAY[32.00, 33.00, 34.00, 35.00], 100000000,     NULL),
        ('PLAFONNE_1M_A_100M',   ARRAY[20.00, 20.00, 20.00, 20.00], 1000000.01,    99999999.99),
        ('PLAFONNE_100M_PLUS',   ARRAY[23.75, 27.50, 31.25, 35.00], 100000000,     NULL),
        ('CFC_ZAI',              ARRAY[16.25, 17.50, 18.75, 20.00], 0,             NULL),
        ('CONVENTION_1_5_MMDH',  ARRAY[20.00, 20.00, 20.00, 20.00], 0,             NULL),
        ('BANQUES_ASSURANCES',   ARRAY[37.75, 38.50, 39.25, 40.00], 0,             NULL)
       ) AS v(cle, t, bmin, bmax)
 CROSS JOIN (VALUES (1, DATE '2023-01-01', DATE '2023-12-31'),
                    (2, DATE '2024-01-01', DATE '2024-12-31'),
                    (3, DATE '2025-01-01', DATE '2025-12-31'),
                    (4, DATE '2026-01-01', NULL::DATE)) AS d(i, du, au);

INSERT INTO compta_parametres_fiscaux (code, cle, rattachement, valide_du, valide_au, valeur, borne_min, borne_max, source, fiabilite) VALUES
 ('AMORT_DEGRESSIF_COEF', 'DUREE_3_A_4_ANS', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 1.5, 3, 4, 'CGI art. 10', 'VERIFIE'),
 ('AMORT_DEGRESSIF_COEF', 'DUREE_5_A_6_ANS', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 2, 5, 6, 'CGI art. 10', 'VERIFIE'),
 ('AMORT_DEGRESSIF_COEF', 'DUREE_PLUS_6_ANS', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 3, 7, NULL, 'CGI art. 10', 'VERIFIE'),
 ('TVA_TAUX', '20.00', 'DATE_OPERATION', '2023-01-01', NULL, 20, NULL, NULL, 'CGI art. 99-A', 'VERIFIE'),
 ('TVA_TAUX', '10.00', 'DATE_OPERATION', '2023-01-01', NULL, 10, NULL, NULL, 'CGI art. 99-B', 'VERIFIE'),
 ('TVA_TAUX', '14.00', 'DATE_OPERATION', '2023-01-01', '2023-12-31', 14, NULL, NULL, 'NC 735 ; art. 99 version 2023 non lu', 'A_VERIFIER'),
 ('TVA_TAUX', '7.00',  'DATE_OPERATION', '2023-01-01', '2023-12-31', 7,  NULL, NULL, 'NC 735 ; art. 99 version 2023 non lu', 'A_VERIFIER'),
 ('TVA_TAUX', '16.00', 'DATE_OPERATION', '2024-01-01', '2024-12-31', 16, NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '13.00', 'DATE_OPERATION', '2024-01-01', '2024-12-31', 13, NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '12.00', 'DATE_OPERATION', '2024-01-01', '2025-12-31', 12, NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '11.00', 'DATE_OPERATION', '2024-01-01', '2024-12-31', 11, NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '8.00',  'DATE_OPERATION', '2024-01-01', '2024-12-31', 8,  NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '18.00', 'DATE_OPERATION', '2025-01-01', '2025-12-31', 18, NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '15.00', 'DATE_OPERATION', '2025-01-01', '2025-12-31', 15, NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('TVA_TAUX', '9.00',  'DATE_OPERATION', '2025-01-01', '2025-12-31', 9,  NULL, NULL, 'CGI art. 247-XXXXI ; guide RAS-TVA p. 5', 'VERIFIE'),
 ('CM_TAUX', 'GENERAL',          'OUVERTURE_EXERCICE', '2023-01-01', NULL, 0.25, NULL, NULL, 'CGI art. 144-I-D ; NC 733 p. 77', 'VERIFIE'),
 ('CM_TAUX', 'PRODUITS_DE_BASE', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 0.15, NULL, NULL, 'CGI art. 144-I-D ; NC 733 p. 77', 'VERIFIE'),
 ('CM_MINIMUM', 'SOCIETE',       'OUVERTURE_EXERCICE', '2023-01-01', NULL, 3000, NULL, NULL, 'CGI art. 144-I-D', 'VERIFIE'),
 ('CM_EXONERATION_MOIS', 'EXPLOITATION', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 36, NULL, NULL, 'CGI art. 144-I-C-1°', 'VERIFIE'),
 ('CM_EXONERATION_MOIS', 'CONSTITUTION', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 60, NULL, NULL, 'CGI art. 144-I-C-1°', 'VERIFIE'),
 ('IS_ACOMPTE_PART', '',         'OUVERTURE_EXERCICE', '2023-01-01', NULL, 25, NULL, NULL, 'CGI art. 170-I, art. 247-XXXVII-B', 'VERIFIE'),
 ('IS_REPORT_DEFICIT_EXERCICES', '', 'OUVERTURE_EXERCICE', '2023-01-01', NULL, 4, NULL, NULL, 'CGI art. 12', 'VERIFIE'),
 ('CSS_TAUX', 'T1', 'OUVERTURE_EXERCICE', '2023-01-01', '2028-12-31', 1.5, 1000000, 4999999.99, 'CGI art. 268, 269, 273', 'VERIFIE'),
 ('CSS_TAUX', 'T2', 'OUVERTURE_EXERCICE', '2023-01-01', '2028-12-31', 2.5, 5000000, 9999999.99, 'CGI art. 268, 269, 273', 'VERIFIE'),
 ('CSS_TAUX', 'T3', 'OUVERTURE_EXERCICE', '2023-01-01', '2028-12-31', 3.5, 10000000, 39999999.99, 'CGI art. 268, 269, 273', 'VERIFIE'),
 ('CSS_TAUX', 'T4', 'OUVERTURE_EXERCICE', '2023-01-01', '2028-12-31', 5.0, 40000000, NULL, 'CGI art. 268, 269, 273', 'VERIFIE'),
 ('TVA_SEUIL_MENSUEL', '',       'ANNEE', '2023-01-01', NULL, 1000000, NULL, NULL, 'CGI art. 108', 'VERIFIE'),
 ('TVA_ESPECES_PLAFOND', 'JOUR', 'DATE_PAIEMENT', '2023-01-01', NULL, 5000, NULL, NULL, 'CGI art. 106-II', 'VERIFIE'),
 ('TVA_ESPECES_PLAFOND', 'MOIS', 'DATE_PAIEMENT', '2023-01-01', NULL, 50000, NULL, NULL, 'CGI art. 106-II', 'VERIFIE'),
 ('ARRONDI_BASE', '',            'DATE_OPERATION', '2023-01-01', NULL, 10, NULL, NULL, 'CGI art. 167', 'VERIFIE'),
 ('ARRONDI_VERSEMENT', '',       'DATE_OPERATION', '2023-01-01', NULL, 1, NULL, NULL, 'CGI art. 167', 'VERIFIE'),
 ('DP_DELAI_JOURS', 'SANS_ACCORD', 'DATE_EMISSION', '2023-07-01', NULL, 60, NULL, NULL, 'C. com. art. 78-2 ; NC 734 p. 2', 'VERIFIE'),
 ('DP_DELAI_JOURS', 'CONVENU_MAX', 'DATE_EMISSION', '2023-07-01', NULL, 120, NULL, NULL, 'C. com. art. 78-2 ; NC 734 p. 2', 'VERIFIE'),
 ('DP_TAUX_MOIS_SUIVANT', '',    'DEBUT_RETARD', '2023-07-01', NULL, 0.85, NULL, NULL, 'C. com. art. 78-3 ; NC 734 p. 3', 'VERIFIE'),
 ('DP_TAUX_DIRECTEUR', '',       'DEBUT_RETARD', '2023-07-01', '2024-05-31', 3.00, NULL, NULL, 'bkam.ma ; exemple NC 734', 'VERIFIE'),
 ('DP_TAUX_DIRECTEUR', '',       'DEBUT_RETARD', '2024-06-01', '2024-11-30', 2.75, NULL, NULL, 'bkam.ma ; date d''effet DGI par la presse', 'SECONDAIRE'),
 ('DP_TAUX_DIRECTEUR', '',       'DEBUT_RETARD', '2024-12-01', '2025-02-28', 2.50, NULL, NULL, 'bkam.ma ; date d''effet DGI par la presse', 'SECONDAIRE'),
 ('DP_TAUX_DIRECTEUR', '',       'DEBUT_RETARD', '2025-03-01', NULL, 2.25, NULL, NULL, 'bkam.ma ; date d''effet déduite, sans communiqué DGI lu', 'A_VERIFIER');
