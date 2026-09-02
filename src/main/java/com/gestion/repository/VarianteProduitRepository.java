package com.gestion.repository;

import com.gestion.persistent.model.VarianteProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VarianteProduitRepository extends JpaRepository<VarianteProduit, Long> {

    List<VarianteProduit> findByProduitParentId(Long produitParentId);

    Optional<VarianteProduit> findBySku(String sku);

    Optional<VarianteProduit> findByCodeBarre(String codeBarre);
}
