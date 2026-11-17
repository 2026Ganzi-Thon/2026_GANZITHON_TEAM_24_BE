package com.ganzithon.toro.domain.stt.dto;

public record Transcription(String text, double durationSeconds) {
    @Override public String toString() { return "Transcription[redacted]"; }
}
