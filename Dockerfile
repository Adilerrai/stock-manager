# Image officielle OpenJDK 17
FROM eclipse-temurin:17-jdk

# Installation des polices requises pour JasperReports (évite les erreurs de rendu PDF en environnement headless)
RUN apt-get update && apt-get install -y fontconfig fonts-dejavu-core && rm -rf /var/lib/apt/lists/*

# Définir le répertoire de travail
WORKDIR /app

# Copier le JAR généré par Maven
COPY target/*.jar /app/app.jar

# Exposer le port de l'application (8009 par défaut)
EXPOSE 8009

# Démarrer l'application Spring Boot
CMD ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
