package com.gestion.repository;

import com.gestion.persistent.model.CrmActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmActiviteRepository extends JpaRepository<CrmActivite, Long> {

    List<CrmActivite> findByPointDeVenteIdOrderByDateActiviteDesc(Long pointDeVenteId);

    List<CrmActivite> findByPointDeVenteIdAndEntrepriseIdOrderByDateActiviteDesc(Long pointDeVenteId, Long entrepriseId);

    List<CrmActivite> findByPointDeVenteIdAndContactIdOrderByDateActiviteDesc(Long pointDeVenteId, Long contactId);

    List<CrmActivite> findByPointDeVenteIdAndOpportuniteIdOrderByDateActiviteDesc(Long pointDeVenteId, Long opportuniteId);

    List<CrmActivite> findByPointDeVenteIdAndVisiteIdOrderByDateActiviteDesc(Long pointDeVenteId, Long visiteId);
}
