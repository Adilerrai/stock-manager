-- Changeset 063 : la passerelle. Aucune clé étrangère vers une table commerciale :
-- une dépense supprimée ou un relevé effacé continue de fonctionner côté commercial et se signale ici.

CREATE TABLE compta_liens_source (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT       NOT NULL,
    source_type        VARCHAR(24)  NOT NULL,
    source_id          BIGINT       NOT NULL,
    evenement          VARCHAR(20)  NOT NULL,
    version            INTEGER      NOT NULL,
    point_de_vente_id  BIGINT       NOT NULL,
    empreinte          CHAR(32)     NOT NULL,
    instantane         JSONB        NOT NULL,
    ecriture_id        BIGINT       NOT NULL,
    cree_le            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cree_par           VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_lie_ecriture FOREIGN KEY (livre_id, ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    -- Le verrou contre la double comptabilisation : pas de numéro de pièce, un couple (type, id).
    CONSTRAINT uk_compta_lie UNIQUE (livre_id, source_type, source_id, evenement, version),
    CONSTRAINT ck_compta_lie_type CHECK (source_type IN (
        'FACTURE', 'AVOIR_CLIENT', 'AVOIR_FOURNISSEUR', 'FACTURE_ACHAT', 'PAIEMENT', 'EFFET',
        'REGLEMENT_FOURNISSEUR', 'DEPENSE', 'VENTE', 'MOUVEMENT_TRESORERIE', 'LIGNE_RELEVE')),
    CONSTRAINT ck_compta_lie_evenement CHECK (evenement IN (
        'COMPTABILISATION', 'REMISE', 'ENCAISSEMENT', 'REJET', 'ECHEANCE', 'IMPUTATION_ACOMPTE', 'DEPART', 'ARRIVEE', 'ANNULATION')),
    CONSTRAINT ck_compta_lie_version CHECK (version >= 1)
);
CREATE INDEX idx_compta_lie_ecriture ON compta_liens_source(livre_id, ecriture_id);
CREATE INDEX idx_compta_lie_type_date ON compta_liens_source(livre_id, source_type, cree_le);
-- Une nouvelle version n'existe que si la précédente a été contre-passée ou corrigée :
-- deux écritures actives pour un même événement d'une même pièce sont impossibles, même par un bug du service.
CREATE FUNCTION compta_liens_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM compta_liens_source l
         WHERE l.livre_id = NEW.livre_id AND l.source_type = NEW.source_type AND l.source_id = NEW.source_id
           AND l.evenement = NEW.evenement AND l.version < NEW.version
           AND NOT EXISTS (SELECT 1 FROM compta_ecritures c
                            WHERE c.livre_id = l.livre_id
                              AND (c.contrepassation_de_id = l.ecriture_id OR c.corrige_ecriture_id = l.ecriture_id))) THEN
        PERFORM compta_refus('compta_lien_deja_actif', format('%s %s %s', NEW.source_type, NEW.source_id, NEW.evenement));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_liens_gardien BEFORE INSERT ON compta_liens_source
    FOR EACH ROW EXECUTE FUNCTION compta_liens_gardien();
CREATE TRIGGER trg_compta_liens_immuables BEFORE UPDATE OR DELETE ON compta_liens_source
    FOR EACH ROW EXECUTE FUNCTION compta_interdire_modification();

-- Une pièce mise de côté par décision explicite, avec motif. Une nouvelle décision remplace l'ancienne.
CREATE TABLE compta_pieces_ecartees (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT       NOT NULL,
    source_type  VARCHAR(24)  NOT NULL,
    source_id    BIGINT       NOT NULL,
    motif        VARCHAR(500) NOT NULL,
    ecarte_le    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ecarte_par   VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_eca_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_eca UNIQUE (livre_id, source_type, source_id)
);

-- Correspondance entre le commercial et le plan : aucune entité commerciale ne porte de compte.
CREATE TABLE compta_correspondances (
    id          BIGSERIAL PRIMARY KEY,
    livre_id  BIGINT       NOT NULL,
    type_cle    VARCHAR(24)  NOT NULL,
    cle         VARCHAR(60)  NOT NULL,
    compte_numero   VARCHAR(12)       NOT NULL,
    modifie_le  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_cor_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_cor UNIQUE (livre_id, type_cle, cle),
    CONSTRAINT ck_compta_cor_type CHECK (type_cle IN (
        'VENTE_TYPE_PRODUIT', 'VENTE_CATEGORIE', 'VENTE_PRODUIT',
        'ACHAT_TYPE_PRODUIT', 'ACHAT_CATEGORIE', 'ACHAT_PRODUIT',
        'RRR_VENTE', 'RRR_ACHAT', 'CATEGORIE_DEPENSE', 'TYPE_CHARGE',
        'COMPTE_FINANCIER', 'BANQUE_DEFAUT', 'CAISSE_DEFAUT'))
);

-- Ce que la source ne contient pas et que l'utilisateur apporte à la comptabilisation :
-- HT et TVA d'une dépense, ligne d'achat immobilisée, numéro de facture du fournisseur, TVA non déductible.
CREATE TABLE compta_ventilations (
    id                         BIGSERIAL PRIMARY KEY,
    livre_id                 BIGINT         NOT NULL,
    source_type                VARCHAR(24)    NOT NULL,
    source_id                  BIGINT         NOT NULL,
    numero                     INTEGER        NOT NULL,
    compte_numero                  VARCHAR(12)         NOT NULL,
    montant_ht                 NUMERIC(15, 2) NOT NULL,
    taux_tva                   NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    montant_tva                NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tva_deductible             BOOLEAN        NOT NULL DEFAULT TRUE,
    motif_non_deductible       VARCHAR(40),
    est_immobilisation         BOOLEAN        NOT NULL DEFAULT FALSE,
    numero_facture_fournisseur VARCHAR(100),
    date_facture_fournisseur   DATE,
    rubrique_tva               VARCHAR(30),
    saisie_le                  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saisie_par                 VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_ven_compte FOREIGN KEY (livre_id, compte_numero) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_ven UNIQUE (livre_id, source_type, source_id, numero),
    CONSTRAINT ck_compta_ven_montants CHECK (montant_ht >= 0 AND montant_tva >= 0)
);

-- Ce que le commercial ne sait pas et que le comptable précise, sans toucher à la table commerciale :
-- compte financier d'un paiement, date d'effet d'un rejet (aucune date n'est stockée), document de compensation.
CREATE TABLE compta_complements_source (
    id                   BIGSERIAL PRIMARY KEY,
    livre_id           BIGINT       NOT NULL,
    source_type          VARCHAR(24)  NOT NULL,
    source_id            BIGINT       NOT NULL,
    evenement            VARCHAR(20)  NOT NULL DEFAULT 'COMPTABILISATION',
    compte_financier_id  BIGINT,
    date_effet           DATE,
    reference_document   VARCHAR(100),
    saisi_le             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saisi_par            VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_cps_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_cps UNIQUE (livre_id, source_type, source_id, evenement)
);

-- Un règlement fournisseur peut couvrir plusieurs factures : le commercial ne connaît qu'une facture par règlement.
CREATE TABLE compta_reglement_affectations (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT         NOT NULL,
    reglement_id       BIGINT         NOT NULL,
    facture_achat_id   BIGINT         NOT NULL,
    montant            NUMERIC(15, 2) NOT NULL,
    saisi_le           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saisi_par          VARCHAR(150)   NOT NULL,
    CONSTRAINT fk_compta_raf_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_raf UNIQUE (livre_id, reglement_id, facture_achat_id),
    CONSTRAINT ck_compta_raf_montant CHECK (montant > 0)
);

-- Un déversement garde son rapport : un échec n'annule pas les autres pièces et ne perd pas le compte rendu.
CREATE TABLE compta_lots (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT       NOT NULL,
    lance_le     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lance_par    VARCHAR(150) NOT NULL,
    termine_le   TIMESTAMP,
    nb_demandes  INTEGER      NOT NULL DEFAULT 0,
    nb_reussies  INTEGER      NOT NULL DEFAULT 0,
    nb_echecs    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_compta_lot_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_lot_livre_id UNIQUE (livre_id, id)
);
CREATE TABLE compta_lot_resultats (
    id           BIGSERIAL PRIMARY KEY,
    livre_id   BIGINT       NOT NULL,
    lot_id       BIGINT       NOT NULL,
    source_type  VARCHAR(24)  NOT NULL,
    source_id    BIGINT       NOT NULL,
    evenement    VARCHAR(20)  NOT NULL,
    reussi       BOOLEAN      NOT NULL,
    ecriture_id  BIGINT,
    code_erreur  VARCHAR(60),
    message      VARCHAR(500),
    CONSTRAINT fk_compta_lor_lot FOREIGN KEY (livre_id, lot_id) REFERENCES compta_lots(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_compta_lor_ecriture FOREIGN KEY (livre_id, ecriture_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT
);
CREATE INDEX idx_compta_lor_lot ON compta_lot_resultats(lot_id);
