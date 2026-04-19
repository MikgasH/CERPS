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
        final String from = request.from().toUpperCase();
        final String to = request.to().toUpperCase();
        final String period = request.period().toUpperCase();

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
