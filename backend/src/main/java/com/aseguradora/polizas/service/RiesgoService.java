package com.aseguradora.polizas.service;

import java.time.Clock;
import java.time.LocalDate;

import com.aseguradora.polizas.domain.Operacion;
import com.aseguradora.polizas.domain.PolizaModificada;
import com.aseguradora.polizas.domain.RecursoNoEncontradoException;
import com.aseguradora.polizas.domain.Riesgo;
import com.aseguradora.polizas.repository.PolizaRepository;
import com.aseguradora.polizas.repository.RiesgoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiesgoService {

	private static final Logger log = LoggerFactory.getLogger(RiesgoService.class);

	private final RiesgoRepository riesgos;
	private final PolizaRepository polizas;
	private final ApplicationEventPublisher eventos;
	private final Clock clock;

	public RiesgoService(RiesgoRepository riesgos, PolizaRepository polizas, ApplicationEventPublisher eventos,
			Clock clock) {
		this.riesgos = riesgos;
		this.polizas = polizas;
		this.eventos = eventos;
		this.clock = clock;
	}

	/** Cancela un riesgo pasando por el agregado {@code Poliza}, que valida las reglas y recalcula canon y prima. */
	@Transactional
	public Riesgo cancelar(Long riesgoId) {
		var riesgo = riesgos.findById(riesgoId).orElseThrow(() -> new RecursoNoEncontradoException("Riesgo", riesgoId));
		var polizaId = riesgo.getPoliza().getId();
		var poliza = polizas.findWithRiesgosById(polizaId)
			.orElseThrow(() -> new RecursoNoEncontradoException("Poliza", polizaId));
		poliza.cancelarRiesgo(riesgo, LocalDate.now(clock));
		log.info("Riesgo cancelado riesgoId={} polizaId={}", riesgoId, polizaId);
		eventos.publishEvent(new PolizaModificada(polizaId, riesgoId, Operacion.RIESGO_CANCELADO));
		return riesgo;
	}
}
