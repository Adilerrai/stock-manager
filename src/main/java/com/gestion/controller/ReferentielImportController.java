package com.gestion.controller;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.enums.CleRepartition;
import com.gestion.persistent.enums.Incoterm;
import com.gestion.persistent.enums.ModeTransport;
import com.gestion.persistent.enums.TypeCharge;
import com.gestion.persistent.model.ConditionPaiement;
import com.gestion.persistent.model.LieuLogistique;
import com.gestion.repository.ConditionPaiementRepository;
import com.gestion.repository.LieuLogistiqueRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/referentiel-import")
public class ReferentielImportController {

    private final LieuLogistiqueRepository lieuLogistiqueRepository;
    private final ConditionPaiementRepository conditionPaiementRepository;

    public ReferentielImportController(LieuLogistiqueRepository lieuLogistiqueRepository,
                                       ConditionPaiementRepository conditionPaiementRepository) {
        this.lieuLogistiqueRepository = lieuLogistiqueRepository;
        this.conditionPaiementRepository = conditionPaiementRepository;
    }

    private Long getTenantId() {
        Long tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : 1L;
    }

    @GetMapping("/incoterms")
    public ResponseEntity<List<Map<String, Object>>> getIncoterms() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Incoterm inc : Incoterm.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", inc.name());
            map.put("libelle", inc.getLibelle());
            map.put("inclutTransportOrigine", inc.isInclutTransportOrigine());
            map.put("inclutFret", inc.isInclutFret());
            map.put("inclutAssurance", inc.isInclutAssurance());
            map.put("inclutDouaneImport", inc.isInclutDouaneImport());
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/types-charges")
    public ResponseEntity<List<Map<String, Object>>> getTypesCharges() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TypeCharge tc : TypeCharge.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", tc.name());
            map.put("libelle", tc.getLibelle());
            map.put("section", tc.getSection());
            map.put("incorporableDefaut", tc.isIncorporableDefaut());
            map.put("cleDefaut", tc.getCleDefaut().name());
            map.put("nationalAutorise", tc.isNationalAutorise());
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/cles-repartition")
    public ResponseEntity<List<Map<String, Object>>> getClesRepartition() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CleRepartition cle : CleRepartition.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", cle.name());
            map.put("libelle", cle.getLibelle());
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/modes-transport")
    public ResponseEntity<List<String>> getModesTransport() {
        List<String> list = new ArrayList<>();
        for (ModeTransport m : ModeTransport.values()) {
            list.add(m.name());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/lieux")
    public ResponseEntity<List<LieuLogistique>> getLieux() {
        return ResponseEntity.ok(lieuLogistiqueRepository.findActifsForTenant(getTenantId()));
    }

    @GetMapping("/conditions-paiement")
    public ResponseEntity<List<ConditionPaiement>> getConditionsPaiement() {
        return ResponseEntity.ok(conditionPaiementRepository.findActifsForTenant(getTenantId()));
    }
}
