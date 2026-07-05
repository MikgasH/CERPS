package org.example.analyticsservice.service;

import com.example.cerps.common.dto.TrendsRequest;
import com.example.cerps.common.dto.TrendsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.analyticsservice.service.cache.TrendsCache;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final TrendsService trendsService;
    private final TrendsCache trendsCache;

    public TrendsResponse calculateTrends(final TrendsRequest request) {
        // Same normalization as TrendsService, so padded input maps to the
        // same cache key as its canonical form instead of a duplicate entry.
        final String from = request.from().trim().toUpperCase();
        final String to = request.to().trim().toUpperCase();
        final String period = request.period().trim().toUpperCase();

        return trendsCache.get(from, to, period)
                .orElseGet(() -> {
                    final TrendsService.TrendsResult result = trendsService.calculateTrends(request);
                    if (result.fromFallback()) {
                        trendsCache.putShort(from, to, period, result.response());
                    } else {
                        trendsCache.put(from, to, period, result.response());
                    }
                    return result.response();
                });
    }

    public void invalidateCache() {
        trendsCache.invalidateAll();
    }
}
