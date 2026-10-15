package com.aseguradora.polizas.integration.core;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import com.aseguradora.polizas.config.CorrelationIdFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter HTTP hacia el servicio de edición del CORE. Aplica timeout, reintentos con backoff exponencial y circuit
 * breaker para que una caída del CORE no degrade la API, y propaga el correlation id.
 */
@Component
public class CoreHttpAdapter implements CorePolizasPort {

	private static final Logger log = LoggerFactory.getLogger(CoreHttpAdapter.class);
	static final String API_KEY_HEADER = "x-api-key";

	private final RestClient restClient;
	private final Retry retry;
	private final CircuitBreaker circuitBreaker;

	public CoreHttpAdapter(RestClient.Builder builder, CoreProperties props) {
		var httpClient = HttpClient.newBuilder().connectTimeout(props.connectTimeout()).build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(props.readTimeout());
		this.restClient = builder.baseUrl(props.baseUrl())
			.requestFactory(requestFactory)
			.defaultHeader(API_KEY_HEADER, props.apiKey())
			.build();
		this.retry = Retry.of("core", RetryConfig.custom()
			.maxAttempts(props.maxIntentos())
			.intervalFunction(IntervalFunction.ofExponentialRandomBackoff(props.esperaInicial(), 2.0))
			.retryOnException(ex -> ex instanceof RestClientException && !(ex instanceof HttpClientErrorException))
			.build());
		this.circuitBreaker = CircuitBreaker.of("core", CircuitBreakerConfig.custom()
			.slidingWindowSize(10)
			.minimumNumberOfCalls(5)
			.failureRateThreshold(50)
			.waitDurationInOpenState(Duration.ofSeconds(30))
			.build());
		retry.getEventPublisher()
			.onRetry(e -> log.warn("Reintento {} hacia el CORE: {}", e.getNumberOfRetryAttempts(),
					e.getLastThrowable().getMessage()));
		circuitBreaker.getEventPublisher()
			.onStateTransition(e -> log.warn("Circuit breaker CORE: {}", e.getStateTransition()));
	}

	@Override
	public void notificar(CoreEvento evento) {
		Supplier<Void> llamada = () -> enviar(evento);
		try {
			Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, llamada)).get();
		}
		catch (CallNotPermittedException ex) {
			throw new CoreNoDisponibleException("Circuit breaker abierto: el CORE no está disponible", ex);
		}
		catch (RestClientException ex) {
			throw new CoreNoDisponibleException("No fue posible notificar al CORE: " + ex.getMessage(), ex);
		}
	}

	private Void enviar(CoreEvento evento) {
		var request = restClient.post().uri("/core-mock/evento").contentType(MediaType.APPLICATION_JSON);
		var correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
		if (correlationId != null) {
			request = request.header(CorrelationIdFilter.HEADER, correlationId);
		}
		request.body(evento).retrieve().toBodilessEntity();
		return null;
	}
}
