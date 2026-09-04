package com.gestion.repository;

import com.gestion.persistent.enums.TypeNotification;
import com.gestion.persistent.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByPointDeVenteIdOrderByDateCreationDesc(Long pointDeVenteId);

    List<Notification> findByPointDeVenteIdAndLuFalseOrderByDateCreationDesc(Long pointDeVenteId);

    List<Notification> findByPointDeVenteIdAndTypeOrderByDateCreationDesc(Long pointDeVenteId, TypeNotification type);

    Long countByPointDeVenteIdAndLuFalse(Long pointDeVenteId);

    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.pointDeVenteId = :tenantId AND n.lu = false")
    void marquerToutesCommeLues(@Param("tenantId") Long tenantId);

    boolean existsByPointDeVenteIdAndTitreAndLuFalse(Long pointDeVenteId, String titre);
}
