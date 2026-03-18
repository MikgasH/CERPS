package org.example.analyticsservice.service.cache;

import com.example.cerps.common.dto.TrendsResponse;

import java.time.Instant;

public record CachedTrends(TrendsResponse response, Instant cachedAt) {
}
