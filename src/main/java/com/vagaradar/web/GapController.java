package com.vagaradar.web;

import com.vagaradar.analysis.GapAnalyzer.GapPriority;
import com.vagaradar.service.GapService;
import com.vagaradar.web.dto.GapDtos.RankingItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gap-analysis")
@Tag(name = "Análise de lacunas", description = "Aderência do seu perfil às vagas e prioridades de estudo")
public class GapController {

    private final GapService gapService;

    public GapController(GapService gapService) {
        this.gapService = gapService;
    }

    @GetMapping("/ranking")
    @Operation(summary = "Vagas ordenadas da mais para a menos aderente ao seu perfil")
    public List<RankingItem> ranking() {
        return gapService.ranking();
    }

    @GetMapping("/study-priorities")
    @Operation(summary = "O que estudar primeiro: lacunas mais frequentes nas vagas em aberto")
    public List<GapPriority> studyPriorities() {
        return gapService.studyPriorities();
    }
}
