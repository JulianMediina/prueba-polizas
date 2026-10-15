package com.aseguradora.polizas.domain;

/** El recurso solicitado no existe (HTTP 404). */
public final class RecursoNoEncontradoException extends DominioException {

	public RecursoNoEncontradoException(String recurso, Object id) {
		super("recurso-no-encontrado", "No existe %s con id %s".formatted(recurso, id));
	}
}
