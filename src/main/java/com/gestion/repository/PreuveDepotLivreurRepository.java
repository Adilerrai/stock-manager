package com.gestion.repository;

import com.gestion.persistent.model.PreuveDepotLivreur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PreuveDepotLivreurRepository extends JpaRepository<PreuveDepotLivreur, Long> {

    List<PreuveDepotLivreur> findByPointDeVenteIdOrderByDateDeclarationDesc(Long pointDeVenteId);

    List<PreuveDepotLivreur> findByPointDeVenteIdAndLivreurUserIdOrderByDateDeclarationDesc(Long pointDeVenteId, Long livreurUserId);
}
