package com.gestion.repository;

import com.gestion.persistent.model.ObjectifCommercial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectifCommercialRepository extends JpaRepository<ObjectifCommercial, Long> {

    Optional<ObjectifCommercial> findByCommercialIdAndAnneeAndMois(Long commercialId, Integer annee, Integer mois);

    List<ObjectifCommercial> findByAnneeAndMois(Integer annee, Integer mois);

    List<ObjectifCommercial> findByCommercialId(Long commercialId);
}
