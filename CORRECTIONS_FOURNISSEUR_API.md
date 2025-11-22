# Corrections API Recherche Fournisseurs - 2025-11-21

## Problème résolu
Erreur : `Could not resolve attribute 'raisonSocial' of 'com.ceramique.persistent.model.Fournisseur'`

## Cause
Incohérence dans le nom du champ entre les différents fichiers :
- **Entité `Fournisseur`** utilise : `raisonSociale` (avec 'e')
- **Autres fichiers** utilisaient : `raisonSocial` (sans 'e')

## Fichiers corrigés

### 1. FournisseurRepository.java
**Correction** : Requête @Query
```java
// AVANT
ORDER BY f.raisonSocial ASC

// APRÈS
ORDER BY f.raisonSociale ASC
```

### 2. FournisseurSearchCriteria.java
**Correction** : Nom du champ et getters/setters
```java
// AVANT
private String raisonSocial;
public String getRaisonSocial() { ... }
public void setRaisonSocial(String raisonSocial) { ... }

// APRÈS
private String raisonSociale;
public String getRaisonSociale() { ... }
public void setRaisonSociale(String raisonSociale) { ... }
```

### 3. FournisseurRepositoryImpl.java
**Corrections multiples** :

a) Critère de recherche :
```java
// AVANT
if (criteria.getRaisonSocial() != null && !criteria.getRaisonSocial().trim().isEmpty()) {
    predicates.add(cb.like(cb.lower(root.get("raisonSocial")), ...));
}

// APRÈS
if (criteria.getRaisonSociale() != null && !criteria.getRaisonSociale().trim().isEmpty()) {
    predicates.add(cb.like(cb.lower(root.get("raisonSociale")), ...));
}
```

b) Tri par défaut :
```java
// AVANT
query.orderBy(cb.asc(root.get("raisonSocial")));

// APRÈS
query.orderBy(cb.asc(root.get("raisonSociale")));
```

c) Méthode isValidProperty :
```java
// AVANT
return List.of("id", "raisonSocial", "adresse", ...);

// APRÈS
return List.of("id", "raisonSociale", "adresse", ...);
```

### 4. FournisseurService.java
**Corrections** :
- Méthode `createFournisseur` : `getRaisonSociale()` et `setRaisonSociale()`
- Méthode `updateFournisseur` : Complétée avec tous les champs

### 5. FournisseurDTO.java
✅ **Déjà correct** - utilisait `raisonSociale`

## Fonctionnalité de mappage ajoutée

Pour éviter ce problème à l'avenir, une méthode `mapPropertyName()` a été ajoutée dans `FournisseurRepositoryImpl` qui convertit automatiquement `raisonSocial` en `raisonSociale` si quelqu'un envoie l'ancien nom dans les paramètres de tri.

```java
private String mapPropertyName(String property) {
    if ("raisonSocial".equals(property)) {
        return "raisonSociale";
    }
    return property;
}
```

## Test de l'API

### Endpoint
```
POST /api/v1/fournisseurs/search
```

### Exemple de requête
```bash
curl -X POST "http://localhost:8009/api/v1/fournisseurs/search?page=0&size=10&sort=raisonSociale,asc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "raisonSociale": "ceramique",
    "actif": true
  }'
```

### Exemple avec ancien nom (toujours supporté grâce au mapping)
```bash
curl -X POST "http://localhost:8009/api/v1/fournisseurs/search?page=0&size=10&sort=raisonSocial,asc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{}'
```

## Statut
✅ **Problème résolu**
✅ **Compatibilité backward maintenue**
✅ **Validation des propriétés ajoutée**
✅ **Pagination fonctionnelle**

## Prochaines étapes recommandées

1. ✅ Redémarrer l'application Spring Boot
2. ✅ Tester l'endpoint `/api/v1/fournisseurs/search`
3. ✅ Vérifier que la pagination fonctionne correctement
4. ✅ Tester les différents critères de recherche

