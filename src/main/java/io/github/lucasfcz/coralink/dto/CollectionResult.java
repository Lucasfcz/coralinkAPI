package io.github.lucasfcz.coralink.dto;

// lembrete -> deletar esse ou phaseResult e so usar um
public record CollectionResult(
        int collected,
        int sourceFailures) {}
