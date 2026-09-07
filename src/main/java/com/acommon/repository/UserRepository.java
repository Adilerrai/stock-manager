package com.acommon.repository;

import com.acommon.persistant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByPointDeVente_Id(Long pointDeVenteId);

    int countByPointDeVente_Id(Long pointDeVenteId);

    Optional<User> findFirstByPointDeVente_IdAndRoleNom(Long pointDeVenteId, String roleNom);

    List<User> findByTenantId(Long tenantId);

    int countByTenantId(Long tenantId);

    Optional<User> findFirstByTenantIdAndRoleNom(Long tenantId, String roleNom);

    List<User> findByTenantIdAndPointDeVente_Id(Long tenantId, Long pointDeVenteId);

    boolean existsByRoleNom(String roleNom);

    default List<User> findByPointDeVenteId(Long pointDeVenteId) {
        return findByPointDeVente_Id(pointDeVenteId);
    }

    default int countByPointDeVenteId(Long pointDeVenteId) {
        return countByPointDeVente_Id(pointDeVenteId);
    }

    default Optional<User> findFirstByPointDeVenteIdAndRoleNom(Long pointDeVenteId, String roleNom) {
        return findFirstByPointDeVente_IdAndRoleNom(pointDeVenteId, roleNom);
    }

    default List<User> findByTenantIdAndPointDeVenteId(Long tenantId, Long pointDeVenteId) {
        return findByTenantIdAndPointDeVente_Id(tenantId, pointDeVenteId);
    }
}