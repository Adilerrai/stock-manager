package com.gestion.repository;

import com.gestion.persistent.model.EcheanceFiscale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EcheanceFiscaleRepository extends JpaRepository<EcheanceFiscale, Long> {
    List<EcheanceFiscale> findByTenantIdAndDateEcheanceBetweenOrderByDateEcheanceAsc(
            Long tenantId, LocalDate dateDebut, LocalDate dateFin);

    List<EcheanceFiscale> findByDateEcheanceBetween(LocalDate dateDebut, LocalDate dateFin);

    List<EcheanceFiscale> findBySocieteIdOrderByDateEcheanceAsc(Long societeId);

    List<EcheanceFiscale> findByTenantIdAndStatutOrderByDateEcheanceAsc(Long tenantId, String statut);

    long countByTenantIdAndStatut(Long tenantId, String statut);
}
