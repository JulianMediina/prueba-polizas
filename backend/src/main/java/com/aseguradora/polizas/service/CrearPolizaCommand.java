package com.aseguradora.polizas.service;

import java.time.LocalDate;
import java.util.List;

import com.aseguradora.polizas.domain.DatosRiesgo;
import com.aseguradora.polizas.domain.TipoPoliza;
import com.aseguradora.polizas.domain.Tomador;

public record CrearPolizaCommand(
		TipoPoliza tipo,
		Tomador tomador,
		LocalDate fechaInicio,
		int mesesVigencia,
		List<DatosRiesgo> riesgos) {
}
