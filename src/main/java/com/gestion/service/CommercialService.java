package com.gestion.service;

import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.PerformanceCommercialDTO;
import com.gestion.persistent.model.ObjectifCommercial;
import com.gestion.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommercialService {

    private final UserRepository userRepository;
    private final ObjectifCommercialRepository objectifCommercialRepository;
    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final ClientRepository clientRepository;
    private final FactureRepository factureRepository;

    public CommercialService(UserRepository userRepository,
                             ObjectifCommercialRepository objectifCommercialRepository,
                             VenteRepository venteRepository,
                             LigneVenteRepository ligneVenteRepository,
                             ClientRepository clientRepository,
                             FactureRepository factureRepository) {
        this.userRepository = userRepository;
        this.objectifCommercialRepository = objectifCommercialRepository;
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.clientRepository = clientRepository;
        this.factureRepository = factureRepository;
    }

    /**
     * Récupère la liste des commerciaux pour l'entreprise courante
     */
    @Transactional(readOnly = true)
    public List<com.acommon.persistant.dto.UserResponse> getCommerciaux() {
        Long currentTenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        if (currentTenant == null) {
            return List.of();
        }

        List<User> users = userRepository.findByTenantId(currentTenant);
        if (users.isEmpty()) {
            users = userRepository.findByPointDeVenteId(currentTenant);
        }

        return users.stream()
                .filter(u -> {
                    String roleNom = (u.getRole() != null && u.getRole().getNom() != null) ? u.getRole().getNom() : "";
                    return roleNom.contains("COMMERCIAL") || roleNom.contains("VENDEUR") || roleNom.contains("ADMIN") || roleNom.contains("GESTIONNAIRE");
                })
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.acommon.persistant.dto.UserResponse mapToUserResponse(User user) {
        com.acommon.persistant.dto.UserResponse response = new com.acommon.persistant.dto.UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setNomComplet(user.getNomComplet());
        response.setTelephone(user.getTelephone());
        response.setGenre(user.getGenre());
        response.setRole(user.getRole() != null ? user.getRole().getNom() : null);
        response.setTenantId(user.getTenantId());
        response.setPointDeVenteId(user.getPointDeVenteId());
        if (user.getPointDeVente() != null) {
            response.setNomPointDeVente(user.getPointDeVente().getNomPointDeVente());
        }
        response.setEnabled(user.isEnabled());
        return response;
    }

    /**
     * Récupère la performance de tous les commerciaux pour une période donnée dans le tenant courant
     */
    @Transactional(readOnly = true)
    public List<PerformanceCommercialDTO> getPerformancesCommerciaux(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        Long currentTenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        if (currentTenant == null) {
            return List.of();
        }

        List<User> users = userRepository.findByTenantId(currentTenant);
        if (users.isEmpty()) {
            users = userRepository.findByPointDeVenteId(currentTenant);
        }

        List<PerformanceCommercialDTO> performances = new ArrayList<>();

        for (User u : users) {
            // Filtrer les utilisateurs qui ont une activité commerciale ou le rôle approprié
            String roleNom = (u.getRole() != null && u.getRole().getNom() != null) ? u.getRole().getNom() : "";
            boolean isCommercial = roleNom.contains("COMMERCIAL") || roleNom.contains("VENDEUR") || roleNom.contains("ADMIN") || roleNom.contains("GESTIONNAIRE");

            Long clientsPortefeuille = clientRepository.countByCommercialIdAndPointDeVenteId(u.getId(), currentTenant);
            if (isCommercial || (clientsPortefeuille != null && clientsPortefeuille > 0)) {
                performances.add(calculerPerformanceUser(u, dateDebut, dateFin, clientsPortefeuille, currentTenant));
            }
        }

        return performances;
    }

    /**
     * Récupère la performance détaillée d'un commercial spécifique dans son tenant
     */
    @Transactional(readOnly = true)
    public PerformanceCommercialDTO getPerformanceByCommercial(Long commercialId, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        Long currentTenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        User user = userRepository.findById(commercialId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + commercialId));

        if (currentTenant != null) {
            boolean matches = currentTenant.equals(user.getTenantId())
                    || (user.getPointDeVente() != null && currentTenant.equals(user.getPointDeVente().getTenantId()))
                    || currentTenant.equals(user.getPointDeVenteId());
            if (!matches) {
                throw new org.springframework.security.access.AccessDeniedException("Accès refusé : cet utilisateur appartient à une autre entreprise");
            }
        }

        Long clientsPortefeuille = clientRepository.countByCommercialIdAndPointDeVenteId(commercialId, currentTenant);
        return calculerPerformanceUser(user, dateDebut, dateFin, clientsPortefeuille, currentTenant);
    }

    /**
     * Définit ou met à jour les objectifs mensuels d'un commercial
     */
    public ObjectifCommercial definirObjectif(Long commercialId, Integer annee, Integer mois,
                                              BigDecimal objectifCA, BigDecimal objectifMarge, String notes) {
        Long currentTenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        User commercial = userRepository.findById(commercialId)
                .orElseThrow(() -> new RuntimeException("Commercial non trouvé avec l'id: " + commercialId));

        if (currentTenant != null) {
            boolean matches = currentTenant.equals(commercial.getTenantId())
                    || (commercial.getPointDeVente() != null && currentTenant.equals(commercial.getPointDeVente().getTenantId()))
                    || currentTenant.equals(commercial.getPointDeVenteId());
            if (!matches) {
                throw new org.springframework.security.access.AccessDeniedException("Accès refusé : ce commercial appartient à une autre entreprise");
            }
        }

        Optional<ObjectifCommercial> opt = objectifCommercialRepository.findByCommercialIdAndAnneeAndMois(commercialId, annee, mois);
        ObjectifCommercial obj = opt.orElseGet(ObjectifCommercial::new);

        obj.setCommercial(commercial);
        obj.setAnnee(annee);
        obj.setMois(mois);
        obj.setObjectifCA(objectifCA != null ? objectifCA : BigDecimal.ZERO);
        obj.setObjectifMarge(objectifMarge != null ? objectifMarge : BigDecimal.ZERO);
        obj.setNotes(notes);
        obj.setPointDeVenteId(currentTenant != null ? currentTenant : 1L);

        return objectifCommercialRepository.save(obj);
    }

    @Transactional(readOnly = true)
    public List<ObjectifCommercial> getObjectifsByCommercial(Long commercialId) {
        Long currentTenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        User commercial = userRepository.findById(commercialId)
                .orElseThrow(() -> new RuntimeException("Commercial non trouvé avec l'id: " + commercialId));

        if (currentTenant != null) {
            boolean matches = currentTenant.equals(commercial.getTenantId())
                    || (commercial.getPointDeVente() != null && currentTenant.equals(commercial.getPointDeVente().getTenantId()))
                    || currentTenant.equals(commercial.getPointDeVenteId());
            if (!matches) {
                throw new org.springframework.security.access.AccessDeniedException("Accès refusé : ce commercial appartient à une autre entreprise");
            }
        }

        return objectifCommercialRepository.findByCommercialId(commercialId);
    }

    private PerformanceCommercialDTO calculerPerformanceUser(User u, LocalDate debut, LocalDate fin, Long clientsPortefeuille, Long tenantId) {
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.atTime(LocalTime.MAX);

        PerformanceCommercialDTO dto = new PerformanceCommercialDTO();
        dto.setCommercialId(u.getId());
        dto.setNomCommercial(u.getNomComplet() != null ? u.getNomComplet() : u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setTelephone(u.getTelephone());

        // 1. CA et nombre de ventes isolés par pointDeVenteId
        BigDecimal ca = venteRepository.sumCAByCommercial(u.getId(), debutDT, finDT, tenantId);
        dto.setCaRealise(ca != null ? ca : BigDecimal.ZERO);

        Long nbVentes = venteRepository.countVentesByCommercial(u.getId(), debutDT, finDT, tenantId);
        dto.setNombreVentes(nbVentes != null ? nbVentes : 0L);

        // 2. Marge réalisée isolée par pointDeVenteId
        List<Object[]> margesRaw = ligneVenteRepository.calculerTotauxMargeByCommercial(u.getId(), debutDT, finDT, tenantId);
        BigDecimal marge = BigDecimal.ZERO;
        if (margesRaw != null && !margesRaw.isEmpty()) {
            Object[] r = margesRaw.get(0);
            if (r != null && r.length >= 3) {
                BigDecimal caHT = r[0] != null ? new BigDecimal(r[0].toString()) : BigDecimal.ZERO;
                BigDecimal coutHT = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                BigDecimal remise = r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO;
                marge = caHT.subtract(coutHT).subtract(remise);
            }
        }
        dto.setMargeRealisee(marge);

        // 3. Objectifs pour le mois en cours
        Optional<ObjectifCommercial> objOpt = objectifCommercialRepository.findByCommercialIdAndAnneeAndMois(
                u.getId(), debut.getYear(), debut.getMonthValue());

        BigDecimal objCA = BigDecimal.ZERO;
        BigDecimal objMarge = BigDecimal.ZERO;
        if (objOpt.isPresent()) {
            objCA = objOpt.get().getObjectifCA() != null ? objOpt.get().getObjectifCA() : BigDecimal.ZERO;
            objMarge = objOpt.get().getObjectifMarge() != null ? objOpt.get().getObjectifMarge() : BigDecimal.ZERO;
        }
        dto.setObjectifCA(objCA);
        dto.setObjectifMarge(objMarge);

        // 4. Taux de réalisation
        if (objCA.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tauxCA = dto.getCaRealise().multiply(new BigDecimal("100")).divide(objCA, 2, RoundingMode.HALF_UP);
            dto.setTauxRealisationCA(tauxCA);
        }

        if (objMarge.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tauxMarge = dto.getMargeRealisee().multiply(new BigDecimal("100")).divide(objMarge, 2, RoundingMode.HALF_UP);
            dto.setTauxRealisationMarge(tauxMarge);
        }

        // 5. Portefeuille clients
        dto.setNombreClientsPortefeuille(clientsPortefeuille != null ? clientsPortefeuille : 0L);

        Long nouveauxClients = clientRepository.countNouveauxClientsByCommercialAndPointDeVenteId(u.getId(), debutDT, finDT, tenantId);
        dto.setNouveauxClientsPeriode(nouveauxClients != null ? nouveauxClients : 0L);

        // 6. Impayés clients sous sa responsabilité isolés par pointDeVenteId
        BigDecimal impayes = factureRepository.sumImpayesByCommercial(u.getId(), tenantId);
        dto.setTotalImpayesClients(impayes != null ? impayes : BigDecimal.ZERO);

        return dto;
    }
}
