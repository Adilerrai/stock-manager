package com.gestion.compta.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;

/**
 * Classe mère des tests d'intégration de la comptabilité. Lancés par {@code mvn -Dgroups=compta test}.
 * <p>
 * Un test en JDBC direct utilise {@link #base()}. Un test qui ajoute {@code @SpringBootTest} reçoit la même base :
 * la source posée ici l'emporte sur {@code application.properties}, et la garde contrôle le contexte avant
 * la création du premier bean. Chaque test crée ses données par {@link SocieteDeTest#creer()}.
 */
@Tag("compta")
@ContextConfiguration(initializers = GardeBase.Contexte.class)
public abstract class AbstractComptaIT {

    @DynamicPropertySource
    static void sourceDeDonnees(DynamicPropertyRegistry proprietes) {
        BaseEmbarquee embarquee = BaseEmbarquee.instance();
        proprietes.add("spring.datasource.url", embarquee::urlJdbc);
        proprietes.add("spring.datasource.username", () -> BaseEmbarquee.UTILISATEUR);
        proprietes.add("spring.datasource.password", () -> BaseEmbarquee.UTILISATEUR);
        // GedService crée son dossier au démarrage : par défaut ./uploads/ged, que .gitignore ne couvre pas
        proprietes.add("storage.ged.path", () -> "target/compta-ged");
    }

    @BeforeAll
    static void garderLaBase() {
        BaseEmbarquee embarquee = BaseEmbarquee.instance();
        GardeBase.controler(embarquee.urlJdbc(), embarquee.port(), embarquee.dossierDonnees());
    }

    protected static DataSource base() {
        return BaseEmbarquee.instance().dataSource();
    }
}
