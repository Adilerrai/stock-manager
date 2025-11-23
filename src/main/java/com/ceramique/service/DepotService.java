package com.ceramique.service;

import com.acommon.annotation.MultitenantSearchMethod;
import com.acommon.exception.ResourceNotFoundException;
import com.ceramique.persistent.dto.DepotDTO;
import com.ceramique.persistent.model.Depot;
import com.ceramique.repository.DepotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepotService {

    private final DepotRepository depotRepository;

    public DepotService(DepotRepository depotRepository) {
        this.depotRepository = depotRepository;
    }

    @Transactional
    @MultitenantSearchMethod(description = "Création d'un nouveau dépôt")
    public Depot createDepot(DepotDTO depotDTO) {

        if (depotRepository.existsByNom(depotDTO.getNom())) {
            throw new IllegalArgumentException("Un dépôt avec ce nom existe déjà");
        }

        Depot depot = new Depot();
        depot.setNom(depotDTO.getNom());
        depot.setDescription(depotDTO.getDescription());
        depot.setAdresse(depotDTO.getAdresse());
        depot.setActif(true);

        return depotRepository.save(depot);
    }

    public List<Depot> getAllDepotsActifs() {
        return depotRepository.findByActifTrue(true);
    }

    public Depot getDepotById(Long depotId) {

        return depotRepository.findById(depotId)
                .orElseThrow(() -> new ResourceNotFoundException("Depot", "id", depotId));
    }

    @Transactional
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