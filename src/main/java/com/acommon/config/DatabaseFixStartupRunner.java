package com.acommon.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * StartupRunner désactivé : aucune exécution de code SQL au démarrage de l'application.
 * Les schémas, migrations et tables sont gérés manuellement par l'administrateur / développeur.
 */
public class DatabaseFixStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFixStartupRunner.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("ℹ️ Aucun script SQL exécuté au démarrage (gestion manuelle des migrations).");
    }
}
