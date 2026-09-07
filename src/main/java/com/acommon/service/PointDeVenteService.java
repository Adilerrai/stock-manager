package com.acommon.service;

import com.acommon.persistant.dto.PointDeVenteRequest;
import com.acommon.persistant.dto.PointDeVenteResponse;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PointDeVenteService {

    private final PointDeVenteRepository pointDeVenteRepository;
    private final UserRepository userRepository;

    public PointDeVenteService(
            PointDeVenteRepository pointDeVenteRepository,
            UserRepository userRepository) {
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.userRepository = userRepository;
    }

    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        if (auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return userRepository.findByEmail(auth.getName())
                .or(() -> userRepository.findByUsername(auth.getName()))
                .orElse(null);
    }

    private Long resolveCurrentTenant() {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        return TenantContext.getCurrentTenant();
    }

    @Transactional(readOnly = true)
    public List<PointDeVenteResponse> getPointsDeVenteByCurrentTenant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isSuperAdmin(auth)) {
            return pointDeVenteRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        Long tenantId = resolveCurrentTenant();
        if (tenantId == null) {
            return List.of();
        }

        List<PointDeVente> list = pointDeVenteRepository.findAllByTenantId(tenantId);
        if (list.isEmpty()) {
            // Fallback si tenantId correspond à l'id du point de vente
            pointDeVenteRepository.findById(tenantId).ifPresent(list::add);
        }

        return list.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PointDeVenteResponse getPointDeVenteById(Long id) {
        PointDeVente pdv = pointDeVenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Point de vente avec l'ID " + id + " introuvable"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isSuperAdmin(auth)) {
            Long tenantId = resolveCurrentTenant();
            if (tenantId != null && !tenantId.equals(pdv.getTenantId()) && !id.equals(tenantId)) {
                throw new AccessDeniedException("Accès refusé : ce point de vente appartient à une autre entreprise");
            }
        }

        return mapToResponse(pdv);
    }

    @Transactional
    public PointDeVenteResponse createPointDeVente(PointDeVenteRequest request) {
        Long tenantId = resolveCurrentTenant();
        if (tenantId == null) {
            throw new AccessDeniedException("Impossible d'identifier l'entreprise (tenant) pour ce point de vente");
        }

        PointDeVente pdv = new PointDeVente();
        pdv.setTenantId(tenantId);
        pdv.setNomPointDeVente(request.getNomPointDeVente().trim());
        pdv.setNom(request.getNomPointDeVente().trim());
        pdv.setAdresse(request.getAdresse());
        pdv.setTelephone(request.getTelephone());
        pdv.setEmail(request.getEmail());
        pdv.setActif(request.getActif() != null ? request.getActif() : true);
        pdv.setDateCreation(LocalDateTime.now());

        PointDeVente saved = pointDeVenteRepository.save(pdv);
        return mapToResponse(saved);
    }

    @Transactional
    public PointDeVenteResponse updatePointDeVente(Long id, PointDeVenteRequest request) {
        PointDeVente pdv = pointDeVenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Point de vente avec l'ID " + id + " introuvable"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isSuperAdmin(auth)) {
            Long tenantId = resolveCurrentTenant();
            if (tenantId != null && !tenantId.equals(pdv.getTenantId()) && !id.equals(tenantId)) {
                throw new AccessDeniedException("Accès refusé : ce point de vente appartient à une autre entreprise");
            }
        }

        pdv.setNomPointDeVente(request.getNomPointDeVente().trim());
        pdv.setNom(request.getNomPointDeVente().trim());
        if (request.getAdresse() != null) pdv.setAdresse(request.getAdresse());
        if (request.getTelephone() != null) pdv.setTelephone(request.getTelephone());
        if (request.getEmail() != null) pdv.setEmail(request.getEmail());
        if (request.getActif() != null) pdv.setActif(request.getActif());

        PointDeVente saved = pointDeVenteRepository.save(pdv);
        return mapToResponse(saved);
    }

    private PointDeVenteResponse mapToResponse(PointDeVente pdv) {
        PointDeVenteResponse res = new PointDeVenteResponse();
        res.setId(pdv.getId());
        res.setTenantId(pdv.getTenantId());
        res.setNomPointDeVente(pdv.getNomPointDeVente());
        res.setNom(pdv.getNom());
        res.setAdresse(pdv.getAdresse());
        res.setTelephone(pdv.getTelephone());
        res.setEmail(pdv.getEmail());
        res.setActif(pdv.getActif());
        res.setDateCreation(pdv.getDateCreation());
        return res;
    }
}
