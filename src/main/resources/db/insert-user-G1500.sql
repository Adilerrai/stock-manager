-- ==========================================
-- SCRIPT D'INSERTION UTILISATEUR G1500
-- Hash password fourni: $2a$12$4mHVLBS0/PbQj1RoNdSI/eysmWbgyrgOHHarRD.dxNjvIXCSrwaam
-- ==========================================

-- Vérifier d'abord si le point de vente et le rôle existent
-- Si non, les créer

-- 1. Créer le point de vente s'il n'existe pas
INSERT INTO point_de_vente (tenant_id, nom_point_de_vente, password)
VALUES (1, 'Magasin Principal', '$2a$10$yfIHYCHVqyYQZMicjQMBWO.1Z7Zt2h9Mn9F5ocYzwWCnPvLeko3Aq')
ON CONFLICT (tenant_id) DO NOTHING;

-- 2. Créer les rôles s'ils n'existent pas
INSERT INTO roles (nom, description) VALUES
('ROLE_ADMIN', 'Administrateur système'),
('ROLE_POINT_DE_VENTE_MANAGER', 'Gérant du point de vente'),
('ROLE_CAISSIER', 'Caissier'),
('ROLE_VENDEUR', 'Vendeur'),
('ROLE_MAGASINIER', 'Magasinier')
ON CONFLICT (nom) DO NOTHING;

-- 3. Insérer l'utilisateur G1500
INSERT INTO users (
    email,
    username,
    password,
    nom_complet,
    telephone,
    genre,
    role_id,
    point_de_vente_id,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    enabled,
    date_creation
)
VALUES (
    'g1500@magasin.dz',
    'G1500',
    '$2a$12$4mHVLBS0/PbQj1RoNdSI/eysmWbgyrgOHHarRD.dxNjvIXCSrwaam',
    'Caissier G1500',
    '0550000000',
    'HOMME',
    (SELECT id FROM roles WHERE nom = 'ROLE_CAISSIER' LIMIT 1),
    (SELECT id FROM point_de_vente WHERE tenant_id = 1 LIMIT 1),
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET
    username = 'G1500',
    password = '$2a$12$4mHVLBS0/PbQj1RoNdSI/eysmWbgyrgOHHarRD.dxNjvIXCSrwaam',
    nom_complet = 'Caissier G1500',
    telephone = '0550000000',
    enabled = true,
    account_non_expired = true,
    account_non_locked = true,
    credentials_non_expired = true;

-- 4. Vérifier que l'utilisateur a été créé
SELECT
    id,
    email,
    username,
    nom_complet,
    role_id,
    point_de_vente_id,
    enabled
FROM users
WHERE username = 'G1500';

-- 5. Afficher les informations complètes avec le rôle
SELECT
    u.id,
    u.email,
    u.username,
    u.nom_complet,
    u.telephone,
    r.nom as role_name,
    u.point_de_vente_id,
    u.enabled,
    u.account_non_expired,
    u.account_non_locked,
    u.credentials_non_expired
FROM users u
LEFT JOIN roles r ON u.role_id = r.id
WHERE u.username = 'G1500';

-- Message de confirmation
SELECT '✅ Utilisateur G1500 créé/mis à jour avec succès!' as message;

