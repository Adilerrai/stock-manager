package com.gestion.service.compta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Le formulaire de TVA, lu depuis une ressource datée : ce que la DGI peut changer n'est jamais écrit en dur.
 * La ressource est vérifiée au démarrage : une rubrique qui vise une ligne absente empêche l'application de démarrer.
 */
@Component
public class FormulaireTva {

    public record LigneFormulaire(String code, String section, String libelle, String taux) {
    }

    public record Rubrique(String code, String libelle, String compteTva, Map<String, String> taux) {
    }

    private final String modele;
    private final List<LigneFormulaire> lignes = new ArrayList<>();
    private final Map<String, LigneFormulaire> parCode = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> ventes = new LinkedHashMap<>();
    private final Map<String, String> traitements = new LinkedHashMap<>();
    private final Map<String, Rubrique> rubriques = new LinkedHashMap<>();

    public FormulaireTva() {
        try (InputStream flux = new ClassPathResource("compta/tva_formulaire.json").getInputStream()) {
            JsonNode racine = new ObjectMapper().readTree(flux);
            modele = racine.path("modele").asText();
            for (JsonNode l : racine.path("lignes")) {
                LigneFormulaire ligne = new LigneFormulaire(l.path("code").asText(), l.path("section").asText(), l.path("libelle").asText(), l.path("taux").isMissingNode() ? null : l.path("taux").asText());
                lignes.add(ligne);
                parCode.put(ligne.code(), ligne);
            }
            racine.path("ventes").fields().forEachRemaining(type -> ventes.put(type.getKey(), texte(type.getValue())));
            traitements.putAll(texte(racine.path("traitements")));
            for (JsonNode r : racine.path("rubriques")) {
                rubriques.put(r.path("code").asText(), new Rubrique(r.path("code").asText(), r.path("libelle").asText(), r.path("compte_tva").asText(), texte(r.path("taux"))));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        verifier();
    }

    private void verifier() {
        Set<String> vises = new LinkedHashSet<>(traitements.values());
        ventes.values().forEach(m -> vises.addAll(m.values()));
        rubriques.values().forEach(r -> vises.addAll(r.taux().values()));
        vises.removeAll(parCode.keySet());
        if (!vises.isEmpty() || modele.isBlank()) {
            throw new IllegalStateException("compta/tva_formulaire.json vise des lignes absentes : " + vises);
        }
        for (Rubrique r : rubriques.values()) {
            if (!"34551".equals(r.compteTva()) && !"34552".equals(r.compteTva())) {
                throw new IllegalStateException("La rubrique " + r.code() + " vise le compte " + r.compteTva() + " : seuls 34551 et 34552 sont admis.");
            }
        }
    }

    private static Map<String, String> texte(JsonNode noeud) {
        Map<String, String> valeurs = new LinkedHashMap<>();
        noeud.fields().forEachRemaining(e -> valeurs.put(e.getKey(), e.getValue().asText()));
        return valeurs;
    }

    public String modele() {
        return modele;
    }

    public List<LigneFormulaire> lignes() {
        return lignes;
    }

    public Optional<LigneFormulaire> ligne(String code) {
        return Optional.ofNullable(parCode.get(code));
    }

    /** Ligne B d'une vente : par type de produit, puis « autres ». Rien si le barème du formulaire ne connaît pas ce taux. */
    public String ligneDeVente(String typeProduit, BigDecimal taux) {
        String cle = cleTaux(taux);
        Map<String, String> parType = ventes.get(typeProduit == null ? "AUTRE" : typeProduit);
        String code = parType == null ? null : parType.get(cle);
        return code != null ? code : ventes.getOrDefault("AUTRE", Map.of()).get(cle);
    }

    public String ligneDeTraitement(String traitement) {
        return traitements.get(traitement);
    }

    public List<Rubrique> rubriques() {
        return List.copyOf(rubriques.values());
    }

    public Optional<Rubrique> rubrique(String code) {
        return Optional.ofNullable(rubriques.get(code));
    }

    public String ligneDeDeduction(String rubrique, BigDecimal taux) {
        Rubrique r = rubriques.get(rubrique);
        return r == null ? null : r.taux().get(cleTaux(taux));
    }

    public static String cleTaux(BigDecimal taux) {
        return taux.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
