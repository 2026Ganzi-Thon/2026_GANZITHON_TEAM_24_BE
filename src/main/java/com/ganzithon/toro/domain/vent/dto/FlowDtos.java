package com.ganzithon.toro.domain.vent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 세션에는 사용자가 확정한 값만 둔다. 원문/AI 초안용 DTO는 로그에 출력하지 않는다. */
public final class FlowDtos {
    private FlowDtos() { }

    public enum Stage { VENTING, CALMING, REFLECTION, SUMMARY, PLAN, RESULT, CLOSED }

    public record EmotionConfirmation(@Size(max = 40) String emotion) {
        public EmotionConfirmation { emotion = clean(emotion); }
        @Override public String toString() { return "EmotionConfirmation[redacted]"; }
    }

    public record CalmCompletion(@NotNull Boolean skipped) { }

    public record NeedsInput(@NotBlank @Size(max = 1000) String situation,
                             @Size(max = 500) String desiredResponse) {
        public NeedsInput { situation = clean(situation); desiredResponse = clean(desiredResponse); }
        @Override public String toString() { return "NeedsInput[redacted]"; }
    }

    public record Reflection(@NotBlank @Size(max = 1000) String situation,
                             @Size(max = 40) String emotion,
                             @Size(max = 300) String need,
                             @Size(max = 500) String desiredResponse) {
        public Reflection {
            situation = clean(situation); emotion = clean(emotion);
            need = clean(need); desiredResponse = clean(desiredResponse);
        }
        @Override public String toString() { return "Reflection[redacted]"; }
    }

    public record SummaryConfirmation(@NotBlank @Size(max = 1200) String summary) {
        public SummaryConfirmation { summary = clean(summary); }
        @Override public String toString() { return "SummaryConfirmation[redacted]"; }
    }

    public record Plan(@NotBlank @Size(max = 300) String ifSituation,
                       @NotBlank @Size(max = 500) String thenAction) {
        public Plan { ifSituation = clean(ifSituation); thenAction = clean(thenAction); }
        @Override public String toString() { return "Plan[redacted]"; }
    }

    public record Empathy(String empathy) {
        @Override public String toString() { return "Empathy[redacted]"; }
    }
    public record NeedsSuggestion(List<String> needs, String desiredResponse, boolean requiresConfirmation) {
        public NeedsSuggestion { needs = List.copyOf(needs); }
        @Override public String toString() { return "NeedsSuggestion[redacted]"; }
    }
    public record SummarySuggestion(String summary, boolean requiresConfirmation) {
        @Override public String toString() { return "SummarySuggestion[redacted]"; }
    }
    public record PlanSuggestion(String ifSituation, String thenAction, boolean requiresConfirmation) {
        @Override public String toString() { return "PlanSuggestion[redacted]"; }
    }
    public record SessionView(UUID sessionId, Stage stage, Instant expiresAt, String emotion,
                              Boolean calmingSkipped, Reflection reflection, String summary, Plan plan) {
        @Override public String toString() { return "SessionView[redacted]"; }
    }
    public record CalmingGuide(int suggestedSeconds, List<String> steps, boolean canSkip) { }
    public record DeletionReceipt(boolean serverStateCleared, boolean requestsSettled,
                                   boolean clientCleanupRequired, boolean externalDeletionVerified) { }

    private static String clean(String text) { return text == null ? "" : text.strip(); }
}
