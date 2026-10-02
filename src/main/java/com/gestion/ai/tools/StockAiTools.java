package com.gestion.ai.tools;

import com.gestion.persistent.model.Produit;
import com.gestion.persistent.model.Stock;
import com.gestion.repository.ProduitRepository;
import com.gestion.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class StockAiTools {

    private final StockRepository stockRepository;
    private final ProduitRepository produitRepository;

    public StockAiTools(StockRepository stockRepository, ProduitRepository produitRepository) {
        this.stockRepository = stockRepository;
        this.produitRepository = produitRepository;
    }

    /**
     * Recherche les stocks disponibles et informations catalogue pour le tenant
     */
    public List<Map<String, Object>> consulterStock(Long tenantId, String recherche) {
        return consulterStock(tenantId, recherche, Set.of(tenantId));
    }

    public List<Map<String, Object>> consulterStock(Long tenantId, String recherche, Set<Long> accessibleTenantIds) {
        // 1. Récupération des produits du tenant ou de l'arbre accessible
        List<Produit> produits = new ArrayList<>();
        if (tenantId != null) {
            produits.addAll(produitRepository.findByPointDeVenteId(tenantId));
        }

        // Si aucun produit sur le tenant spécifique et qu'on a d'autres tenants autorisés (ex: Holding/Filiale)
        if (produits.isEmpty() && accessibleTenantIds != null) {
            for (Long tId : accessibleTenantIds) {
                if (!Objects.equals(tId, tenantId)) {
                    produits.addAll(produitRepository.findByPointDeVenteId(tId));
                }
            }
        }

        // 2. Nettoyage des mots parasites dans la recherche (ex: "le produits cable" -> "cable")
        String cleanedQuery = cleanSearchQuery(recherche);

        List<Map<String, Object>> resultats = new ArrayList<>();

        for (Produit p : produits) {
            Stock s = stockRepository.findByProduitId(p.getId()).orElse(null);

            BigDecimal quantiteDispo = (s != null && s.getQuantiteDisponible() != null)
                    ? s.getQuantiteDisponible() : BigDecimal.ZERO;

            BigDecimal seuilAlerte = (s != null && s.getSeuilAlerte() != null)
                    ? s.getSeuilAlerte() : (p.getStockMinimum() != null ? p.getStockMinimum() : BigDecimal.ZERO);

            boolean match = cleanedQuery.isEmpty() || matchesProduct(p, cleanedQuery);

            boolean enAlerte = quantiteDispo.compareTo(seuilAlerte) <= 0;

            if (match) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("produitId", p.getId());
                item.put("designation", p.getNom() != null ? p.getNom() : p.getDesignation());
                item.put("reference", p.getReference() != null ? p.getReference() : "-");
                item.put("quantiteDisponible", quantiteDispo);
                item.put("seuilAlerte", seuilAlerte);
                item.put("prixVenteHT", p.getPrixVenteHT());
                item.put("prixVenteTTC", p.getPrixVenteTTC());
                item.put("enAlerte", enAlerte);
                item.put("statutStock", quantiteDispo.compareTo(BigDecimal.ZERO) > 0 ? "EN_STOCK" : "RUPTURE_OU_NON_INITIALISE");
                resultats.add(item);
            }
        }

        return resultats;
    }

    /**
     * Liste tous les articles en alerte de stock (quantité <= seuil d'alerte)
     */
    public List<Map<String, Object>> alertesRuptureStock(Long tenantId) {
        return alertesRuptureStock(tenantId, Set.of(tenantId));
    }

    public List<Map<String, Object>> alertesRuptureStock(Long tenantId, Set<Long> accessibleTenantIds) {
        List<Produit> produits = new ArrayList<>(produitRepository.findByPointDeVenteId(tenantId));
        if (produits.isEmpty() && accessibleTenantIds != null) {
            for (Long tId : accessibleTenantIds) {
                if (!Objects.equals(tId, tenantId)) {
                    produits.addAll(produitRepository.findByPointDeVenteId(tId));
                }
            }
        }

        List<Map<String, Object>> alertes = new ArrayList<>();
        for (Produit p : produits) {
            Stock s = stockRepository.findByProduitId(p.getId()).orElse(null);
            BigDecimal quantiteDispo = (s != null && s.getQuantiteDisponible() != null)
                    ? s.getQuantiteDisponible() : BigDecimal.ZERO;
            BigDecimal seuilAlerte = (s != null && s.getSeuilAlerte() != null)
                    ? s.getSeuilAlerte() : (p.getStockMinimum() != null ? p.getStockMinimum() : BigDecimal.ZERO);

            if (quantiteDispo.compareTo(seuilAlerte) <= 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("produitId", p.getId());
                item.put("designation", p.getNom() != null ? p.getNom() : p.getDesignation());
                item.put("reference", p.getReference());
                item.put("quantiteDisponible", quantiteDispo);
                item.put("seuilAlerte", seuilAlerte);
                alertes.add(item);
            }
        }
        return alertes;
    }

    private boolean matchesProduct(Produit p, String query) {
        String nomNorm = normalize(p.getNom());
        String desigNorm = normalize(p.getDesignation());
        String descNorm = normalize(p.getDescription());
        String refNorm = normalize(p.getReference());
        String qNorm = normalize(query);

        return nomNorm.contains(qNorm)
                || desigNorm.contains(qNorm)
                || descNorm.contains(qNorm)
                || refNorm.contains(qNorm);
    }

    private String cleanSearchQuery(String recherche) {
        if (recherche == null) return "";
        String s = recherche.trim().toLowerCase();

        // Mots parasites courants dans les questions IA
        String[] stopWords = {
                "le produit", "les produits", "le produit ", "produits", "produit",
                "l'article", "les articles", "article", "articles",
                "du", "de la", "des", "le", "la", "les", "un", "une",
                "notre", "nos", "stock de", "stock du", "stock des", "stock"
        };

        for (String sw : stopWords) {
            if (s.startsWith(sw + " ")) {
                s = s.substring(sw.length()).trim();
            }
        }
        return s.trim();
    }

    private String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").trim();
    }
}
