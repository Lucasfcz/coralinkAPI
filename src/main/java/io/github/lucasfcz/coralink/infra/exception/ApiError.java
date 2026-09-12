package io.github.lucasfcz.coralink.infra.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message
) {}
