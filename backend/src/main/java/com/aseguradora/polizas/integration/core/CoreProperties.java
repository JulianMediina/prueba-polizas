package com.aseguradora.polizas.integration.core;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración del cliente hacia el servicio de edición del CORE. */
@Validated
@ConfigurationProperties("app.core")
public record CoreProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotNull Duration connectTimeout,
		@NotNull Duration readTimeout,
		@Min(1) int maxIntentos,
		@NotNull Duration esperaInicial) {
}
