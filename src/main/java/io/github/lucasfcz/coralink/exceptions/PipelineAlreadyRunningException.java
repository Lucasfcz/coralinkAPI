package io.github.lucasfcz.coralink.exceptions;

public class PipelineAlreadyRunningException extends RuntimeException {
    public PipelineAlreadyRunningException() { super("Pipeline is already running"); }
}
