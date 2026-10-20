package com.gestion.repository;

import com.gestion.persistent.enums.StatutContact;
import com.gestion.persistent.model.CrmContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrmContactRepository extends JpaRepository<CrmContact, Long> {

    List<CrmContact> findByPointDeVenteIdOrderByNomAsc(Long pointDeVenteId);

    List<CrmContact> findByPointDeVenteIdAndEntrepriseId(Long pointDeVenteId, Long entrepriseId);

    List<CrmContact> findByPointDeVenteIdAndCommercialResponsableId(Long pointDeVenteId, Long commercialId);

    List<CrmContact> findByPointDeVenteIdAndStatut(Long pointDeVenteId, StatutContact statut);

    Optional<CrmContact> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    @Query("SELECT c FROM CrmContact c WHERE c.pointDeVenteId = :pointDeVenteId AND " +
           "(LOWER(c.nomComplet) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.mobile) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<CrmContact> searchContacts(@Param("query") String query, @Param("pointDeVenteId") Long pointDeVenteId);

    // Détection de doublons (par email ou téléphone dans le même tenant)
    @Query("SELECT c FROM CrmContact c WHERE c.pointDeVenteId = :pointDeVenteId AND " +
           "((:email IS NOT NULL AND :email <> '' AND LOWER(c.email) = LOWER(:email)) " +
           "OR (:telephone IS NOT NULL AND :telephone <> '' AND (c.telephone = :telephone OR c.mobile = :telephone)))")
    List<CrmContact> findDoublonsPotentiels(@Param("email") String email, @Param("telephone") String telephone, @Param("pointDeVenteId") Long pointDeVenteId);

    long countByPointDeVenteId(Long pointDeVenteId);

    long countByPointDeVenteIdAndCommercialResponsableId(Long pointDeVenteId, Long commercialId);

    long countByPointDeVenteIdAndEntrepriseId(Long pointDeVenteId, Long entrepriseId);
}
