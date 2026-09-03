package com.gestion.repository;

import com.gestion.persistent.model.MouvementTresorerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementTresorerieRepository extends JpaRepository<MouvementTresorerie, Long> {

    List<MouvementTresorerie> findByCompteSourceIdOrCompteDestinationIdOrderByDateMouvementDesc(Long sourceId, Long destId);

    @Query("SELECT m FROM MouvementTresorerie m WHERE m.dateMouvement BETWEEN :debut AND :fin ORDER BY m.dateMouvement DESC")
    List<MouvementTresorerie> findByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT m FROM MouvementTresorerie m WHERE (m.compteSource.id = :compteId OR m.compteDestination.id = :compteId) " +
           "AND m.dateMouvement BETWEEN :debut AND :fin ORDER BY m.dateMouvement DESC")
    List<MouvementTresorerie> findByCompteAndPeriode(@Param("compteId") Long compteId,
                                                     @Param("debut") LocalDateTime debut,
                                                     @Param("fin") LocalDateTime fin);
}
