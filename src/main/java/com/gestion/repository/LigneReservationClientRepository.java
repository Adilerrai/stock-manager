package com.gestion.repository;

import com.gestion.persistent.model.LigneReservationClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneReservationClientRepository extends JpaRepository<LigneReservationClient, Long> {
    List<LigneReservationClient> findByReservationClientId(Long reservationClientId);
}
