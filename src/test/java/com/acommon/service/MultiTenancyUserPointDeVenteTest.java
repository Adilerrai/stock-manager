package com.acommon.service;

import com.acommon.persistant.dto.*;
import com.acommon.persistant.model.PointDeVente;
import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.PointDeVenteRepository;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import com.gestion.persistent.model.Depot;
import com.gestion.persistent.model.EntrepriseProfile;
import com.gestion.repository.DepotRepository;
import com.gestion.repository.EntrepriseProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiTenancyUserPointDeVenteTest {

    @Mock
    private PointDeVenteRepository pointDeVenteRepository;
    @Mock
    private EntrepriseProfileRepository entrepriseProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private DepotRepository depotRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private EntrepriseManagementService entrepriseManagementService;
    private UserService userService;
    private PointDeVenteService pointDeVenteService;

    @BeforeEach
    void setUp() {
        entrepriseManagementService = new EntrepriseManagementService(
                pointDeVenteRepository,
                entrepriseProfileRepository,
                userRepository,
                roleRepository,
                depotRepository,
                passwordEncoder
        );

        userService = new UserService(
                userRepository,
                roleRepository,
                pointDeVenteRepository,
                passwordEncoder
        );

        pointDeVenteService = new PointDeVenteService(
                pointDeVenteRepository,
                userRepository
        );

        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void testCreerEntreprise_CreatesUniqueTenantAndSingleAdminWithRelationship() {
        // Given
        EntrepriseRegistrationRequest request = new EntrepriseRegistrationRequest();
        request.setNomEntreprise("Tech Solutions");
        request.setAdminEmail("admin@tech.com");
        request.setAdminUsername("techadmin");
        request.setAdminPassword("Password123!");
        request.setAdminNomComplet("Admin Tech");

        when(pointDeVenteRepository.existsByNomPointDeVente("Tech Solutions")).thenReturn(false);
        when(userRepository.existsByEmail("admin@tech.com")).thenReturn(false);
        when(userRepository.existsByUsername("techadmin")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        PointDeVente savedPdv = new PointDeVente();
        savedPdv.setId(100L);
        savedPdv.setTenantId(11L);
        savedPdv.setNomPointDeVente("Tech Solutions");
        when(pointDeVenteRepository.save(any(PointDeVente.class))).thenReturn(savedPdv);

        EntrepriseProfile savedProfile = new EntrepriseProfile();
        savedProfile.setId(200L);
        savedProfile.setNomEntreprise("Tech Solutions");
        when(entrepriseProfileRepository.save(any(EntrepriseProfile.class))).thenReturn(savedProfile);

        when(depotRepository.save(any(Depot.class))).thenReturn(new Depot());

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setNom("ROLE_ADMIN");
        when(roleRepository.findByNom("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(500L);
            return u;
        });

        // When
        EntrepriseResponse response = entrepriseManagementService.creerEntreprise(request);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.getTenantId());
        assertEquals("Tech Solutions", response.getNomEntreprise());
        assertEquals(500L, response.getAdminUserId());
        assertEquals("admin@tech.com", response.getAdminEmail());

        // Verify that PointDeVente is saved
        verify(pointDeVenteRepository, atLeastOnce()).save(argThat(pdv ->
                pdv.getNomPointDeVente().equals("Tech Solutions")));

        // Verify that the created user has role ROLE_ADMIN and is attached to the PointDeVente
        verify(userRepository).save(argThat(u ->
                u.getRole().getNom().equals("ROLE_ADMIN") &&
                u.getPointDeVente() != null &&
                u.getPointDeVente().getId().equals(100L)));
    }

    @Test
    void testCreateUserByCompanyAdmin_AutomaticallyInheritsConnectedAdminTenant() {
        // Given : Connected Admin of Tenant 11
        Role adminRole = new Role();
        adminRole.setNom("ROLE_ADMIN");

        PointDeVente adminPdv = new PointDeVente();
        adminPdv.setId(100L);
        adminPdv.setTenantId(11L);
        adminPdv.setNomPointDeVente("Tech Solutions - Siège");

        User connectedAdmin = new User();
        connectedAdmin.setId(500L);
        connectedAdmin.setEmail("admin@tech.com");
        connectedAdmin.setUsername("techadmin");
        connectedAdmin.setTenantId(11L);
        connectedAdmin.setPointDeVente(adminPdv);
        connectedAdmin.setRole(adminRole);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                connectedAdmin, null, connectedAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail("vendeur@tech.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("vendeur1")).thenReturn(Optional.empty());

        // When the admin creates a new seller without specifying pointDeVenteId
        UserCreationRequest request = new UserCreationRequest();
        request.setEmail("vendeur@tech.com");
        request.setUsername("vendeur1");
        request.setPassword("Vend2026!");
        request.setNomComplet("Vendeur Un");
        request.setRole("VENDEUR");

        Role vendeurRole = new Role();
        vendeurRole.setId(2L);
        vendeurRole.setNom("ROLE_VENDEUR");
        when(roleRepository.findByNom("ROLE_VENDEUR")).thenReturn(Optional.of(vendeurRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(501L);
            return u;
        });

        UserResponse userResponse = userService.createUser(request);

        // Then
        assertNotNull(userResponse);
        assertEquals(11L, userResponse.getTenantId()); // Inherited from connected admin
        assertEquals(100L, userResponse.getPointDeVenteId()); // Attached to admin's PointDeVente
        assertEquals("Tech Solutions - Siège", userResponse.getNomPointDeVente());
        assertEquals("ROLE_VENDEUR", userResponse.getRole());

        verify(userRepository).save(argThat(u ->
                u.getTenantId().equals(11L) &&
                u.getPointDeVente() != null &&
                u.getPointDeVente().getId().equals(100L)));
    }

    @Test
    void testCreateUserByCompanyAdmin_RejectsPointDeVenteFromDifferentTenant() {
        // Given : Connected Admin of Tenant 11
        Role adminRole = new Role();
        adminRole.setNom("ROLE_ADMIN");

        User connectedAdmin = new User();
        connectedAdmin.setId(500L);
        connectedAdmin.setEmail("admin@tech.com");
        connectedAdmin.setTenantId(11L);
        connectedAdmin.setRole(adminRole);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                connectedAdmin, null, connectedAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Another company's PointDeVente belonging to tenant 99
        PointDeVente foreignPdv = new PointDeVente();
        foreignPdv.setId(999L);
        foreignPdv.setTenantId(99L);
        when(pointDeVenteRepository.findById(999L)).thenReturn(Optional.of(foreignPdv));

        // When : Admin attempts to assign user to foreign point of sale
        UserCreationRequest request = new UserCreationRequest();
        request.setEmail("pirate@tech.com");
        request.setUsername("pirate");
        request.setPassword("SecPass123!");
        request.setNomComplet("Pirate User");
        request.setRole("VENDEUR");
        request.setPointDeVenteId(999L); // Malicious / invalid assignment

        // Then : Must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUsers_StrictTenantIsolation() {
        // Given : Connected Admin of Tenant 11
        Role adminRole = new Role();
        adminRole.setNom("ROLE_ADMIN");

        User connectedAdmin = new User();
        connectedAdmin.setId(500L);
        connectedAdmin.setEmail("admin@tech.com");
        connectedAdmin.setTenantId(11L);
        connectedAdmin.setRole(adminRole);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                connectedAdmin, null, connectedAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User userTenant11 = new User();
        userTenant11.setId(501L);
        userTenant11.setEmail("user1@tech.com");
        userTenant11.setTenantId(11L);
        userTenant11.setRole(adminRole);

        when(userRepository.findByTenantId(11L)).thenReturn(List.of(userTenant11));

        // When
        List<UserResponse> users = userService.getUsers(null);

        // Then
        assertEquals(1, users.size());
        assertEquals(501L, users.get(0).getId());
        assertEquals(11L, users.get(0).getTenantId());
        verify(userRepository).findByTenantId(11L);
        verify(userRepository, never()).findAll();
    }

    @Test
    void testSuperAdmin_CanCreateUserForAnyAgence() {
        // Given : Connected SuperAdmin
        Role superAdminRole = new Role();
        superAdminRole.setNom("ROLE_SUPERADMIN");

        User superAdmin = new User();
        superAdmin.setId(1L);
        superAdmin.setEmail("superadmin@erp.com");
        superAdmin.setUsername("superadmin");
        superAdmin.setRole(superAdminRole);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                superAdmin, null, superAdmin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Any agency (PointDeVente 250)
        PointDeVente agenceFes = new PointDeVente();
        agenceFes.setId(250L);
        agenceFes.setTenantId(25L);
        agenceFes.setNomPointDeVente("Agence Fès");
        when(pointDeVenteRepository.findById(250L)).thenReturn(Optional.of(agenceFes));

        when(userRepository.findByEmail("commercial.fes@erp.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("comm_fes")).thenReturn(Optional.empty());

        Role commRole = new Role();
        commRole.setId(5L);
        commRole.setNom("ROLE_COMMERCIAL");
        when(roleRepository.findByNom("ROLE_COMMERCIAL")).thenReturn(Optional.of(commRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(700L);
            return u;
        });

        // When
        UserCreationRequest request = new UserCreationRequest();
        request.setEmail("commercial.fes@erp.com");
        request.setUsername("comm_fes");
        request.setPassword("Secret2026!");
        request.setNomComplet("Commercial Fès");
        request.setRole("ROLE_COMMERCIAL");
        request.setPointDeVenteId(250L);

        UserResponse response = userService.createUser(request);

        // Then : SuperAdmin successfully created user for Agence Fès
        assertNotNull(response);
        assertEquals(250L, response.getPointDeVenteId());
        assertEquals("Agence Fès", response.getNomPointDeVente());
        assertEquals(25L, response.getTenantId());
        verify(userRepository).save(argThat(u ->
                u.getPointDeVente() != null && u.getPointDeVente().getId().equals(250L)));
    }

    @Test
    void testPointDeVenteAdmin_CannotCreateUserForAnotherPointDeVente() {
        // Given : Connected Admin of Agence Marrakech (PDV 100)
        Role adminRole = new Role();
        adminRole.setNom("ROLE_ADMIN");

        PointDeVente pdvMarrakech = new PointDeVente();
        pdvMarrakech.setId(100L);
        pdvMarrakech.setTenantId(10L);
        pdvMarrakech.setNomPointDeVente("Agence Marrakech");

        User adminMarrakech = new User();
        adminMarrakech.setId(50L);
        adminMarrakech.setEmail("admin.marrakech@erp.com");
        adminMarrakech.setUsername("admin_kech");
        adminMarrakech.setPointDeVente(pdvMarrakech);
        adminMarrakech.setTenantId(10L);
        adminMarrakech.setRole(adminRole);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminMarrakech, null, adminMarrakech.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When : Admin Marrakech attempts to create a user for Agence Tanger (PDV 200)
        UserCreationRequest request = new UserCreationRequest();
        request.setEmail("vendeur.tanger@erp.com");
        request.setUsername("vend_tanger");
        request.setPassword("Secret2026!");
        request.setNomComplet("Vendeur Tanger");
        request.setRole("ROLE_VENDEUR");
        request.setPointDeVenteId(200L); // Different point de vente

        // Then : Must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testPointDeVenteAdmin_CannotAccessUserFromAnotherAgencyOrSuperAdmin() {
        // Given : Connected Admin of Agence Marrakech (PDV 100)
        Role adminRole = new Role();
        adminRole.setNom("ROLE_ADMIN");

        PointDeVente pdvMarrakech = new PointDeVente();
        pdvMarrakech.setId(100L);
        pdvMarrakech.setTenantId(10L);
        pdvMarrakech.setNomPointDeVente("Agence Marrakech");

        User adminMarrakech = new User();
        adminMarrakech.setId(50L);
        adminMarrakech.setEmail("admin.marrakech@erp.com");
        adminMarrakech.setUsername("admin_kech");
        adminMarrakech.setPointDeVente(pdvMarrakech);
        adminMarrakech.setTenantId(10L);
        adminMarrakech.setRole(adminRole);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminMarrakech, null, adminMarrakech.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 1. Target user from Marrakech (same agency)
        User userMarrakech = new User();
        userMarrakech.setId(101L);
        userMarrakech.setPointDeVente(pdvMarrakech);
        userMarrakech.setTenantId(10L);
        userMarrakech.setRole(adminRole);
        when(userRepository.findById(101L)).thenReturn(Optional.of(userMarrakech));

        // 2. Target user from Agence Rabat (different agency PDV 200)
        PointDeVente pdvRabat = new PointDeVente();
        pdvRabat.setId(200L);
        pdvRabat.setTenantId(20L);

        User userRabat = new User();
        userRabat.setId(201L);
        userRabat.setPointDeVente(pdvRabat);
        userRabat.setTenantId(20L);
        userRabat.setRole(adminRole);
        when(userRepository.findById(201L)).thenReturn(Optional.of(userRabat));

        // 3. Target user is SuperAdmin
        Role superAdminRole = new Role();
        superAdminRole.setNom("ROLE_SUPERADMIN");
        User superAdminUser = new User();
        superAdminUser.setId(1L);
        superAdminUser.setRole(superAdminRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdminUser));

        // When & Then
        // Marrakech admin CAN view Marrakech user
        assertNotNull(userService.getUserById(101L));

        // Marrakech admin CANNOT view Rabat user
        assertThrows(AccessDeniedException.class, () -> userService.getUserById(201L));

        // Marrakech admin CANNOT view SuperAdmin
        assertThrows(AccessDeniedException.class, () -> userService.getUserById(1L));
    }
}
