# 🔧 RÉSOLUTION PROBLÈME D'AUTHENTIFICATION

## ❌ Problème
L'erreur "Utilisateur non trouvé dans ce point de vente" apparaît lors de la tentative de connexion avec :
- **Username:** G1500
- **Password:** admin123

## 🔍 Diagnostic

### Étape 1 : Vérifier les utilisateurs existants
Appelez l'API de debug pour voir tous les utilisateurs :
```
GET http://localhost:8009/api/users/debug/list
```

### Étape 2 : Vérifier si le username G1500 existe
```
GET http://localhost:8009/api/users/debug/check/G1500
```

---

## ✅ SOLUTIONS

### Solution 1 : Mettre à jour un utilisateur existant (RECOMMANDÉ)

Si vous avez déjà un utilisateur admin, mettez à jour son username :

```sql
-- 1. Vérifier les utilisateurs
SELECT id, email, username, nom_complet FROM users;

-- 2. Mettre à jour l'utilisateur (remplacer ID par votre ID)
UPDATE users 
SET username = 'G1500' 
WHERE id = 1; -- ou WHERE email = 'admin@example.com'

-- 3. Vérifier
SELECT id, email, username, nom_complet FROM users WHERE username = 'G1500';
```

---

### Solution 2 : Créer un nouvel utilisateur via SQL

```sql
-- 1. Vérifier le point de vente
SELECT id, nom FROM point_de_vente;

-- 2. Vérifier les rôles disponibles
SELECT id, nom FROM roles;

-- 3. Créer l'utilisateur G1500
-- Note: Le password 'admin123' hashé avec BCrypt
INSERT INTO users (
    email, 
    username, 
    password, 
    nom_complet, 
    point_de_vente_id, 
    role_id, 
    account_non_expired, 
    account_non_locked, 
    credentials_non_expired, 
    enabled
)
VALUES (
    'g1500@magasin.dz',
    'G1500',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/IH9ejEpR8xKEjpFAhCy3xyXJSP.4ZG', -- Hash de 'admin123'
    'Caissier G1500',
    1, -- ID du point de vente (à vérifier)
    2, -- ID du rôle CAISSIER (à vérifier)
    true,
    true,
    true,
    true
);
```

---

### Solution 3 : Créer via l'API

```bash
POST http://localhost:8009/api/users/create
Content-Type: application/json
Authorization: Bearer <ADMIN_TOKEN>

{
  "email": "g1500@magasin.dz",
  "username": "G1500",
  "password": "admin123",
  "nomComplet": "Caissier G1500",
  "telephone": "0550000000",
  "roleId": 2,
  "pointDeVenteId": 1
}
```

---

## 🔐 Générer un Hash BCrypt pour un mot de passe

Si vous voulez créer un hash pour un autre mot de passe :

### En Java (dans le code) :
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("admin123");
System.out.println(hash);
```

### Ou utiliser un outil en ligne :
https://bcrypt-generator.com/
- Entrer : `admin123`
- Rounds : 10
- Copier le hash généré

---

## 📝 Vérification après correction

### Test 1 : Vérifier en base
```sql
SELECT id, email, username, nom_complet, point_de_vente_id 
FROM users 
WHERE username = 'G1500';
```

**Résultat attendu :**
```
id | email               | username | nom_complet      | point_de_vente_id
1  | g1500@magasin.dz   | G1500    | Caissier G1500  | 1
```

### Test 2 : Tester l'authentification
```bash
POST http://localhost:8009/api/auth/login/username
Content-Type: application/json

{
  "username": "G1500",
  "password": "admin123"
}
```

**Résultat attendu :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "email": "g1500@magasin.dz",
  "username": "G1500",
  "nomComplet": "Caissier G1500",
  "role": "CAISSIER",
  "pointDeVenteId": 1
}
```

---

## 🚨 Problèmes courants

### Problème 1 : Username est NULL
**Symptôme :** `username = null` dans la base

**Solution :**
```sql
UPDATE users SET username = 'G1500' WHERE id = 1;
```

### Problème 2 : Mauvais hash de mot de passe
**Symptôme :** Authentification échoue avec "Bad credentials"

**Solution :** Régénérer le hash BCrypt
```sql
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMye/IH9ejEpR8xKEjpFAhCy3xyXJSP.4ZG'
WHERE username = 'G1500';
```

### Problème 3 : Point de vente manquant
**Symptôme :** `point_de_vente_id is null`

**Solution :**
```sql
-- Vérifier les points de vente
SELECT id, nom FROM point_de_vente;

-- Assigner à l'utilisateur
UPDATE users 
SET point_de_vente_id = 1 
WHERE username = 'G1500';
```

### Problème 4 : Rôle manquant
**Symptôme :** `role_id is null`

**Solution :**
```sql
-- Vérifier les rôles
SELECT id, nom FROM roles;

-- Assigner un rôle
UPDATE users 
SET role_id = 2 
WHERE username = 'G1500';
```

---

## 🎯 SOLUTION RAPIDE (Copy-Paste)

Si vous voulez simplement faire fonctionner l'authentification rapidement :

```sql
-- Tout-en-un : Vérifier et corriger
UPDATE users 
SET 
    username = 'G1500',
    password = '$2a$10$N9qo8uLOickgx2ZMRZoMye/IH9ejEpR8xKEjpFAhCy3xyXJSP.4ZG',
    nom_complet = 'Caissier G1500',
    enabled = true,
    account_non_expired = true,
    account_non_locked = true,
    credentials_non_expired = true
WHERE id = 1;

-- Vérifier
SELECT id, email, username, nom_complet FROM users WHERE username = 'G1500';
```

---

## 📞 Endpoints de Debug ajoutés

J'ai ajouté ces endpoints pour vous aider :

1. **Liste tous les utilisateurs :**
   ```
   GET http://localhost:8009/api/users/debug/list
   ```

2. **Vérifier un username :**
   ```
   GET http://localhost:8009/api/users/debug/check/G1500
   ```

Ces endpoints vous montreront exactement quels utilisateurs existent et quels sont leurs usernames.

---

## ✅ Checklist finale

- [ ] Exécuter les requêtes SQL de vérification
- [ ] Mettre à jour ou créer l'utilisateur G1500
- [ ] Vérifier que le username est bien "G1500" (pas null)
- [ ] Vérifier que le password est bien hashé en BCrypt
- [ ] Vérifier que point_de_vente_id n'est pas null
- [ ] Vérifier que role_id n'est pas null
- [ ] Tester l'authentification via Postman
- [ ] Vérifier les logs de la console

---

**Date de création :** 2025-01-20  
**Problème :** Authentification username G1500  
**Status :** Endpoints de debug ajoutés + Script SQL de correction créé
ript 