package com.gestion.repository;

import com.gestion.persistent.model.Banque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BanqueRepository extends JpaRepository<Banque, Long> {

    Optional<Banque> findByCode(String code);

    Optional<Banque> findByCodeAndPointDeVenteId(String code, Long pointDeVenteId);

    List<Banque> findByPointDeVenteIdAndActifTrueOrderByNomAsc(Long pointDeVenteId);

    List<Banque> findByPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    List<Banque> findByActifTrueOrderByNomAsc();

    boolean existsByCode(String code);

    boolean existsByNom(String nom);
}
