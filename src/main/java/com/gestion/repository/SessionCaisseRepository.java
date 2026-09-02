package com.gestion.repository;

import com.gestion.persistent.enums.StatutSessionCaisse;
import com.gestion.persistent.model.SessionCaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    Optional<SessionCaisse> findByReference(String reference);

    Optional<SessionCaisse> findFirstByCaissierIdAndStatutOrderByDateOuvertureDesc(Long caissierId, StatutSessionCaisse statut);

    List<SessionCaisse> findByStatutOrderByDateOuvertureDesc(StatutSessionCaisse statut);

    List<SessionCaisse> findByPointDeVenteIdOrderByDateOuvertureDesc(Long pointDeVenteId);
}
