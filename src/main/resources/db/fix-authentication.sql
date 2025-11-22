-- Script pour diagnostiquer et corriger les problèmes d'authentification

-- 1. Vérifier tous les utilisateurs
SELECT id, email, username, nom_complet, point_de_vente_id
FROM users;

-- 2. Vérifier les utilisateurs sans username
SELECT id, email, username, nom_complet
FROM users
WHERE username IS NULL OR username = '';

-- 3. Si vous voulez créer/mettre à jour l'utilisateur G1500
-- Option A: Mettre à jour un utilisateur existant
UPDATE users
SET username = 'G1500'
WHERE email = 'admin@example.com' OR id = 1;

-- Option B: Créer un nouvel utilisateur G1500 si nécessaire
-- D'abord, vérifier s'il existe un point de ventea
SELECT id, nom FROM point_de_vente;

-- Ensuite, vérifier s'il existe un rôle
SELECT id, nom FROM roles;

-- Créer l'utilisateur G1500 (ajuster les IDs selon votre base)
INSERT INTO users (email, username, password, nom_complet, point_de_vente_id, role_id, account_non_expired, account_non_locked, credentials_non_expired, enabled)
VALUES (
    'g1500@magasin.dz',
    'G1500',
    '$2a$12$4mHVLBS0/PbQj1RoNdSI/eysmWbgyrgOHHarRD.dxNjvIXCSrwaam', -- Remplacer par le hash bcrypt de 'admin123'
    'Caissier G1500',
    1, -- ID du point de vente
    2, -- ID du rôle (par exemple CAISSIER)
    true,
    true,
    true,
    true
)
ON CONFLICT (email) DO UPDATE
SET username = 'G1500';

-- 4. Vérifier que le username est bien présent
SELECT id, email, username, nom_complet
FROM users
WHERE username = 'G1500';

