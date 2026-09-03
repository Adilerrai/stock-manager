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
     * Récupère la performance de tous les commerciaux pour une période donnée
     */
    @Transactional(readOnly = true)
    public List<PerformanceCommercialDTO> getPerformancesCommerciaux(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        List<User> users = userRepository.findAll();
        List<PerformanceCommercialDTO> performances = new ArrayList<>();

        for (User u : users) {
            // Filtrer les utilisateurs qui ont une activité commerciale ou le rôle approprié
            String roleNom = (u.getRole() != null && u.getRole().getNom() != null) ? u.getRole().getNom() : "";
            boolean isCommercial = roleNom.contains("COMMERCIAL") || roleNom.contains("VENDEUR") || roleNom.contains("ADMIN") || roleNom.contains("GESTIONNAIRE");

            Long clientsPortefeuille = clientRepository.countByCommercialId(u.getId());
            if (isCommercial || (clientsPortefeuille != null && clientsPortefeuille > 0)) {
                performances.add(calculerPerformanceUser(u, dateDebut, dateFin, clientsPortefeuille));
            }
        }

        return performances;
    }

    /**
     * Récupère la performance détaillée d'un commercial spécifique
     */
    @Transactional(readOnly = true)
    public PerformanceCommercialDTO getPerformanceByCommercial(Long commercialId, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();

        User user = userRepository.findById(commercialId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + commercialId));

        Long clientsPortefeuille = clientRepository.countByCommercialId(commercialId);
        return calculerPerformanceUser(user, dateDebut, dateFin, clientsPortefeuille);
    }

    /**
     * Définit ou met à jour les objectifs mensuels d'un commercial
     */
    public ObjectifCommercial definirObjectif(Long commercialId, Integer annee, Integer mois,
                                              BigDecimal objectifCA, BigDecimal objectifMarge, String notes) {
        User commercial = userRepository.findById(commercialId)
                .orElseThrow(() -> new RuntimeException("Commercial non trouvé avec l'id: " + commercialId));

        Optional<ObjectifCommercial> opt = objectifCommercialRepository.findByCommercialIdAndAnneeAndMois(commercialId, annee, mois);
        ObjectifCommercial obj = opt.orElseGet(ObjectifCommercial::new);

        obj.setCommercial(commercial);
        obj.setAnnee(annee);
        obj.setMois(mois);
        obj.setObjectifCA(objectifCA != null ? objectifCA : BigDecimal.ZERO);
        obj.setObjectifMarge(objectifMarge != null ? objectifMarge : BigDecimal.ZERO);
        obj.setNotes(notes);

        return objectifCommercialRepository.save(obj);
    }

    @Transactional(readOnly = true)
    public List<ObjectifCommercial> getObjectifsByCommercial(Long commercialId) {
        return objectifCommercialRepository.findByCommercialId(commercialId);
    }

    private PerformanceCommercialDTO calculerPerformanceUser(User u, LocalDate debut, LocalDate fin, Long clientsPortefeuille) {
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.atTime(LocalTime.MAX);

        PerformanceCommercialDTO dto = new PerformanceCommercialDTO();
        dto.setCommercialId(u.getId());
        dto.setNomCommercial(u.getNomComplet() != null ? u.getNomComplet() : u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setTelephone(u.getTelephone());

        // 1. CA et nombre de ventes
        BigDecimal ca = venteRepository.sumCAByCommercial(u.getId(), debutDT, finDT);
        dto.setCaRealise(ca != null ? ca : BigDecimal.ZERO);

        Long nbVentes = venteRepository.countVentesByCommercial(u.getId(), debutDT, finDT);
        dto.setNombreVentes(nbVentes != null ? nbVentes : 0L);

        // 2. Marge réalisée
        List<Object[]> margesRaw = ligneVenteRepository.calculerTotauxMargeByCommercial(u.getId(), debutDT, finDT);
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

        Long nouveauxClients = clientRepository.countNouveauxClientsByCommercial(u.getId(), debutDT, finDT);
        dto.setNouveauxClientsPeriode(nouveauxClients != null ? nouveauxClients : 0L);

        // 6. Impayés clients sous sa responsabilité
        BigDecimal impayes = factureRepository.sumImpayesByCommercial(u.getId());
        dto.setTotalImpayesClients(impayes != null ? impayes : BigDecimal.ZERO);

        return dto;
    }
}
