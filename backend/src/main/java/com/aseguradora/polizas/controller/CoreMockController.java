package com.aseguradora.polizas.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock del servicio agnóstico de edición del CORE (capa media WebLogic). Su único propósito es registrar en el log
 * que la operación se intentó enviar al CORE.
 */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

	private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

	@PostMapping("/evento")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Recibido evento(@Valid @RequestBody EventoRequest evento) {
		log.info("CORE-MOCK evento={} polizaId={} riesgoId={} operacion={}", evento.evento(), evento.polizaId(),
				evento.riesgoId(), evento.operacion());
		return new Recibido("REGISTRADO", evento.evento(), evento.polizaId());
	}

	public record EventoRequest(
			@NotBlank @Pattern(regexp = "[A-Z_]{1,40}", message = "evento inválido") String evento,
			@NotNull Long polizaId,
			Long riesgoId,
			@Size(max = 40) @Pattern(regexp = "[A-Z_]*", message = "operación inválida") String operacion) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Recibido(String estado, String evento, Long polizaId) {
	}
}
