package com.gestion.controller;

import com.gestion.persistent.dto.EntrepriseProfileDTO;
import com.gestion.persistent.model.EntrepriseProfile;
import com.gestion.service.EntrepriseProfileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/entreprise")
@CrossOrigin(origins = "*")
public class EntrepriseProfileController {

    private final EntrepriseProfileService entrepriseProfileService;

    public EntrepriseProfileController(EntrepriseProfileService entrepriseProfileService) {
        this.entrepriseProfileService = entrepriseProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<EntrepriseProfileDTO> getProfile() {
        return ResponseEntity.ok(entrepriseProfileService.getProfileByCurrentTenant());
    }

    @PutMapping("/profile")
    public ResponseEntity<EntrepriseProfileDTO> updateProfile(@RequestBody EntrepriseProfileDTO dto) {
        return ResponseEntity.ok(entrepriseProfileService.updateProfile(dto));
    }

    @PostMapping("/logo")
    public ResponseEntity<EntrepriseProfileDTO> uploadLogo(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(entrepriseProfileService.uploadLogo(file));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<Void> removeLogo() {
        entrepriseProfileService.removeLogo();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        EntrepriseProfile profile = entrepriseProfileService.getProfileEntityByCurrentTenant();
        if (!profile.hasLogo()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.IMAGE_PNG;
        if (profile.getLogoContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(profile.getLogoContentType());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + 
                        (profile.getLogoFileName() != null ? profile.getLogoFileName() : "logo.png") + "\"")
                .body(profile.getLogoData());
    }
}
