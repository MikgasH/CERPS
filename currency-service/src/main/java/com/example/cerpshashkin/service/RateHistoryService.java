package com.example.cerpshashkin.service;

import com.example.cerps.common.dto.RateHistoryResponse;
import com.example.cerps.common.dto.RatePoint;
import com.example.cerpshashkin.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateHistoryService {

    private final ExchangeRateRepository exchangeRateRepository;

    public RateHistoryResponse getRateHistory(
            final String from, final String to,
            final Instant startDate, final Instant endDate) {

        final String fromCode = from.trim().toUpperCase();
        final String toCode = to.trim().toUpperCase();

        log.info("Fetching rate history for {} -> {} ({} to {})", fromCode, toCode, startDate, endDate);

        final List<Object[]> results = exchangeRateRepository
                .findRatesWithCrossSupport(fromCode, toCode, startDate, endDate);

        final List<RatePoint> points = results.stream()
                .map(this::toRatePoint)
                .toList();

        log.info("Rate history for {} -> {}: {} points", fromCode, toCode, points.size());
        return new RateHistoryResponse(fromCode, toCode, points);
    }

    private RatePoint toRatePoint(final Object[] row) {
        final BigDecimal rate = (BigDecimal) row[3];
        final Object timestampObj = row[5];
        final Instant timestamp;
        if (timestampObj instanceof java.sql.Timestamp ts) {
            timestamp = ts.toInstant();
        } else if (timestampObj instanceof java.time.OffsetDateTime odt) {
            timestamp = odt.toInstant();
        } else {
            timestamp = (Instant) timestampObj;
        }
        return new RatePoint(timestamp, rate);
    }
}
