package com.aseguradora.polizas.integration.core;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contrato del servicio agnóstico de edición (capa media WebLogic → CORE).
 * {@code evento} y {@code polizaId} son los campos que exige el contrato; el resto son complementarios.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoreEvento(String evento, Long polizaId, Long riesgoId, String operacion) {

	public static final String ACTUALIZACION = "ACTUALIZACION";

	public static CoreEvento actualizacion(Long polizaId, Long riesgoId, String operacion) {
		return new CoreEvento(ACTUALIZACION, polizaId, riesgoId, operacion);
	}
}
