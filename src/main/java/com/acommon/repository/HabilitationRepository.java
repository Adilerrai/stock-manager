package com.acommon.repository;

import com.acommon.persistant.model.Habilitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface HabilitationRepository extends JpaRepository<Habilitation, Long> {
    Optional<Habilitation> findByNom(String nom);
    boolean existsByNom(String nom);
    List<Habilitation> findByNomIn(Set<String> noms);
}
