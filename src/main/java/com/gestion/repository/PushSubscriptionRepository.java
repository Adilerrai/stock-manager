package com.gestion.repository;

import com.gestion.persistent.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserId(Long userId);

    /** Un appareil réenregistré par un autre compte change de propriétaire : l'ancien ne reçoit plus rien. */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO push_subscriptions (user_id, endpoint, p256dh, auth)
            VALUES (:userId, :endpoint, :p256dh, :auth)
            ON CONFLICT (endpoint) DO UPDATE
               SET user_id = EXCLUDED.user_id, p256dh = EXCLUDED.p256dh, auth = EXCLUDED.auth,
                   date_modification = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void enregistrer(@Param("userId") Long userId, @Param("endpoint") String endpoint,
                     @Param("p256dh") String p256dh, @Param("auth") String auth);

    @Modifying
    @Transactional
    @Query("DELETE FROM PushSubscription s WHERE s.endpoint = :endpoint AND s.userId = :userId")
    int retirer(@Param("userId") Long userId, @Param("endpoint") String endpoint);

    @Modifying
    @Transactional
    @Query("DELETE FROM PushSubscription s WHERE s.endpoint = :endpoint")
    int retirerExpire(@Param("endpoint") String endpoint);

    /** L'admin du tenant d'office, les autres rôles par habilitation. Un compte désactivé ou client ne reçoit rien. */
    @Query("""
            SELECT DISTINCT s FROM PushSubscription s, User u
              JOIN u.role r
              LEFT JOIN r.habilitations h
              LEFT JOIN u.pointDeVente p
             WHERE s.userId = u.id
               AND u.enabled = true
               AND u.clientId IS NULL
               AND (u.tenantId = :tenantId OR p.id = :tenantId OR p.tenantId = :tenantId)
               AND (r.nom = 'ROLE_ADMIN' OR h.nom = :habilitation)
            """)
    List<PushSubscription> destinataires(@Param("tenantId") Long tenantId, @Param("habilitation") String habilitation);
}
