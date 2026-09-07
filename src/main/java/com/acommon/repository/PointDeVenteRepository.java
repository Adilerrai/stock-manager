package com.acommon.repository;

import com.acommon.persistant.model.PointDeVente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PointDeVenteRepository extends JpaRepository<PointDeVente, Long> {

    Optional<PointDeVente> findByTenantId(Long tenantId);

    List<PointDeVente> findAllByTenantId(Long tenantId);

    Optional<PointDeVente> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByIdAndTenantId(Long id, Long tenantId);

    Optional<PointDeVente> findByNomPointDeVente(String nomPointDeVente);

    boolean existsByTenantId(Long tenantId);

    boolean existsByNomPointDeVente(String nomPointDeVente);

    boolean existsByEmail(String email);

    @Query("SELECT MAX(p.tenantId) FROM PointDeVente p")
    Long findMaxTenantId();

    Page<PointDeVente> findByActif(Boolean actif, Pageable pageable);

    @Query("SELECT p FROM PointDeVente p WHERE " +
           "(LOWER(p.nomPointDeVente) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.telephone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PointDeVente> searchPointsDeVente(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM PointDeVente p WHERE p.actif = :actif AND " +
           "(LOWER(p.nomPointDeVente) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.telephone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PointDeVente> searchPointsDeVenteByActif(@Param("search") String search, @Param("actif") Boolean actif, Pageable pageable);
}
