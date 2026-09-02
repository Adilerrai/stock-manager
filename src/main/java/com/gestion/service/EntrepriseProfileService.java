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

import java.time.LocalDateTime;

@Service
@Transactional
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

    public EntrepriseProfile getProfileEntityByCurrentTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = 1L;

        final Long currentTenantId = tenantId;
        return entrepriseProfileRepository.findByPointDeVenteId(currentTenantId)
                .orElseGet(() -> createDefaultProfile(currentTenantId));
    }

    public EntrepriseProfileDTO getProfileByCurrentTenant() {
        EntrepriseProfile profile = getProfileEntityByCurrentTenant();
        return entrepriseProfileMapper.toDto(profile);
    }

    public EntrepriseProfileDTO updateProfile(EntrepriseProfileDTO dto) {
        EntrepriseProfile profile = getProfileEntityByCurrentTenant();

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
        profile.setNumeroIdentificationStatistique(dto.getNumeroIdentificationStatistique());
        profile.setArticleImposition(dto.getArticleImposition());
        profile.setCompteBancaireRib(dto.getCompteBancaireRib());
        profile.setNomBanque(dto.getNomBanque());
        profile.setPiedPage(dto.getPiedPage());
        if (dto.getDevise() != null && !dto.getDevise().isBlank()) {
            profile.setDevise(dto.getDevise());
        }
        profile.setDateMiseAJour(LocalDateTime.now());

        EntrepriseProfile saved = entrepriseProfileRepository.save(profile);
        return entrepriseProfileMapper.toDto(saved);
    }

    public EntrepriseProfileDTO uploadLogo(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new CommonException("Fichier image vide", HttpStatus.BAD_REQUEST, "LOGO_EMPTY");
        }

        long maxBytes = 5L * 1024 * 1024; // 5 MB
        if (file.getSize() > maxBytes) {
            throw new CommonException("Le logo est trop volumineux (max 5MB)", HttpStatus.PAYLOAD_TOO_LARGE, "LOGO_TOO_LARGE");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/webp"))) {
            throw new CommonException("Format non supporté (JPEG, PNG, WEBP acceptés)", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "LOGO_FORMAT_INVALID");
        }

        byte[] compressedData = imageCompressionService.compressImage(file.getBytes(), contentType);

        EntrepriseProfile profile = getProfileEntityByCurrentTenant();
        profile.setLogoData(compressedData);
        profile.setLogoContentType(contentType);
        profile.setLogoFileName(file.getOriginalFilename());
        profile.setDateMiseAJour(LocalDateTime.now());

        EntrepriseProfile saved = entrepriseProfileRepository.save(profile);
        return entrepriseProfileMapper.toDto(saved);
    }

    public void removeLogo() {
        EntrepriseProfile profile = getProfileEntityByCurrentTenant();
        profile.setLogoData(null);
        profile.setLogoContentType(null);
        profile.setLogoFileName(null);
        profile.setDateMiseAJour(LocalDateTime.now());
        entrepriseProfileRepository.save(profile);
    }

    private EntrepriseProfile createDefaultProfile(Long tenantId) {
        EntrepriseProfile defaultProfile = new EntrepriseProfile();
        defaultProfile.setPointDeVenteId(tenantId);
        defaultProfile.setNomEntreprise("POINT DE VENTE SAAS");
        defaultProfile.setActivite("Commerce de Gros & Détail");
        defaultProfile.setAdresse("Zone Industrielle");
        defaultProfile.setVille("Alger");
        defaultProfile.setCodePostal("16000");
        defaultProfile.setTelephone("021 00 00 00");
        defaultProfile.setEmail("contact@pointvente.dz");
        defaultProfile.setRegistreCommerce("16/00-1234567B16");
        defaultProfile.setNumeroIdentificationFiscale("001612345678901");
        defaultProfile.setNumeroIdentificationStatistique("0016123456789");
        defaultProfile.setArticleImposition("16123456789");
        defaultProfile.setCompteBancaireRib("002 00012 1234567890 12");
        defaultProfile.setNomBanque("Banque Nationale d'Algérie (BNA)");
        defaultProfile.setPiedPage("Garantie légale selon réglementation en vigueur. Merci pour votre confiance.");
        defaultProfile.setDevise("DZD");
        defaultProfile.setDateMiseAJour(LocalDateTime.now());
        return entrepriseProfileRepository.save(defaultProfile);
    }
}
