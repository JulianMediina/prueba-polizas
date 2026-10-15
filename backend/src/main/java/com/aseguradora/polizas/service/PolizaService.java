package com.aseguradora.polizas.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.aseguradora.polizas.domain.EstadoPoliza;
import com.aseguradora.polizas.domain.Operacion;
import com.aseguradora.polizas.domain.Poliza;
import com.aseguradora.polizas.domain.PolizaModificada;
import com.aseguradora.polizas.domain.RecursoNoEncontradoException;
import com.aseguradora.polizas.domain.ReglaNegocioException;
import com.aseguradora.polizas.domain.Renovacion;
import com.aseguradora.polizas.domain.Riesgo;
import com.aseguradora.polizas.domain.DatosRiesgo;
import com.aseguradora.polizas.domain.TipoPoliza;
import com.aseguradora.polizas.repository.IpcRepository;
import com.aseguradora.polizas.repository.PolizaRepository;
import com.aseguradora.polizas.repository.RenovacionRepository;
import com.aseguradora.polizas.repository.RiesgoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso sobre pólizas. Orquesta repositorios y eventos; las reglas de negocio están en {@link Poliza}.
 */
@Service
@Transactional(readOnly = true)
public class PolizaService {

	private static final Logger log = LoggerFactory.getLogger(PolizaService.class);

	private final PolizaRepository polizas;
	private final RiesgoRepository riesgos;
	private final IpcRepository ipcs;
	private final RenovacionRepository renovaciones;
	private final ApplicationEventPublisher eventos;
	private final Clock clock;

	public PolizaService(PolizaRepository polizas, RiesgoRepository riesgos, IpcRepository ipcs,
			RenovacionRepository renovaciones, ApplicationEventPublisher eventos, Clock clock) {
		this.polizas = polizas;
		this.riesgos = riesgos;
		this.ipcs = ipcs;
		this.renovaciones = renovaciones;
		this.eventos = eventos;
		this.clock = clock;
	}

	public Page<Poliza> listar(TipoPoliza tipo, EstadoPoliza estado, Pageable pageable) {
		if (tipo != null && estado != null) {
			return polizas.findByTipoAndEstado(tipo, estado, pageable);
		}
		if (tipo != null) {
			return polizas.findByTipo(tipo, pageable);
		}
		if (estado != null) {
			return polizas.findByEstado(estado, pageable);
		}
		return polizas.findAll(pageable);
	}

	public Poliza obtener(Long id) {
		return polizas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Poliza", id));
	}

	public List<Riesgo> listarRiesgos(Long polizaId) {
		if (!polizas.existsById(polizaId)) {
			throw new RecursoNoEncontradoException("Poliza", polizaId);
		}
		return riesgos.findByPolizaIdOrderById(polizaId);
	}

	@Transactional
	public Poliza crear(CrearPolizaCommand cmd) {
		var prefijo = cmd.tipo() == TipoPoliza.INDIVIDUAL ? "IND" : "COL";
		var numero = "%s-%07d".formatted(prefijo, polizas.siguienteNumero());
		var poliza = polizas.save(Poliza.crear(numero, cmd.tipo(), cmd.tomador(), cmd.fechaInicio(),
				cmd.mesesVigencia(), cmd.riesgos()));
		log.info("Póliza creada id={} numero={} tipo={} riesgos={}", poliza.getId(), numero, poliza.getTipo(),
				poliza.getRiesgos().size());
		eventos.publishEvent(new PolizaModificada(poliza.getId(), null, Operacion.CREACION));
		return poliza;
	}

	@Transactional
	public Renovacion renovar(Long id) {
		var poliza = cargarAgregado(id);
		// Se valida antes de buscar el IPC para responder con la regla correcta (409) y no con un 422 por IPC.
		poliza.validarRenovable();
		var anio = poliza.anioIpcParaRenovar();
		var ipc = ipcs.findById(anio)
			.orElseThrow(() -> new ReglaNegocioException("ipc-no-configurado",
					"No hay IPC configurado para el año %d; no es posible renovar la póliza %s"
						.formatted(anio, poliza.getNumero())));
		var renovacion = renovaciones.save(poliza.renovar(ipc));
		log.info("Póliza renovada id={} ipcAnio={} ipc={} canonAnterior={} canonNuevo={}", id, anio,
				ipc.getPorcentaje(), renovacion.getCanonAnterior(), renovacion.getCanonNuevo());
		eventos.publishEvent(new PolizaModificada(id, null, Operacion.RENOVACION));
		return renovacion;
	}

	@Transactional
	public Poliza cancelar(Long id) {
		var poliza = cargarAgregado(id);
		poliza.cancelar(LocalDate.now(clock));
		log.info("Póliza cancelada id={} numero={}", id, poliza.getNumero());
		eventos.publishEvent(new PolizaModificada(id, null, Operacion.CANCELACION));
		return poliza;
	}

	@Transactional
	public Riesgo agregarRiesgo(Long polizaId, DatosRiesgo datos) {
		var poliza = cargarAgregado(polizaId);
		var riesgo = riesgos.save(poliza.agregarRiesgo(datos, LocalDate.now(clock)));
		log.info("Riesgo agregado polizaId={} riesgoId={}", polizaId, riesgo.getId());
		eventos.publishEvent(new PolizaModificada(polizaId, riesgo.getId(), Operacion.RIESGO_AGREGADO));
		return riesgo;
	}

	private Poliza cargarAgregado(Long id) {
		return polizas.findWithRiesgosById(id).orElseThrow(() -> new RecursoNoEncontradoException("Poliza", id));
	}
}
