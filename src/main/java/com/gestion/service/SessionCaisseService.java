package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.StatutSessionCaisse;
import com.gestion.persistent.model.SessionCaisse;
import com.gestion.repository.SessionCaisseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SessionCaisseService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final UserRepository userRepository;

    public SessionCaisseService(SessionCaisseRepository sessionCaisseRepository,
                                UserRepository userRepository) {
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.userRepository = userRepository;
    }

    public SessionCaisse ouvrirSession(Long userId, BigDecimal fondDeCaisseInitial, String notes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userId));

        // Vérifier s'il a déjà une session ouverte
        Optional<SessionCaisse> existante = sessionCaisseRepository
                .findFirstByCaissierIdAndStatutOrderByDateOuvertureDesc(userId, StatutSessionCaisse.OUVERTE);
        if (existante.isPresent()) {
            return existante.get(); // Retourner la session déjà ouverte
        }

        SessionCaisse session = new SessionCaisse();
        session.setReference(genererReference());
        session.setDateOuverture(LocalDateTime.now());
        session.setCaissier(user);
        session.setFondDeCaisseInitial(fondDeCaisseInitial != null ? fondDeCaisseInitial : BigDecimal.ZERO);
        session.setStatut(StatutSessionCaisse.OUVERTE);
        session.setNotes(notes);

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            session.setPointDeVenteId(tenantId);
        }

        session.calculerTotaux();
        return sessionCaisseRepository.save(session);
    }

    public void enregistrerEncaissement(Long sessionId, BigDecimal montant, ModePaiement mode) {
        SessionCaisse session = getSessionById(sessionId);
        if (session.getStatut() == StatutSessionCaisse.CLOTUREE) {
            return;
        }

        session.setTotalVentes(session.getTotalVentes().add(montant));

        if (mode == ModePaiement.ESPECES) {
            session.setTotalEspeces(session.getTotalEspeces().add(montant));
        } else if (mode == ModePaiement.CARTE_BANCAIRE) {
            session.setTotalCarte(session.getTotalCarte().add(montant));
        } else if (mode == ModePaiement.CHEQUE) {
            session.setTotalCheque(session.getTotalCheque().add(montant));
        } else if (mode == ModePaiement.VIREMENT) {
            session.setTotalVirement(session.getTotalVirement().add(montant));
        } else if (mode == ModePaiement.CREDIT) {
            session.setTotalCredit(session.getTotalCredit().add(montant));
        }

        session.calculerTotaux();
        sessionCaisseRepository.save(session);
    }

    public SessionCaisse cloturerSession(Long sessionId, BigDecimal montantReelCloture, String notes) {
        SessionCaisse session = getSessionById(sessionId);
        if (session.getStatut() == StatutSessionCaisse.CLOTUREE) {
            throw new RuntimeException("Cette session est déjà clôturée");
        }

        session.setDateCloture(LocalDateTime.now());
        session.setMontantReelCloture(montantReelCloture != null ? montantReelCloture : BigDecimal.ZERO);
        session.setStatut(StatutSessionCaisse.CLOTUREE);
        if (notes != null) {
            session.setNotes((session.getNotes() != null ? session.getNotes() + " | " : "") + notes);
        }

        session.calculerTotaux();
        return sessionCaisseRepository.save(session);
    }

    public Optional<SessionCaisse> getSessionActive(Long userId) {
        return sessionCaisseRepository.findFirstByCaissierIdAndStatutOrderByDateOuvertureDesc(userId, StatutSessionCaisse.OUVERTE);
    }

    public SessionCaisse getSessionById(Long id) {
        return sessionCaisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session de caisse non trouvée: " + id));
    }

    public List<SessionCaisse> getAllSessions() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return sessionCaisseRepository.findByPointDeVenteIdOrderByDateOuvertureDesc(tenantId);
        }
        return sessionCaisseRepository.findAll();
    }

    private String genererReference() {
        String prefixe = "CSS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = sessionCaisseRepository.count() + 1;
        return prefixe + String.format("%04d", count);
    }
}
