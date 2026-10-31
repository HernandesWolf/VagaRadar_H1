package com.vagaradar.web.dto;

import com.vagaradar.analysis.GapAnalyzer.GapReport;
import com.vagaradar.domain.ApplicationStatus;
import com.vagaradar.domain.WorkMode;

import java.util.List;

public final class GapDtos {

    private GapDtos() {
    }

    public record JobGapResponse(Long jobId, String title, String company, GapReport report) {
    }

    public record RankingItem(Long jobId, String title, String company, WorkMode workMode, int score,
                              String verdict, List<String> missingMandatory,
                              ApplicationStatus applicationStatus) {
    }
}
