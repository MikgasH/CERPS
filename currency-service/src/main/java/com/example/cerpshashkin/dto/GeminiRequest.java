package com.example.cerpshashkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeminiRequest(
        @JsonProperty("system_instruction") SystemInstruction systemInstruction,
        List<Content> contents
) {
    public record SystemInstruction(List<Part> parts) { }

    public record Content(List<Part> parts) { }

    public record Part(String text) { }

    public static GeminiRequest of(final String systemPrompt, final String userPrompt) {
        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content(List.of(new Part(userPrompt))))
        );
    }
}
