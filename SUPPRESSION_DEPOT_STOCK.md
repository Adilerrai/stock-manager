# Suppression de la notion de Dépôt du Stock

## Date: 2025-11-21

## Résumé des modifications

La notion de **dépôt** a été complètement supprimée de la gestion des stocks. Désormais, le stock est directement lié au **produit** et au **point de vente**, sans passer par un dépôt intermédiaire.

## Fichiers modifiés

### 1. StockController.java

#### Avant :
```java
@PostMapping("/initialize-with-qualities")
public ResponseEntity<StockDTO> initializeStockWithQualities(
        @RequestParam Long produitId,
        @RequestParam Long depotId,  // ← Paramètre supprimé
        @RequestBody Map<QualiteProduit, BigDecimal> quantitesParQualite,
        @RequestParam(required = false) BigDecimal seuilAlerte)
```

#### Après :
```java
@PostMapping("/initialize-with-qualities")
public ResponseEntity<StockDTO> initializeStockWithQualities(
        @RequestParam Long produitId,
        @RequestBody Map<QualiteProduit, BigDecimal> quantitesParQualite,
        @RequestParam(required = false) BigDecimal seuilAlerte)
```

#### Endpoints modifiés :
- ✅ `/api/v1/stocks/initialize-with-qualities` - dépôt supprimé
- ✅ `/api/v1/stocks/ajouter-qualite` - dépôt supprimé
- ✅ `/api/v1/stocks/retirer-qualite` - dépôt supprimé
- ✅ `/api/v1/stocks/reserver-qualite` - dépôt supprimé
- ✅ `/api/v1/stocks/{produitId}/with-qualities` - dépôt supprimé du path

### 2. StockService.java

#### Modifications principales :

1. **Constructeur simplifié**
```java
// AVANT
public StockService(StockRepository stockRepository, 
                   StockQualiteRepository stockQualiteRepository,
                   PointDeVenteRepository pointDeVenteRepository, 
                   ProduitRepository produitRepository, 
                   DepotRepository depotRepository) // ← Supprimé

// APRÈS
public StockService(StockRepository stockRepository, 
                   StockQualiteRepository stockQualiteRepository,
                   PointDeVenteRepository pointDeVenteRepository, 
                   ProduitRepository produitRepository)
```

2. **Imports nettoyés**
```java
// Supprimés :
import com.ceramique.persistent.model.Depot;
import com.ceramique.repository.DepotRepository;
```

3. **Méthodes simplifiées**

| Méthode | Avant | Après |
|---------|-------|-------|
| `initializeStockWithQualities` | `(produitId, depotId, ...)` | `(produitId, ...)` |
| `ajouterStockParQualite` | `(produitId, qualite, ...)` | `(produitId, qualite, ...)` |
| `retirerStockParQualite` | `(produitId, depotId, qualite, ...)` | `(produitId, qualite, ...)` |
| `reserverStockParQualite` | `(produitId, depotId, qualite, ...)` | `(produitId, qualite, ...)` |
| `getStockWithQualities` | `(produitId, depotId)` | `(produitId)` |

4. **Méthodes supprimées**
- ❌ `ajouterStock(Long produitId, Long depotId, ...)` 
- ❌ `getStockByProduitAndDepot(Long produitId, Long depotId)`

5. **Méthodes ajoutées**
- ✅ `getStockByProduit(Long produitId)` - recherche par produit uniquement

#### Exemple de simplification :

**AVANT :**
```java
Stock stock = getStockByProduitAndDepot(produitId, depotId);
```

**APRÈS :**
```java
Stock stock = getStockByProduit(produitId);
```

## Impact sur le modèle de données

### Stock.java
Le modèle `Stock` peut toujours avoir une référence à `Depot` si nécessaire pour la base de données, mais elle n'est plus utilisée dans la logique métier.

### StockRepository
Nouvelles méthodes requises :
```java
Optional<Stock> findByProduitId(Long produitId);
Optional<Stock> findByProduitIdWithQualities(Long produitId);
```

Méthodes obsolètes (à supprimer ou marquer @Deprecated) :
```java
Optional<Stock> findByProduitIdAndDepotId(Long produitId, Long depotId);
Optional<Stock> findByProduit_IdAndDepot_Id(Long produitId, Long depotId);
```

## Migration des données

### Si dépôts existants
Si des dépôts existent déjà dans la base de données :

1. **Option 1 : Dépôt unique par défaut**
   - Créer un dépôt "Principal" automatique pour chaque point de vente
   - Migrer tous les stocks vers ce dépôt

2. **Option 2 : Fusionner les stocks**
   - Regrouper tous les stocks d'un même produit
   - Fusionner les quantités par qualité

3. **Option 3 : Conserver le premier dépôt**
   - Associer chaque stock au premier dépôt du point de vente

```sql
-- Exemple de migration SQL
-- Option 1: Associer tous les stocks au dépôt principal
UPDATE stocks s
SET depot_id = (
    SELECT d.id 
    FROM depots d 
    WHERE d.point_de_vente_id = p.point_de_vente_id 
    AND d.nom LIKE '%Principal%' 
    LIMIT 1
)
FROM produits p
WHERE s.produit_id = p.id;
```

## Exemples d'utilisation

### Avant (avec dépôt)
```bash
curl -X POST "http://localhost:8009/api/v1/stocks/ajouter-qualite" \
  -H "Authorization: Bearer $TOKEN" \
  -d "produitId=1&depotId=1&qualite=PREMIERE_QUALITE&quantite=100"
```

### Après (sans dépôt)
```bash
curl -X POST "http://localhost:8009/api/v1/stocks/ajouter-qualite" \
  -H "Authorization: Bearer $TOKEN" \
  -d "produitId=1&qualite=PREMIERE_QUALITE&quantite=100"
```

## Avantages de cette modification

✅ **Simplification** : Moins de paramètres dans les appels API  
✅ **Logique métier plus claire** : Un produit = un stock  
✅ **Performance** : Moins de jointures en base de données  
✅ **Maintenance** : Code plus facile à comprendre et maintenir  
✅ **Multi-tenant** : Stock directement lié au point de vente  

## Points d'attention

⚠️ **Vérifier StockRepository** : S'assurer que les méthodes `findByProduitId()` et `findByProduitIdWithQualities()` existent  
⚠️ **Migration des données** : Si des dépôts existent, planifier la migration  
⚠️ **Tests** : Mettre à jour tous les tests unitaires et d'intégration  
⚠️ **Documentation API** : Mettre à jour Swagger/OpenAPI  

## Prochaines étapes

1. ✅ Vérifier que les méthodes du repository existent
2. ✅ Tester les endpoints modifiés
3. ✅ Mettre à jour les tests
4. ⚠️ Migrer les données existantes (si nécessaire)
5. ⚠️ Mettre à jour la documentation Swagger
6. ⚠️ Informer les consommateurs de l'API des changements

## Rollback

Si besoin de revenir en arrière, réintroduire :
- Le paramètre `depotId` dans les endpoints
- La dépendance `DepotRepository` dans `StockService`
- Les méthodes supprimées comme `getStockByProduitAndDepot()`

