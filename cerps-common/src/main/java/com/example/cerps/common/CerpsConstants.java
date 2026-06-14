package com.example.cerps.common;

public final class CerpsConstants {

    private CerpsConstants() {
    }

    // BigDecimal scales
    public static final int CALCULATION_SCALE = 6;
    public static final int DISPLAY_SCALE = 2;

    // Correlation ID
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC = "correlationId";
    public static final String SERVICE_NAME_MDC = "service";

    // Error URI prefix (RFC 7807)
    public static final String ERROR_URI_PREFIX = "https://cerps.example.com/errors/";

    // Exchange rate source
    public static final String EXCHANGE_RATE_SOURCE_AGGREGATED = "AGGREGATED";
    public static final String EXCHANGE_RATE_SOURCE_FRANKFURTER = "FRANKFURTER";

    // Rate validation
    public static final long MAX_RATE_VALUE = 1_000_000L;

    // Percentage multiplier
    public static final int HUNDRED = 100;

    // Trends downsampling limits per period
    public static final int MAX_POINTS_1D = 24;
    public static final int MAX_POINTS_7D = 56;
    public static final int MAX_POINTS_30D = 90;
    public static final int MAX_POINTS_90D = 180;
    public static final int MAX_POINTS_180D = 270;
    public static final int MAX_POINTS_1Y = 365;
    // Multi-year periods cap at one point per day (same as 1Y): the 2-3 year
    // daily series (~730/~1095 points) is downsampled to 365 server-side; the
    // Android chart re-runs LTTB on top.
    public static final int MAX_POINTS_2Y = 365;
    public static final int MAX_POINTS_3Y = 365;
}
