package com.vagaradar.web;

import com.vagaradar.service.AlertService;
import com.vagaradar.service.StatsService;
import com.vagaradar.web.dto.Alert;
import com.vagaradar.web.dto.StatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Painel", description = "Estatísticas de candidaturas e alertas de prazos")
public class DashboardController {

    private final StatsService statsService;
    private final AlertService alertService;

    public DashboardController(StatsService statsService, AlertService alertService) {
        this.statsService = statsService;
        this.alertService = alertService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Indicadores: funil, taxa de resposta, conversão em entrevistas, tecnologias mais pedidas")
    public StatsResponse stats() {
        return statsService.compute();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Alertas de prazos, entrevistas, follow-ups e candidaturas paradas")
    public List<Alert> alerts(@RequestParam(required = false) Integer days) {
        return alertService.compute(days);
    }
}
