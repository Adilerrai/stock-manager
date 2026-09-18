package com.gestion.repository;

import com.gestion.persistent.model.CollaborateurSociete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborateurSocieteRepository extends JpaRepository<CollaborateurSociete, Long> {

    List<CollaborateurSociete> findBySocieteId(Long societeId);

    List<CollaborateurSociete> findByUserId(Long userId);

    Optional<CollaborateurSociete> findByUserIdAndSocieteId(Long userId, Long societeId);

    boolean existsByUserIdAndSocieteId(Long userId, Long societeId);

    void deleteByUserIdAndSocieteId(Long userId, Long societeId);

    long countByUserId(Long userId);
}
