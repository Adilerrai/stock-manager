package com.gestion.repository;

import com.gestion.persistent.enums.StatutDemandePrix;
import com.gestion.persistent.model.DemandePrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandePrixRepository extends JpaRepository<DemandePrix, Long> {

    List<DemandePrix> findByPointDeVenteIdOrderByDateCreationDesc(Long pointDeVenteId);

    Optional<DemandePrix> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    Optional<DemandePrix> findByPointDeVenteIdAndNumeroDdp(Long pointDeVenteId, String numeroDdp);

    List<DemandePrix> findByPointDeVenteIdAndDemandeAchatId(Long pointDeVenteId, String demandeAchatId);

    List<DemandePrix> findByStatutAndPointDeVenteId(StatutDemandePrix statut, Long pointDeVenteId);
}
