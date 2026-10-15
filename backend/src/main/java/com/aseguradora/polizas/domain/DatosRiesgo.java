package com.aseguradora.polizas.domain;

import java.math.BigDecimal;

/** Datos necesarios para incluir un riesgo (inmueble arrendado) en una póliza. */
public record DatosRiesgo(
		String direccion,
		String ciudad,
		String arrendatarioDocumento,
		String arrendatarioNombre,
		String arrendadorDocumento,
		String arrendadorNombre,
		BigDecimal canonMensual) {
}
