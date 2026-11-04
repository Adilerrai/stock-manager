-- Changeset 057 : l'abonnement à la comptabilité, porté par une table neuve.
-- Aucune table commerciale n'est modifiée : point_de_vente.module_comptabilite_actif reste en place, inerte.
-- Pas de ligne = pas d'abonnement : le module est fermé par défaut pour tous les tenants existants.

CREATE TABLE compta_abonnements (
    point_de_vente_id  BIGINT       PRIMARY KEY,
    actif              BOOLEAN      NOT NULL DEFAULT TRUE,
    souscrit_le        DATE         NOT NULL,
    modifie_le         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par        VARCHAR(150) NOT NULL,
    CONSTRAINT fk_compta_abo_pdv FOREIGN KEY (point_de_vente_id) REFERENCES point_de_vente(id) ON DELETE CASCADE
);
