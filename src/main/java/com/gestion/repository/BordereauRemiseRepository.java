package com.gestion.repository;

import com.gestion.persistent.enums.StatutRemise;
import com.gestion.persistent.model.BordereauRemise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BordereauRemiseRepository extends JpaRepository<BordereauRemise, Long> {

    Optional<BordereauRemise> findByNumeroBordereau(String numeroBordereau);

    List<BordereauRemise> findByStatutOrderByDateRemiseDesc(StatutRemise statut);

    List<BordereauRemise> findByPointDeVenteIdOrderByDateRemiseDesc(Long pointDeVenteId);
}
