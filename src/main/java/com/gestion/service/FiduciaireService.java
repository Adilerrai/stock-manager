package com.gestion.service;

import com.acommon.config.JwtUtil;
import com.acommon.persistant.dto.JwtAuthenticationResponse;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.dto.DashboardFiduciaireDTO;
import com.gestion.persistent.dto.EcheanceFiscaleDTO;
import com.gestion.persistent.dto.MereDTO;
import com.gestion.persistent.dto.SocieteDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.persistent.enums.RegimeTva;
import com.gestion.persistent.model.EcheanceFiscale;
import com.gestion.persistent.model.Mere;
import com.gestion.persistent.model.Societe;
import com.gestion.repository.EcheanceFiscaleRepository;
import com.gestion.repository.MereRepository;
import com.gestion.repository.SocieteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FiduciaireService {

    private final SocieteRepository societeRepository;
    private final EcheanceFiscaleRepository echeanceRepository;
    private final AuditService auditService;
    private final MereRepository mereRepository;
    private final UserRepository userRepository;
    private final PointDeVenteRepository pointDeVenteRepository;
    private final JwtUtil jwtUtil;
    private final com.gestion.repository.CollaborateurSocieteRepository collaborateurRepository;

    public FiduciaireService(SocieteRepository societeRepository,
                             EcheanceFiscaleRepository echeanceRepository,
                             AuditService auditService,
                             MereRepository mereRepository,
                             UserRepository userRepository,
                             PointDeVenteRepository pointDeVenteRepository,
                             JwtUtil jwtUtil,
                             com.gestion.repository.CollaborateurSocieteRepository collaborateurRepository) {
        this.societeRepository = societeRepository;
        this.echeanceRepository = echeanceRepository;
        this.auditService = auditService;
        this.mereRepository = mereRepository;
        this.userRepository = userRepository;
        this.pointDeVenteRepository = pointDeVenteRepository;
        this.jwtUtil = jwtUtil;
        this.collaborateurRepository = collaborateurRepository;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User u) {
                return userRepository.findById(u.getId()).orElse(u);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Long getCurrentMereId() {
        User user = getCurrentUser();
        if (user != null && user.getMereId() != null) {
            return user.getMereId();
        }
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    // =========================================================================
    // SWITCH DE SOCIÉTÉ ACTIVE (SANS AUCUN HEADER CUSTOM)
    // =========================================================================

    public JwtAuthenticationResponse switchSociete(Long targetSocieteId) {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        Long mereId = user.getMereId();

        // 1. Recherche de la société cible
        Societe targetSociete = societeRepository.findById(targetSocieteId)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable avec l'ID: " + targetSocieteId));

        // 2. Vérification stricte d'isolation : la société DOIT appartenir à la Mère de l'utilisateur !
        if (mereId != null && targetSociete.getMereId() != null && !mereId.equals(targetSociete.getMereId())) {
            throw new SecurityException("Accès refusé : la société '" + targetSociete.getRaisonSociale() 
                    + "' n'appartient pas à votre cabinet/holding Mère.");
        }

        // 2b. Vérification de l'affectation du collaborateur (si non-admin)
        boolean isAdmin = user.getRole() != null && 
                ("ROLE_ADMIN".equals(user.getRole().getNom()) || "ROLE_SUPERADMIN".equals(user.getRole().getNom()));
        if (!isAdmin) {
            long nbAffectations = collaborateurRepository.countByUserId(user.getId());
            if (nbAffectations > 0 && !collaborateurRepository.existsByUserIdAndSocieteId(user.getId(), targetSociete.getId())) {
                throw new SecurityException("Accès refusé : vous n'êtes pas affecté au dossier '" + targetSociete.getRaisonSociale() + "'.");
            }
        }

        if (Boolean.FALSE.equals(targetSociete.getActif())) {
            throw new IllegalStateException("Impossible de basculer vers une société inactive.");
        }

        // 3. Mise à jour de la société active en base
        user.setTenantId(targetSociete.getId());
        pointDeVenteRepository.findById(targetSociete.getId()).ifPresent(user::setPointDeVente);
        user.setPointDeVenteId(targetSociete.getId());
        userRepository.save(user);

        // 4. Positionnement du contexte de thread
        TenantContext.setCurrentTenant(targetSociete.getId());

        // 5. Régénération du token JWT avec le nouveau tenantId (la société active)
        String newToken = jwtUtil.generateToken(user, targetSociete.getId(), targetSociete.getId());

        // 6. Audit
        auditService.logCreation("SwitchSociete", targetSociete.getId(), 
                "Bascule vers société: " + targetSociete.getRaisonSociale() + " (ID: " + targetSociete.getId() + ")");

        return JwtAuthenticationResponse.builder()
                .token(newToken)
                .id(user.getId())
                .email(user.getEmail())
                .nomComplet(user.getNomComplet())
                .telephone(user.getTelephone())
                .genre(user.getGenre())
                .username(user.getUsername())
                .role(user.getRole() != null ? user.getRole().getNom() : null)
                .habilitations(
                        user.getRole() != null && user.getRole().getHabilitations() != null
                                ? user.getRole().getHabilitations().stream()
                                        .map(com.acommon.persistant.model.Habilitation::getNom)
                                        .sorted()
                                        .collect(Collectors.toList())
                                : List.of()
                )
                .tenantId(targetSociete.getId())
                .pointDeVenteId(targetSociete.getId())
                .nomPointDeVente(targetSociete.getRaisonSociale())
                .tokenType("Bearer")
                .build();
    }

    @Transactional(readOnly = true)
    public List<SocieteDTO> listerMesSocietes() {
        Long mereId = getCurrentMereId();
        User user = getCurrentUser();
        List<Societe> societes = societeRepository.findByMereIdAndActifTrueOrderByRaisonSocialeAsc(mereId);

        boolean isAdmin = user == null || (user.getRole() != null && 
                ("ROLE_ADMIN".equals(user.getRole().getNom()) || "ROLE_SUPERADMIN".equals(user.getRole().getNom())));

        if (!isAdmin && user != null) {
            long nbAffectations = collaborateurRepository.countByUserId(user.getId());
            if (nbAffectations > 0) {
                java.util.Set<Long> societeIdsAutorisees = collaborateurRepository.findByUserId(user.getId())
                        .stream().map(com.gestion.persistent.model.CollaborateurSociete::getSocieteId)
                        .collect(Collectors.toSet());
                societes = societes.stream()
                        .filter(s -> societeIdsAutorisees.contains(s.getId()))
                        .collect(Collectors.toList());
            }
        }

        return societes.stream().map(this::toDto).collect(Collectors.toList());
    }

    // =========================================================================
    // AFFECTATION COLLABORATEURS PAR DOSSIER
    // =========================================================================

    public com.gestion.persistent.dto.CollaborateurSocieteDTO assignerCollaborateur(Long societeId, Long userId, String roleDossier) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable ID: " + userId));
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + societeId));

        com.gestion.persistent.model.CollaborateurSociete cs = collaborateurRepository.findByUserIdAndSocieteId(userId, societeId)
                .orElseGet(() -> new com.gestion.persistent.model.CollaborateurSociete(userId, societeId, roleDossier));
        cs.setRoleDossier(roleDossier != null ? roleDossier : "GESTIONNAIRE");
        com.gestion.persistent.model.CollaborateurSociete saved = collaborateurRepository.save(cs);

        auditService.logCreation("COLLABORATEUR_SOCIETE", saved.getId(), 
                "Affectation collaborateur " + user.getNomComplet() + " au dossier " + societe.getRaisonSociale());

        com.gestion.persistent.dto.CollaborateurSocieteDTO dto = new com.gestion.persistent.dto.CollaborateurSocieteDTO();
        dto.setId(saved.getId());
        dto.setUserId(user.getId());
        dto.setNomComplet(user.getNomComplet());
        dto.setEmail(user.getEmail());
        dto.setSocieteId(societe.getId());
        dto.setRaisonSociale(societe.getRaisonSociale());
        dto.setRoleDossier(saved.getRoleDossier());
        dto.setDateAffectation(saved.getDateAffectation());
        return dto;
    }

    public void retirerCollaborateur(Long societeId, Long userId) {
        collaborateurRepository.findByUserIdAndSocieteId(userId, societeId).ifPresent(cs -> {
            collaborateurRepository.delete(cs);
            auditService.logSuppression("COLLABORATEUR_SOCIETE", cs.getId(), 
                    "Retrait collaborateur " + userId + " du dossier " + societeId);
        });
    }

    @Transactional(readOnly = true)
    public List<com.gestion.persistent.dto.CollaborateurSocieteDTO> listerCollaborateursSociete(Long societeId) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + societeId));

        return collaborateurRepository.findBySocieteId(societeId).stream().map(cs -> {
            com.gestion.persistent.dto.CollaborateurSocieteDTO dto = new com.gestion.persistent.dto.CollaborateurSocieteDTO();
            dto.setId(cs.getId());
            dto.setUserId(cs.getUserId());
            dto.setSocieteId(cs.getSocieteId());
            dto.setRaisonSociale(societe.getRaisonSociale());
            dto.setRoleDossier(cs.getRoleDossier());
            dto.setDateAffectation(cs.getDateAffectation());
            userRepository.findById(cs.getUserId()).ifPresent(u -> {
                dto.setNomComplet(u.getNomComplet());
                dto.setEmail(u.getEmail());
            });
            return dto;
        }).collect(Collectors.toList());
    }

    // =========================================================================
    // GESTION DES MÈRES (CABINETS / HOLDINGS)
    // =========================================================================

    public MereDTO creerMere(MereDTO dto) {
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du Cabinet/Holding est obligatoire");
        }
        Mere mere = new Mere();
        mere.setNom(dto.getNom().trim());
        mere.setIce(dto.getIce());
        mere.setRc(dto.getRc());
        mere.setIdentifiantFiscal(dto.getIdentifiantFiscal());
        mere.setPatente(dto.getPatente());
        mere.setFormeJuridique(dto.getFormeJuridique() != null ? dto.getFormeJuridique() : "SARL");
        mere.setAdresse(dto.getAdresse());
        mere.setVille(dto.getVille());
        mere.setCodePostal(dto.getCodePostal());
        mere.setTelephone(dto.getTelephone());
        mere.setEmail(dto.getEmail());
        mere.setSiteWeb(dto.getSiteWeb());
        mere.setActif(true);
        Mere saved = mereRepository.save(mere);
        auditService.logCreation("MERE", saved.getId(), "Création Cabinet/Holding Mère: " + saved.getNom());
        return toMereDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MereDTO> listerMeres() {
        return mereRepository.findByActifTrueOrderByNomAsc().stream()
                .map(this::toMereDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MereDTO getMere(Long id) {
        return mereRepository.findById(id)
                .map(this::toMereDto)
                .orElseThrow(() -> new IllegalArgumentException("Cabinet/Holding Mère introuvable ID: " + id));
    }

    // =========================================================================
    // GESTION MULTI-SOCIÉTÉS PAR MÈRE
    // =========================================================================

    public SocieteDTO creerSociete(SocieteDTO dto) {
        Long mereId = (dto.getMereId() != null) ? dto.getMereId() : getCurrentMereId();

        String code = (dto.getCode() != null && !dto.getCode().trim().isEmpty())
                ? dto.getCode().trim().toUpperCase()
                : "SOC-" + (societeRepository.countByMereIdAndActifTrue(mereId) + 1);

        if (societeRepository.existsByCodeAndMereId(code, mereId)) {
            throw new IllegalArgumentException("Une société avec le code " + code + " existe déjà pour votre cabinet/holding.");
        }

        Societe societe = new Societe();
        societe.setMereId(mereId);
        societe.setTenantId(mereId);
        societe.setCode(code);
        societe.setRaisonSociale(dto.getRaisonSociale() != null ? dto.getRaisonSociale() : "Société " + code);
        societe.setFormeJuridique(dto.getFormeJuridique() != null ? dto.getFormeJuridique() : "SARL");
        societe.setIce(dto.getIce());
        societe.setRc(dto.getRc());
        societe.setIdentifiantFiscal(dto.getIdentifiantFiscal());
        societe.setPatente(dto.getPatente());
        societe.setCnss(dto.getCnss());
        societe.setCapitalSocial(dto.getCapitalSocial() != null ? dto.getCapitalSocial() : BigDecimal.ZERO);
        societe.setAdresse(dto.getAdresse());
        societe.setVille(dto.getVille());
        societe.setCodePostal(dto.getCodePostal());
        societe.setTelephone(dto.getTelephone());
        societe.setEmail(dto.getEmail());
        societe.setSiteWeb(dto.getSiteWeb());
        societe.setActivitePrincipale(dto.getActivitePrincipale());
        societe.setRegimeTva(dto.getRegimeTva() != null ? dto.getRegimeTva() : RegimeTva.ENCAISSEMENT);
        societe.setPeriodiciteTva(dto.getPeriodiciteTva() != null ? dto.getPeriodiciteTva() : "MENSUELLE");
        societe.setExerciceEnCours(dto.getExerciceEnCours() != null ? dto.getExerciceEnCours() : LocalDate.now().getYear());
        societe.setDateClotureExercice(dto.getDateClotureExercice() != null ? dto.getDateClotureExercice() : "31/12");
        societe.setResponsableDossier(dto.getResponsableDossier());
        societe.setActif(true);

        long nbSocietesExistantes = societeRepository.countByMereIdAndActifTrue(mereId);
        if (nbSocietesExistantes == 0 || Boolean.TRUE.equals(dto.getIsParDefaut())) {
            desactiverAncienneParDefaut(mereId);
            societe.setIsParDefaut(true);
        } else {
            societe.setIsParDefaut(false);
        }

        Societe saved = societeRepository.save(societe);

        // Synchroniser / Créer un PointDeVente jumeau pour assurer la compatibilité complète
        try {
            PointDeVente pdv = new PointDeVente();
            pdv.setNom(saved.getRaisonSociale());
            pdv.setNomPointDeVente(saved.getRaisonSociale() + " (" + saved.getCode() + ")");
            pdv.setMereId(mereId);
            pdv.setTenantId(saved.getId()); // Le PointDeVente porte le tenantId de la société
            pdv.setAdresse(saved.getAdresse());
            pdv.setTelephone(saved.getTelephone());
            pdv.setEmail(saved.getEmail());
            pdv.setActif(true);
            pointDeVenteRepository.save(pdv);
        } catch (Exception ignored) {
        }

        auditService.logCreation("SOCIETE", saved.getId(), "Création société cliente / filiale : " + saved.getCode() + " - " + saved.getRaisonSociale());

        // Générer automatiquement le calendrier fiscal annuel
        genererEcheancierFiscalAnnuel(saved.getId(), saved.getExerciceEnCours());

        return toDto(saved);
    }

    public SocieteDTO modifierSociete(Long id, SocieteDTO dto) {
        Long mereId = getCurrentMereId();
        Societe societe = societeRepository.findByIdAndMereId(id, mereId)
                .or(() -> societeRepository.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + id));

        if (dto.getRaisonSociale() != null) societe.setRaisonSociale(dto.getRaisonSociale());
        if (dto.getFormeJuridique() != null) societe.setFormeJuridique(dto.getFormeJuridique());
        if (dto.getIce() != null) societe.setIce(dto.getIce());
        if (dto.getRc() != null) societe.setRc(dto.getRc());
        if (dto.getIdentifiantFiscal() != null) societe.setIdentifiantFiscal(dto.getIdentifiantFiscal());
        if (dto.getPatente() != null) societe.setPatente(dto.getPatente());
        if (dto.getCnss() != null) societe.setCnss(dto.getCnss());
        if (dto.getCapitalSocial() != null) societe.setCapitalSocial(dto.getCapitalSocial());
        if (dto.getAdresse() != null) societe.setAdresse(dto.getAdresse());
        if (dto.getVille() != null) societe.setVille(dto.getVille());
        if (dto.getCodePostal() != null) societe.setCodePostal(dto.getCodePostal());
        if (dto.getTelephone() != null) societe.setTelephone(dto.getTelephone());
        if (dto.getEmail() != null) societe.setEmail(dto.getEmail());
        if (dto.getSiteWeb() != null) societe.setSiteWeb(dto.getSiteWeb());
        if (dto.getActivitePrincipale() != null) societe.setActivitePrincipale(dto.getActivitePrincipale());
        if (dto.getRegimeTva() != null) societe.setRegimeTva(dto.getRegimeTva());
        if (dto.getPeriodiciteTva() != null) societe.setPeriodiciteTva(dto.getPeriodiciteTva());
        if (dto.getExerciceEnCours() != null) societe.setExerciceEnCours(dto.getExerciceEnCours());
        if (dto.getDateClotureExercice() != null) societe.setDateClotureExercice(dto.getDateClotureExercice());
        if (dto.getResponsableDossier() != null) societe.setResponsableDossier(dto.getResponsableDossier());
        if (dto.getActif() != null) societe.setActif(dto.getActif());

        if (Boolean.TRUE.equals(dto.getIsParDefaut()) && !Boolean.TRUE.equals(societe.getIsParDefaut())) {
            desactiverAncienneParDefaut(mereId);
            societe.setIsParDefaut(true);
        }

        Societe saved = societeRepository.save(societe);
        auditService.logModification("SOCIETE", saved.getId(), "TOUT", "", "", "Modification société " + saved.getCode());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SocieteDTO> listerSocietes() {
        Long mereId = getCurrentMereId();
        return societeRepository.findByMereIdOrderByRaisonSocialeAsc(mereId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SocieteDTO getSociete(Long id) {
        return societeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + id));
    }

    public void supprimerSociete(Long id) {
        Societe societe = societeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + id));
        societe.setActif(false);
        societeRepository.save(societe);
        auditService.logSuppression("SOCIETE", id, "Désactivation / archivage société " + societe.getCode());
    }

    public SocieteDTO setSocieteParDefaut(Long id) {
        Societe societe = societeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + id));
        desactiverAncienneParDefaut(societe.getMereId());
        societe.setIsParDefaut(true);
        return toDto(societeRepository.save(societe));
    }

    private void desactiverAncienneParDefaut(Long mereId) {
        societeRepository.findByMereIdAndIsParDefautTrue(mereId).ifPresent(s -> {
            s.setIsParDefaut(false);
            societeRepository.save(s);
        });
    }

    // =========================================================================
    // MOTEUR D'ÉCHÉANCES FISCALES MAROCAINES
    // =========================================================================

    public List<EcheanceFiscaleDTO> genererEcheancierFiscalAnnuel(Long societeId, int annee) {
        Societe societe = societeRepository.findById(societeId)
                .orElseThrow(() -> new IllegalArgumentException("Société introuvable ID: " + societeId));

        List<EcheanceFiscale> echeances = new ArrayList<>();

        // 1. Les 4 Acomptes de l'IS (31 mars, 30 juin, 30 septembre, 31 décembre)
        if (!"AUTO_ENTREPRENEUR".equalsIgnoreCase(societe.getFormeJuridique()) &&
            !"PERSONNE_PHYSIQUE".equalsIgnoreCase(societe.getFormeJuridique())) {
            echeances.add(creerEcheance(societe, "ACOMPTE_IS_1", LocalDate.of(annee, 3, 31), "1er Acompte IS " + annee, annee));
            echeances.add(creerEcheance(societe, "ACOMPTE_IS_2", LocalDate.of(annee, 6, 30), "2ème Acompte IS " + annee, annee));
            echeances.add(creerEcheance(societe, "ACOMPTE_IS_3", LocalDate.of(annee, 9, 30), "3ème Acompte IS " + annee, annee));
            echeances.add(creerEcheance(societe, "ACOMPTE_IS_4", LocalDate.of(annee, 12, 31), "4ème Acompte IS " + annee, annee));
            echeances.add(creerEcheance(societe, "LIASSE_FISCALE", LocalDate.of(annee, 3, 31), "Dépôt de la Liasse Fiscale (Exercice " + (annee - 1) + ")", annee));
        }

        // 2. TVA : Déclaration Mensuelle ou Trimestrielle
        if ("TRIMESTRIELLE".equalsIgnoreCase(societe.getPeriodiciteTva())) {
            echeances.add(creerEcheance(societe, "TVA_T1", LocalDate.of(annee, 4, 30), "TVA 1er Trimestre " + annee, annee));
            echeances.add(creerEcheance(societe, "TVA_T2", LocalDate.of(annee, 7, 31), "TVA 2ème Trimestre " + annee, annee));
            echeances.add(creerEcheance(societe, "TVA_T3", LocalDate.of(annee, 10, 31), "TVA 3ème Trimestre " + annee, annee));
            echeances.add(creerEcheance(societe, "TVA_T4", LocalDate.of(annee + 1, 1, 31), "TVA 4ème Trimestre " + annee, annee));
        } else {
            String[] mois = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
            for (int m = 1; m <= 12; m++) {
                LocalDate dateLimite = (m == 12)
                        ? LocalDate.of(annee + 1, 1, 31)
                        : LocalDate.of(annee, m + 1, 1).plusMonths(1).minusDays(1);
                echeances.add(creerEcheance(societe, "TVA_M" + m, dateLimite, "Déclaration TVA " + mois[m - 1] + " " + annee, annee));
            }
        }

        // 3. Taxe Professionnelle (31 Mai)
        echeances.add(creerEcheance(societe, "TAXE_PROFESSIONNELLE", LocalDate.of(annee, 5, 31), "Taxe Professionnelle " + annee, annee));

        // 4. Déclaration Annuelle des Traitements et Salaires (IR/9421 - Fin Février)
        echeances.add(creerEcheance(societe, "DECLARATION_SALAIRES_IR", LocalDate.of(annee, 2, 28), "Déclaration des Salaires 9421 " + annee, annee));

        List<EcheanceFiscale> saved = echeanceRepository.saveAll(echeances);
        return saved.stream().map(this::toEcheanceDto).collect(Collectors.toList());
    }

    private EcheanceFiscale creerEcheance(Societe societe, String type, LocalDate dateLimite, String libelle, int annee) {
        EcheanceFiscale ef = new EcheanceFiscale();
        ef.setSociete(societe);
        ef.setTypeEcheance(type);
        ef.setDateEcheance(dateLimite);
        ef.setLibelle(libelle);
        ef.setExercice(annee);
        ef.setStatut(dateLimite.isBefore(LocalDate.now()) ? "EN_RETARD" : "A_VENIR");
        ef.setMontantEstime(BigDecimal.ZERO);
        ef.setMontantPaye(BigDecimal.ZERO);
        return ef;
    }

    public EcheanceFiscaleDTO marquerEcheanceStatut(Long echeanceId, String statut, BigDecimal montantPaye, String referencePaiement) {
        EcheanceFiscale ef = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new IllegalArgumentException("Échéance introuvable ID: " + echeanceId));

        ef.setStatut(statut);
        if (montantPaye != null) ef.setMontantPaye(montantPaye);
        if (referencePaiement != null) ef.setReferencePaiement(referencePaiement);
        if ("PAYEE".equalsIgnoreCase(statut) || "TELEDECLAREE".equalsIgnoreCase(statut)) {
            ef.setDatePaiement(LocalDate.now());
        }

        EcheanceFiscale saved = echeanceRepository.save(ef);
        auditService.logModification("ECHEANCE_FISCALE", saved.getId(), "statut", "", statut,
                "Mise à jour échéance " + saved.getLibelle() + " -> " + statut);
        return toEcheanceDto(saved);
    }

    // =========================================================================
    // CONSOLE / DASHBOARD GLOBAL FIDUCIAIRE
    // =========================================================================

    @Transactional(readOnly = true)
    public DashboardFiduciaireDTO getDashboardFiduciaire() {
        Long mereId = getCurrentMereId();
        DashboardFiduciaireDTO dash = new DashboardFiduciaireDTO();

        long nbSocietes = societeRepository.countByMereIdAndActifTrue(mereId);
        dash.setTotalSocietes(nbSocietes);
        dash.setSocietesActives(nbSocietes);

        LocalDate now = LocalDate.now();
        LocalDate dans30Jours = now.plusDays(30);

        List<EcheanceFiscale> toutesEcheances = echeanceRepository.findByDateEcheanceBetween(now.minusMonths(1), dans30Jours);

        long echeancesRetard = toutesEcheances.stream()
                .filter(e -> "EN_RETARD".equals(e.getStatut()) || (e.getDateEcheance().isBefore(now) && !"PAYEE".equals(e.getStatut())))
                .count();

        long echeances30J = toutesEcheances.stream()
                .filter(e -> !e.getDateEcheance().isBefore(now) && !e.getDateEcheance().isAfter(dans30Jours))
                .count();

        dash.setEcheancesEnRetard(echeancesRetard);
        dash.setEcheancesAFaire(echeances30J);

        dash.setProchainesEcheances(
                toutesEcheances.stream()
                        .filter(e -> !"PAYEE".equals(e.getStatut()))
                        .sorted((a, b) -> a.getDateEcheance().compareTo(b.getDateEcheance()))
                        .limit(10)
                        .map(this::toEcheanceDto)
                        .collect(Collectors.toList())
        );

        List<Societe> societes = societeRepository.findByMereIdOrderByRaisonSocialeAsc(mereId);
        List<DashboardFiduciaireDTO.SyntheseDossierDTO> syntheses = societes.stream().map(s -> {
            DashboardFiduciaireDTO.SyntheseDossierDTO syn = new DashboardFiduciaireDTO.SyntheseDossierDTO();
            syn.setSocieteId(s.getId());
            syn.setCode(s.getCode());
            syn.setRaisonSociale(s.getRaisonSociale());
            syn.setIce(s.getIce());
            syn.setFormeJuridique(s.getFormeJuridique());
            syn.setResponsable(s.getResponsableDossier());
            syn.setExercice(s.getExerciceEnCours());
            syn.setPeriodiciteTva(s.getPeriodiciteTva());
            long retard = echeanceRepository.countByTenantIdAndStatut(s.getId(), "EN_RETARD");
            syn.setAlertesRetard(retard);
            return syn;
        }).collect(Collectors.toList());
        dash.setSynthesesDossiers(syntheses);

        return dash;
    }

    // =========================================================================
    // CONVERSIONS DTO
    // =========================================================================

    private MereDTO toMereDto(Mere m) {
        if (m == null) return null;
        MereDTO dto = new MereDTO();
        dto.setId(m.getId());
        dto.setNom(m.getNom());
        dto.setIce(m.getIce());
        dto.setRc(m.getRc());
        dto.setIdentifiantFiscal(m.getIdentifiantFiscal());
        dto.setPatente(m.getPatente());
        dto.setFormeJuridique(m.getFormeJuridique());
        dto.setAdresse(m.getAdresse());
        dto.setVille(m.getVille());
        dto.setCodePostal(m.getCodePostal());
        dto.setTelephone(m.getTelephone());
        dto.setEmail(m.getEmail());
        dto.setSiteWeb(m.getSiteWeb());
        dto.setActif(m.getActif());
        dto.setDateCreation(m.getDateCreation());
        dto.setNombreSocietes(societeRepository.countByMereIdAndActifTrue(m.getId()));
        return dto;
    }

    private SocieteDTO toDto(Societe s) {
        SocieteDTO dto = new SocieteDTO();
        dto.setId(s.getId());
        dto.setMereId(s.getMereId());
        dto.setTenantId(s.getTenantId());
        dto.setCode(s.getCode());
        dto.setRaisonSociale(s.getRaisonSociale());
        dto.setFormeJuridique(s.getFormeJuridique());
        dto.setIce(s.getIce());
        dto.setRc(s.getRc());
        dto.setIdentifiantFiscal(s.getIdentifiantFiscal());
        dto.setPatente(s.getPatente());
        dto.setCnss(s.getCnss());
        dto.setCapitalSocial(s.getCapitalSocial());
        dto.setAdresse(s.getAdresse());
        dto.setVille(s.getVille());
        dto.setCodePostal(s.getCodePostal());
        dto.setTelephone(s.getTelephone());
        dto.setEmail(s.getEmail());
        dto.setSiteWeb(s.getSiteWeb());
        dto.setActivitePrincipale(s.getActivitePrincipale());
        dto.setRegimeTva(s.getRegimeTva());
        dto.setPeriodiciteTva(s.getPeriodiciteTva());
        dto.setExerciceEnCours(s.getExerciceEnCours());
        dto.setDateClotureExercice(s.getDateClotureExercice());
        dto.setResponsableDossier(s.getResponsableDossier());
        dto.setIsParDefaut(s.getIsParDefaut());
        dto.setActif(s.getActif());
        dto.setDateCreation(s.getDateCreation());
        return dto;
    }

    private EcheanceFiscaleDTO toEcheanceDto(EcheanceFiscale ef) {
        EcheanceFiscaleDTO dto = new EcheanceFiscaleDTO();
        dto.setId(ef.getId());
        dto.setSocieteId(ef.getSociete() != null ? ef.getSociete().getId() : null);
        dto.setSocieteNom(ef.getSociete() != null ? ef.getSociete().getRaisonSociale() : "");
        dto.setSocieteIce(ef.getSociete() != null ? ef.getSociete().getIce() : "");
        dto.setTypeEcheance(ef.getTypeEcheance());
        dto.setLibelle(ef.getLibelle());
        dto.setDateEcheance(ef.getDateEcheance());
        dto.setStatut(ef.getStatut());
        dto.setMontantEstime(ef.getMontantEstime());
        dto.setMontantPaye(ef.getMontantPaye());
        dto.setDatePaiement(ef.getDatePaiement());
        dto.setReferencePaiement(ef.getReferencePaiement());
        dto.setCommentaire(ef.getCommentaire());
        return dto;
    }
}
