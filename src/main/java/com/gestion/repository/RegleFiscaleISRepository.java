package com.gestion.repository;

import com.gestion.persistent.model.RegleFiscaleIS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegleFiscaleISRepository extends JpaRepository<RegleFiscaleIS, Long> {

    Optional<RegleFiscaleIS> findByAnneeFiscaleAndPointDeVenteId(Integer anneeFiscale, Long pointDeVenteId);

    boolean existsByAnneeFiscaleAndPointDeVenteId(Integer anneeFiscale, Long pointDeVenteId);
}
