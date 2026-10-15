package com.aseguradora.polizas.domain;

/**
 * Evento de dominio: el estado de una póliza o de uno de sus riesgos cambió.
 * Se publica dentro de la transacción y se procesa después del commit.
 */
public record PolizaModificada(Long polizaId, Long riesgoId, Operacion operacion) {
}
