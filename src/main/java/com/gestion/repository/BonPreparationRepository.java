package com.gestion.repository;

import com.gestion.persistent.enums.StatutPreparation;
import com.gestion.persistent.model.BonPreparation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BonPreparationRepository extends JpaRepository<BonPreparation, Long> {

    Optional<BonPreparation> findByNumeroPreparation(String numeroPreparation);

    Optional<BonPreparation> findByCommandeClientId(Long commandeClientId);

    List<BonPreparation> findByStatutOrderByDateCreationDesc(StatutPreparation statut);

    List<BonPreparation> findByClientIdOrderByDateCreationDesc(Long clientId);

    List<BonPreparation> findByPointDeVenteIdOrderByDateCreationDesc(Long pointDeVenteId);
}
