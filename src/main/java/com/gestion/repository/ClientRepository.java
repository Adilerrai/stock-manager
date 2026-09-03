package com.gestion.repository;

import com.gestion.persistent.enums.CategorieClient;
import com.gestion.persistent.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {


    List<Client> findByPointDeVenteId(Long pointDeVenteId);

    List<Client> findByActifAndPointDeVenteId(Boolean actif, Long pointDeVenteId);

    List<Client> findByPointDeVenteIdAndCategorie(Long pointDeVenteId, CategorieClient categorie);

    Optional<Client> findByTelephoneAndPointDeVenteId(String telephone, Long pointDeVenteId);

    @Query("SELECT c FROM Client c WHERE c.pointDeVenteId = :pointDeVenteId AND " +
           "(LOWER(c.nomComplet) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Client> searchClients(@Param("search") String search, @Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT c FROM Client c WHERE c.pointDeVenteId = :pointDeVenteId AND c.creditUtilise > c.creditAutorise")
    List<Client> findClientsAvecDepassementCredit(@Param("pointDeVenteId") Long pointDeVenteId);

    // Fallbacks
    @Query("SELECT c FROM Client c WHERE " +
           "(LOWER(c.nomComplet) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Client> searchClients(@Param("search") String search);

    List<Client> findByActif(Boolean actif);
    List<Client> findByCategorie(CategorieClient categorie);
    Optional<Client> findByTelephone(String telephone);
    @Query("SELECT c FROM Client c WHERE c.creditUtilise > c.creditAutorise")
    List<Client> findClientsAvecDepassementCredit();

    List<Client> findByCommercialId(Long commercialId);

    Long countByCommercialId(Long commercialId);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.commercial.id = :commercialId AND c.dateCreation BETWEEN :debut AND :fin")
    Long countNouveauxClientsByCommercial(@Param("commercialId") Long commercialId,
                                          @Param("debut") java.time.LocalDateTime debut,
                                          @Param("fin") java.time.LocalDateTime fin);
}


