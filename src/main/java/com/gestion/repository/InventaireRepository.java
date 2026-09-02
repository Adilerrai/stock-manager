package com.gestion.repository;

import com.gestion.persistent.enums.StatutInventaire;
import com.gestion.persistent.model.Inventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventaireRepository extends JpaRepository<Inventaire, Long> {

    Optional<Inventaire> findByReference(String reference);

    List<Inventaire> findByDepotIdOrderByDateInventaireDesc(Long depotId);

    List<Inventaire> findByStatutOrderByDateInventaireDesc(StatutInventaire statut);

    List<Inventaire> findByPointDeVenteIdOrderByDateInventaireDesc(Long pointDeVenteId);
}
