package com.gestion.repository;

import com.gestion.persistent.enums.StatutNumeroReserve;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.persistent.model.NumeroReserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NumeroReserveRepository extends JpaRepository<NumeroReserve, Long> {

    List<NumeroReserve> findByPointDeVenteIdAndTypeDocumentAndStatutOrderByValeurSequenceAsc(
            Long pointDeVenteId, TypeDocumentCodification typeDocument, StatutNumeroReserve statut);

    Optional<NumeroReserve> findFirstByPointDeVenteIdAndTypeDocumentAndStatutOrderByValeurSequenceAsc(
            Long pointDeVenteId, TypeDocumentCodification typeDocument, StatutNumeroReserve statut);

    Optional<NumeroReserve> findByPointDeVenteIdAndTypeDocumentAndNumeroAndStatut(
            Long pointDeVenteId, TypeDocumentCodification typeDocument, String numero, StatutNumeroReserve statut);

    Optional<NumeroReserve> findByPointDeVenteIdAndNumero(Long pointDeVenteId, String numero);

    boolean existsByPointDeVenteIdAndTypeDocumentAndNumeroAndStatut(
            Long pointDeVenteId, TypeDocumentCodification typeDocument, String numero, StatutNumeroReserve statut);
}
