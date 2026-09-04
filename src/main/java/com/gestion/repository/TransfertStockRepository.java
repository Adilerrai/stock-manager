package com.gestion.repository;

import com.gestion.persistent.enums.StatutTransfert;
import com.gestion.persistent.model.Depot;
import com.gestion.persistent.model.TransfertStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransfertStockRepository extends JpaRepository<TransfertStock, Long> {

    List<TransfertStock> findByPointDeVenteIdOrderByDateTransfertDescIdDesc(Long pointDeVenteId);

    List<TransfertStock> findByPointDeVenteIdAndStatutOrderByDateTransfertDesc(Long pointDeVenteId, StatutTransfert statut);

    List<TransfertStock> findByPointDeVenteIdAndDepotSourceOrderByDateTransfertDesc(Long pointDeVenteId, Depot depotSource);

    List<TransfertStock> findByPointDeVenteIdAndDepotDestinationOrderByDateTransfertDesc(Long pointDeVenteId, Depot depotDestination);

    Optional<TransfertStock> findByNumeroTransfertAndPointDeVenteId(String numeroTransfert, Long pointDeVenteId);

    @Query("SELECT COUNT(t) FROM TransfertStock t WHERE t.pointDeVenteId = :tenantId AND t.numeroTransfert LIKE :prefix%")
    Long countByPrefixAndTenant(@Param("prefix") String prefix, @Param("tenantId") Long tenantId);
}
