package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.DepotDTO;
import com.gestion.persistent.model.Depot;
import com.gestion.repository.DepotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepotService {

    private final DepotRepository depotRepository;

    public DepotService(DepotRepository depotRepository) {
        this.depotRepository = depotRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @Transactional
    public Depot createDepot(DepotDTO depotDTO) {
        Long tenantId = getTenantId();
        if (depotRepository.existsByNomAndPointDeVenteId(depotDTO.getNom(), tenantId)) {
            throw new IllegalArgumentException("Un dépôt avec ce nom existe déjà");
        }

        Depot depot = new Depot();
        depot.setNom(depotDTO.getNom());
        depot.setDescription(depotDTO.getDescription());
        depot.setAdresse(depotDTO.getAdresse());
        depot.setActif(true);
        depot.setPointDeVenteId(tenantId);

        return depotRepository.save(depot);
    }

    public List<Depot> getAllDepotsActifs() {
        Long tenantId = getTenantId();
        return depotRepository.findByPointDeVenteIdAndActifTrue(tenantId);
    }

    public Depot getDepotById(Long depotId) {
        Long tenantId = getTenantId();
        return depotRepository.findByIdAndPointDeVenteId(depotId, tenantId)
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
    public void deleteDepot(Long depotId) {
        Depot depot = getDepotById(depotId);
        depot.setActif(false);
        depotRepository.save(depot);
    }
}
