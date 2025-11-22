package com.acommon.service;

import com.acommon.persistant.dto.PointDeVenteCreationRequest;
import com.acommon.persistant.dto.PointDeVenteResponse;
import com.acommon.persistant.model.PointDeVente;

import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class PointDeVenteService {


    @Autowired
    private PointDeVenteRepository pointDeVenteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Transactional
    public PointDeVenteResponse createPointDeVente(PointDeVenteCreationRequest request) {
        if (pointDeVenteRepository.findByNomPointDeVente(request.getNomPointDeVente()).isPresent()) {
            throw new IllegalArgumentException("Un point de vente avec ce nom existe déjà");
        }

        Long countMax = pointDeVenteRepository.count();

        PointDeVente pointDeVente = new PointDeVente();
        pointDeVente.setNomPointDeVente(request.getNomPointDeVente());
        pointDeVente.setPassword(passwordEncoder.encode(request.getPassword()));
        pointDeVente.setTenantId(countMax + 1);

        pointDeVente = pointDeVenteRepository.save(pointDeVente);
        return mapToResponse(pointDeVente);
    }

    private PointDeVenteResponse mapToResponse(PointDeVente pointDeVente) {
        PointDeVenteResponse response = new PointDeVenteResponse();
        response.setId(pointDeVente.getId());
        response.setNomPointDeVente(pointDeVente.getNomPointDeVente());
        response.setTenantId(pointDeVente.getTenantId());

        return response;
    }

    public List<PointDeVenteResponse> getAllPointsDeVente() {
        List<PointDeVente> pointsDeVente = pointDeVenteRepository.findAll();
        return pointsDeVente.stream()
                .map(this::mapToResponse)
                .toList();
    }
}