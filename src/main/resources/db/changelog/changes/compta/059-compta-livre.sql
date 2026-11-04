-- Changeset 059 : le livre et ses référentiels.
-- Un livre = les comptes d'une entité juridique. Toutes les tables du module portent livre_id.
-- Le livre garde sa propre identité : aucune ligne n'est créée dans societes pour un tenant commercial,
-- car FiduciaireService.listerMesSocietes l'afficherait dans le sélecteur de dossier.
-- Clés étrangères composites (livre_id, x) : une ligne ne peut pas viser un objet d'un autre livre.

-- Refus uniforme : le message commence par « check constraint "code" », que GlobalExceptionHandler traduit déjà
-- (400 CHECK_CONSTRAINT_VIOLATION, avec le nom), sans modifier ce gestionnaire. Le service contrôle avant : ce refus reste le filet.
CREATE FUNCTION compta_refus(p_code TEXT, p_detail TEXT) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'check constraint "%" : %', p_code, COALESCE(p_detail, '') USING ERRCODE = '23514', CONSTRAINT = p_code;
END $$;

CREATE TABLE compta_livres (
    id                       BIGSERIAL    PRIMARY KEY,
    societe_id               BIGINT,
    modele                   VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',
    date_demarrage           DATE         NOT NULL,
    devise                   CHAR(3)      NOT NULL DEFAULT 'MAD',
    raison_sociale           VARCHAR(255) NOT NULL,
    forme_juridique          VARCHAR(50),
    identifiant_fiscal       VARCHAR(50),
    ice                      VARCHAR(30),
    registre_commerce        VARCHAR(100),
    taxe_professionnelle     VARCHAR(100),
    adresse                  VARCHAR(500),
    ville                    VARCHAR(100),
    activite_principale      VARCHAR(255),
    date_constitution        DATE,
    date_debut_exploitation  DATE,
    categorie_is             VARCHAR(40)  NOT NULL DEFAULT 'GENERAL',
    remise_globale_mode      VARCHAR(20)  NOT NULL DEFAULT 'NET_TVA_FACTUREE',
    active_le                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active_par               VARCHAR(150) NOT NULL,
    -- Un dossier de cabinet : la ligne societes choisie dans le sélecteur. NULL pour un tenant commercial.
    CONSTRAINT fk_compta_livres_societe FOREIGN KEY (societe_id) REFERENCES societes(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_livres_societe UNIQUE (societe_id),
    CONSTRAINT ck_compta_livres_modele CHECK (modele IN ('NORMAL')),
    CONSTRAINT ck_compta_livres_remise CHECK (remise_globale_mode IN ('NET_TVA_FACTUREE', 'NET_TVA_REDUITE'))
);

-- Le pont avec le commercial : les pièces lues sont celles des points de vente rattachés, à partir de la date de rattachement.
CREATE TABLE compta_livre_points_de_vente (
    point_de_vente_id  BIGINT      PRIMARY KEY,
    livre_id           BIGINT      NOT NULL,
    date_rattachement  DATE        NOT NULL,
    cree_le            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compta_lpv_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_lpv_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT
);
CREATE INDEX idx_compta_lpv_livre ON compta_livre_points_de_vente(livre_id);

CREATE TABLE compta_comptes (
    id            BIGSERIAL PRIMARY KEY,
    livre_id    BIGINT       NOT NULL,
    numero        VARCHAR(12)  NOT NULL,
    libelle       VARCHAR(255) NOT NULL,
    classe        SMALLINT     GENERATED ALWAYS AS (substr(numero, 1, 1)::SMALLINT) STORED,
    sens_normal   VARCHAR(6)   NOT NULL,
    nature        VARCHAR(40)  NOT NULL,
    imputable     BOOLEAN      NOT NULL DEFAULT TRUE,
    exige_tiers   BOOLEAN      NOT NULL DEFAULT FALSE,
    lettrable     BOOLEAN      NOT NULL DEFAULT FALSE,
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    origine       VARCHAR(10)  NOT NULL,
    cree_le       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compta_comptes_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_comptes_numero UNIQUE (livre_id, numero),
    CONSTRAINT ck_compta_comptes_numero CHECK (numero ~ '^[1-8][0-9]{3,11}$'),
    CONSTRAINT ck_compta_comptes_sens CHECK (sens_normal IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_compta_comptes_origine CHECK (origine IN ('CGNC', 'EXTENSION', 'SOCIETE')),
    CONSTRAINT ck_compta_comptes_tiers_lettrable CHECK (NOT exige_tiers OR lettrable)
);

CREATE TABLE compta_tiers (
    id                   BIGSERIAL PRIMARY KEY,
    livre_id           BIGINT       NOT NULL,
    type_tiers           VARCHAR(12)  NOT NULL,
    source_type          VARCHAR(12),
    source_id            BIGINT,
    code_auxiliaire      VARCHAR(20)  NOT NULL,
    nom                  VARCHAR(255) NOT NULL,
    ice                  VARCHAR(30),
    identifiant_fiscal   VARCHAR(50),
    compte_collectif  VARCHAR(12),
    entreprise_liee      BOOLEAN      NOT NULL DEFAULT FALSE,
    organisme_public     BOOLEAN      NOT NULL DEFAULT FALSE,
    non_resident         BOOLEAN      NOT NULL DEFAULT FALSE,
    actif                BOOLEAN      NOT NULL DEFAULT TRUE,
    cree_le              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le           TIMESTAMP,
    CONSTRAINT fk_compta_tiers_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_tiers_collectif FOREIGN KEY (livre_id, compte_collectif) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_tiers_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_tiers_code UNIQUE (livre_id, code_auxiliaire),
    CONSTRAINT uk_compta_tiers_source UNIQUE (livre_id, source_type, source_id),
    CONSTRAINT ck_compta_tiers_type CHECK (type_tiers IN ('CLIENT', 'FOURNISSEUR', 'PRESTATAIRE', 'ASSOCIE', 'PERSONNEL', 'ETAT', 'AUTRE')),
    CONSTRAINT ck_compta_tiers_source CHECK ((source_type IS NULL) = (source_id IS NULL)),
    CONSTRAINT ck_compta_tiers_source_type CHECK (source_type IS NULL OR source_type IN ('CLIENT', 'FOURNISSEUR', 'PRESTATAIRE'))
);
CREATE INDEX idx_compta_tiers_nom ON compta_tiers(livre_id, nom);

CREATE TABLE compta_journaux (
    id                     BIGSERIAL PRIMARY KEY,
    livre_id             BIGINT       NOT NULL,
    code                   VARCHAR(6)   NOT NULL,
    libelle                VARCHAR(120) NOT NULL,
    type_journal           VARCHAR(12)  NOT NULL,
    compte_tresorerie   VARCHAR(12),
    actif                  BOOLEAN      NOT NULL DEFAULT TRUE,
    cree_le                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compta_journaux_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT fk_compta_journaux_compte FOREIGN KEY (livre_id, compte_tresorerie) REFERENCES compta_comptes(livre_id, numero) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_journaux_code UNIQUE (livre_id, code),
    CONSTRAINT uk_compta_journaux_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_journaux_type UNIQUE (livre_id, id, type_journal),
    CONSTRAINT ck_compta_journaux_type CHECK (type_journal IN ('VENTES', 'ACHATS', 'BANQUE', 'CAISSE', 'OD', 'A_NOUVEAUX', 'CLOTURE'))
);
-- Un seul journal d'à-nouveaux et un seul journal de clôture par livre (avis CNC n° 24, annexe 2.3 c).
CREATE UNIQUE INDEX uq_compta_journal_an ON compta_journaux(livre_id) WHERE type_journal = 'A_NOUVEAUX';
CREATE UNIQUE INDEX uq_compta_journal_cl ON compta_journaux(livre_id) WHERE type_journal = 'CLOTURE';

CREATE TABLE compta_exercices (
    id               BIGSERIAL PRIMARY KEY,
    livre_id       BIGINT       NOT NULL,
    code             VARCHAR(20)  NOT NULL,
    date_debut       DATE         NOT NULL,
    date_fin         DATE         NOT NULL,
    statut           VARCHAR(8)   NOT NULL DEFAULT 'OUVERT',
    precedent_id     BIGINT,
    resultat_net     NUMERIC(15, 2),
    figeage_courant  INTEGER,
    arrete_le        TIMESTAMP,
    arrete_par       VARCHAR(150),
    cloture_le       TIMESTAMP,
    cloture_par      VARCHAR(150),
    CONSTRAINT fk_compta_exercices_livre FOREIGN KEY (livre_id) REFERENCES compta_livres(id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_exercices_livre_id UNIQUE (livre_id, id),
    CONSTRAINT uk_compta_exercices_code UNIQUE (livre_id, code),
    CONSTRAINT uk_compta_exercices_debut UNIQUE (livre_id, date_debut),
    CONSTRAINT fk_compta_exercices_precedent FOREIGN KEY (livre_id, precedent_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_compta_exercices_statut CHECK (statut IN ('OUVERT', 'ARRETE', 'CLOTURE')),
    -- Loi 9-88 art. 7 : « sans pouvoir pour autant excéder 12 mois ».
    CONSTRAINT ck_compta_exercices_duree CHECK (date_fin >= date_debut AND date_fin < date_debut + INTERVAL '12 months')
);
CREATE UNIQUE INDEX uq_compta_exercice_premier ON compta_exercices(livre_id) WHERE precedent_id IS NULL;
CREATE UNIQUE INDEX uq_compta_exercice_suivant ON compta_exercices(livre_id, precedent_id) WHERE precedent_id IS NOT NULL;

CREATE TABLE compta_periodes (
    id            BIGSERIAL PRIMARY KEY,
    livre_id    BIGINT       NOT NULL,
    exercice_id   BIGINT       NOT NULL,
    numero        SMALLINT     NOT NULL,
    type_periode  VARCHAR(12)  NOT NULL,
    date_debut    DATE         NOT NULL,
    date_fin      DATE         NOT NULL,
    statut        VARCHAR(8)   NOT NULL DEFAULT 'A_VENIR',
    cloturee_le   TIMESTAMP,
    cloturee_par  VARCHAR(150),
    CONSTRAINT fk_compta_periodes_exercice FOREIGN KEY (livre_id, exercice_id) REFERENCES compta_exercices(livre_id, id) ON DELETE RESTRICT,
    CONSTRAINT uk_compta_periodes_numero UNIQUE (livre_id, exercice_id, numero),
    CONSTRAINT uk_compta_periodes_exercice UNIQUE (livre_id, id, exercice_id),
    CONSTRAINT ck_compta_periodes_type CHECK (type_periode IN ('OUVERTURE', 'COURANTE', 'CLOTURE')),
    CONSTRAINT ck_compta_periodes_numero CHECK (
        (type_periode = 'OUVERTURE' AND numero = 0) OR
        (type_periode = 'CLOTURE' AND numero = 99) OR
        (type_periode = 'COURANTE' AND numero BETWEEN 1 AND 13)),
    CONSTRAINT ck_compta_periodes_statut CHECK (statut IN ('A_VENIR', 'OUVERTE', 'CLOTUREE')),
    CONSTRAINT ck_compta_periodes_dates CHECK (date_fin >= date_debut)
);
CREATE INDEX idx_compta_periodes_dates ON compta_periodes(livre_id, type_periode, date_debut);

-- Exercices : contiguïté, dates figées, cycle OUVERT -> ARRETE -> CLOTURE, clôture chronologique.
CREATE FUNCTION compta_exercices_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_fin_prec    DATE;
    v_statut_prec VARCHAR(8);
BEGIN
    IF NEW.precedent_id IS NOT NULL THEN
        SELECT date_fin, statut INTO v_fin_prec, v_statut_prec
          FROM compta_exercices WHERE id = NEW.precedent_id AND livre_id = NEW.livre_id;
    END IF;
    IF TG_OP = 'INSERT' THEN
        IF NEW.statut <> 'OUVERT' THEN
            PERFORM compta_refus('compta_exercice_statut', 'un exercice naît ouvert');
        END IF;
        IF NEW.precedent_id IS NOT NULL AND NEW.date_debut <> v_fin_prec + 1 THEN
            PERFORM compta_refus('compta_exercice_non_contigu', format('%s ne suit pas %s', NEW.date_debut, v_fin_prec));
        END IF;
        RETURN NEW;
    END IF;
    IF (NEW.livre_id, NEW.date_debut, NEW.date_fin) IS DISTINCT FROM (OLD.livre_id, OLD.date_debut, OLD.date_fin)
       OR NEW.precedent_id IS DISTINCT FROM OLD.precedent_id THEN
        PERFORM compta_refus('compta_exercice_dates_figees', NULL);
    END IF;
    IF NEW.statut <> OLD.statut THEN
        IF OLD.statut = 'CLOTURE' THEN
            PERFORM compta_refus('compta_exercice_clos', 'aucune réouverture après clôture définitive');
        END IF;
        IF NOT ((OLD.statut = 'OUVERT' AND NEW.statut = 'ARRETE')
             OR (OLD.statut = 'ARRETE' AND NEW.statut = 'OUVERT')
             OR (OLD.statut = 'ARRETE' AND NEW.statut = 'CLOTURE')) THEN
            PERFORM compta_refus('compta_exercice_transition', format('%s vers %s', OLD.statut, NEW.statut));
        END IF;
        IF NEW.statut IN ('ARRETE', 'CLOTURE') AND NEW.precedent_id IS NOT NULL AND v_statut_prec <> 'CLOTURE' THEN
            PERFORM compta_refus('compta_cloture_non_chronologique', 'l''exercice précédent n''est pas clôturé');
        END IF;
        IF NEW.statut = 'ARRETE' AND EXISTS (
            SELECT 1 FROM compta_periodes p
             WHERE p.exercice_id = NEW.id AND p.type_periode = 'COURANTE' AND p.statut <> 'CLOTUREE') THEN
            PERFORM compta_refus('compta_periodes_ouvertes', 'toutes les périodes courantes doivent être clôturées');
        END IF;
        IF NEW.statut = 'CLOTURE' AND EXISTS (
            SELECT 1 FROM compta_periodes p
             WHERE p.exercice_id = NEW.id AND p.type_periode = 'CLOTURE' AND p.statut <> 'CLOTUREE') THEN
            PERFORM compta_refus('compta_periode_cloture_ouverte', 'la période de clôture doit être fermée');
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_exercices_gardien BEFORE INSERT OR UPDATE ON compta_exercices
    FOR EACH ROW EXECUTE FUNCTION compta_exercices_gardien();

-- Périodes : avis CNC n° 24, annexe 2.3 d. Au plus deux périodes courantes ouvertes, jamais de réouverture.
CREATE FUNCTION compta_periodes_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_nb INTEGER;
BEGIN
    PERFORM pg_advisory_xact_lock(7301, NEW.livre_id::INTEGER);
    IF TG_OP = 'UPDATE' THEN
        IF (NEW.livre_id, NEW.exercice_id, NEW.numero, NEW.type_periode, NEW.date_debut, NEW.date_fin)
           IS DISTINCT FROM (OLD.livre_id, OLD.exercice_id, OLD.numero, OLD.type_periode, OLD.date_debut, OLD.date_fin) THEN
            PERFORM compta_refus('compta_periode_figee', NULL);
        END IF;
        IF NEW.statut = OLD.statut THEN
            RETURN NEW;
        END IF;
        IF NOT ((OLD.statut = 'A_VENIR' AND NEW.statut = 'OUVERTE') OR (OLD.statut = 'OUVERTE' AND NEW.statut = 'CLOTUREE')) THEN
            PERFORM compta_refus('compta_periode_transition', format('%s vers %s interdit', OLD.statut, NEW.statut));
        END IF;
    END IF;
    IF NEW.type_periode = 'COURANTE' AND NEW.statut = 'CLOTUREE' THEN
        IF EXISTS (SELECT 1 FROM compta_periodes p
                    WHERE p.livre_id = NEW.livre_id AND p.type_periode = 'COURANTE'
                      AND p.date_debut < NEW.date_debut AND p.statut <> 'CLOTUREE') THEN
            PERFORM compta_refus('compta_periode_non_chronologique', 'une période antérieure reste ouverte');
        END IF;
        IF EXISTS (SELECT 1 FROM compta_brouillons b
                    WHERE b.livre_id = NEW.livre_id AND b.date_ecriture BETWEEN NEW.date_debut AND NEW.date_fin) THEN
            PERFORM compta_refus('compta_brouillons_restants', format('période %s non clôturable', NEW.numero));
        END IF;
    END IF;
    IF NEW.type_periode = 'COURANTE' AND NEW.statut = 'OUVERTE' THEN
        SELECT count(*) INTO v_nb FROM compta_periodes p
         WHERE p.livre_id = NEW.livre_id AND p.type_periode = 'COURANTE' AND p.statut = 'OUVERTE'
           AND p.id IS DISTINCT FROM NEW.id;
        IF v_nb >= 2 THEN
            PERFORM compta_refus('compta_deux_periodes_max', 'deux périodes courantes sont déjà ouvertes');
        END IF;
        IF EXISTS (SELECT 1 FROM compta_periodes p
                    WHERE p.livre_id = NEW.livre_id AND p.type_periode = 'COURANTE'
                      AND p.date_debut < NEW.date_debut AND p.statut = 'A_VENIR') THEN
            PERFORM compta_refus('compta_periode_non_chronologique', 'une période antérieure n''est pas ouverte');
        END IF;
    END IF;
    -- La période de clôture : ouverte une fois les périodes courantes de l'exercice fermées et l'exercice précédent clos,
    -- fermée seulement pendant la clôture définitive.
    IF NEW.type_periode = 'CLOTURE' AND NEW.statut = 'OUVERTE' THEN
        IF EXISTS (SELECT 1 FROM compta_periodes p
                    WHERE p.exercice_id = NEW.exercice_id AND p.type_periode = 'COURANTE' AND p.statut <> 'CLOTUREE') THEN
            PERFORM compta_refus('compta_periodes_ouvertes', 'période de clôture non ouvrable');
        END IF;
        IF EXISTS (SELECT 1 FROM compta_exercices e JOIN compta_exercices q ON q.id = e.precedent_id
                    WHERE e.id = NEW.exercice_id AND q.statut <> 'CLOTURE') THEN
            PERFORM compta_refus('compta_cloture_non_chronologique', 'l''exercice précédent n''est pas clôturé');
        END IF;
    END IF;
    IF NEW.type_periode = 'CLOTURE' AND NEW.statut = 'CLOTUREE' AND NOT EXISTS (
        SELECT 1 FROM compta_exercices e WHERE e.id = NEW.exercice_id AND e.statut = 'ARRETE') THEN
        PERFORM compta_refus('compta_exercice_non_arrete', 'la période de clôture se ferme pendant la clôture définitive');
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_periodes_gardien BEFORE INSERT OR UPDATE ON compta_periodes
    FOR EACH ROW EXECUTE FUNCTION compta_periodes_gardien();

-- Numéro d'un compte : figé. La suppression d'un compte mouvementé est refusée par la clé étrangère (avis 2.6).
CREATE FUNCTION compta_comptes_gardien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.numero <> OLD.numero OR NEW.livre_id <> OLD.livre_id THEN
        PERFORM compta_refus('compta_compte_fige', 'un numéro de compte ne change pas');
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_comptes_gardien BEFORE UPDATE ON compta_comptes
    FOR EACH ROW EXECUTE FUNCTION compta_comptes_gardien();

-- Code auxiliaire : C000123, F000045, P000009, X000001.
CREATE FUNCTION compta_tiers_code() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.code_auxiliaire IS NULL THEN
        NEW.code_auxiliaire := CASE NEW.type_tiers WHEN 'CLIENT' THEN 'C' WHEN 'FOURNISSEUR' THEN 'F' WHEN 'PRESTATAIRE' THEN 'P' ELSE 'X' END
                               || lpad(NEW.id::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_compta_tiers_code BEFORE INSERT ON compta_tiers
    FOR EACH ROW EXECUTE FUNCTION compta_tiers_code();
