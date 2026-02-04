package org.example.analyticsservice.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final TrendsService trendsService;

    public TrendsResponse calculateTrends(final TrendsRequest request) {
        return trendsService.calculateTrends(request);
    }
}
