-- Changeset 067 : IS, déficits, liasse et états de l'ETIC.

CREATE TABLE compta_is_calculs (
    id                     BIGSERIAL PRIMARY KEY,
    livre_id             BIGINT         NOT NULL,
    exercice_id            BIGINT         NOT NULL,
    categorie_taux         VARCHAR(40)    NOT NULL,
    resultat_comptable     NUMERIC(15, 2) NOT NULL,
    reintegrations         NUMERIC(15, 2) NOT NULL DEFAULT 0,
    deductions             NUMERIC(15, 2) NOT NULL DEFAULT 0,
    deficits_imputes       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    resultat_fiscal        NUMERIC(15, 2) NOT NULL,
    base_arrondie          NUMERIC(15, 2) NOT NULL,
    taux_is                NUMERIC(6, 3)  NOT NULL,
    is_theorique           NUMERIC(15, 2) NOT NULL,
    base_cm                NUMERIC(15, 2) NOT NULL,
    cm                     NUMERIC(15, 2) NOT NULL,
    cm_exoneree            BOOLEAN        NOT NULL DEFAULT FALSE,
    impot_du               NUMERIC(15, 2) NOT NULL,
    acomptes_verses        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    excedent_anterieur     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    reliquat               NUMERIC(15, 2) NOT NULL DEFAULT 0,
    excedent               NUMERIC(15, 2) NOT NULL DEFAULT 0,
    css                    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    parametres             JSONB          NOT NULL,
    statut                 VARCHAR(10)    NOT NULL DEFAULT 'BROUILLON',
    ecriture_charge_id     BIGINT,
    ecriture_liquidation_id BIGINT,
    valide_le              TIMESTAMP,
    valide_par             VARCHAR(150),
    CONSTRAINT fk_compta_isc_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_isc_ecriture FOREIGN KEY (livre_id, ecriture_charge_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_isc_liquidation FOREIGN KEY (livre_id, ecriture_liquidation_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_isc UNIQUE (livre_id, exercice_id),
    -- CGI art. 144 : l'impôt exigible est le plus élevé des deux, aucun crédit de CM sur l'IS suivant.
    CONSTRAINT ck_compta_isc_max CHECK (impot_du = GREATEST(is_theorique, cm)),
    CONSTRAINT ck_compta_isc_statut CHECK (statut IN ('BROUILLON', 'VALIDE'))
);

CREATE TABLE compta_is_acomptes (
    id            BIGSERIAL PRIMARY KEY,
    livre_id    BIGINT         NOT NULL,
    exercice_id   BIGINT         NOT NULL,
    numero        SMALLINT       NOT NULL,
    echeance      DATE           NOT NULL,
    montant_du    NUMERIC(15, 2) NOT NULL,
    montant_paye  NUMERIC(15, 2),
    date_paiement DATE,
    reference     VARCHAR(100),
    ecriture_id   BIGINT,
    CONSTRAINT fk_compta_isa_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_isa_ecriture FOREIGN KEY (livre_id, ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_isa UNIQUE (livre_id, exercice_id, numero),
    CONSTRAINT ck_compta_isa_numero CHECK (numero BETWEEN 1 AND 4)
);

CREATE TABLE compta_deficits (
    id                   BIGSERIAL PRIMARY KEY,
    livre_id           BIGINT         NOT NULL,
    exercice_origine_id  BIGINT,
    annee_origine        INTEGER        NOT NULL,
    nature               VARCHAR(14)    NOT NULL,
    montant_initial      NUMERIC(15, 2) NOT NULL,
    montant_restant      NUMERIC(15, 2) NOT NULL,
    saisi                BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_compta_def_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_def_exercice FOREIGN KEY (livre_id, exercice_origine_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_def UNIQUE (livre_id, annee_origine, nature),
    CONSTRAINT ck_compta_def_nature CHECK (nature IN ('HORS_AMORT', 'AMORTISSEMENTS')),
    CONSTRAINT ck_compta_def_montants CHECK (montant_restant >= 0 AND montant_restant <= montant_initial)
);

-- Cellules saisies de la liasse (catégories D et E de la section 10.7).
CREATE TABLE compta_liasse_saisies (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT         NOT NULL,
    exercice_id  BIGINT         NOT NULL,
    tableau      VARCHAR(12)    NOT NULL,
    code_ligne   VARCHAR(40)    NOT NULL,
    colonne      VARCHAR(30)    NOT NULL,
    valeur_num   NUMERIC(15, 2),
    valeur_txt   TEXT,
    modifie_le   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par  VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_lis_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_lis UNIQUE (livre_id, exercice_id, tableau, code_ligne, colonne)
);

-- Registres répétables : T7, T11, T13, T15, T17, T18, T19, B8, B9. Le schéma d'une ligne vient de etats_cgnc.json.
-- Vide tant que rien n'est saisi : capital, titres et crédit-bail ne s'inventent pas.
CREATE TABLE compta_liasse_registres (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT       NOT NULL,
    exercice_id  BIGINT       NOT NULL,
    registre     VARCHAR(12)  NOT NULL,
    numero       INTEGER      NOT NULL,
    donnees      JSONB        NOT NULL,
    modifie_le   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par  VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_lir_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_lir UNIQUE (livre_id, exercice_id, registre, numero)
);

CREATE TABLE compta_liasses (
    id            BIGSERIAL PRIMARY KEY,
    livre_id    BIGINT       NOT NULL,
    exercice_id   BIGINT       NOT NULL,
    version       INTEGER      NOT NULL DEFAULT 1,
    modele_edi    VARCHAR(20)  NOT NULL,
    statut        VARCHAR(10)  NOT NULL DEFAULT 'BROUILLON',
    figeage       INTEGER      NOT NULL,
    xml_sha256    CHAR(64),
    xml_contenu   BYTEA,
    non_valide_edi BOOLEAN     NOT NULL DEFAULT TRUE,
    valide_le     TIMESTAMP,
    valide_par    VARCHAR(150),
    depose_le     DATE,
    numero_recu   VARCHAR(60),
    CONSTRAINT fk_compta_lia_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_lia UNIQUE (livre_id, exercice_id, version),
    CONSTRAINT ck_compta_lia_statut CHECK (statut IN ('BROUILLON', 'VALIDEE', 'DEPOSEE'))
);
