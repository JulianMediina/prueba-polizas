package com.aseguradora.polizas.integration.core;

import com.aseguradora.polizas.domain.PolizaModificada;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sincroniza con el CORE después del commit y fuera del hilo de la petición: una falla del CORE no revierte ni
 * demora la operación de negocio. Simplificación consciente: en el diseño objetivo (Módulo 1) este envío se hace con
 * un Transactional Outbox para garantizar entrega "al menos una vez".
 */
@Component
public class CoreSyncListener {

	private static final Logger log = LoggerFactory.getLogger(CoreSyncListener.class);

	private final CorePolizasPort core;

	public CoreSyncListener(CorePolizasPort core) {
		this.core = core;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alModificarPoliza(PolizaModificada evento) {
		try {
			core.notificar(CoreEvento.actualizacion(evento.polizaId(), evento.riesgoId(), evento.operacion().name()));
			log.info("CORE notificado polizaId={} operacion={}", evento.polizaId(), evento.operacion());
		}
		catch (RuntimeException ex) {
			log.error("CORE no notificado polizaId={} operacion={} causa={}", evento.polizaId(), evento.operacion(),
					ex.getMessage());
		}
	}
}
