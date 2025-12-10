package com.example.cerps.common.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.util.Currency;

public final class ResponseConverter {

    private ResponseConverter() {
    }

    public static class CurrencyDeserializer extends JsonDeserializer<Currency> {
        @Override
        public Currency deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            return Currency.getInstance(p.getValueAsString());
        }
    }

    public static class TimestampToInstantDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            return Instant.ofEpochSecond(p.getValueAsLong());
        }
    }
}
