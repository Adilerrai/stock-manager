package com.gestion.repository;

import com.gestion.persistent.model.Mere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MereRepository extends JpaRepository<Mere, Long> {
    List<Mere> findByActifTrueOrderByNomAsc();
    Optional<Mere> findByIdAndActifTrue(Long id);
    boolean existsByNomIgnoreCase(String nom);
}
