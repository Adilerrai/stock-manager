package com.gestion.repository;

import com.gestion.persistent.model.LiasseFiscaleDonnees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LiasseFiscaleDonneesRepository extends JpaRepository<LiasseFiscaleDonnees, Long> {
    Optional<LiasseFiscaleDonnees> findByAnneeAndPointDeVenteId(Integer annee, Long pointDeVenteId);
}
