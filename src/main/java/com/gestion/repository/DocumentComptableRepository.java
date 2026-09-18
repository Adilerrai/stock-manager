package com.gestion.repository;

import com.gestion.persistent.model.DocumentComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentComptableRepository extends JpaRepository<DocumentComptable, Long> {

    List<DocumentComptable> findByPointDeVenteIdOrderByDateUploadDesc(Long pointDeVenteId);

    List<DocumentComptable> findByPointDeVenteIdAndEcritureIdOrderByDateUploadDesc(Long pointDeVenteId, Long ecritureId);

    List<DocumentComptable> findByPointDeVenteIdAndFactureAchatIdOrderByDateUploadDesc(Long pointDeVenteId, Long factureAchatId);

    List<DocumentComptable> findByPointDeVenteIdAndFactureVenteIdOrderByDateUploadDesc(Long pointDeVenteId, Long factureVenteId);

    List<DocumentComptable> findByPointDeVenteIdAndPaiementIdOrderByDateUploadDesc(Long pointDeVenteId, Long paiementId);

    List<DocumentComptable> findByPointDeVenteIdAndTypePieceOrderByDateUploadDesc(Long pointDeVenteId, String typePiece);

    Optional<DocumentComptable> findByIdAndPointDeVenteId(Long id, Long pointDeVenteId);

    long countByPointDeVenteId(Long pointDeVenteId);
}
