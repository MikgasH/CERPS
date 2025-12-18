package com.example.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void filter_WithExistingCorrelationId_ShouldPreserveIt() {
        String existingCorrelationId = "existing-correlation-123";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(any());
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String correlationIdInRequest = capturedExchange.getRequest()
                .getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(correlationIdInRequest).isEqualTo(existingCorrelationId);
    }

    @Test
    void filter_WithoutCorrelationId_ShouldGenerateNew() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(any());
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String generatedCorrelationId = capturedExchange.getRequest()
                .getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(generatedCorrelationId).isNotNull();
        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(generatedCorrelationId).matches(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );
    }

    @Test
    void filter_ShouldAddCorrelationIdToResponse() {
        String correlationId = "test-correlation-456";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        String responseCorrelationId = exchange.getResponse()
                .getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(responseCorrelationId).isEqualTo(correlationId);
    }

    @Test
    void filter_WithBlankCorrelationId_ShouldGenerateNew() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/currencies")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String generatedCorrelationId = capturedExchange.getRequest()
                .getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(generatedCorrelationId).doesNotContainOnlyWhitespaces();
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedence() {
        int order = filter.getOrder();
        assertThat(order).isEqualTo(Integer.MIN_VALUE);
    }
}
