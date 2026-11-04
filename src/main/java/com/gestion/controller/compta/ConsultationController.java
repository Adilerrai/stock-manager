package com.gestion.controller.compta;

import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceAgeeDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceAuxiliaireDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.BalanceDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.GrandLivreCompteDTO;
import com.gestion.persistent.dto.compta.ConsultationDtos.PageJournalDTO;
import com.gestion.service.compta.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(value = "/api/v1/compta/exercices/{id}", produces = "application/json")
@CrossOrigin(origins = "*")
@Tag(name = "Comptabilité : consultations")
public class ConsultationController {

    private final ConsultationService service;

    public ConsultationController(ConsultationService service) {
        this.service = service;
    }

    @GetMapping("/journal")
    @Operation(operationId = "comptaConsultationJournal", summary = "Livre-journal, par page")
    public ResponseEntity<PageJournalDTO> journal(@PathVariable Long id, @RequestParam(required = false) Long journalId,
                                                  @RequestParam(required = false) Long periodeId, @RequestParam(required = false) String curseur,
                                                  @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.journal(id, journalId, periodeId, curseur, taille));
    }

    @GetMapping("/grand-livre")
    @Operation(operationId = "comptaConsultationGrandLivre", summary = "Grand livre d'un compte ou d'un tiers, par page")
    public ResponseEntity<GrandLivreCompteDTO> grandLivre(@PathVariable Long id, @RequestParam String compte, @RequestParam(required = false) Long tiersId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                                                    @RequestParam(defaultValue = "false") boolean masquerLettrees,
                                                    @RequestParam(required = false) String curseur, @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.grandLivre(id, compte, tiersId, du, au, masquerLettrees, curseur, taille));
    }

    @GetMapping("/balance")
    @Operation(operationId = "comptaConsultationBalance", summary = "Balance à six colonnes")
    public ResponseEntity<BalanceDTO> balance(@PathVariable Long id,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete,
                                              @RequestParam(required = false) Integer classe, @RequestParam(defaultValue = "false") boolean expurgee,
                                              @RequestParam(required = false) String curseur, @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.balance(id, arrete, classe, expurgee, curseur, taille));
    }

    @GetMapping("/balance-auxiliaire")
    @Operation(operationId = "comptaConsultationBalanceAuxiliaire", summary = "Balance par tiers d'un compte collectif")
    public ResponseEntity<BalanceAuxiliaireDTO> balanceAuxiliaire(@PathVariable Long id, @RequestParam String compte, @RequestParam(required = false) String q,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete,
                                                                  @RequestParam(required = false) String curseur, @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.balanceAuxiliaire(id, compte, q, arrete, curseur, taille));
    }

    @GetMapping("/balance-agee")
    @Operation(operationId = "comptaConsultationBalanceAgee", summary = "Balance âgée des lignes non lettrées")
    public ResponseEntity<BalanceAgeeDTO> balanceAgee(@PathVariable Long id, @RequestParam String compte,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrete,
                                                      @RequestParam(required = false) String curseur, @RequestParam(required = false) Integer taille) {
        return ResponseEntity.ok(service.balanceAgee(id, compte, arrete, curseur, taille));
    }
}
