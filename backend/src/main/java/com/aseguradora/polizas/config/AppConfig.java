package com.aseguradora.polizas.config;

import java.time.Clock;
import java.time.ZoneId;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AppConfig {

	/** Reloj inyectable: las fechas de negocio (cancelación, inclusión) usan la zona de Colombia y son testeables. */
	@Bean
	Clock clock(@Value("${app.zona-horaria:America/Bogota}") String zona) {
		return Clock.system(ZoneId.of(zona));
	}

	/** Propaga el MDC (correlation id) a las tareas {@code @Async}, como la sincronización con el CORE. */
	@Bean
	TaskDecorator mdcTaskDecorator() {
		return runnable -> {
			var contexto = MDC.getCopyOfContextMap();
			return () -> {
				if (contexto != null) {
					MDC.setContextMap(contexto);
				}
				try {
					runnable.run();
				}
				finally {
					MDC.clear();
				}
			};
		};
	}
}
