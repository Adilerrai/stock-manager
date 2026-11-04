-- Une charge constatée d'avance se marque dès le brouillard : l'extourne se décide à la saisie, pas au moment de la clôture.
ALTER TABLE compta_brouillons ADD COLUMN a_extourner BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE compta_brouillons ADD CONSTRAINT ck_compta_bro_extourne CHECK (NOT a_extourner OR nature = 'INVENTAIRE');
