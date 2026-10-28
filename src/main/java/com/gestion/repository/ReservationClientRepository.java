package com.gestion.repository;

import com.gestion.persistent.enums.StatutReservationClient;
import com.gestion.persistent.model.ReservationClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationClientRepository extends JpaRepository<ReservationClient, Long>, ReservationClientRepositoryCustom {

    List<ReservationClient> findByPointDeVenteIdOrderByDateReservationDesc(Long pointDeVenteId);

    List<ReservationClient> findByClientIdAndPointDeVenteIdOrderByDateReservationDesc(Long clientId, Long pointDeVenteId);

    List<ReservationClient> findByClientIdOrderByDateReservationDesc(Long clientId);

    List<ReservationClient> findByStatutAndPointDeVenteIdOrderByDateReservationDesc(StatutReservationClient statut, Long pointDeVenteId);

    Optional<ReservationClient> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    Optional<ReservationClient> findByIdAndClientId(Long id, Long clientId);

    Optional<ReservationClient> findByPointDeVenteIdAndNumeroReservation(Long pointDeVenteId, String numeroReservation);

    Optional<ReservationClient> findFirstByDevisGenereId(Long devisGenereId);

    long countByClientIdAndPointDeVenteId(Long clientId, Long pointDeVenteId);

    long countByClientIdAndStatutIn(Long clientId, List<StatutReservationClient> statuts);
}
