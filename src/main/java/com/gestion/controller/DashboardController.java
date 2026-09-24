package com.gestion.controller;

import com.gestion.persistent.dto.DashboardDTO;
import com.gestion.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VOIR') or hasAuthority('RAPPORT_VOIR')")
    public ResponseEntity<DashboardDTO> getDashboard() {
        DashboardDTO metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }
}
