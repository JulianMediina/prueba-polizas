package com.aseguradora.polizas.domain;

/** La operación no es válida para el estado actual del recurso (HTTP 409). */
public final class ConflictoEstadoException extends DominioException {

	public ConflictoEstadoException(String codigo, String mensaje) {
		super(codigo, mensaje);
	}
}
