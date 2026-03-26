package com.example.cerps.common.dto;

import java.util.List;

public record RateHistoryResponse(
        String from,
        String to,
        List<RatePoint> points
) {
}
