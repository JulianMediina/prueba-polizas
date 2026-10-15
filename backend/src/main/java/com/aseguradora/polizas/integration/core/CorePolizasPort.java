package com.aseguradora.polizas.integration.core;

/**
 * Puerto de salida hacia el CORE de seguros. El dominio no conoce el protocolo (REST, SOAP, cola);
 * cada adapter lo implementa. Hoy: {@link CoreHttpAdapter} contra el mock {@code POST /core-mock/evento}.
 */
public interface CorePolizasPort {

	/**
	 * Informa al CORE que una póliza o riesgo cambió de estado.
	 * @throws CoreNoDisponibleException si no fue posible entregar el evento tras los reintentos
	 */
	void notificar(CoreEvento evento);
}
