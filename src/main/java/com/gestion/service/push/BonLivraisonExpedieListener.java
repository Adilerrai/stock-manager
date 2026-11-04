package com.gestion.service.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BonLivraisonExpedieListener {

    private static final Logger log = LoggerFactory.getLogger(BonLivraisonExpedieListener.class);

    private final PushNotificationService notifications;

    public BonLivraisonExpedieListener(PushNotificationService notifications) {
        this.notifications = notifications;
    }

    /** Après le commit : une expédition annulée par un rollback ne notifie personne. */
    @Async("pushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surExpedition(BonLivraisonExpedieEvent evenement) {
        try {
            notifications.notifierBlExpedie(evenement);
        } catch (RuntimeException e) {
            log.error("Notification du BL {} impossible", evenement.blId(), e);
        }
    }
}
