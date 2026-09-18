package com.gestion.repository;

import com.gestion.persistent.model.Societe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocieteRepository extends JpaRepository<Societe, Long> {
    List<Societe> findByTenantIdOrderByRaisonSocialeAsc(Long tenantId);
    List<Societe> findByTenantIdAndActifTrueOrderByRaisonSocialeAsc(Long tenantId);
    Optional<Societe> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Societe> findByTenantIdAndIsParDefautTrue(Long tenantId);
    Optional<Societe> findByCodeAndTenantId(String code, Long tenantId);
    boolean existsByCodeAndTenantId(String code, Long tenantId);
    long countByTenantIdAndActifTrue(Long tenantId);

    // Queries par Mère
    List<Societe> findByMereIdOrderByRaisonSocialeAsc(Long mereId);
    List<Societe> findByMereIdAndActifTrueOrderByRaisonSocialeAsc(Long mereId);
    Optional<Societe> findByIdAndMereId(Long id, Long mereId);
    Optional<Societe> findByMereIdAndIsParDefautTrue(Long mereId);
    Optional<Societe> findByCodeAndMereId(String code, Long mereId);
    boolean existsByCodeAndMereId(String code, Long mereId);
    long countByMereIdAndActifTrue(Long mereId);
}
