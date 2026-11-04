package com.gestion.service.push;

import com.acommon.exception.CommonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.persistent.model.PushSubscription;
import com.gestion.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PushNotificationService {

    public static final String HABILITATION_BL_EXPEDIE = "NOTIF_BL_EXPEDIE";

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    /** Le backend poste vers ces URL : sans cette liste, un utilisateur pourrait lui faire appeler n'importe quel hôte. */
    private static final Set<String> HOTES_EXACTS = Set.of("fcm.googleapis.com", "push.apple.com",
            "updates.push.services.mozilla.com");
    private static final List<String> SUFFIXES_HOTES = List.of(".push.apple.com", ".push.services.mozilla.com",
            ".notify.windows.com");
    private static final int TAILLE_CLE_P256DH = 65;
    private static final int TAILLE_SECRET_AUTH = 16;

    private final PushSubscriptionRepository abonnements;
    private final WebPushSender sender;
    private final ObjectMapper json;

    public PushNotificationService(PushSubscriptionRepository abonnements, WebPushSender sender, ObjectMapper json) {
        this.abonnements = abonnements;
        this.sender = sender;
        this.json = json;
    }

    public boolean actif() {
        return sender.actif();
    }

    public String clePublique() {
        return sender.clePublique();
    }

    public void abonner(Long userId, String endpoint, String p256dh, String auth) {
        if (!endpointAutorise(endpoint)) {
            throw new CommonException("Service de notification non reconnu", HttpStatus.BAD_REQUEST);
        }
        if (!cleValide(p256dh, TAILLE_CLE_P256DH) || !cleValide(auth, TAILLE_SECRET_AUTH)) {
            throw new CommonException("Clés d'abonnement invalides", HttpStatus.BAD_REQUEST);
        }
        abonnements.enregistrer(userId, endpoint, p256dh, auth);
    }

    public void desabonner(Long userId, String endpoint) {
        abonnements.retirer(userId, endpoint);
    }

    /** Le numéro seul : la notification reste lisible sur un écran verrouillé, sans client ni montant. */
    public void notifierBlExpedie(BonLivraisonExpedieEvent evenement) {
        List<PushSubscription> destinataires = abonnements.destinataires(evenement.pointDeVenteId(), HABILITATION_BL_EXPEDIE);
        envoyer(destinataires, "BL " + evenement.numeroBl() + " expédié", "Le bon de livraison vient d'être expédié.",
                "/livraisons/expedition/" + evenement.blId(), "bl-expedie-" + evenement.blId());
    }

    /** @return le nombre d'appareils joints */
    public int envoyerTest(Long userId) {
        return envoyer(abonnements.findByUserId(userId), "Notifications activées",
                "Cet appareil recevra les notifications de Tadbeer.", "/settings/notifications", "test");
    }

    private int envoyer(List<PushSubscription> destinataires, String titre, String corps, String lien, String tag) {
        byte[] message = message(titre, corps, lien, tag);
        int envoyes = 0;
        for (PushSubscription abonnement : destinataires) {
            WebPushSender.Resultat resultat = sender.envoyer(abonnement.getEndpoint(), abonnement.getP256dh(), abonnement.getAuth(), message);
            if (resultat == WebPushSender.Resultat.ENVOYE) {
                envoyes++;
            } else if (resultat == WebPushSender.Resultat.EXPIRE) {
                abonnements.retirerExpire(abonnement.getEndpoint());
            }
        }
        log.info("Push '{}' : {} envoyé(s) sur {} appareil(s)", tag, envoyes, destinataires.size());
        return envoyes;
    }

    private byte[] message(String titre, String corps, String lien, String tag) {
        try {
            return json.writeValueAsBytes(Map.of("titre", titre, "corps", corps, "lien", lien, "tag", tag));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Message de notification illisible", e);
        }
    }

    static boolean endpointAutorise(String endpoint) {
        if (endpoint == null || endpoint.isBlank() || endpoint.length() > 2048) {
            return false;
        }
        try {
            URI uri = new URI(endpoint);
            String hote = uri.getHost();
            if (!"https".equals(uri.getScheme()) || hote == null || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return false;
            }
            String enMinuscules = hote.toLowerCase();
            return HOTES_EXACTS.contains(enMinuscules) || SUFFIXES_HOTES.stream().anyMatch(enMinuscules::endsWith);
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    private static boolean cleValide(String valeur, int tailleAttendue) {
        if (valeur == null) {
            return false;
        }
        try {
            return Base64.getUrlDecoder().decode(valeur).length == tailleAttendue;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
