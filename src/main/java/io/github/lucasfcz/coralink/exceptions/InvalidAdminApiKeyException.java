package io.github.lucasfcz.coralink.exceptions;

public class InvalidAdminApiKeyException extends RuntimeException {
    public InvalidAdminApiKeyException() { super("Invalid admin API key"); }
}
