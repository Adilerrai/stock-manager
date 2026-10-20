package com.gestion.controller;

import com.gestion.mapper.CrmMapper;
import com.gestion.persistent.dto.CrmActiviteDTO;
import com.gestion.persistent.dto.CrmDashboardKPIsDTO;
import com.gestion.service.CrmActiviteService;
import com.gestion.service.CrmDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm/dashboard")
@CrossOrigin(origins = "*")
public class CrmDashboardController {

    private final CrmDashboardService dashboardService;
    private final CrmActiviteService activiteService;
    private final CrmMapper crmMapper;

    public CrmDashboardController(CrmDashboardService dashboardService,
                                  CrmActiviteService activiteService,
                                  CrmMapper crmMapper) {
        this.dashboardService = dashboardService;
        this.activiteService = activiteService;
        this.crmMapper = crmMapper;
    }

    @GetMapping
    public ResponseEntity<CrmDashboardKPIsDTO> getKPIs() {
        return ResponseEntity.ok(dashboardService.getDashboardKPIs());
    }

    @GetMapping("/activites")
    public ResponseEntity<List<CrmActiviteDTO>> getTimelineGlobale() {
        List<CrmActiviteDTO> dtos = activiteService.getTimelineGlobale().stream()
                .map(crmMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
