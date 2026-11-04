-- Changeset 060 : le journal. Une ligne de compta_ecritures est validée par définition.
-- Les brouillons vivent ailleurs : aucun état ne peut les lire par accident.

CREATE TABLE compta_sequences_journal (
    livre_id      BIGINT NOT NULL,
    exercice_id     BIGINT NOT NULL,
    journal_id      BIGINT NOT NULL,
    dernier_numero  BIGINT NOT NULL,
    derniere_date   DATE   NOT NULL,
    CONSTRAINT pk_compta_sequences PRIMARY KEY (livre_id, exercice_id, journal_id),
    CONSTRAINT ck_compta_seq_numero CHECK (dernier_numero >= 0),
    CONSTRAINT fk_compta_seq_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_seq_journal FOREIGN KEY (livre_id, journal_id) REFERENCES compta_journaux(livre_id, id) ON DELETE RESTRICT
);

CREATE TABLE compta_ecritures (
    id                     BIGSERIAL PRIMARY KEY,
    livre_id             BIGINT         NOT NULL,
    exercice_id            BIGINT         NOT NULL,
    periode_id             BIGINT         NOT NULL,
    journal_id             BIGINT         NOT NULL,
    type_journal           VARCHAR(12)    NOT NULL,
    numero                 BIGINT         NOT NULL,
    date_ecriture          DATE           NOT NULL,
    date_piece             DATE           NOT NULL,
    ref_piece              VARCHAR(100)   NOT NULL,
    libelle                VARCHAR(255)   NOT NULL,
    nature                 VARCHAR(10)    NOT NULL,
    origine                VARCHAR(16)    NOT NULL,
    est_contrepassation    BOOLEAN        NOT NULL DEFAULT FALSE,
    contrepassation_de_id  BIGINT,
    corrige_ecriture_id    BIGINT,
    motif                  VARCHAR(500),
    a_extourner            BOOLEAN        NOT NULL DEFAULT FALSE,
    nb_lignes              INTEGER        NOT NULL,
    total_debit            NUMERIC(15, 2) NOT NULL,
    total_credit           NUMERIC(15, 2) NOT NULL,
    saisie_le              TIMESTAMP      NOT NULL,
    saisie_par             VARCHAR(150)   NOT NULL,
    valide_le              TIMESTAMP      NOT NULL,
    valide_par             VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_ecr_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_ecr_periode FOREIGN KEY (livre_id, periode_id, exercice_id) REFERENCES compta_periodes(livre_id, id, exercice_id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_ecr_journal FOREIGN KEY (livre_id, journal_id, type_journal) REFERENCES compta_journaux(livre_id, id, type_journal) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_ecr_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_ecr_numero UNIQUE (livre_id, exercice_id, journal_id, numero),
    CONSTRAINT uk_compta_ecr_denorm UNIQUE (livre_id, id, exercice_id, journal_id, date_ecriture, nature, est_contrepassation),
    CONSTRAINT uk_compta_ecr_contrepassation UNIQUE (contrepassation_de_id),
    CONSTRAINT fk_compta_ecr_contrepassation FOREIGN KEY (livre_id, contrepassation_de_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_ecr_corrige FOREIGN KEY (livre_id, corrige_ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_ecr_nature CHECK (nature IN ('COURANTE', 'INVENTAIRE', 'A_NOUVEAU', 'CLOTURE')),
    CONSTRAINT ck_compta_ecr_origine CHECK (origine IN ('SAISIE', 'PASSERELLE', 'OUVERTURE', 'CLOTURE', 'IMMOBILISATION', 'INVENTAIRE', 'TVA', 'IS', 'RAPPROCHEMENT', 'AFFECTATION', 'MODELE')),
    -- Nature et journal se déterminent l'un l'autre : un à-nouveau ou une clôture ne peut pas être mal rangé.
    CONSTRAINT ck_compta_ecr_nature_an CHECK ((nature = 'A_NOUVEAU') = (type_journal = 'A_NOUVEAUX')),
    CONSTRAINT ck_compta_ecr_nature_cl CHECK ((nature = 'CLOTURE') = (type_journal = 'CLOTURE')),
    CONSTRAINT ck_compta_ecr_contrepassation CHECK (NOT est_contrepassation OR contrepassation_de_id IS NOT NULL),
    CONSTRAINT ck_compta_ecr_contrepassation_nature CHECK (contrepassation_de_id IS NULL OR nature IN ('COURANTE', 'INVENTAIRE')),
    CONSTRAINT ck_compta_ecr_une_correction CHECK (contrepassation_de_id IS NULL OR corrige_ecriture_id IS NULL),
    CONSTRAINT ck_compta_ecr_extourne CHECK (NOT a_extourner OR nature = 'INVENTAIRE'),
    CONSTRAINT ck_compta_ecr_piece CHECK (btrim(ref_piece) <> '' AND btrim(libelle) <> ''),
    CONSTRAINT ck_compta_ecr_lignes CHECK (nb_lignes >= 2),
    -- Loi 9-88 art. 2 : au moins deux comptes, sommes égales. L'écriture à zéro est refusée.
    CONSTRAINT ck_compta_ecr_equilibre CHECK (total_debit = total_credit AND total_debit > 0)
);
-- Un seul à-nouveau et une seule clôture par exercice : les à-nouveaux ne peuvent pas doubler.
CREATE UNIQUE INDEX uq_compta_ecr_an ON compta_ecritures(livre_id, exercice_id) WHERE nature = 'A_NOUVEAU';
CREATE UNIQUE INDEX uq_compta_ecr_cl ON compta_ecritures(livre_id, exercice_id) WHERE nature = 'CLOTURE';
-- Une écriture n'est corrigée qu'une fois sur l'exercice ouvert.
CREATE UNIQUE INDEX uq_compta_ecr_corrige ON compta_ecritures(corrige_ecriture_id) WHERE corrige_ecriture_id IS NOT NULL;
CREATE INDEX idx_compta_ecr_piece ON compta_ecritures(livre_id, ref_piece, date_piece);
CREATE INDEX idx_compta_ecr_date ON compta_ecritures(livre_id, exercice_id, date_ecriture, id);

CREATE TABLE compta_lignes (
    id                   BIGSERIAL PRIMARY KEY,
    livre_id           BIGINT         NOT NULL,
    ecriture_id          BIGINT         NOT NULL,
    numero_ligne         INTEGER        NOT NULL,
    exercice_id          BIGINT         NOT NULL,
    journal_id           BIGINT         NOT NULL,
    date_ecriture        DATE           NOT NULL,
    nature_ecriture      VARCHAR(10)    NOT NULL,
    est_contrepassation  BOOLEAN        NOT NULL,
    compte_numero            VARCHAR(12)         NOT NULL,
    tiers_id             BIGINT,
    debit                NUMERIC(15, 2) NOT NULL DEFAULT 0,
    credit               NUMERIC(15, 2) NOT NULL DEFAULT 0,
    libelle              VARCHAR(255),
    ref_piece            VARCHAR(100)   NOT NULL,
    date_piece           DATE           NOT NULL,
    date_echeance        DATE,
    nature_mouvement     VARCHAR(12),
    hors_flux            BOOLEAN        NOT NULL DEFAULT FALSE,
    devise               CHAR(3),
    montant_devise       NUMERIC(15, 2),
    taux_tva             NUMERIC(5, 2),
    base_tva             NUMERIC(15, 2),
    ligne_origine_id     BIGINT,
    point_de_vente_id    BIGINT,
    CONSTRAINT fk_compta_lig_ecriture FOREIGN KEY (livre_id, ecriture_id, exercice_id, journal_id, date_ecriture, nature_ecriture, est_contrepassation)
        REFERENCES compta_ecritures(livre_id, id, exercice_id, journal_id, date_ecriture, nature, est_contrepassation) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_lig_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_lig_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_lig_numero UNIQUE (ecriture_id, numero_ligne),
    CONSTRAINT uk_compta_lig_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_lig_lettrage UNIQUE (livre_id, id, compte_numero, tiers_id),
    CONSTRAINT fk_compta_lig_origine FOREIGN KEY (livre_id, ligne_origine_id) REFERENCES compta_lignes(livre_id, id) ON DELETE RESTRICT,
    -- Un sens, un montant strictement positif. La correction passe par la contre-passation, jamais par un négatif.
    CONSTRAINT ck_compta_lig_sens CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)),
    CONSTRAINT ck_compta_lig_numero CHECK (numero_ligne >= 1),
    -- Règles lisibles sur le numéro : ni déclencheur ni jointure.
    CONSTRAINT ck_compta_lig_compte CHECK (compte_numero ~ '^[1-8][0-9]{3,11}$'),
    CONSTRAINT ck_compta_lig_3455 CHECK (compte_numero <> '3455'),
    CONSTRAINT ck_compta_lig_tiers CHECK (tiers_id IS NULL OR compte_numero ~ '^[34]'),
    CONSTRAINT ck_compta_lig_an_classe CHECK (nature_ecriture <> 'A_NOUVEAU' OR compte_numero ~ '^[1-5]'),
    CONSTRAINT ck_compta_lig_cl_classe CHECK (nature_ecriture <> 'CLOTURE' OR compte_numero ~ '^([678]|119)'),
    CONSTRAINT ck_compta_lig_classe_8 CHECK (compte_numero !~ '^8' OR nature_ecriture = 'CLOTURE'),
    CONSTRAINT ck_compta_lig_mouvement CHECK (nature_mouvement IS NULL OR nature_mouvement IN ('ACQUISITION', 'PRODUCTION', 'VIREMENT', 'CESSION', 'RETRAIT', 'AFFECTATION')),
    CONSTRAINT ck_compta_lig_devise CHECK ((devise IS NULL) = (montant_devise IS NULL))
);
-- Balance et états : parcours d'index seul sur l'exercice.
CREATE INDEX idx_compta_lig_balance ON compta_lignes(livre_id, exercice_id, compte_numero, tiers_id)
    INCLUDE (debit, credit, nature_ecriture, date_ecriture, est_contrepassation);
-- Grand livre par compte, pagination par curseur.
CREATE INDEX idx_compta_lig_grand_livre ON compta_lignes(livre_id, exercice_id, compte_numero, date_ecriture, id);
-- Grand livre et lettrage par tiers.
CREATE INDEX idx_compta_lig_tiers ON compta_lignes(livre_id, tiers_id, compte_numero, date_ecriture, id) WHERE tiers_id IS NOT NULL;
-- Clôture : retrouver la ligne d'à-nouveau d'une ligne ouverte.
CREATE INDEX idx_compta_lig_origine ON compta_lignes(ligne_origine_id) WHERE ligne_origine_id IS NOT NULL;
-- Tableaux T4, T14, TF : seules les lignes marquées.
CREATE INDEX idx_compta_lig_marquees ON compta_lignes(livre_id, exercice_id, compte_numero, nature_mouvement, hors_flux)
    INCLUDE (debit, credit, est_contrepassation, date_ecriture, nature_ecriture)
    WHERE nature_mouvement IS NOT NULL OR hors_flux;

-- Avant insertion d'une écriture : période ouverte, cycle de l'exercice, contre-passation, numéro.
-- Le numéro n'est jamais fourni par l'application : le verrou de ligne de l'upsert tient jusqu'au commit,
-- un rollback rend le numéro. Pas de trou, pas de doublon (avis CNC n° 24, annexe 2.2).
CREATE FUNCTION compta_ecritures_avant_insert() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_per   RECORD;
    v_ex    VARCHAR(8);
    v_orig  RECORD;
    v_num   BIGINT;
    v_der   DATE;
BEGIN
    SELECT statut, type_periode, date_debut, date_fin INTO v_per
      FROM compta_periodes
     WHERE id = NEW.periode_id AND livre_id = NEW.livre_id AND exercice_id = NEW.exercice_id
       FOR SHARE;
    IF NOT FOUND THEN
        PERFORM compta_refus('compta_periode_inconnue', NULL);
    END IF;
    IF v_per.statut <> 'OUVERTE' THEN
        PERFORM compta_refus('compta_periode_fermee', format('statut %s', v_per.statut));
    END IF;
    IF NEW.date_ecriture NOT BETWEEN v_per.date_debut AND v_per.date_fin THEN
        PERFORM compta_refus('compta_date_hors_periode', format('%s hors de %s à %s', NEW.date_ecriture, v_per.date_debut, v_per.date_fin));
    END IF;
    IF NOT ((NEW.nature = 'A_NOUVEAU' AND v_per.type_periode = 'OUVERTURE')
         OR (NEW.nature IN ('INVENTAIRE', 'CLOTURE') AND v_per.type_periode = 'CLOTURE')
         OR (NEW.nature = 'COURANTE' AND v_per.type_periode = 'COURANTE')) THEN
        PERFORM compta_refus('compta_nature_periode', format('%s dans une période %s', NEW.nature, v_per.type_periode));
    END IF;
    SELECT statut INTO v_ex FROM compta_exercices WHERE id = NEW.exercice_id AND livre_id = NEW.livre_id FOR SHARE;
    IF v_ex = 'CLOTURE' THEN
        PERFORM compta_refus('compta_exercice_clos', NULL);
    END IF;
    IF (v_ex = 'ARRETE') <> (NEW.nature = 'CLOTURE') THEN
        PERFORM compta_refus('compta_exercice_arrete', format('exercice %s, nature %s', v_ex, NEW.nature));
    END IF;
    -- À-nouveau : reprise libre sur le premier exercice, sinon seulement pendant la clôture de l'exercice précédent.
    IF NEW.nature = 'A_NOUVEAU' AND EXISTS (
        SELECT 1 FROM compta_exercices e JOIN compta_exercices q ON q.id = e.precedent_id
         WHERE e.id = NEW.exercice_id AND q.statut <> 'ARRETE') THEN
        PERFORM compta_refus('compta_an_premature', 'l''exercice précédent n''est pas en cours de clôture');
    END IF;
    -- est_contrepassation est calculé ici : vrai dans le même exercice (mouvements expurgés), faux pour une extourne.
    NEW.est_contrepassation := FALSE;
    IF NEW.contrepassation_de_id IS NOT NULL THEN
        SELECT exercice_id, journal_id, nature, est_contrepassation, a_extourner INTO v_orig
          FROM compta_ecritures WHERE id = NEW.contrepassation_de_id AND livre_id = NEW.livre_id;
        IF v_orig.est_contrepassation THEN
            PERFORM compta_refus('compta_contrepassation', 'une contre-passation ne se contre-passe pas');
        END IF;
        IF v_orig.journal_id <> NEW.journal_id THEN
            PERFORM compta_refus('compta_contrepassation', 'même journal exigé');
        END IF;
        IF v_orig.nature NOT IN ('COURANTE', 'INVENTAIRE') THEN
            PERFORM compta_refus('compta_contrepassation', format('nature %s non contre-passable', v_orig.nature));
        END IF;
        -- Entre deux exercices, seule l'extourne d'une écriture d'inventaire est un miroir.
        IF v_orig.exercice_id <> NEW.exercice_id AND NOT v_orig.a_extourner THEN
            PERFORM compta_refus('compta_contrepassation', 'exercice différent : passer une écriture de correction');
        END IF;
        NEW.est_contrepassation := v_orig.exercice_id = NEW.exercice_id;
    END IF;
    -- Numéro attribué sous verrou de ligne : le verrou tient jusqu'au commit, un rollback rend le numéro.
    INSERT INTO compta_sequences_journal (livre_id, exercice_id, journal_id, dernier_numero, derniere_date)
    VALUES (NEW.livre_id, NEW.exercice_id, NEW.journal_id, 0, NEW.date_ecriture)
    ON CONFLICT (livre_id, exercice_id, journal_id) DO NOTHING;
    SELECT dernier_numero, derniere_date INTO v_num, v_der
      FROM compta_sequences_journal
     WHERE livre_id = NEW.livre_id AND exercice_id = NEW.exercice_id AND journal_id = NEW.journal_id
       FOR UPDATE;
    -- Chronologie : un folio mensuel d'un journal ne se rouvre pas une fois le mois suivant entamé dans ce journal.
    IF date_trunc('month', NEW.date_ecriture) < date_trunc('month', v_der) THEN
        PERFORM compta_refus('compta_chronologie_journal', format('%s antérieure au mois de la dernière écriture du journal (%s)', NEW.date_ecriture, v_der));
    END IF;
    UPDATE compta_sequences_journal
       SET dernier_numero = v_num + 1, derniere_date = GREATEST(v_der, NEW.date_ecriture)
     WHERE livre_id = NEW.livre_id AND exercice_id = NEW.exercice_id AND journal_id = NEW.journal_id;
    NEW.numero := v_num + 1;
    -- Pris après le verrou : à l'intérieur d'un journal, le numéro et l'horodatage croissent ensemble.
    NEW.valide_le := clock_timestamp();
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_ecritures_avant_insert BEFORE INSERT ON compta_ecritures
    FOR EACH ROW EXECUTE FUNCTION compta_ecritures_avant_insert();

-- Au commit : toutes les lignes annoncées sont là, les totaux sont exacts, une contre-passation est un miroir.
CREATE FUNCTION compta_ecritures_scellement() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_nb INTEGER;
    v_d  NUMERIC;
    v_c  NUMERIC;
BEGIN
    SELECT count(*), COALESCE(sum(debit), 0), COALESCE(sum(credit), 0) INTO v_nb, v_d, v_c
      FROM compta_lignes WHERE ecriture_id = NEW.id;
    IF v_nb <> NEW.nb_lignes OR v_d <> NEW.total_debit OR v_c <> NEW.total_credit THEN
        PERFORM compta_refus('compta_ecriture_incoherente', format('écriture %s, %s lignes, débit %s, crédit %s', NEW.id, v_nb, v_d, v_c));
    END IF;
    IF NEW.contrepassation_de_id IS NOT NULL AND EXISTS (
        (SELECT compte_numero, tiers_id, sum(debit), sum(credit) FROM compta_lignes WHERE ecriture_id = NEW.id GROUP BY 1, 2)
        EXCEPT
        (SELECT compte_numero, tiers_id, sum(credit), sum(debit) FROM compta_lignes WHERE ecriture_id = NEW.contrepassation_de_id GROUP BY 1, 2)) THEN
        PERFORM compta_refus('compta_contrepassation_non_miroir', format('écriture %s', NEW.id));
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER trg_compta_ecritures_scellement AFTER INSERT ON compta_ecritures
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION compta_ecritures_scellement();

-- Avant insertion d'une ligne : case prévue par l'en-tête, compte imputable, tiers présent si le compte l'exige.
-- nb_lignes + UNIQUE(ecriture_id, numero_ligne) : aucune ligne ne peut s'ajouter à une écriture déjà scellée.
CREATE FUNCTION compta_lignes_avant_insert() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_nb  INTEGER;
    v_c   RECORD;
BEGIN
    SELECT nb_lignes INTO v_nb FROM compta_ecritures WHERE id = NEW.ecriture_id;
    IF NEW.numero_ligne > v_nb THEN
        PERFORM compta_refus('compta_ecriture_scellee', format('ligne %s pour %s annoncées', NEW.numero_ligne, v_nb));
    END IF;
    SELECT imputable, actif, exige_tiers INTO v_c
      FROM compta_comptes WHERE livre_id = NEW.livre_id AND numero = NEW.compte_numero;
    IF NEW.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND (NOT v_c.imputable OR NOT v_c.actif) THEN
        PERFORM compta_refus('compta_compte_non_imputable', NEW.compte_numero);
    END IF;
    IF v_c.exige_tiers AND NEW.tiers_id IS NULL THEN
        PERFORM compta_refus('compta_tiers_obligatoire', NEW.compte_numero);
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_lignes_avant_insert BEFORE INSERT ON compta_lignes
    FOR EACH ROW EXECUTE FUNCTION compta_lignes_avant_insert();

-- Immuabilité : la même fonction sert à toutes les tables en ajout seul.
CREATE FUNCTION compta_interdire_modification() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM compta_refus('compta_immuable', format('%s interdit sur %s', TG_OP, TG_TABLE_NAME));
    RETURN NULL;
END $$;
CREATE TRIGGER trg_compta_ecritures_immuables BEFORE UPDATE OR DELETE ON compta_ecritures
    FOR EACH ROW EXECUTE FUNCTION compta_interdire_modification();
CREATE TRIGGER trg_compta_ecritures_truncate BEFORE TRUNCATE ON compta_ecritures
    FOR EACH STATEMENT EXECUTE FUNCTION compta_interdire_modification();
CREATE TRIGGER trg_compta_lignes_immuables BEFORE UPDATE OR DELETE ON compta_lignes
    FOR EACH ROW EXECUTE FUNCTION compta_interdire_modification();
CREATE TRIGGER trg_compta_lignes_truncate BEFORE TRUNCATE ON compta_lignes
    FOR EACH STATEMENT EXECUTE FUNCTION compta_interdire_modification();

-- Les brouillons : modifiables, déséquilibrés tant qu'on veut, sans numéro, sans effet sur aucun état.
CREATE TABLE compta_brouillons (
    id             BIGSERIAL PRIMARY KEY,
    livre_id     BIGINT       NOT NULL,
    journal_id     BIGINT       NOT NULL,
    date_ecriture  DATE         NOT NULL,
    date_piece     DATE,
    ref_piece      VARCHAR(100),
    libelle        VARCHAR(255),
    nature         VARCHAR(10)  NOT NULL DEFAULT 'COURANTE',
    motif          VARCHAR(500),
    cree_le        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cree_par       VARCHAR(150) NOT NULL,
    modifie_le     TIMESTAMP,
    CONSTRAINT fk_compta_bro_journal FOREIGN KEY (livre_id, journal_id) REFERENCES compta_journaux(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_bro_livre_id UNIQUE (livre_id, id),
    CONSTRAINT ck_compta_bro_nature CHECK (nature IN ('COURANTE', 'INVENTAIRE', 'A_NOUVEAU'))
);
CREATE INDEX idx_compta_bro_date ON compta_brouillons(livre_id, date_ecriture);

CREATE TABLE compta_brouillon_lignes (
    id                BIGSERIAL PRIMARY KEY,
    livre_id        BIGINT         NOT NULL,
    brouillon_id      BIGINT         NOT NULL,
    numero_ligne      INTEGER        NOT NULL,
    compte_numero         VARCHAR(12),
    tiers_id          BIGINT,
    debit             NUMERIC(15, 2) NOT NULL DEFAULT 0,
    credit            NUMERIC(15, 2) NOT NULL DEFAULT 0,
    libelle           VARCHAR(255),
    date_echeance     DATE,
    nature_mouvement  VARCHAR(12),
    hors_flux         BOOLEAN        NOT NULL DEFAULT FALSE,
    devise            CHAR(3),
    montant_devise    NUMERIC(15, 2),
    taux_tva          NUMERIC(5, 2),
    base_tva          NUMERIC(15, 2),
    CONSTRAINT fk_compta_brl_brouillon FOREIGN KEY (livre_id, brouillon_id) REFERENCES compta_brouillons(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_compta_brl_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_brl_tiers FOREIGN KEY (livre_id, tiers_id) REFERENCES compta_tiers(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_brl_numero UNIQUE (brouillon_id, numero_ligne),
    CONSTRAINT ck_compta_brl_montants CHECK (debit >= 0 AND credit >= 0 AND NOT (debit > 0 AND credit > 0))
);

-- Pièces jointes : table neuve, la clé fk_documents_comptables_ecriture reste sur l'ancienne table.
CREATE TABLE compta_pieces_jointes (
    livre_id   BIGINT       NOT NULL,
    ecriture_id  BIGINT       NOT NULL,
    document_id  BIGINT       NOT NULL,
    lie_le       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lie_par      VARCHAR(150) NOT NULL,
    CONSTRAINT pk_compta_pj PRIMARY KEY (ecriture_id, document_id),
    CONSTRAINT fk_compta_pj_ecriture FOREIGN KEY (livre_id, ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    -- Une pièce rattachée à une écriture validée ne se supprime plus : conservation de 10 ans (CGI art. 211).
    CONSTRAINT fk_compta_pj_document FOREIGN KEY (document_id) REFERENCES documents_comptables(id) ON DELETE RESTRICT
);
