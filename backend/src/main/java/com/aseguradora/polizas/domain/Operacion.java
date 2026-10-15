package com.aseguradora.polizas.domain;

/** Operaciones que modifican el estado de una póliza o de un riesgo y deben informarse al CORE. */
public enum Operacion {
	CREACION,
	RENOVACION,
	CANCELACION,
	RIESGO_AGREGADO,
	RIESGO_CANCELADO
}
