package com.gestion.repository;

import com.gestion.persistent.model.RelanceClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelanceClientRepository extends JpaRepository<RelanceClient, Long> {

    List<RelanceClient> findByClientIdOrderByDateRelanceDesc(Long clientId);

    List<RelanceClient> findByFactureIdOrderByDateRelanceDesc(Long factureId);

    List<RelanceClient> findByPointDeVenteIdOrderByDateRelanceDesc(Long pointDeVenteId);
}
