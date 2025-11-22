package com.acommon.repository;

import com.acommon.persistant.model.PointDeVente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointDeVenteRepository extends JpaRepository<PointDeVente, Long> {
    Optional<PointDeVente> findByNomPointDeVente(String nomPointDeVente);
    Optional<PointDeVente> findByTenantId(Long tenantId);
}