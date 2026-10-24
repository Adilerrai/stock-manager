package com.gestion.repository;

import com.gestion.persistent.model.AllocationCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationChargeRepository extends JpaRepository<AllocationCharge, Long> {

    List<AllocationCharge> findByCharge_Id(Long chargeId);

    List<AllocationCharge> findByLigneCommande_Id(Long ligneCommandeId);

    void deleteByCharge_Id(Long chargeId);

    void deleteByLigneCommande_Id(Long ligneCommandeId);
}
