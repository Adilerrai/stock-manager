# 📚 DOCUMENTATION COMPLÈTE DES API - POINT DE VENTE

**Application :** Système de Point de Vente pour Magasin de Céramique  
**Base URL :** `http://localhost:8009`  
**Version :** 1.0  
**Date :** 2025-01-20

---

## 🔐 1. AUTHENTIFICATION & AUTORISATION

### Base Path: `/api/auth`

#### 1.1 Connexion par Username
```http
POST /api/auth/login/username
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "username": "string",    // Required - Nom d'utilisateur
  "password": "string"     // Required - Mot de passe
}
```

**Réponse (200 OK):**
```json
{
  "token": "string",           // JWT Token
  "tokenType": "Bearer",       // Type de token
  "id": "number",              // ID utilisateur
  "email": "string",           // Email
  "username": "string",        // Username
  "nomComplet": "string",      // Nom complet
  "telephone": "string",       // Téléphone
  "genre": "string",           // HOMME/FEMME
  "role": "string",            // Nom du rôle
  "pointDeVenteId": "number"   // ID du point de vente
}
```

**Erreurs possibles:**
- `400 Bad Request` - Données invalides
- `401 Unauthorized` - Identifiants incorrects

---

## 👥 2. GESTION DES UTILISATEURS

### Base Path: `/api/users`

#### 2.1 Créer un utilisateur
```http
POST /api/users/create
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "email": "string",           // Required - Email unique
  "username": "string",        // Required - Username unique
  "password": "string",        // Required - Mot de passe (min 6 caractères)
  "nomComplet": "string",      // Required - Nom complet
  "telephone": "string",       // Optional
  "genre": "string",           // Optional - HOMME/FEMME
  "roleId": "number",          // Required - ID du rôle
  "pointDeVenteId": "number"   // Required - ID du point de vente
}
```

**Réponse (200 OK):**
```json
{
  "id": "number",
  "email": "string",
  "nomComplet": "string",
  "telephone": "string",
  "genre": "string",
  "role": "string",
  "pointDeVenteId": "number"
}
```

#### 2.2 Lister tous les utilisateurs (Debug)
```http
GET /api/users/debug/list
```

**Réponse (200 OK):**
```json
[
  {
    "id": "number",
    "email": "string",
    "username": "string",
    "nomComplet": "string",
    "pointDeVenteId": "number"
  }
]
```

#### 2.3 Vérifier un username
```http
GET /api/users/debug/check/{username}
```

**Paramètres (Path):**
- `username` - Username à vérifier

**Réponse (200 OK):**
```json
{
  "exists": "boolean",
  "id": "number",
  "email": "string",
  "username": "string",
  "nomComplet": "string"
}
```

---

## 🏢 3. GESTION DES POINTS DE VENTE

### Base Path: `/api/points-de-vente`

#### 3.1 Obtenir le point de vente actuel
```http
GET /api/points-de-vente/current
Authorization: Bearer {token}
```

**Réponse (200 OK):**
```json
{
  "id": "number",
  "nomPointDeVente": "string",
  "tenantId": "number"
}
```

---

## 👤 4. GESTION DES CLIENTS

### Base Path: `/api/clients`

#### 4.1 Créer un client
```http
POST /api/clients
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "nom": "string",                          // Required
  "prenom": "string",                       // Optional
  "telephone": "string",                    // Optional
  "email": "string",                        // Optional
  "adresse": "string",                      // Optional
  "ville": "string",                        // Optional
  "codePostal": "string",                   // Optional
  "categorie": "string",                    // PARTICULIER, PROFESSIONNEL, CHANTIER, ARCHITECTE, ENTREPRISE
  "numeroRegistreCommerce": "string",       // Optional - Pour professionnels
  "numeroIdentificationFiscale": "string",  // Optional - NIF
  "creditAutorise": "number",               // Optional - Montant crédit autorisé
  "notes": "string"                         // Optional
}
```

**Réponse (201 Created):**
```json
{
  "id": "number",
  "nom": "string",
  "prenom": "string",
  "nomComplet": "string",
  "telephone": "string",
  "email": "string",
  "adresse": "string",
  "ville": "string",
  "codePostal": "string",
  "categorie": "string",
  "numeroRegistreCommerce": "string",
  "numeroIdentificationFiscale": "string",
  "creditAutorise": "number",
  "creditUtilise": "number",
  "creditDisponible": "number",
  "pointsFidelite": "number",
  "actif": "boolean",
  "dateCreation": "datetime",
  "notes": "string"
}
```

#### 4.2 Obtenir un client
```http
GET /api/clients/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID du client

**Réponse (200 OK):** Même structure que POST

#### 4.3 Modifier un client
```http
PUT /api/clients/{id}
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `id` - ID du client

**Paramètres (Body):** Même structure que POST

**Réponse (200 OK):** Client modifié

#### 4.4 Lister tous les clients
```http
GET /api/clients
Authorization: Bearer {token}
```

**Réponse (200 OK):**
```json
[
  {
    "id": "number",
    "nom": "string",
    "nomComplet": "string",
    "telephone": "string",
    "email": "string",
    "categorie": "string",
    "creditAutorise": "number",
    "creditUtilise": "number",
    "actif": "boolean"
  }
]
```

#### 4.5 Lister les clients actifs
```http
GET /api/clients/actifs
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste de clients

#### 4.6 Lister les clients par catégorie
```http
GET /api/clients/categorie/{categorie}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `categorie` - PARTICULIER, PROFESSIONNEL, CHANTIER, ARCHITECTE, ENTREPRISE

**Réponse (200 OK):** Liste de clients

#### 4.7 Rechercher des clients
```http
GET /api/clients/search?query={query}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `query` - Texte de recherche (nom, téléphone, email)

**Réponse (200 OK):** Liste de clients correspondants

#### 4.8 Trouver un client par téléphone
```http
GET /api/clients/telephone/{telephone}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `telephone` - Numéro de téléphone

**Réponse (200 OK):** Client trouvé ou 404

#### 4.9 Activer un client
```http
PATCH /api/clients/{id}/activer
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID du client

**Réponse (200 OK):** Message de confirmation

#### 4.10 Désactiver un client
```http
PATCH /api/clients/{id}/desactiver
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID du client

**Réponse (200 OK):** Message de confirmation

#### 4.11 Clients avec dépassement de crédit
```http
GET /api/clients/depassement-credit
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des clients en dépassement

---

## 🛒 5. GESTION DES VENTES

### Base Path: `/api/ventes`

#### 5.1 Créer une vente
```http
POST /api/ventes?vendeurId={vendeurId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Query):**
- `vendeurId` - ID du vendeur (user)

**Paramètres (Body):**
```json
{
  "client": {                // Optional - null pour vente sans client
    "id": "number"
  },
  "notes": "string"          // Optional
}
```

**Réponse (201 Created):**
```json
{
  "id": "number",
  "numeroTicket": "string",        // TK-1-20250120-000001
  "dateVente": "datetime",
  "client": {
    "id": "number",
    "nomComplet": "string"
  },
  "vendeur": {
    "id": "number",
    "nomComplet": "string"
  },
  "lignes": [],
  "montantHT": "number",
  "montantTVA": "number",
  "montantTTC": "number",
  "remiseGlobale": "number",
  "montantFinal": "number",
  "statut": "string",              // EN_COURS, VALIDEE, CONFIRMEE, LIVREE, FACTUREE, ANNULEE
  "montantPaye": "number",
  "montantRestant": "number",
  "notes": "string"
}
```

#### 5.2 Ajouter une ligne à la vente
```http
POST /api/ventes/{venteId}/lignes
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Paramètres (Body):**
```json
{
  "produit": {
    "id": "number"               // Required - ID du produit
  },
  "quantite": "number",          // Required - Quantité (boîtes ou m²)
  "surfaceM2": "number",         // Optional - Surface en m² demandée
  "remisePourcentage": "number"  // Optional - Remise en %
}
```

**Réponse (200 OK):** Vente avec la ligne ajoutée

#### 5.3 Supprimer une ligne de vente
```http
DELETE /api/ventes/{venteId}/lignes/{ligneId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `venteId` - ID de la vente
- `ligneId` - ID de la ligne à supprimer

**Réponse (200 OK):** Vente mise à jour

#### 5.4 Valider une vente
```http
POST /api/ventes/{venteId}/valider
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Effet:** 
- Change le statut à VALIDEE
- Déduit le stock automatiquement
- Empêche les modifications ultérieures

**Réponse (200 OK):** Vente validée

#### 5.5 Annuler une vente
```http
POST /api/ventes/{venteId}/annuler?motif={motif}&userId={userId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Paramètres (Query):**
- `motif` - Raison de l'annulation
- `userId` - ID de l'utilisateur qui annule

**Effet:**
- Change le statut à ANNULEE
- Remet le stock si la vente était validée
- Enregistre la traçabilité (qui, quand, pourquoi)

**Réponse (200 OK):** Vente annulée

#### 5.6 Appliquer une remise globale
```http
PATCH /api/ventes/{venteId}/remise?remise={montant}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Paramètres (Query):**
- `remise` - Montant de la remise en DA

**Réponse (200 OK):** Vente avec remise appliquée

#### 5.7 Obtenir une vente
```http
GET /api/ventes/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID de la vente

**Réponse (200 OK):** Détails complets de la vente

#### 5.8 Lister toutes les ventes
```http
GET /api/ventes
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste de toutes les ventes

#### 5.9 Ventes par client
```http
GET /api/ventes/client/{clientId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `clientId` - ID du client

**Réponse (200 OK):** Liste des ventes du client

#### 5.10 Ventes par période
```http
GET /api/ventes/periode?dateDebut={dateDebut}&dateFin={dateFin}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `dateDebut` - Date de début (ISO 8601: 2025-01-01T00:00:00)
- `dateFin` - Date de fin (ISO 8601: 2025-01-31T23:59:59)

**Réponse (200 OK):** Liste des ventes de la période

#### 5.11 Ventes non soldées
```http
GET /api/ventes/non-soldees
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des ventes avec montant restant > 0

#### 5.12 Chiffre d'affaires
```http
GET /api/ventes/chiffre-affaires?dateDebut={dateDebut}&dateFin={dateFin}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `dateDebut` - Date de début
- `dateFin` - Date de fin

**Réponse (200 OK):**
```json
{
  "chiffreAffaires": "number"
}
```

---

## 📄 6. GESTION DES FACTURES

### Base Path: `/api/factures`

#### 6.1 Créer une facture
```http
POST /api/factures?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Query):**
- `userId` - ID de l'utilisateur qui émet la facture

**Paramètres (Body):**
```json
{
  "client": {
    "id": "number"               // Required
  },
  "vente": {                     // Optional - pour lier à une vente
    "id": "number"
  },
  "dateEcheance": "date",        // Optional - Date d'échéance
  "conditionsPaiement": "string", // Optional
  "notes": "string"              // Optional
}
```

**Réponse (201 Created):**
```json
{
  "id": "number",
  "numeroFacture": "string",     // FACT-2025-000001
  "dateFacture": "date",
  "dateEcheance": "date",
  "client": {
    "id": "number",
    "nomComplet": "string"
  },
  "vente": {
    "id": "number",
    "numeroTicket": "string"
  },
  "emisePar": {
    "id": "number",
    "nomComplet": "string"
  },
  "lignes": [],
  "montantHT": "number",
  "montantTVA": "number",
  "montantTTC": "number",
  "remiseGlobale": "number",
  "montantFinal": "number",
  "statut": "string",            // BROUILLON, EN_ATTENTE, VALIDEE, ENVOYEE, PAYEE_PARTIELLEMENT, PAYEE_TOTALEMENT, ANNULEE
  "montantPaye": "number",
  "montantRestant": "number",
  "conditionsPaiement": "string",
  "notes": "string",
  "cheminPdf": "string"
}
```

#### 6.2 Créer une facture depuis une vente
```http
POST /api/factures/depuis-vente/{venteId}?userId={userId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Paramètres (Query):**
- `userId` - ID de l'utilisateur qui émet

**Réponse (201 Created):** Facture créée avec les lignes copiées de la vente

#### 6.3 Obtenir une facture
```http
GET /api/factures/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID de la facture

**Réponse (200 OK):** Détails de la facture

#### 6.4 Lister toutes les factures
```http
GET /api/factures
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste de toutes les factures

#### 6.5 Factures par client
```http
GET /api/factures/client/{clientId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `clientId` - ID du client

**Réponse (200 OK):** Liste des factures du client

#### 6.6 Factures par période
```http
GET /api/factures/periode?dateDebut={dateDebut}&dateFin={dateFin}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `dateDebut` - Date de début (format: 2025-01-01)
- `dateFin` - Date de fin (format: 2025-01-31)

**Réponse (200 OK):** Liste des factures de la période

#### 6.7 Factures impayées
```http
GET /api/factures/impayees
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des factures avec montant restant > 0

#### 6.8 Factures échues
```http
GET /api/factures/echues
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des factures dépassant la date d'échéance

#### 6.9 Valider une facture
```http
POST /api/factures/{factureId}/valider
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `factureId` - ID de la facture

**Réponse (200 OK):** Facture validée

#### 6.10 Annuler une facture
```http
POST /api/factures/{factureId}/annuler?motif={motif}&userId={userId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `factureId` - ID de la facture

**Paramètres (Query):**
- `motif` - Raison de l'annulation
- `userId` - ID de l'utilisateur qui annule

**Réponse (200 OK):** Facture annulée

---

## 💳 7. GESTION DES PAIEMENTS

### Base Path: `/api/paiements`

#### 7.1 Enregistrer un paiement pour une vente
```http
POST /api/paiements/vente/{venteId}?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Paramètres (Query):**
- `userId` - ID de l'utilisateur qui encaisse

**Paramètres (Body):**
```json
{
  "montant": "number",               // Required - Montant du paiement
  "modePaiement": "string",          // Required - ESPECES, CARTE_BANCAIRE, CHEQUE, VIREMENT, CREDIT
  "referencePaiement": "string",     // Optional - Référence
  "nomBanque": "string",             // Optional - Pour chèque/virement
  "numeroCheque": "string",          // Optional - Pour chèque
  "dateEcheance": "datetime",        // Optional - Pour chèque
  "notes": "string"                  // Optional
}
```

**Réponse (201 Created):**
```json
{
  "id": "number",
  "numeroPaiement": "string",    // PAY-1-20250120-000001
  "datePaiement": "datetime",
  "vente": {
    "id": "number",
    "numeroTicket": "string"
  },
  "client": {
    "id": "number",
    "nomComplet": "string"
  },
  "montant": "number",
  "modePaiement": "string",
  "referencePaiement": "string",
  "nomBanque": "string",
  "numeroCheque": "string",
  "dateEcheance": "datetime",
  "encaissePar": {
    "id": "number",
    "nomComplet": "string"
  },
  "notes": "string",
  "annule": "boolean"
}
```

#### 7.2 Enregistrer un paiement pour une facture
```http
POST /api/paiements/facture/{factureId}?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `factureId` - ID de la facture

**Paramètres (Query):**
- `userId` - ID de l'utilisateur qui encaisse

**Paramètres (Body):** Même structure que pour vente

**Réponse (201 Created):** Paiement créé

#### 7.3 Annuler un paiement
```http
POST /api/paiements/{paiementId}/annuler?motif={motif}&userId={userId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `paiementId` - ID du paiement

**Paramètres (Query):**
- `motif` - Raison de l'annulation
- `userId` - ID de l'utilisateur qui annule

**Réponse (200 OK):** Paiement annulé

#### 7.4 Paiements par vente
```http
GET /api/paiements/vente/{venteId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `venteId` - ID de la vente

**Réponse (200 OK):** Liste des paiements de la vente

#### 7.5 Paiements par facture
```http
GET /api/paiements/facture/{factureId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `factureId` - ID de la facture

**Réponse (200 OK):** Liste des paiements de la facture

#### 7.6 Paiements par client
```http
GET /api/paiements/client/{clientId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `clientId` - ID du client

**Réponse (200 OK):** Liste des paiements du client

#### 7.7 Paiements par période
```http
GET /api/paiements/periode?dateDebut={dateDebut}&dateFin={dateFin}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `dateDebut` - Date de début (ISO 8601)
- `dateFin` - Date de fin (ISO 8601)

**Réponse (200 OK):** Liste des paiements de la période

#### 7.8 Total des paiements par période
```http
GET /api/paiements/total-periode?dateDebut={dateDebut}&dateFin={dateFin}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `dateDebut` - Date de début
- `dateFin` - Date de fin

**Réponse (200 OK):**
```json
{
  "total": "number"
}
```

#### 7.9 Total par mode de paiement
```http
GET /api/paiements/total-mode-paiement?modePaiement={mode}&dateDebut={dateDebut}&dateFin={dateFin}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `modePaiement` - ESPECES, CARTE_BANCAIRE, CHEQUE, VIREMENT, CREDIT
- `dateDebut` - Date de début
- `dateFin` - Date de fin

**Réponse (200 OK):**
```json
{
  "total": "number",
  "modePaiement": "string"
}
```

---

## 📦 8. GESTION DES PRODUITS

### Base Path: `/api/produits`

#### 8.1 Créer un produit
```http
POST /api/produits
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "reference": "string",                // Required - Unique
  "designation": "string",              // Required
  "description": "string",              // Required
  "longueurCm": "number",               // Optional
  "largeurCm": "number",                // Optional
  "epaisseurMm": "number",              // Optional
  "format": "string",                   // Optional - Ex: "60x60"
  "codeBarre": "string",                // Optional
  "couleur": "string",                  // Optional
  "texture": "string",                  // Optional
  "finition": "string",                 // Optional - Mat, Brillant, Satiné
  "origine": "string",                  // Optional - Pays d'origine
  "serie": "string",                    // Optional
  "surfaceParBoiteM2": "number",        // Optional - Surface couverte par boîte
  "quantiteParBoite": "number",         // Optional - Nombre de carreaux par boîte
  "categorieArticle": "string",         // Optional - SOL, MUR, EXTERIEUR, FAIENCE, GRES, MARBRE, etc.
  "groupeArticle": "string",            // Optional
  "prixAchat": "number",                // Optional
  "prixVente": "number",                // Required
  "uniteMesureStock": "string"          // Optional - M2, UNITE, BOITE
}
```

**Réponse (201 Created):** Produit créé

#### 8.2 Obtenir un produit
```http
GET /api/produits/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID du produit

**Réponse (200 OK):** Détails du produit

#### 8.3 Modifier un produit
```http
PUT /api/produits/{id}
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `id` - ID du produit

**Paramètres (Body):** Même structure que POST

**Réponse (200 OK):** Produit modifié

#### 8.4 Lister tous les produits
```http
GET /api/produits
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste de tous les produits

#### 8.5 Rechercher des produits
```http
GET /api/produits/search?query={query}
Authorization: Bearer {token}
```

**Paramètres (Query):**
- `query` - Texte de recherche (référence, designation)

**Réponse (200 OK):** Liste des produits correspondants

#### 8.6 Uploader une image produit
```http
POST /api/produits/{id}/image
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Paramètres (Path):**
- `id` - ID du produit

**Paramètres (Form):**
- `file` - Fichier image (JPEG, PNG)

**Réponse (200 OK):** Message de confirmation

---

## 📊 9. GESTION DU STOCK

### Base Path: `/api/stocks`

#### 9.1 Initialiser un stock avec qualités
```http
POST /api/stocks/init
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "produitId": "number",           // Required
  "depotId": "number",             // Required
  "quantitesParQualite": {
    "PREMIERE_QUALITE": "number",  // Quantité
    "DEUXIEME_QUALITE": "number",
    "TROISIEME_QUALITE": "number"
  },
  "seuilAlerte": "number"          // Seuil d'alerte
}
```

**Réponse (201 Created):** Stock créé

#### 9.2 Ajouter au stock par qualité
```http
POST /api/stocks/ajouter
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "produitId": "number",
  "depotId": "number",
  "qualite": "string",         // PREMIERE_QUALITE, DEUXIEME_QUALITE, TROISIEME_QUALITE
  "quantite": "number"
}
```

**Réponse (200 OK):** Stock mis à jour

#### 9.3 Retirer du stock par qualité
```http
POST /api/stocks/retirer
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "produitId": "number",
  "depotId": "number",
  "qualite": "string",
  "quantite": "number"
}
```

**Réponse (200 OK):** Stock mis à jour

#### 9.4 Réserver du stock
```http
POST /api/stocks/reserver
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "produitId": "number",
  "depotId": "number",
  "qualite": "string",
  "quantite": "number"
}
```

**Réponse (200 OK):** Confirmation de réservation

#### 9.5 Obtenir le stock avec qualités
```http
GET /api/stocks/{produitId}/{depotId}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `produitId` - ID du produit
- `depotId` - ID du dépôt

**Réponse (200 OK):**
```json
{
  "id": "number",
  "produit": {
    "id": "number",
    "reference": "string",
    "designation": "string"
  },
  "depot": {
    "id": "number",
    "nom": "string"
  },
  "stocksQualite": [
    {
      "id": "number",
      "qualite": "string",
      "quantiteDisponible": "number",
      "quantiteReservee": "number",
      "seuilAlerte": "number"
    }
  ]
}
```

#### 9.6 Lister tous les stocks
```http
GET /api/stocks
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste de tous les stocks

#### 9.7 Stocks en alerte
```http
GET /api/stocks/alertes
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des stocks sous le seuil d'alerte

#### 9.8 Stocks par qualité
```http
GET /api/stocks/qualite/{qualite}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `qualite` - PREMIERE_QUALITE, DEUXIEME_QUALITE, TROISIEME_QUALITE

**Réponse (200 OK):** Liste des stocks de cette qualité

---

## 🏭 10. GESTION DES DÉPÔTS

### Base Path: `/api/depots`

#### 10.1 Créer un dépôt
```http
POST /api/depots
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "nom": "string",              // Required
  "description": "string",      // Optional
  "adresse": "string",          // Optional
  "responsable": "string",      // Optional
  "telephone": "string",        // Optional
  "capaciteTotale": "number"    // Optional
}
```

**Réponse (201 Created):** Dépôt créé

#### 10.2 Lister tous les dépôts
```http
GET /api/depots
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des dépôts

#### 10.3 Obtenir un dépôt
```http
GET /api/depots/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID du dépôt

**Réponse (200 OK):** Détails du dépôt

---

## 🏢 11. GESTION DES FOURNISSEURS

### Base Path: `/api/fournisseurs`

#### 11.1 Créer un fournisseur
```http
POST /api/fournisseurs
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "nom": "string",                          // Required
  "codeFournisseur": "string",              // Optional - Unique
  "adresse": "string",                      // Optional
  "ville": "string",                        // Optional
  "telephone": "string",                    // Optional
  "email": "string",                        // Optional
  "numeroRegistreCommerce": "string",       // Optional
  "numeroIdentificationFiscale": "string",  // Optional
  "conditionsPaiement": "string",           // Optional
  "delaiLivraisonJours": "number"           // Optional
}
```

**Réponse (201 Created):** Fournisseur créé

#### 11.2 Lister tous les fournisseurs
```http
GET /api/fournisseurs
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des fournisseurs

#### 11.3 Obtenir un fournisseur
```http
GET /api/fournisseurs/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID du fournisseur

**Réponse (200 OK):** Détails du fournisseur

---

## 📋 12. GESTION DES COMMANDES FOURNISSEURS

### Base Path: `/api/commandes`

#### 12.1 Créer une commande
```http
POST /api/commandes
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "fournisseur": {
    "id": "number"                  // Required
  },
  "dateLivraisonPrevue": "date",    // Optional
  "notes": "string"                 // Optional
}
```

**Réponse (201 Created):** Commande créée

#### 12.2 Ajouter une ligne à la commande
```http
POST /api/commandes/{commandeId}/lignes
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `commandeId` - ID de la commande

**Paramètres (Body):**
```json
{
  "produit": {
    "id": "number"
  },
  "quantite": "number",
  "prixUnitaire": "number",
  "qualite": "string",          // PREMIERE_QUALITE, etc.
  "notes": "string"
}
```

**Réponse (200 OK):** Commande mise à jour

#### 12.3 Valider une commande
```http
POST /api/commandes/{commandeId}/valider
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `commandeId` - ID de la commande

**Réponse (200 OK):** Commande validée

#### 12.4 Lister toutes les commandes
```http
GET /api/commandes
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des commandes

#### 12.5 Obtenir une commande
```http
GET /api/commandes/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID de la commande

**Réponse (200 OK):** Détails de la commande

---

## 🚚 13. GESTION DES LIVRAISONS

### Base Path: `/api/livraisons`

#### 13.1 Créer une livraison
```http
POST /api/livraisons
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Body):**
```json
{
  "commande": {
    "id": "number"              // Required
  },
  "depot": {
    "id": "number"              // Required - Dépôt de destination
  },
  "dateLivraison": "datetime",  // Optional - Par défaut maintenant
  "notes": "string"             // Optional
}
```

**Réponse (201 Created):** Livraison créée

#### 13.2 Enregistrer une réception
```http
POST /api/livraisons/{livraisonId}/receptionner
Authorization: Bearer {token}
Content-Type: application/json
```

**Paramètres (Path):**
- `livraisonId` - ID de la livraison

**Paramètres (Body):**
```json
{
  "lignesLivraison": [
    {
      "ligneCommandeId": "number",
      "quantiteLivree": "number",
      "qualite": "string"
    }
  ],
  "recuParUserId": "number"
}
```

**Réponse (200 OK):** Livraison réceptionnée (stock mis à jour)

#### 13.3 Lister toutes les livraisons
```http
GET /api/livraisons
Authorization: Bearer {token}
```

**Réponse (200 OK):** Liste des livraisons

#### 13.4 Obtenir une livraison
```http
GET /api/livraisons/{id}
Authorization: Bearer {token}
```

**Paramètres (Path):**
- `id` - ID de la livraison

**Réponse (200 OK):** Détails de la livraison

---

## 📈 14. STATISTIQUES & RAPPORTS

### Base Path: `/api/stats`

> **Note:** Ces endpoints ne sont pas encore implémentés mais sont planifiés

#### 14.1 Dashboard KPI
```http
GET /api/stats/dashboard
Authorization: Bearer {token}
```

**Réponse attendue:**
```json
{
  "caJour": "number",
  "caHier": "number",
  "caMois": "number",
  "nbVentesJour": "number",
  "nbClientsNouveaux": "number",
  "stocksEnAlerte": "number",
  "facturesEchues": "number",
  "topProduits": [
    {
      "produit": "string",
      "quantiteVendue": "number",
      "montantTotal": "number"
    }
  ]
}
```

---

## 🔑 CODES DE RÉPONSE HTTP

| Code | Signification | Description |
|------|---------------|-------------|
| 200  | OK | Requête réussie |
| 201  | Created | Ressource créée avec succès |
| 204  | No Content | Requête réussie sans contenu |
| 400  | Bad Request | Paramètres invalides |
| 401  | Unauthorized | Non authentifié |
| 403  | Forbidden | Non autorisé |
| 404  | Not Found | Ressource non trouvée |
| 409  | Conflict | Conflit (ex: doublon) |
| 500  | Internal Server Error | Erreur serveur |

---

## 🔐 AUTHENTIFICATION

Toutes les API (sauf `/api/auth/login/username`) nécessitent un token JWT dans le header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Obtention du token:**
1. Faire une requête POST sur `/api/auth/login/username`
2. Récupérer le `token` de la réponse
3. L'inclure dans le header `Authorization` de toutes les requêtes suivantes

---

## 📝 FORMATS DE DONNÉES

### Dates
- **Date seule:** `YYYY-MM-DD` (ex: `2025-01-20`)
- **Date et heure:** ISO 8601 `YYYY-MM-DDTHH:mm:ss` (ex: `2025-01-20T14:30:00`)

### Montants
- Format: `number` (virgule flottante)
- Devise: DA (Dinar Algérien)
- Exemple: `1500.50`

### Énumérations

#### CategorieClient
- `PARTICULIER`
- `PROFESSIONNEL`
- `CHANTIER`
- `ARCHITECTE`
- `ENTREPRISE`

#### StatutVente
- `EN_COURS`
- `VALIDEE`
- `CONFIRMEE`
- `LIVREE`
- `FACTUREE`
- `ANNULEE`

#### StatutFacture
- `BROUILLON`
- `EN_ATTENTE`
- `VALIDEE`
- `ENVOYEE`
- `PAYEE_PARTIELLEMENT`
- `PAYEE_TOTALEMENT`
- `ANNULEE`
- `EN_RETARD`

#### ModePaiement
- `ESPECES`
- `CARTE_BANCAIRE`
- `CHEQUE`
- `VIREMENT`
- `CREDIT`

#### QualiteProduit
- `PREMIERE_QUALITE`
- `DEUXIEME_QUALITE`
- `TROISIEME_QUALITE`

#### CategorieArticle
- `SOL`
- `MUR`
- `EXTERIEUR`
- `FAIENCE`
- `GRES`
- `MARBRE`
- `GRANITE`
- `PORCELAINE`
- `MOSAIQUE`
- `ACCESSOIRES`

---

## 🧪 EXEMPLES D'UTILISATION

### Exemple complet : Créer une vente et l'encaisser

```bash
# 1. Se connecter
POST http://localhost:8009/api/auth/login/username
{
  "username": "G1500",
  "password": "votre_mot_de_passe"
}
# Réponse: { "token": "xxx..." }

# 2. Créer une vente
POST http://localhost:8009/api/ventes?vendeurId=1
Authorization: Bearer xxx...
{
  "client": { "id": 5 }
}
# Réponse: { "id": 100, "numeroTicket": "TK-1-20250120-000001", ... }

# 3. Ajouter un produit
POST http://localhost:8009/api/ventes/100/lignes
Authorization: Bearer xxx...
{
  "produit": { "id": 10 },
  "quantite": 5,
  "surfaceM2": 22.5
}

# 4. Valider la vente
POST http://localhost:8009/api/ventes/100/valider
Authorization: Bearer xxx...

# 5. Encaisser
POST http://localhost:8009/api/paiements/vente/100?userId=1
Authorization: Bearer xxx...
{
  "montant": 15000.00,
  "modePaiement": "ESPECES"
}
```

---

## 📞 SUPPORT

Pour toute question sur l'utilisation des API :
- Consulter Swagger UI : `http://localhost:8009/swagger-ui.html`
- Vérifier les logs serveur en cas d'erreur 500
- Consulter la documentation technique dans le code source

---

**Date de création :** 2025-01-20  
**Version API :** 1.0  
**Port par défaut :** 8009  
**Base de données :** PostgreSQL

