package com.aseguradora.polizas.integration.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.aseguradora.polizas.config.CorrelationIdFilter;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.client.RestClient;

class CoreHttpAdapterTest {

	HttpServer server;
	final AtomicInteger respuesta = new AtomicInteger(202);
	final AtomicInteger llamadas = new AtomicInteger();
	final List<String> cuerpos = new CopyOnWriteArrayList<>();
	final List<String> apiKeys = new CopyOnWriteArrayList<>();
	final List<String> correlationIds = new CopyOnWriteArrayList<>();
	CoreHttpAdapter adapter;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/core-mock/evento", exchange -> {
			llamadas.incrementAndGet();
			cuerpos.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			apiKeys.add(exchange.getRequestHeaders().getFirst("x-api-key"));
			correlationIds.add(exchange.getRequestHeaders().getFirst(CorrelationIdFilter.HEADER));
			exchange.sendResponseHeaders(respuesta.get(), -1);
			exchange.close();
		});
		server.start();
		var props = new CoreProperties("http://127.0.0.1:" + server.getAddress().getPort(), "key-core",
				Duration.ofSeconds(1), Duration.ofSeconds(1), 3, Duration.ofMillis(10));
		adapter = new CoreHttpAdapter(RestClient.builder(), props);
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
		MDC.clear();
	}

	@Test
	void enviaElContratoConApiKeyYCorrelationId() {
		MDC.put(CorrelationIdFilter.MDC_KEY, "corr-1");

		adapter.notificar(CoreEvento.actualizacion(555L, null, "RENOVACION"));

		assertThat(llamadas).hasValue(1);
		assertThat(cuerpos.getFirst()).contains("\"evento\":\"ACTUALIZACION\"")
			.contains("\"polizaId\":555")
			.contains("\"operacion\":\"RENOVACION\"")
			.doesNotContain("riesgoId");
		assertThat(apiKeys).containsExactly("key-core");
		assertThat(correlationIds).containsExactly("corr-1");
	}

	@Test
	void reintentaErrores5xxYLuegoFalla() {
		respuesta.set(503);

		assertThatThrownBy(() -> adapter.notificar(CoreEvento.actualizacion(1L, null, "CANCELACION")))
			.isInstanceOf(CoreNoDisponibleException.class);
		assertThat(llamadas).hasValue(3);
	}

	@Test
	void noReintentaErrores4xx() {
		respuesta.set(401);

		assertThatThrownBy(() -> adapter.notificar(CoreEvento.actualizacion(1L, null, "CANCELACION")))
			.isInstanceOf(CoreNoDisponibleException.class);
		assertThat(llamadas).hasValue(1);
	}

	@Test
	void abreElCircuitoTrasFallasRepetidas() {
		respuesta.set(500);
		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> adapter.notificar(CoreEvento.actualizacion(1L, null, "X")))
				.isInstanceOf(CoreNoDisponibleException.class);
		}
		var llamadasAntes = llamadas.get();

		assertThatThrownBy(() -> adapter.notificar(CoreEvento.actualizacion(1L, null, "X")))
			.isInstanceOf(CoreNoDisponibleException.class)
			.hasMessageContaining("Circuit breaker abierto");
		assertThat(llamadas).hasValue(llamadasAntes);
	}
}
