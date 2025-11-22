# 🚀 GUIDE DE RECRÉATION DE LA BASE DE DONNÉES

## 📋 Informations importantes

**Hash du mot de passe fourni:** `$2a$12$4mHVLBS0/PbQj1RoNdSI/eysmWbgyrgOHHarRD.dxNjvIXCSrwaam`

**Utilisateur créé:**
- **Username:** G1500
- **Email:** g1500@magasin.dz
- **Rôle:** CAISSIER

---

## 🗑️ ÉTAPE 1 : Supprimer l'ancienne base de données

### Option A : Via ligne de commande PostgreSQL

```bash
# Se connecter en tant que postgres
psql -U postgres

# Supprimer la base de données
DROP DATABASE IF EXISTS pointvente_db;

# Recréer la base de données
CREATE DATABASE pointvente_db;

# Se déconnecter
\q
```

### Option B : Via pgAdmin
1. Ouvrir pgAdmin
2. Clic droit sur `pointvente_db`
3. Sélectionner "Delete/Drop"
4. Confirmer
5. Clic droit sur "Databases"
6. Sélectionner "Create > Database"
7. Nom: `pointvente_db`
8. Cliquer "Save"

---

## 📥 ÉTAPE 2 : Exécuter le script SQL

### Option A : Via ligne de commande

```bash
# Se placer dans le dossier du projet
cd C:\Projects\Point-vente\point-vente

# Exécuter le script
psql -U postgres -d pointvente_db -f src\main\resources\db\create-full-database.sql
```

### Option B : Via pgAdmin

1. Ouvrir pgAdmin
2. Sélectionner `pointvente_db`
3. Cliquer sur "Tools" > "Query Tool"
4. Ouvrir le fichier `create-full-database.sql`
5. Cliquer sur "Execute" (▶️)
6. Vérifier les messages de confirmation

### Option C : Via DBeaver

1. Ouvrir DBeaver
2. Se connecter à `pointvente_db`
3. Clic droit sur la connexion
4. "SQL Editor" > "Open SQL Script"
5. Sélectionner `create-full-database.sql`
6. Cliquer sur "Execute SQL Statement" (Ctrl+Enter)

---

## ✅ ÉTAPE 3 : Vérifier la création

Exécutez ces requêtes pour vérifier :

```sql
-- Vérifier les tables créées
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Vérifier l'utilisateur G1500
SELECT id, email, username, nom_complet, enabled 
FROM users 
WHERE username = 'G1500';

-- Vérifier le point de vente
SELECT id, tenant_id, nom FROM point_de_vente;

-- Vérifier les rôles
SELECT id, nom FROM roles;
```

**Résultats attendus:**

```
✅ 30+ tables créées
✅ Utilisateur G1500 trouvé avec username='G1500'
✅ Point de vente avec tenant_id=1
✅ 5 rôles créés (ADMIN, MANAGER, CAISSIER, VENDEUR, MAGASINIER)
```

---

## 🔐 ÉTAPE 4 : Tester l'authentification

### Via Postman/cURL

```bash
POST http://localhost:8009/api/auth/login/username
Content-Type: application/json

{
  "username": "G1500",
  "password": "votre_mot_de_passe_en_clair"
}
```

**Réponse attendue :**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "email": "g1500@magasin.dz",
  "username": "G1500",
  "nomComplet": "Caissier G1500",
  "role": "ROLE_CAISSIER",
  "pointDeVenteId": 1
}
```

---

## 🔧 ÉTAPE 5 : Redémarrer l'application Spring Boot

```bash
# Arrêter l'application si elle tourne
# Puis redémarrer
mvn spring-boot:run
```

**Logs attendus:**
```
✅ Hibernate: create table if not exists...
✅ JPA initialized
✅ Started PointVenteApplication in X seconds
✅ Tomcat started on port(s): 8009
```

---

## 📊 Structure de la base créée

Le script crée **toutes les tables nécessaires** :

### Tables de base
- ✅ point_de_vente
- ✅ roles
- ✅ users
- ✅ habilitations

### Tables produits
- ✅ produits
- ✅ produits_images
- ✅ depots
- ✅ fournisseurs

### Tables stock
- ✅ stocks
- ✅ stocks_qualite
- ✅ mouvements_stock

### Tables clients & ventes
- ✅ clients
- ✅ ventes
- ✅ lignes_vente
- ✅ factures
- ✅ lignes_facture
- ✅ paiements

### Tables commandes fournisseurs
- ✅ commandes
- ✅ lignes_commande
- ✅ livraisons
- ✅ lignes_livraison

### Données initiales insérées
- ✅ 1 Point de vente (tenant_id=1)
- ✅ 5 Rôles
- ✅ 2 Utilisateurs (G1500 + admin)
- ✅ 1 Dépôt principal

---

## 🚨 Problèmes possibles

### Problème 1 : "database is being accessed by other users"

**Solution :**
```sql
-- Forcer la déconnexion de tous les utilisateurs
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'pointvente_db'
  AND pid <> pg_backend_pid();

-- Puis supprimer la base
DROP DATABASE pointvente_db;
```

### Problème 2 : "permission denied"

**Solution :** Exécuter en tant que superuser
```bash
psql -U postgres -d pointvente_db -f create-full-database.sql
```

### Problème 3 : "table already exists"

Le script utilise `CREATE TABLE IF NOT EXISTS`, donc pas de problème.
Si vous voulez repartir de zéro, supprimez d'abord la base.

### Problème 4 : L'application ne démarre pas

**Solution :**
1. Vérifier que PostgreSQL est démarré
2. Vérifier `application.properties` :
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/pointvente_db
   spring.datasource.username=postgres
   spring.datasource.password=123456
   spring.jpa.hibernate.ddl-auto=update
   ```

---

## 📝 Commandes SQL utiles après création

```sql
-- Compter le nombre de tables
SELECT COUNT(*) as nb_tables 
FROM information_schema.tables 
WHERE table_schema = 'public';

-- Lister tous les utilisateurs
SELECT id, username, email, nom_complet 
FROM users;

-- Ajouter un produit test
INSERT INTO produits (
    reference, designation, prix_vente, 
    actif, point_de_vente_id
)
VALUES (
    'CARR001', 
    'Carreau Test 60x60', 
    1500.00, 
    true, 
    1
);

-- Ajouter un client test
INSERT INTO clients (
    nom, prenom, telephone, 
    categorie, actif, point_de_vente_id
)
VALUES (
    'Dupont', 'Jean', '0550123456',
    'PARTICULIER', true, 1
);

-- Vérifier la cohérence
SELECT 
    'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'produits', COUNT(*) FROM produits
UNION ALL
SELECT 'clients', COUNT(*) FROM clients
UNION ALL
SELECT 'depots', COUNT(*) FROM depots;
```

---

## ✅ Checklist finale

- [ ] Base de données supprimée
- [ ] Base de données recréée
- [ ] Script SQL exécuté sans erreurs
- [ ] Utilisateur G1500 vérifié dans la base
- [ ] Point de vente créé (tenant_id=1)
- [ ] Rôles créés
- [ ] Dépôt principal créé
- [ ] Application Spring Boot redémarrée
- [ ] Test de connexion avec G1500 réussi
- [ ] Token JWT reçu

---

## 🎉 Succès !

Si tous les points de la checklist sont cochés, votre base de données est prête !

Vous pouvez maintenant :
1. ✅ Vous connecter avec G1500
2. ✅ Créer des produits
3. ✅ Créer des clients
4. ✅ Faire des ventes
5. ✅ Gérer le stock
6. ✅ Émettre des factures

---

**Fichier SQL:** `src/main/resources/db/create-full-database.sql`  
**Date:** 2025-01-20  
**Hash password:** `$2a$12$4mHVLBS0/PbQj1RoNdSI/eysmWbgyrgOHHarRD.dxNjvIXCSrwaam`

