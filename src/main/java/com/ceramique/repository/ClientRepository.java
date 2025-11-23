package com.ceramique.repository;

import com.ceramique.persistent.enums.CategorieClient;
import com.ceramique.persistent.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByPointDeVenteId(Long pointDeVenteId);

    List<Client> findByActif(Boolean actif);

    List<Client> findByCategorie(CategorieClient categorie);

    Optional<Client> findByTelephone(String telephone);

    Optional<Client> findByEmailAndPointDeVenteId(String email, Long pointDeVenteId);

    @Query("SELECT c FROM Client c WHERE c.pointDeVente.id = :pointDeVenteId " +
           "AND (LOWER(c.nomComplet) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Client> searchClients(@Param("pointDeVenteId") Long pointDeVenteId, @Param("search") String search);

    @Query("SELECT c FROM Client c WHERE c.creditUtilise > c.creditAutorise")
    List<Client> findClientsAvecDepassementCredit();
}

