package com.gestion.repository;

import com.gestion.persistent.model.LigneAvoir;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneAvoirRepository extends JpaRepository<LigneAvoir, Long> {
    List<LigneAvoir> findByAvoirId(Long avoirId);
}
