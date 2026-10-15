package com.aseguradora.polizas.config;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * La API key llega por variable de entorno ({@code API_KEY}). Si falta, la aplicación no arranca (fail-fast).
 */
@Validated
@ConfigurationProperties("app.security")
public record ApiSecurityProperties(@NotBlank String apiKey, List<String> corsAllowedOrigins) {

	public ApiSecurityProperties {
		corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
	}
}
