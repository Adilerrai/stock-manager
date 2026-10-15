package com.gestion.repository;

import com.gestion.persistent.model.LigneDepenseConsommable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneDepenseConsommableRepository extends JpaRepository<LigneDepenseConsommable, Long> {

    List<LigneDepenseConsommable> findByDepenseId(Long depenseId);

    List<LigneDepenseConsommable> findByConsommableId(Long consommableId);

    void deleteByDepenseId(Long depenseId);
}
