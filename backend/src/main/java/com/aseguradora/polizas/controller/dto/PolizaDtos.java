package com.aseguradora.polizas.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.aseguradora.polizas.domain.EstadoPoliza;
import com.aseguradora.polizas.domain.EstadoRiesgo;
import com.aseguradora.polizas.domain.TipoPoliza;

/** Contratos de entrada y salida de la API. Las entidades JPA nunca se exponen directamente. */
public final class PolizaDtos {

	private PolizaDtos() {
	}

	// ---------- Entrada ----------

	public record CrearPolizaRequest(
			@NotNull TipoPoliza tipo,
			@NotNull @Valid TomadorRequest tomador,
			@NotNull LocalDate fechaInicio,
			@Min(1) @Max(60) int mesesVigencia,
			@NotEmpty @Size(max = 200) List<@Valid @NotNull RiesgoRequest> riesgos) {
	}

	public record TomadorRequest(
			@NotBlank @Pattern(regexp = "CC|CE|NIT|PA|TI", message = "debe ser CC, CE, NIT, PA o TI") String tipoDocumento,
			@NotBlank @Pattern(regexp = "[0-9A-Za-z-]{4,20}", message = "documento inválido") String documento,
			@NotBlank @Size(max = 150) String nombre,
			@Email @Size(max = 150) String email,
			@Pattern(regexp = "[0-9+ ]{7,20}", message = "teléfono inválido") String telefono) {
	}

	public record PersonaRequest(
			@NotBlank @Pattern(regexp = "[0-9A-Za-z-]{4,20}", message = "documento inválido") String documento,
			@NotBlank @Size(max = 150) String nombre) {
	}

	public record RiesgoRequest(
			@NotBlank @Size(max = 200) String direccion,
			@NotBlank @Size(max = 80) String ciudad,
			@NotNull @Valid PersonaRequest arrendatario,
			@NotNull @Valid PersonaRequest arrendador,
			@NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal canonMensual) {
	}

	// ---------- Salida ----------

	public record TomadorResponse(String tipoDocumento, String documento, String nombre, String email,
			String telefono) {
	}

	public record PersonaResponse(String documento, String nombre) {
	}

	public record PolizaResponse(
			Long id,
			String numero,
			TipoPoliza tipo,
			EstadoPoliza estado,
			TomadorResponse tomador,
			LocalDate fechaInicio,
			LocalDate fechaFin,
			int mesesVigencia,
			BigDecimal canonMensual,
			BigDecimal prima,
			LocalDate fechaCancelacion) {
	}

	public record RiesgoResponse(
			Long id,
			Long polizaId,
			String direccion,
			String ciudad,
			PersonaResponse arrendatario,
			PersonaResponse arrendador,
			BigDecimal canonMensual,
			EstadoRiesgo estado,
			LocalDate fechaInclusion,
			LocalDate fechaCancelacion) {
	}

	public record RenovacionResponse(
			PolizaResponse poliza,
			int anioIpc,
			BigDecimal ipcAplicado,
			BigDecimal canonAnterior,
			BigDecimal canonNuevo,
			BigDecimal primaAnterior,
			BigDecimal primaNueva,
			LocalDate fechaInicioAnterior,
			LocalDate fechaFinAnterior) {
	}

	public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
	}
}
