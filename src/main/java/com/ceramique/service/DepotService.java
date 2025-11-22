package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.repository.PointDeVenteRepository;
import com.ceramique.persistent.dto.DepotDTO;
import com.ceramique.persistent.model.Depot;
import com.ceramique.repository.DepotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepotService {

    private final DepotRepository depotRepository;
    private final PointDeVenteRepository pointDeVenteRepository;

    public DepotService(DepotRepository depotRepository, PointDeVenteRepository pointDeVenteRepository) {
        this.depotRepository = depotRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un nouveau dépôt")
    public Depot createDepot(DepotDTO depotDTO) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        if (depotRepository.existsByNomAndPointDeVente_Id(depotDTO.getNom(), pointDeVente.getId())) {
            throw new IllegalArgumentException("Un dépôt avec ce nom existe déjà");
        }

        Depot depot = new Depot();
        depot.setNom(depotDTO.getNom());
        depot.setDescription(depotDTO.getDescription());
        depot.setAdresse(depotDTO.getAdresse());
        depot.setPointDeVente(pointDeVente);
        depot.setActif(true);

        return depotRepository.save(depot);
    }

    @MultitenantSearchMethod(description = "Récupération de tous les dépôts actifs")
    public List<Depot> getAllDepotsActifs() {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return depotRepository.findByPointDeVente_IdAndActifTrue(pointDeVente.getId());
    }

    @MultitenantSearchMethod(description = "Récupération d'un dépôt par ID")
    public Depot getDepotById(Long depotId) {
        Long tenantId = TenantContext.getCurrentTenant();
        PointDeVente pointDeVente = pointDeVenteRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PointDeVente", "tenantId", tenantId));

        return depotRepository.findByIdAndPointDeVente_Id(depotId, pointDeVente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Depot", "id", depotId));
    }

    @Transactional
    @MultitenantSearchMethod(description = "Mise à jour d'un dépôt")
    public Depot updateDepot(Long depotId, DepotDTO depotDTO) {
        Depot depot = getDepotById(depotId);
        
        depot.setNom(depotDTO.getNom());
        depot.setDescription(depotDTO.getDescription());
        depot.setAdresse(depotDTO.getAdresse());
        depot.setActif(depotDTO.getActif());

        return depotRepository.save(depot);
    }

    @Transactional
    @MultitenantSearchMethod(description = "Suppression d'un dépôt")
    public void deleteDepot(Long depotId) {
        Depot depot = getDepotById(depotId);
        depot.setActif(false);
        depotRepository.save(depot);
    }
}