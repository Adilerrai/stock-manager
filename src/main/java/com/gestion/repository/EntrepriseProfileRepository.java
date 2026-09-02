package com.gestion.repository;

import com.gestion.persistent.model.EntrepriseProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntrepriseProfileRepository extends JpaRepository<EntrepriseProfile, Long> {
    Optional<EntrepriseProfile> findByPointDeVenteId(Long pointDeVenteId);
}
