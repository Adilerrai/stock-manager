package com.gestion.repository;

import com.gestion.persistent.enums.TypeCompteFinancier;
import com.gestion.persistent.model.CompteFinancier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompteFinancierRepository extends JpaRepository<CompteFinancier, Long> {

    Optional<CompteFinancier> findByCode(String code);

    List<CompteFinancier> findByActifTrue();

    List<CompteFinancier> findByTypeAndActifTrue(TypeCompteFinancier type);

    @Query("SELECT COALESCE(SUM(c.soldeActuel), 0) FROM CompteFinancier c WHERE c.type = :type AND c.actif = true")
    BigDecimal sumSoldeByType(TypeCompteFinancier type);

    @Query("SELECT COALESCE(SUM(c.soldeActuel), 0) FROM CompteFinancier c WHERE c.actif = true")
    BigDecimal sumSoldeGlobal();
}
