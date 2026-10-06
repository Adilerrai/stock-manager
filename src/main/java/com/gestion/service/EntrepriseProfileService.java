package com.gestion.service;

import com.acommon.exception.CommonException;
import com.acommon.persistant.model.TenantContext;
import com.gestion.mapper.EntrepriseProfileMapper;
import com.gestion.persistent.dto.EntrepriseProfileDTO;
import com.gestion.persistent.model.EntrepriseProfile;
import com.gestion.repository.EntrepriseProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class EntrepriseProfileService {

    private final EntrepriseProfileRepository entrepriseProfileRepository;
    private final EntrepriseProfileMapper entrepriseProfileMapper;
    private final ImageCompressionService imageCompressionService;

    public EntrepriseProfileService(EntrepriseProfileRepository entrepriseProfileRepository,
            EntrepriseProfileMapper entrepriseProfileMapper,
            ImageCompressionService imageCompressionService) {
        this.entrepriseProfileRepository = entrepriseProfileRepository;
        this.entrepriseProfileMapper = entrepriseProfileMapper;
        this.imageCompressionService = imageCompressionService;
    }

    @Transactional(readOnly = true)
    public EntrepriseProfile getProfileEntityByCurrentTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null)
            tenantId = 1L;

        final Long currentTenantId = tenantId;
        return entrepriseProfileRepository.findByPointDeVenteId(currentTenantId)
                .orElseGet(() -> buildDefaultProfile(currentTenantId));
    }

    @Transactional(readOnly = true)
    public EntrepriseProfileDTO getProfileByCurrentTenant() {
        EntrepriseProfile profile = getProfileEntityByCurrentTenant();
        return entrepriseProfileMapper.toDto(profile);
    }

    @Transactional
    public EntrepriseProfileDTO updateProfile(EntrepriseProfileDTO dto) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null)
            tenantId = 1L;

        final Long currentTenantId = tenantId;
        EntrepriseProfile profile = entrepriseProfileRepository.findByPointDeVenteId(currentTenantId)
                .orElseGet(() -> buildDefaultProfile(currentTenantId));

        if (dto.getNomEntreprise() != null && !dto.getNomEntreprise().isBlank()) {
            profile.setNomEntreprise(dto.getNomEntreprise());
        }
        profile.setActivite(dto.getActivite());
        profile.setAdresse(dto.getAdresse());
        profile.setVille(dto.getVille());
        profile.setCodePostal(dto.getCodePostal());
        profile.setTelephone(dto.getTelephone());
        profile.setTelephoneSecondaire(dto.getTelephoneSecondaire());
        profile.setEmail(dto.getEmail());
        profile.setSiteWeb(dto.getSiteWeb());
        profile.setRegistreCommerce(dto.getRegistreCommerce());
        profile.setNumeroIdentificationFiscale(dto.getNumeroIdentificationFiscale());
        profile.setNumeroIdentificationStatistique(dto.getIce() != null ? dto.getIce() : dto.getNumeroIdentificationStatistique());
        profile.setIce(dto.getIce() != null ? dto.getIce() : dto.getNumeroIdentificationStatistique());
        profile.setArticleImposition(dto.getPatente() != null ? dto.getPatente() : dto.getArticleImposition());
        profile.setPatente(dto.getPatente() != null ? dto.getPatente() : dto.getArticleImposition());
        profile.setCnss(dto.getCnss());
        profile.setTelephoneSecondaire(dto.getGsm() != null ? dto.getGsm() : dto.getTelephoneSecondaire());
        profile.setGsm(dto.getGsm() != null ? dto.getGsm() : dto.getTelephoneSecondaire());
        profile.setCompteBancaireRib(dto.getCompteBancaireRib());
        profile.setNomBanque(dto.getNomBanque());
        profile.setPiedPage(dto.getPiedPage());
        if (dto.getDevise() != null && !dto.getDevise().isBlank()) {
            profile.setDevise(dto.getDevise());
        }
        if (dto.getVenteStockNegatif() != null) {
            profile.setVenteStockNegatif(dto.getVenteStockNegatif());
        }

        // === 5 RÈGLES MÉTIER ERP CONFIGURABLES ===
        if (dto.getApprobationAchatActive() != null) {
            profile.setApprobationAchatActive(dto.getApprobationAchatActive());
        }
        if (dto.getSeuilApprobationAchat() != null) {
            profile.setSeuilApprobationAchat(dto.getSeuilApprobationAchat());
        }
        if (dto.getBlocageEncoursClientActif() != null) {
            profile.setBlocageEncoursClientActif(dto.getBlocageEncoursClientActif());
        }
        if (dto.getValidationRemiseMaxActive() != null) {
            profile.setValidationRemiseMaxActive(dto.getValidationRemiseMaxActive());
        }
        if (dto.getSeuilRemiseMaxPourcentage() != null) {
            profile.setSeuilRemiseMaxPourcentage(dto.getSeuilRemiseMaxPourcentage());
        }
        if (dto.getInterdictionAutoApprobation() != null) {
            profile.setInterdictionAutoApprobation(dto.getInterdictionAutoApprobation());
        }
        if (dto.getToleranceEcartReceptionActive() != null) {
            profile.setToleranceEcartReceptionActive(dto.getToleranceEcartReceptionActive());
        }
        if (dto.getToleranceEcartPourcentage() != null) {
            profile.setToleranceEcartPourcentage(dto.getToleranceEcartPourcentage());
        }

        profile.setDateMiseAJour(LocalDateTime.now());

        EntrepriseProfile saved = entrepriseProfileRepository.save(profile);
        return entrepriseProfileMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public boolean isVenteStockNegatifAutorisee() {
        try {
            EntrepriseProfile profile = getProfileEntityByCurrentTenant();
            return Boolean.TRUE.equals(profile.getVenteStockNegatif());
        } catch (Exception e) {
            return false;
        }
    }

    // === Helpers pour les 5 Règles Métier ===

    @Transactional(readOnly = true)
    public boolean isApprobationAchatActive() {
        try {
            return Boolean.TRUE.equals(getProfileEntityByCurrentTenant().getApprobationAchatActive());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getSeuilApprobationAchat() {
        try {
            BigDecimal seuil = getProfileEntityByCurrentTenant().getSeuilApprobationAchat();
            return seuil != null ? seuil : new BigDecimal("10000.00");
        } catch (Exception e) {
            return new BigDecimal("10000.00");
        }
    }

    @Transactional(readOnly = true)
    public boolean isBlocageEncoursClientActif() {
        try {
            return Boolean.TRUE.equals(getProfileEntityByCurrentTenant().getBlocageEncoursClientActif());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isValidationRemiseMaxActive() {
        try {
            return Boolean.TRUE.equals(getProfileEntityByCurrentTenant().getValidationRemiseMaxActive());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getSeuilRemiseMaxPourcentage() {
        try {
            BigDecimal seuil = getProfileEntityByCurrentTenant().getSeuilRemiseMaxPourcentage();
            return seuil != null ? seuil : new BigDecimal("10.00");
        } catch (Exception e) {
            return new BigDecimal("10.00");
        }
    }

    @Transactional(readOnly = true)
    public boolean isInterdictionAutoApprobation() {
        try {
            return Boolean.TRUE.equals(getProfileEntityByCurrentTenant().getInterdictionAutoApprobation());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isToleranceEcartReceptionActive() {
        try {
            return Boolean.TRUE.equals(getProfileEntityByCurrentTenant().getToleranceEcartReceptionActive());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getToleranceEcartPourcentage() {
        try {
            BigDecimal tol = getProfileEntityByCurrentTenant().getToleranceEcartPourcentage();
            return tol != null ? tol : new BigDecimal("5.00");
        } catch (Exception e) {
            return new BigDecimal("5.00");
        }
    }

    @Transactional
    public EntrepriseProfileDTO uploadLogo(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new CommonException("Fichier image vide", HttpStatus.BAD_REQUEST, "LOGO_EMPTY");
        }

        long maxBytes = 5L * 1024 * 1024; // 5 MB
        if (file.getSize() > maxBytes) {
            throw new CommonException("Le logo est trop volumineux (max 5MB)", HttpStatus.PAYLOAD_TOO_LARGE,
                    "LOGO_TOO_LARGE");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/webp"))) {
            throw new CommonException("Format non supporté (JPEG, PNG, WEBP acceptés)",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "LOGO_FORMAT_INVALID");
        }

        byte[] compressedData = imageCompressionService.compressImage(file.getBytes(), contentType);

        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null)
            tenantId = 1L;

        final Long currentTenantId = tenantId;
        EntrepriseProfile profile = entrepriseProfileRepository.findByPointDeVenteId(currentTenantId)
                .orElseGet(() -> buildDefaultProfile(currentTenantId));

        profile.setLogoData(compressedData);
        profile.setLogoContentType(contentType);
        profile.setLogoFileName(file.getOriginalFilename());
        profile.setDateMiseAJour(LocalDateTime.now());

        EntrepriseProfile saved = entrepriseProfileRepository.save(profile);
        return entrepriseProfileMapper.toDto(saved);
    }

    @Transactional
    public void removeLogo() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null)
            tenantId = 1L;

        final Long currentTenantId = tenantId;
        entrepriseProfileRepository.findByPointDeVenteId(currentTenantId).ifPresent(profile -> {
            profile.setLogoData(null);
            profile.setLogoContentType(null);
            profile.setLogoFileName(null);
            profile.setDateMiseAJour(LocalDateTime.now());
            entrepriseProfileRepository.save(profile);
        });
    }

    private EntrepriseProfile buildDefaultProfile(Long tenantId) {
        EntrepriseProfile defaultProfile = new EntrepriseProfile();
        defaultProfile.setPointDeVenteId(tenantId);
        defaultProfile.setNomEntreprise("");
        defaultProfile.setActivite("");
        defaultProfile.setAdresse("");
        defaultProfile.setVille("");
        defaultProfile.setCodePostal("");
        defaultProfile.setTelephone("");
        defaultProfile.setTelephoneSecondaire("");
        defaultProfile.setGsm("");
        defaultProfile.setEmail("");
        defaultProfile.setSiteWeb("");
        defaultProfile.setRegistreCommerce("");
        defaultProfile.setNumeroIdentificationFiscale("");
        defaultProfile.setNumeroIdentificationStatistique("");
        defaultProfile.setIce("");
        defaultProfile.setArticleImposition("");
        defaultProfile.setPatente("");
        defaultProfile.setCnss("");
        defaultProfile.setCompteBancaireRib("");
        defaultProfile.setNomBanque("");
        defaultProfile.setPiedPage("");
        defaultProfile.setDevise("MAD");
        defaultProfile.setVenteStockNegatif(false);
        defaultProfile.setApprobationAchatActive(false);
        defaultProfile.setSeuilApprobationAchat(new BigDecimal("10000.00"));
        defaultProfile.setBlocageEncoursClientActif(false);
        defaultProfile.setValidationRemiseMaxActive(false);
        defaultProfile.setSeuilRemiseMaxPourcentage(new BigDecimal("10.00"));
        defaultProfile.setInterdictionAutoApprobation(false);
        defaultProfile.setToleranceEcartReceptionActive(false);
        defaultProfile.setToleranceEcartPourcentage(new BigDecimal("5.00"));
        defaultProfile.setDateMiseAJour(LocalDateTime.now());
        return defaultProfile;
    }
}
