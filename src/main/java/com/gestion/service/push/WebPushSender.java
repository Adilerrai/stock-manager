package com.gestion.service.push;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Chiffre et poste un message vers le service de push du navigateur. Ne lève jamais : l'appelant lit le résultat. */
@Component
public class WebPushSender {

    public enum Resultat { ENVOYE, EXPIRE, ECHEC }

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);
    private static final int TTL_SECONDES = 24 * 60 * 60;
    private static final long DELAI_SECONDES = 10;

    private final PushService service;
    private final String clePublique;

    public WebPushSender(@Value("${push.vapid.public-key:}") String clePublique,
                         @Value("${push.vapid.private-key:}") String clePrivee,
                         @Value("${push.vapid.subject:}") String sujet) {
        this.clePublique = clePublique;
        this.service = creerService(clePublique, clePrivee, sujet);
    }

    /** Des clés absentes ou invalides coupent le push, pas le démarrage de l'application. */
    private static PushService creerService(String clePublique, String clePrivee, String sujet) {
        if (clePublique == null || clePublique.isBlank() || clePrivee == null || clePrivee.isBlank()) {
            log.info("Web Push inactif : PUSH_VAPID_PUBLIC_KEY et PUSH_VAPID_PRIVATE_KEY ne sont pas définies");
            return null;
        }
        // Ajouté en dernier : les fournisseurs du JDK gardent la priorité pour le reste de l'application
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            return new PushService(clePublique, clePrivee, sujet);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("Web Push inactif : clés VAPID invalides ({})", e.getClass().getSimpleName());
            return null;
        }
    }

    public boolean actif() {
        return service != null;
    }

    public String clePublique() {
        return actif() ? clePublique : null;
    }

    public Resultat envoyer(String endpoint, String p256dh, String auth, byte[] message) {
        if (service == null) {
            return Resultat.ECHEC;
        }
        Future<HttpResponse> reponse = null;
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, message, TTL_SECONDES);
            reponse = service.sendAsync(notification, Encoding.AES128GCM);
            int statut = reponse.get(DELAI_SECONDES, TimeUnit.SECONDS).getStatusLine().getStatusCode();
            if (statut >= 200 && statut < 300) {
                return Resultat.ENVOYE;
            }
            if (statut == 404 || statut == 410) {
                return Resultat.EXPIRE;
            }
            log.warn("Push refusé par {} : HTTP {}", hote(endpoint), statut);
            return Resultat.ECHEC;
        } catch (TimeoutException e) {
            reponse.cancel(true);
            log.warn("Push sans réponse de {} après {} s", hote(endpoint), DELAI_SECONDES);
            return Resultat.ECHEC;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultat.ECHEC;
        } catch (Exception e) {
            log.warn("Push impossible vers {} : {}", hote(endpoint), e.getClass().getSimpleName());
            return Resultat.ECHEC;
        }
    }

    /** L'URL complète d'un endpoint vaut un droit d'envoi : seul l'hôte va dans les journaux. */
    private static String hote(String endpoint) {
        try {
            return URI.create(endpoint).getHost();
        } catch (IllegalArgumentException e) {
            return "?";
        }
    }
}
