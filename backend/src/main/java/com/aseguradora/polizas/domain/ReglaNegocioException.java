package com.aseguradora.polizas.domain;

/** La solicitud es válida en forma, pero viola una regla de negocio (HTTP 422). */
public final class ReglaNegocioException extends DominioException {

	public ReglaNegocioException(String codigo, String mensaje) {
		super(codigo, mensaje);
	}
}
