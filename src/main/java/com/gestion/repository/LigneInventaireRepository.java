package com.gestion.repository;

import com.gestion.persistent.model.LigneInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneInventaireRepository extends JpaRepository<LigneInventaire, Long> {
    List<LigneInventaire> findByInventaireId(Long inventaireId);
}
