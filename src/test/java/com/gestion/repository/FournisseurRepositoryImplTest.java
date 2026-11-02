package com.gestion.repository;

import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.FournisseurSearchCriteria;
import com.gestion.persistent.enums.TypeFournisseur;
import com.gestion.persistent.model.Fournisseur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration de la recherche multicritère des fournisseurs ({@link FournisseurRepositoryImpl})
 * sur la base PostgreSQL locale (paramètres de application.properties, comme pour l'application).
 * Les données insérées sont annulées en fin de test (transaction de test, rollback automatique).
 */
@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",   // le schéma est déjà géré par l'application
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FournisseurRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    /** Identifiants des points de vente (tenants) créés pour le test — fournisseurs.point_de_vente_id est une FK. */
    private Long tenant;
    private Long autreTenant;

    @BeforeEach
    void setUp() {
        tenant = em.persistAndGetId(new PointDeVente(0L, "PDV-TEST-FOURNISSEURS-A"), Long.class);
        autreTenant = em.persistAndGetId(new PointDeVente(0L, "PDV-TEST-FOURNISSEURS-B"), Long.class);

        TenantContext.setCurrentTenant(tenant);
        creer("Juovi Environmental Protection Equipment", f -> {
            f.setContact("Emily Zhang");
            f.setEmail("Emily@juovi.com");
            f.setTelephone("086-18930092192");
            f.setVille("Shanghai");
            f.setPays("Chine");
            f.setTypeFournisseur(TypeFournisseur.INTERNATIONAL);
            f.setDevise("USD");
            f.setDelaiPaiementJours(0);
            f.setIce("ICE-CN-001");
        });
        creer("univers caoutchouc", f -> {
            f.setVille("Casablanca");
            f.setTypeFournisseur(TypeFournisseur.NATIONAL);
            f.setDevise("MAD");
            f.setDelaiPaiementJours(30);
            f.setIce("001234567000089");
        });
        creer("World Jet", f -> {
            f.setTypeFournisseur(TypeFournisseur.INTERNATIONAL);
            f.setDevise("EUR");
            f.setDelaiPaiementJours(60);
        });
        // Ancien enregistrement sans type ni devise : doit être traité comme NATIONAL / MAD
        creer("Fournisseur Divers (ancien)", f -> {
            f.setTypeFournisseur(null);
            f.setDevise(null);
            f.setDelaiPaiementJours(15);
            f.setVille("Rabat");
        });
        creer("Atlas Inactif", f -> {
            f.setActif(false);
            f.setVille("Tanger");
            f.setDelaiPaiementJours(45);
        });

        // Un fournisseur d'un autre tenant : ne doit jamais remonter
        TenantContext.setCurrentTenant(autreTenant);
        creer("Shanghai Autre Tenant", f -> f.setVille("Shanghai"));
        TenantContext.setCurrentTenant(tenant);

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ─── Scénarios ───────────────────────────────────────────────────────────

    @Test
    void sansCritere_retourneTousLesFournisseursDuTenant_triParDefautInsensibleALaCasse() {
        Page<Fournisseur> page = search(c -> {});
        assertEquals(5, page.getTotalElements());
        assertEquals(List.of(
                "Atlas Inactif",
                "Fournisseur Divers (ancien)",
                "Juovi Environmental Protection Equipment",
                "univers caoutchouc",
                "World Jet"), noms(page));
    }

    @Test
    void query_rechercheGlobaleInsensibleALaCasse_surPlusieursColonnes() {
        assertEquals(List.of("Juovi Environmental Protection Equipment"), noms(search(c -> c.setQuery("SHANGHAI"))), "ville");
        assertEquals(List.of("Juovi Environmental Protection Equipment"), noms(search(c -> c.setQuery("emily@"))), "email");
        assertEquals(List.of("Juovi Environmental Protection Equipment"), noms(search(c -> c.setQuery("18930092192"))), "téléphone");
        assertEquals(List.of("Juovi Environmental Protection Equipment"), noms(search(c -> c.setQuery("  zhang "))), "contact (espaces ignorés)");
        assertEquals(List.of("univers caoutchouc"), noms(search(c -> c.setQuery("001234567"))), "ICE");
        assertEquals(List.of("univers caoutchouc"), noms(search(c -> c.setQuery("CAOUTCHOUC"))), "raison sociale");
        assertTrue(search(c -> c.setQuery("introuvable-xyz")).isEmpty());
        assertEquals(5, search(c -> c.setQuery("   ")).getTotalElements(), "query vide ignorée");
    }

    @Test
    void query_neRemontePasLesFournisseursDesAutresTenants() {
        // "Shanghai" existe aussi chez AUTRE_TENANT
        assertEquals(List.of("Juovi Environmental Protection Equipment"), noms(search(c -> c.setQuery("shanghai"))));
    }

    @Test
    void filtreActif() {
        assertEquals(List.of("Atlas Inactif"), noms(search(c -> c.setActif(false))));
        assertEquals(4, search(c -> c.setActif(true)).getTotalElements());
    }

    @Test
    void filtreTypeFournisseur_nationalInclutLesAnciensSansType() {
        assertEquals(List.of("Atlas Inactif", "Fournisseur Divers (ancien)", "univers caoutchouc"),
                noms(search(c -> c.setTypeFournisseur(TypeFournisseur.NATIONAL))));
        assertEquals(List.of("Juovi Environmental Protection Equipment", "World Jet"),
                noms(search(c -> c.setTypeFournisseur(TypeFournisseur.INTERNATIONAL))));
    }

    @Test
    void filtreDevise_insensibleALaCasse_madInclutLesAnciensSansDevise() {
        assertEquals(List.of("Atlas Inactif", "Fournisseur Divers (ancien)", "univers caoutchouc"),
                noms(search(c -> c.setDevise("mad"))));
        assertEquals(List.of("Juovi Environmental Protection Equipment"), noms(search(c -> c.setDevise("USD"))));
    }

    @Test
    void filtreDelaiPaiement_bornesInclusives() {
        assertEquals(List.of("Atlas Inactif", "univers caoutchouc", "World Jet"),
                noms(search(c -> c.setDelaiPaiementMin(30))));
        assertEquals(List.of("Fournisseur Divers (ancien)", "Juovi Environmental Protection Equipment"),
                noms(search(c -> c.setDelaiPaiementMax(15))));
        assertEquals(List.of("Atlas Inactif", "univers caoutchouc"),
                noms(search(c -> {
                    c.setDelaiPaiementMin(30);
                    c.setDelaiPaiementMax(45);
                })));
    }

    @Test
    void criteresCombines_enEt() {
        assertEquals(List.of("World Jet"), noms(search(c -> {
            c.setTypeFournisseur(TypeFournisseur.INTERNATIONAL);
            c.setDevise("EUR");
        })));
        assertTrue(search(c -> {
            c.setTypeFournisseur(TypeFournisseur.INTERNATIONAL);
            c.setVille("Casablanca");
        }).isEmpty());
        assertEquals(List.of("univers caoutchouc"), noms(search(c -> {
            c.setQuery("casa");
            c.setActif(true);
            c.setPays("Maroc"); // valeur par défaut de l'entité
        })));
    }

    @Test
    void tri_listeBlanche_alias_etInsensibleALaCasse() {
        // alias "nom" → raisonSociale, ordre décroissant
        assertEquals(List.of(
                        "World Jet",
                        "univers caoutchouc",
                        "Juovi Environmental Protection Equipment",
                        "Fournisseur Divers (ancien)",
                        "Atlas Inactif"),
                noms(search(c -> {}, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "nom")))));

        // propriété inconnue ignorée → tri par défaut
        assertEquals(List.of(
                        "Atlas Inactif",
                        "Fournisseur Divers (ancien)",
                        "Juovi Environmental Protection Equipment",
                        "univers caoutchouc",
                        "World Jet"),
                noms(search(c -> {}, PageRequest.of(0, 10, Sort.by("proprieteInexistante")))));

        // tri numérique sur le délai de paiement
        assertEquals(List.of(
                        "Juovi Environmental Protection Equipment",
                        "Fournisseur Divers (ancien)",
                        "univers caoutchouc",
                        "Atlas Inactif",
                        "World Jet"),
                noms(search(c -> {}, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "delaiPaiementJours")))));

        // tri sur une colonne enum
        List<String> parType = noms(search(c -> {}, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "typeFournisseur"))));
        assertEquals(5, parType.size());
    }

    @Test
    void pagination_totalEtPagesCoherents_sansDoublon() {
        Page<Fournisseur> p0 = search(c -> {}, PageRequest.of(0, 2));
        Page<Fournisseur> p1 = search(c -> {}, PageRequest.of(1, 2));
        Page<Fournisseur> p2 = search(c -> {}, PageRequest.of(2, 2));

        assertEquals(5, p0.getTotalElements());
        assertEquals(3, p0.getTotalPages());
        assertEquals(2, p0.getNumberOfElements());
        assertEquals(2, p1.getNumberOfElements());
        assertEquals(1, p2.getNumberOfElements());

        List<String> tous = new ArrayList<>(noms(p0));
        tous.addAll(noms(p1));
        tous.addAll(noms(p2));
        assertEquals(5, new HashSet<>(tous).size(), "aucun doublon entre les pages");
    }

    @Test
    void criteriaNull_estTraiteCommeAucunCritere() {
        assertEquals(5, fournisseurRepository.findByCriteria(null, PageRequest.of(0, 10)).getTotalElements());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Fournisseur creer(String raisonSociale, Consumer<Fournisseur> init) {
        Fournisseur f = new Fournisseur();
        f.setRaisonSociale(raisonSociale);
        init.accept(f);
        return em.persist(f);
    }

    private Page<Fournisseur> search(Consumer<FournisseurSearchCriteria> init) {
        return search(init, PageRequest.of(0, 10));
    }

    private Page<Fournisseur> search(Consumer<FournisseurSearchCriteria> init, PageRequest pageable) {
        FournisseurSearchCriteria c = new FournisseurSearchCriteria();
        init.accept(c);
        return fournisseurRepository.findByCriteria(c, pageable);
    }

    private static List<String> noms(Page<Fournisseur> page) {
        return page.getContent().stream().map(Fournisseur::getRaisonSociale).toList();
    }
}
