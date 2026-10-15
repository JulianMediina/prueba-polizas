package com.aseguradora.polizas;

import java.io.IOException;
import java.io.UncheckedIOException;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base para tests de integración: PostgreSQL 16 real embebido (no requiere Docker), compartido por toda la JVM de
 * tests. Flyway aplica las mismas migraciones que en producción.
 */
public abstract class IntegracionBase {

	public static final String API_KEY = "clave-de-prueba";

	private static final EmbeddedPostgres POSTGRES = iniciar();

	private static EmbeddedPostgres iniciar() {
		try {
			return EmbeddedPostgres.builder().start();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo iniciar PostgreSQL embebido", ex);
		}
	}

	@DynamicPropertySource
	static void propiedades(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "");
		registry.add("app.security.api-key", () -> API_KEY);
	}
}
