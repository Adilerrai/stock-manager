# API de Recherche de Fournisseurs avec Pagination

## Endpoint
`POST /api/v1/fournisseurs/search`

## Description
Permet de rechercher des fournisseurs selon des critères multiples avec pagination et tri.

## Corps de la requête (JSON)

```json
{
  "raisonSocial": "string (optionnel)",
  "adresse": "string (optionnel)",
  "telephone": "string (optionnel)",
  "email": "string (optionnel)",
  "contact": "string (optionnel)",
  "actif": true/false (optionnel)
}
```

## Paramètres de pagination (Query Parameters)

- `page` : Numéro de la page (commence à 0, défaut: 0)
- `size` : Nombre d'éléments par page (défaut: 20)
- `sort` : Critère de tri au format `champ,direction` (ex: `raisonSocial,asc`)

## Champs disponibles pour le tri

- `raisonSocial` : Raison sociale
- `adresse` : Adresse
- `telephone` : Téléphone
- `email` : Email
- `contact` : Nom du contact
- `actif` : Statut actif
- `dateCreation` : Date de création

## Exemples d'utilisation

### 1. Recherche simple sans critères (tous les fournisseurs actifs)
```bash
curl -X POST "http://localhost:8080/api/v1/fournisseurs/search?page=0&size=10&sort=raisonSocial,asc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{}'
```

### 2. Recherche par raison sociale
```bash
curl -X POST "http://localhost:8080/api/v1/fournisseurs/search?page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "raisonSocial": "ceramique"
  }'
```

### 3. Recherche par ville dans l'adresse
```bash
curl -X POST "http://localhost:8080/api/v1/fournisseurs/search?page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "adresse": "Casablanca"
  }'
```

### 4. Recherche multicritères
```bash
curl -X POST "http://localhost:8080/api/v1/fournisseurs/search?page=0&size=20&sort=raisonSocial,desc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "raisonSocial": "ceramique",
    "adresse": "Casa",
    "actif": true
  }'
```

### 5. Recherche avec tri multiple
```bash
curl -X POST "http://localhost:8080/api/v1/fournisseurs/search?page=0&size=10&sort=actif,desc&sort=raisonSocial,asc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{}'
```

## Format de la réponse

```json
{
  "content": [
    {
      "id": 1,
      "raisonSocial": "Ceramique du Maroc",
      "adresse": "123 Rue des Potiers, Casablanca",
      "telephone": "0522-123456",
      "email": "contact@ceramique-maroc.ma",
      "contact": "Mohammed Alami",
      "actif": true
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 47,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 10,
  "empty": false
}
```

## Métadonnées de pagination

- `content` : Liste des fournisseurs de la page actuelle
- `totalPages` : Nombre total de pages
- `totalElements` : Nombre total d'éléments
- `size` : Taille de la page
- `number` : Numéro de la page actuelle
- `first` : Indique si c'est la première page
- `last` : Indique si c'est la dernière page
- `numberOfElements` : Nombre d'éléments dans la page actuelle
- `empty` : Indique si la page est vide

## Notes importantes

1. **Recherche insensible à la casse** : Les recherches sur les champs texte (raisonSocial, adresse, email, contact) sont insensibles à la casse.

2. **Recherche partielle (LIKE)** : Les critères de recherche utilisent le pattern LIKE avec % (contient), ce qui permet des recherches partielles.

3. **Multi-tenant** : Les résultats sont automatiquement filtrés par le point de vente de l'utilisateur connecté.

4. **Critères combinés** : Tous les critères fournis sont combinés avec l'opérateur AND.

5. **Pagination par défaut** : Si aucun paramètre de pagination n'est fourni, les valeurs par défaut sont appliquées (page=0, size=20, sort=raisonSocial,asc).

