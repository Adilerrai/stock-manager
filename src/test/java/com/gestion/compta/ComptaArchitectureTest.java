package com.gestion.compta;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ce que ces règles garantissent : une panne comptable ne peut pas atteindre une vente, et aucune requête n'oublie son livre. */
@Tag("compta")
class ComptaArchitectureTest {

    private static final String PAQUET_DEPOTS = "com.gestion.repository.compta";
    /** Passent du contexte au livre, ou lisent un référentiel sans livre. */
    private static final Set<String> DEPOTS_SANS_LIVRE = Set.of("LivreCourantRepository", "AbonnementRepository", "ParametreFiscalRepository");
    private static final Set<String> METHODES_SANS_LIVRE = Set.of(
            "CompteRepository.titres", "LivreRepository.creer", "LivreRepository.periodeOuverte",
            "SourcesRepository.types", "LivreRepository.identiteDuProfil", "LivreRepository.identiteDuPointDeVente", "LivreRepository.identiteDuDossier");
    private static final Path SOURCES = Path.of("src/main/java/com/gestion");
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/changelog/changes/compta");

    private static JavaClasses classes;

    @BeforeAll
    static void importer() {
        classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("com");
    }

    @Test
    void rienHorsDeLaComptabiliteNeDependDElle() {
        noClasses().that().resideOutsideOfPackages("..compta..")
                .should().dependOnClassesThat().resideInAnyPackage("..compta..")
                .check(classes);
    }

    @Test
    void laComptabiliteNeDependDAucuneEntiteNiDepotCommercial() {
        noClasses().that().resideInAnyPackage("..compta..")
                .should().dependOnClassesThat().resideInAnyPackage("com.gestion.persistent.model..", "com.gestion.repository")
                .check(classes);
    }

    @Test
    void leMoteurDesEtatsResteUneFonctionPure() {
        noClasses().that().haveSimpleNameStartingWith("MoteurEtats").or().haveSimpleNameStartingWith("DefinitionsEtats")
                .or().haveSimpleNameStartingWith("ReglesComptabilisation").or().haveSimpleNameStartingWith("ContexteRegles")
                .or().haveSimpleNameStartingWith("PieceSource").or().haveSimpleNameStartingWith("PropositionEcriture")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "com.gestion.repository..", "java.sql..")
                .check(classes);
    }

    @Test
    void chaqueMethodeDUnDepotPrendLeLivreEnPremier() throws Exception {
        List<String> fautes = new ArrayList<>();
        for (Path source : fichiers(SOURCES.resolve("repository/compta"), ".java")) {
            String nom = source.getFileName().toString().replace(".java", "");
            if (DEPOTS_SANS_LIVRE.contains(nom)) {
                continue;
            }
            Class<?> depot = Class.forName(PAQUET_DEPOTS + "." + nom);
            if (!depot.isAnnotationPresent(Repository.class)) {
                continue;
            }
            for (Method methode : depot.getDeclaredMethods()) {
                if (!Modifier.isPublic(methode.getModifiers()) || METHODES_SANS_LIVRE.contains(nom + "." + methode.getName())) {
                    continue;
                }
                boolean livre = methode.getParameterCount() > 0 && methode.getParameters()[0].getName().equals("livreId");
                if (!livre) {
                    fautes.add(nom + "." + methode.getName());
                }
            }
        }
        assertEquals(List.of(), fautes);
    }

    @Test
    void lesMigrationsDeLaComptabiliteNeTouchentAucuneTableExistante() throws IOException {
        Pattern interdit = Pattern.compile("(?im)^\\s*(DROP\\s+TABLE|ALTER\\s+TABLE\\s+(?!compta_)|UPDATE\\s+(?!compta_)|DELETE\\s+FROM\\s+(?!compta_))");
        List<Path> fautifs = new ArrayList<>();
        for (Path fichier : fichiers(MIGRATIONS, ".sql")) {
            if (interdit.matcher(Files.readString(fichier, StandardCharsets.UTF_8)).find()) {
                fautifs.add(fichier);
            }
        }
        assertEquals(List.of(), fautifs);
    }

    @Test
    void laComptabiliteNEcritDansAucuneTableCommerciale() throws IOException {
        Pattern ecriture = Pattern.compile("(?i)(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\s+(?!compta_)[a-z_]+\\s+(\\(|SET|WHERE|VALUES|[a-z]\\s)");
        List<Path> fautifs = new ArrayList<>();
        for (Path racine : List.of(SOURCES.resolve("repository/compta"), SOURCES.resolve("service/compta"))) {
            for (Path fichier : fichiers(racine, ".java")) {
                if (ecriture.matcher(Files.readString(fichier, StandardCharsets.UTF_8)).find()) {
                    fautifs.add(fichier);
                }
            }
        }
        assertEquals(List.of(), fautifs);
    }

    private static List<Path> fichiers(Path racine, String extension) throws IOException {
        try (Stream<Path> chemins = Files.walk(racine)) {
            return chemins.filter(c -> c.toString().endsWith(extension)).toList();
        }
    }
}
