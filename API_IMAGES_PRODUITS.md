# API d'Upload d'Images pour les Produits

## Vue d'ensemble

L'API permet d'attacher une image à chaque produit. Chaque produit peut avoir **une seule image**.

## Endpoints disponibles

### 1. Créer un produit avec image (Création rapide)

**Endpoint:** `POST /api/v1/produits/quick-add-with-image`

**Type:** `multipart/form-data`

**Paramètres:**
- `reference` (String, requis) : Référence du produit
- `designation` (String, requis) : Nom du produit
- `description` (String, requis) : Description
- `prixAchat` (String, requis) : Prix d'achat
- `prixVente` (String, requis) : Prix de vente
- `image` (File, optionnel) : Fichier image (JPG, PNG, etc.)

**Exemple avec cURL:**
```bash
curl -X POST "http://localhost:8009/api/v1/produits/quick-add-with-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "reference=CARRELAGE-001" \
  -F "designation=Carrelage 60x60 Blanc" \
  -F "description=Carrelage en céramique blanc brillant" \
  -F "prixAchat=150.00" \
  -F "prixVente=250.00" \
  -F "image=@/chemin/vers/image.jpg"
```

**Exemple avec Postman:**
1. Sélectionner POST
2. URL: `http://localhost:8009/api/v1/produits/quick-add-with-image`
3. Headers: `Authorization: Bearer YOUR_JWT_TOKEN`
4. Body: form-data
   - reference: CARRELAGE-001
   - designation: Carrelage 60x60 Blanc
   - description: Carrelage céramique
   - prixAchat: 150.00
   - prixVente: 250.00
   - image: [Sélectionner fichier]

**Réponse (200 OK):**
```json
{
  "id": 1,
  "reference": "CARRELAGE-001",
  "designation": "Carrelage 60x60 Blanc",
  "description": "Carrelage en céramique blanc brillant",
  "prixAchat": 150.00,
  "prixVente": 250.00,
  "image": {
    "id": 1,
    "fileName": "image.jpg",
    "contentType": "image/jpeg",
    "dateUpload": "2025-11-21T20:30:00"
  },
  "actif": true
}
```

---

### 2. Uploader une image pour un produit existant

**Endpoint:** `POST /api/v1/produits/{id}/upload-image`

**Type:** `multipart/form-data`

**Paramètres:**
- `id` (Path) : ID du produit
- `image` (File, requis) : Fichier image

**Exemple avec cURL:**
```bash
curl -X POST "http://localhost:8009/api/v1/produits/1/upload-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "image=@/chemin/vers/image.jpg"
```

**Exemple avec Postman:**
1. Sélectionner POST
2. URL: `http://localhost:8009/api/v1/produits/1/upload-image`
3. Headers: `Authorization: Bearer YOUR_JWT_TOKEN`
4. Body: form-data
   - image: [Sélectionner fichier]

**Réponse (200 OK):**
```json
{
  "id": 1,
  "reference": "CARRELAGE-001",
  "designation": "Carrelage 60x60 Blanc",
  "image": {
    "id": 2,
    "fileName": "nouvelle-image.jpg",
    "contentType": "image/jpeg",
    "dateUpload": "2025-11-21T20:35:00"
  }
}
```

**Note:** Si une image existe déjà, elle sera **remplacée** par la nouvelle.

---

### 3. Récupérer l'image d'un produit

**Endpoint:** `GET /api/v1/produits/{id}/image`

**Paramètres:**
- `id` (Path) : ID du produit

**Exemple avec cURL:**
```bash
curl -X GET "http://localhost:8009/api/v1/produits/1/image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output produit-image.jpg
```

**Exemple d'utilisation dans HTML:**
```html
<img src="http://localhost:8009/api/v1/produits/1/image" 
     alt="Image du produit"
     style="max-width: 300px;" />
```

**Exemple d'utilisation en JavaScript:**
```javascript
fetch('http://localhost:8009/api/v1/produits/1/image', {
  headers: {
    'Authorization': 'Bearer ' + token
  }
})
.then(response => response.blob())
.then(blob => {
  const imageUrl = URL.createObjectURL(blob);
  document.getElementById('produit-image').src = imageUrl;
});
```

**Réponse:**
- Status: 200 OK
- Content-Type: image/jpeg (ou image/png, etc.)
- Body: Données binaires de l'image

**Réponse si pas d'image:**
- Status: 404 Not Found

---

### 4. Supprimer l'image d'un produit

**Endpoint:** `DELETE /api/v1/produits/{id}/image`

**Paramètres:**
- `id` (Path) : ID du produit

**Exemple avec cURL:**
```bash
curl -X DELETE "http://localhost:8009/api/v1/produits/1/image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Réponse (204 No Content):**
Pas de contenu, l'image a été supprimée.

---

### 5. Créer un produit normal (sans image)

**Endpoint:** `POST /api/v1/produits/add`

**Type:** `application/json`

**Exemple:**
```bash
curl -X POST "http://localhost:8009/api/v1/produits/add" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reference": "CARRELAGE-002",
    "designation": "Carrelage 30x60 Gris",
    "description": "Carrelage gris mat",
    "prixAchat": 120.00,
    "prixVente": 200.00
  }'
```

Ensuite, vous pouvez ajouter l'image avec l'endpoint `/upload-image`.

---

### 6. Récupérer un produit avec son image

**Endpoint:** `GET /api/v1/produits/get/{id}`

**Réponse:**
```json
{
  "id": 1,
  "reference": "CARRELAGE-001",
  "designation": "Carrelage 60x60 Blanc",
  "description": "Carrelage en céramique blanc brillant",
  "prixAchat": 150.00,
  "prixVente": 250.00,
  "image": {
    "id": 1,
    "fileName": "image.jpg",
    "contentType": "image/jpeg",
    "base64Data": "/9j/4AAQSkZJRgABAQEA...", // Image encodée en base64
    "dateUpload": "2025-11-21T20:30:00"
  },
  "actif": true
}
```

---

## Fonctionnalités

### ✅ Compression automatique
Les images sont **automatiquement compressées** lors de l'upload pour optimiser l'espace de stockage et la vitesse de chargement.

### ✅ Types d'images supportés
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- BMP (.bmp)
- WEBP (.webp)

### ✅ Stockage
Les images sont stockées dans la base de données PostgreSQL dans la table `produit_images`.

### ✅ Multi-tenant
Chaque produit et son image sont automatiquement liés au point de vente de l'utilisateur connecté.

---

## Scénarios d'utilisation

### Scénario 1: Création rapide avec image
```bash
# 1. Créer le produit avec image en une seule requête
curl -X POST "http://localhost:8009/api/v1/produits/quick-add-with-image" \
  -H "Authorization: Bearer $TOKEN" \
  -F "reference=PROD-001" \
  -F "designation=Carrelage Blanc" \
  -F "description=Belle qualité" \
  -F "prixAchat=100" \
  -F "prixVente=180" \
  -F "image=@image.jpg"
```

### Scénario 2: Créer puis ajouter l'image
```bash
# 1. Créer le produit
PRODUIT=$(curl -X POST "http://localhost:8009/api/v1/produits/add" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reference":"PROD-002","designation":"Carrelage Gris","description":"Qualité premium","prixAchat":120,"prixVente":200}' \
  | jq -r '.id')

# 2. Uploader l'image
curl -X POST "http://localhost:8009/api/v1/produits/$PRODUIT/upload-image" \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@image.jpg"
```

### Scénario 3: Remplacer une image
```bash
# Simplement uploader une nouvelle image (remplace l'ancienne)
curl -X POST "http://localhost:8009/api/v1/produits/1/upload-image" \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@nouvelle-image.jpg"
```

### Scénario 4: Afficher l'image dans une application web
```html
<!DOCTYPE html>
<html>
<head>
    <title>Catalogue Produits</title>
</head>
<body>
    <h1>Nos Produits</h1>
    <div id="produits"></div>

    <script>
        const token = 'YOUR_JWT_TOKEN';
        
        // Récupérer la liste des produits
        fetch('http://localhost:8009/api/v1/produits/all', {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(res => res.json())
        .then(produits => {
            const container = document.getElementById('produits');
            produits.forEach(produit => {
                const div = document.createElement('div');
                div.innerHTML = `
                    <h3>${produit.designation}</h3>
                    <p>${produit.description}</p>
                    <p>Prix: ${produit.prixVente} MAD</p>
                    <img src="http://localhost:8009/api/v1/produits/${produit.id}/image" 
                         alt="${produit.designation}"
                         style="max-width: 200px; height: auto;"
                         onerror="this.style.display='none'" />
                `;
                container.appendChild(div);
            });
        });
    </script>
</body>
</html>
```

---

## Limitations

- **Une seule image par produit** : Si vous uploadez une nouvelle image, l'ancienne sera remplacée.
- **Taille maximale** : Par défaut, Spring Boot limite la taille des fichiers. Vérifier `application.properties` pour `spring.servlet.multipart.max-file-size`.
- **Format de l'image** : Les images sont compressées automatiquement, donc la qualité peut être légèrement réduite.

---

## Configuration

Dans `application.properties`, vous pouvez configurer :

```properties
# Taille maximale d'un fichier
spring.servlet.multipart.max-file-size=10MB

# Taille maximale d'une requête
spring.servlet.multipart.max-request-size=10MB
```

---

## Codes d'erreur

| Code | Description |
|------|-------------|
| 200 | Succès |
| 204 | Suppression réussie (pas de contenu) |
| 404 | Produit ou image non trouvé |
| 500 | Erreur serveur (problème d'upload ou compression) |

---

## Support

Pour toute question ou problème, vérifier les logs de l'application Spring Boot.

