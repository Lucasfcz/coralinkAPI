package io.github.lucasfcz.coralink.infra.exception;

public class BadResponseException extends RuntimeException {
    public BadResponseException(String message) {
        super(message);
    }
}
