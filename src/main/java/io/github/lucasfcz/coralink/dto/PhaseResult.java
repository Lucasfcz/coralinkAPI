package io.github.lucasfcz.coralink.dto;

public record PhaseResult(
        int relevantFound,
        int irrelevantFound
) {
    public static PhaseResult empty() { return new PhaseResult(0, 0); }
}