package org.example.analyticsservice.controller;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.service.TrendsService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Analytics", description = "Currency trends analytics")
public class AnalyticsController {

    private final TrendsService trendsService;

    @GetMapping("/trends")
    @Operation(summary = "Calculate currency trend over period")
    @ApiResponse(responseCode = "200", description = "Trend calculated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid parameters")
    @ApiResponse(responseCode = "404", description = "Insufficient data")
    public ResponseEntity<TrendsResponse> getTrends(
            @Valid @ModelAttribute final TrendsRequest request
    ) {
        log.info("GET /api/v1/analytics/trends - from={}, to={}, period={}",
                request.from(), request.to(), request.period());

        final TrendsResponse response = trendsService.calculateTrends(request);

        return ResponseEntity.ok(response);
    }
}
