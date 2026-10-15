package com.aseguradora.polizas.integration.core;

public class CoreNoDisponibleException extends RuntimeException {

	public CoreNoDisponibleException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
