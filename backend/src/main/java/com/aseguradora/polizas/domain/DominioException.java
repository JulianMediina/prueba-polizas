package com.aseguradora.polizas.domain;

/**
 * Excepción base del dominio. El {@code codigo} es estable y legible por máquinas;
 * se expone como el tipo del ProblemDetail.
 */
public abstract sealed class DominioException extends RuntimeException
		permits RecursoNoEncontradoException, ConflictoEstadoException, ReglaNegocioException {

	private final String codigo;

	protected DominioException(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}
}
