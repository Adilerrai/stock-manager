package com.gestion.repository;

import com.gestion.persistent.model.DeclarationTva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeclarationTvaRepository extends JpaRepository<DeclarationTva, Long> {
    Optional<DeclarationTva> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);
    Optional<DeclarationTva> findByPeriodeAndPointDeVenteId(String periode, Long pointDeVenteId);
    List<DeclarationTva> findByPointDeVenteIdOrderByPeriodeDesc(Long pointDeVenteId);
}
