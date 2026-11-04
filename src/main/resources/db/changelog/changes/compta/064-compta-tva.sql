-- Changeset 064 : TVA (annexe C). La table declarations_tva de l'ancien module reste en place, orpheline.

CREATE TABLE compta_tva_parametres (
    id                  BIGSERIAL PRIMARY KEY,
    livre_id          BIGINT         NOT NULL,
    annee               INTEGER        NOT NULL,
    assujetti           BOOLEAN        NOT NULL DEFAULT TRUE,
    fait_generateur     VARCHAR(12)    NOT NULL,
    periodicite         VARCHAR(13)    NOT NULL,
    prorata             NUMERIC(5, 2)  NOT NULL DEFAULT 100,
    prorata_provisoire  BOOLEAN        NOT NULL DEFAULT FALSE,
    premiere_periode    INTEGER,
    credit_initial      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_compta_tvp_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_tvp UNIQUE (livre_id, annee),
    CONSTRAINT ck_compta_tvp_fait CHECK (fait_generateur IN ('ENCAISSEMENT', 'DEBIT')),
    CONSTRAINT ck_compta_tvp_periodicite CHECK (periodicite IN ('MENSUELLE', 'TRIMESTRIELLE')),
    CONSTRAINT ck_compta_tvp_prorata CHECK (prorata >= 0 AND prorata <= 100)
);

-- Traitement fiscal par produit : aucun champ exonération ou hors champ n'existe côté commercial.
CREATE TABLE compta_tva_traitements_produit (
    id          BIGSERIAL PRIMARY KEY,
    livre_id  BIGINT       NOT NULL,
    produit_id  BIGINT       NOT NULL,
    traitement  VARCHAR(16)  NOT NULL,
    ligne_code  VARCHAR(6),
    modifie_le  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_tvtp_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_tvtp UNIQUE (livre_id, produit_id),
    CONSTRAINT ck_compta_tvtp CHECK (traitement IN ('TAXABLE', 'HORS_CHAMP', 'EXONERE_91', 'EXONERE_92', 'SUSPENSION_94'))
);

CREATE TABLE compta_tva_declarations (
    id                       BIGSERIAL PRIMARY KEY,
    livre_id               BIGINT         NOT NULL,
    annee                    INTEGER        NOT NULL,
    numero_periode           INTEGER        NOT NULL,
    periode_debut            DATE           NOT NULL,
    periode_fin              DATE           NOT NULL,
    periodicite              VARCHAR(13)    NOT NULL,
    regime_dgi               SMALLINT       NOT NULL,
    fait_generateur          VARCHAR(12)    NOT NULL,
    version                  INTEGER        NOT NULL DEFAULT 1,
    declaration_origine_id   BIGINT,
    statut                   VARCHAR(12)    NOT NULL DEFAULT 'BROUILLON',
    date_limite              DATE           NOT NULL,
    credit_anterieur         NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tva_exigible             NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tva_deductible           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tva_due                  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    credit_periode           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    calcule_le               TIMESTAMP,
    valide_le                TIMESTAMP,
    valide_par               VARCHAR(150),
    depose_le                DATE,
    numero_recu              VARCHAR(60),
    paye_le                  DATE,
    mode_paiement            VARCHAR(30),
    ecriture_liquidation_id  BIGINT,
    ecriture_paiement_id     BIGINT,
    CONSTRAINT fk_compta_tvd_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_tvd_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_tvd UNIQUE (livre_id, annee, regime_dgi, numero_periode, version),
    CONSTRAINT uk_compta_tvd_debut UNIQUE (livre_id, periode_debut, version),
    CONSTRAINT ck_compta_tvd_dates CHECK (periode_fin >= periode_debut),
    CONSTRAINT fk_compta_tvd_origine FOREIGN KEY (livre_id, declaration_origine_id) REFERENCES compta_tva_declarations(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_tvd_liquidation FOREIGN KEY (livre_id, ecriture_liquidation_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_tvd_paiement FOREIGN KEY (livre_id, ecriture_paiement_id) REFERENCES compta_ecritures(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_tvd_statut CHECK (statut IN ('BROUILLON', 'VALIDEE', 'EDI_ENVOYE', 'EDI_TRAITE', 'EDI_REJETE', 'DEPOSEE', 'PAYEE')),
    CONSTRAINT ck_compta_tvd_fait CHECK (fait_generateur IN ('ENCAISSEMENT', 'DEBIT')),
    CONSTRAINT ck_compta_tvd_regime CHECK (regime_dgi BETWEEN 1 AND 4),
    CONSTRAINT ck_compta_tvd_periode CHECK (numero_periode BETWEEN 1 AND 12),
    -- Une période est soit due, soit créditrice : le crédit se reporte, il ne disparaît pas.
    CONSTRAINT ck_compta_tvd_resultat CHECK (tva_due >= 0 AND credit_periode >= 0 AND NOT (tva_due > 0 AND credit_periode > 0))
);

CREATE TABLE compta_tva_operations_collectees (
    id                 BIGSERIAL PRIMARY KEY,
    livre_id         BIGINT         NOT NULL,
    declaration_id     BIGINT         NOT NULL,
    source_type        VARCHAR(24)    NOT NULL,
    source_id          BIGINT         NOT NULL,
    facture_id         BIGINT,
    point_de_vente_id  BIGINT         NOT NULL,
    empreinte_source   CHAR(32)       NOT NULL,
    date_generateur    DATE           NOT NULL,
    compte_financier_id BIGINT,
    mode               VARCHAR(30),
    reference          VARCHAR(100),
    tiers_nom          VARCHAR(255),
    facture_numero     VARCHAR(100),
    taux               NUMERIC(5, 2)  NOT NULL,
    ttc                NUMERIC(15, 2) NOT NULL,
    ht                 NUMERIC(15, 2) NOT NULL,
    tva                NUMERIC(15, 2) NOT NULL,
    traitement         VARCHAR(16)    NOT NULL DEFAULT 'TAXABLE',
    ligne_code         VARCHAR(6),
    motif_surcharge    VARCHAR(500),
    CONSTRAINT fk_compta_tvc_declaration FOREIGN KEY (livre_id, declaration_id) REFERENCES compta_tva_declarations(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_compta_tvc_montants CHECK (ht + tva = ttc),
    CONSTRAINT ck_compta_tvc_traitement CHECK (traitement IN ('TAXABLE', 'HORS_CHAMP', 'EXONERE_91', 'EXONERE_92', 'SUSPENSION_94')),
    CONSTRAINT ck_compta_tvc_source CHECK (source_type IN ('PAIEMENT', 'FACTURE', 'AVOIR_CLIENT', 'REGULARISATION', 'MANUEL'))
);
-- Une opération n'entre que dans une déclaration. facture_id entre dans la clé :
-- un chèque sur trois factures au même taux donne trois lignes.
CREATE UNIQUE INDEX uq_compta_tvc_unique ON compta_tva_operations_collectees(livre_id, source_type, source_id, COALESCE(facture_id, 0), taux)
    WHERE source_type NOT IN ('REGULARISATION', 'MANUEL');
CREATE INDEX idx_compta_tvc_declaration ON compta_tva_operations_collectees(declaration_id);

CREATE TABLE compta_tva_operations_deductibles (
    id                  BIGSERIAL PRIMARY KEY,
    livre_id          BIGINT         NOT NULL,
    declaration_id      BIGINT         NOT NULL,
    source_type         VARCHAR(24)    NOT NULL,
    source_id           BIGINT         NOT NULL,
    facture_achat_id    BIGINT,
    point_de_vente_id   BIGINT         NOT NULL,
    empreinte_source    CHAR(32)       NOT NULL,
    ordre               INTEGER        NOT NULL DEFAULT 0,
    numero_facture      VARCHAR(100)   NOT NULL,
    date_facture        DATE           NOT NULL,
    fournisseur_nom     VARCHAR(255)   NOT NULL,
    fournisseur_if      VARCHAR(50),
    fournisseur_ice     VARCHAR(30),
    designation         VARCHAR(255)   NOT NULL,
    rubrique_code       VARCHAR(30)    NOT NULL,
    ligne_code          VARCHAR(6),
    compte_tva          VARCHAR(5)     NOT NULL,
    ht                  NUMERIC(15, 2) NOT NULL,
    taux                NUMERIC(5, 2)  NOT NULL,
    tva                 NUMERIC(15, 2) NOT NULL,
    ttc                 NUMERIC(15, 2) NOT NULL,
    prorata             NUMERIC(5, 2)  NOT NULL DEFAULT 100,
    tva_deductible      NUMERIC(15, 2) NOT NULL,
    mp_id               SMALLINT       NOT NULL,
    reference_paiement  VARCHAR(100),
    date_paiement       DATE           NOT NULL,
    compte_financier_id BIGINT,
    motif_surcharge     VARCHAR(500),
    CONSTRAINT fk_compta_tvo_declaration FOREIGN KEY (livre_id, declaration_id) REFERENCES compta_tva_declarations(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_compta_tvo_montants CHECK (ht + tva = ttc),
    CONSTRAINT ck_compta_tvo_mp CHECK (mp_id BETWEEN 1 AND 7),
    -- 34551 sur immobilisations, 34552 sur charges (CGNC ; CGI art. 112-I-8°). 3455 seul est refusé.
    CONSTRAINT ck_compta_tvo_compte CHECK (compte_tva IN ('34551', '34552')),
    CONSTRAINT ck_compta_tvo_source CHECK (source_type IN ('REGLEMENT_FOURNISSEUR', 'DEPENSE', 'CHARGE_IMPORT', 'LIGNE_RELEVE', 'REGULARISATION', 'MANUEL'))
);
CREATE UNIQUE INDEX uq_compta_tvo_unique ON compta_tva_operations_deductibles(livre_id, source_type, source_id, COALESCE(facture_achat_id, 0), taux)
    WHERE source_type NOT IN ('REGULARISATION', 'MANUEL');
CREATE INDEX idx_compta_tvo_declaration ON compta_tva_operations_deductibles(declaration_id);

CREATE TABLE compta_tva_declaration_lignes (
    id              BIGSERIAL PRIMARY KEY,
    livre_id      BIGINT         NOT NULL,
    declaration_id  BIGINT         NOT NULL,
    code            VARCHAR(6)     NOT NULL,
    base_calculee   NUMERIC(15, 2),
    taxe_calculee   NUMERIC(15, 2),
    base_retenue    NUMERIC(15, 2),
    taxe_retenue    NUMERIC(15, 2),
    motif_ecart     VARCHAR(500),
    CONSTRAINT fk_compta_tvl_declaration FOREIGN KEY (livre_id, declaration_id) REFERENCES compta_tva_declarations(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT uk_compta_tvl UNIQUE (declaration_id, code)
);

CREATE TABLE compta_tva_classements_releve (
    id              BIGSERIAL PRIMARY KEY,
    livre_id      BIGINT        NOT NULL,
    ligne_releve_id BIGINT        NOT NULL,
    sens            VARCHAR(6)    NOT NULL,
    rubrique_code   VARCHAR(30),
    classement      VARCHAR(12)   NOT NULL,
    libelle         VARCHAR(255),
    CONSTRAINT fk_compta_tvx_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_tvx UNIQUE (livre_id, ligne_releve_id),
    CONSTRAINT ck_compta_tvx_classement CHECK (classement IN ('RUBRIQUE', 'HORS_CHAMP', 'NON_TAXABLE'))
);

CREATE TABLE compta_tva_fichiers_edi (
    id              BIGSERIAL PRIMARY KEY,
    livre_id      BIGINT       NOT NULL,
    declaration_id  BIGINT       NOT NULL,
    type_fichier    VARCHAR(8)   NOT NULL,
    nom_fichier     VARCHAR(255) NOT NULL,
    sha256          CHAR(64)     NOT NULL,
    contenu         BYTEA        NOT NULL,
    genere_le       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    genere_par      VARCHAR(150) NOT NULL,
    etat_dgi        VARCHAR(12)  NOT NULL DEFAULT 'NON_ENVOYE',
    commentaire_dgi VARCHAR(1000),
    etat_le         TIMESTAMP,
    CONSTRAINT fk_compta_tve_declaration FOREIGN KEY (livre_id, declaration_id) REFERENCES compta_tva_declarations(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_tve_type CHECK (type_fichier IN ('RD', 'RDRS', 'AL', 'NR', 'CD', 'TVARS')),
    CONSTRAINT ck_compta_tve_etat CHECK (etat_dgi IN ('NON_ENVOYE', 'EN_ATTENTE', 'REJETE', 'TRAITE'))
);

CREATE TABLE compta_tva_controles (
    id              BIGSERIAL PRIMARY KEY,
    livre_id      BIGINT       NOT NULL,
    declaration_id  BIGINT       NOT NULL,
    code            VARCHAR(6)   NOT NULL,
    gravite         VARCHAR(12)  NOT NULL,
    message         VARCHAR(500) NOT NULL,
    objet_type      VARCHAR(24),
    objet_id        BIGINT,
    etat            VARCHAR(8)   NOT NULL DEFAULT 'OUVERT',
    motif           VARCHAR(500),
    accepte_par     VARCHAR(150),
    CONSTRAINT fk_compta_tvq_declaration FOREIGN KEY (livre_id, declaration_id) REFERENCES compta_tva_declarations(livre_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_compta_tvq_gravite CHECK (gravite IN ('BLOQUANT', 'ALERTE', 'ALERTE_FORTE', 'NON_EFFECTUE')),
    CONSTRAINT ck_compta_tvq_etat CHECK (etat IN ('OUVERT', 'ACCEPTE')),
    CONSTRAINT ck_compta_tvq_motif CHECK (etat = 'OUVERT' OR motif IS NOT NULL)
);

-- Une déclaration validée ne se recalcule pas : sa photo est figée.
CREATE FUNCTION compta_tva_operations_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_decl BIGINT := CASE WHEN TG_OP = 'DELETE' THEN OLD.declaration_id ELSE NEW.declaration_id END;
BEGIN
    IF EXISTS (SELECT 1 FROM compta_tva_declarations d WHERE d.id = v_decl AND d.statut <> 'BROUILLON') THEN
        PERFORM compta_refus('compta_tva_figee', format('déclaration %s', v_decl));
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_tvc_gardien BEFORE INSERT OR UPDATE OR DELETE ON compta_tva_operations_collectees
    FOR EACH ROW EXECUTE FUNCTION compta_tva_operations_gardien();
CREATE TRIGGER trg_compta_tvo_gardien BEFORE INSERT OR UPDATE OR DELETE ON compta_tva_operations_deductibles
    FOR EACH ROW EXECUTE FUNCTION compta_tva_operations_gardien();

-- Une déclaration validée ne change plus de montants. Dévalider n'est possible qu'avant tout envoi.
CREATE FUNCTION compta_tva_declarations_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.statut <> 'BROUILLON' THEN
            PERFORM compta_refus('compta_tva_figee', format('déclaration %s', OLD.id));
        END IF;
        RETURN OLD;
    END IF;
    IF OLD.statut <> 'BROUILLON' AND NEW.statut <> 'BROUILLON'
       AND (NEW.credit_anterieur, NEW.tva_exigible, NEW.tva_deductible, NEW.tva_due, NEW.credit_periode, NEW.periode_debut, NEW.periode_fin, NEW.version)
           IS DISTINCT FROM (OLD.credit_anterieur, OLD.tva_exigible, OLD.tva_deductible, OLD.tva_due, OLD.credit_periode, OLD.periode_debut, OLD.periode_fin, OLD.version) THEN
        PERFORM compta_refus('compta_tva_figee', format('déclaration %s', OLD.id));
    END IF;
    IF NEW.statut = 'BROUILLON' AND OLD.statut NOT IN ('BROUILLON', 'VALIDEE', 'EDI_REJETE') THEN
        PERFORM compta_refus('compta_tva_figee', format('retour en brouillon depuis %s', OLD.statut));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_tvd_gardien BEFORE UPDATE OR DELETE ON compta_tva_declarations
    FOR EACH ROW EXECUTE FUNCTION compta_tva_declarations_gardien();
