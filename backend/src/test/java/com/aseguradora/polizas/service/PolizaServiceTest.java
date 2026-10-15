package com.aseguradora.polizas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.aseguradora.polizas.domain.ConflictoEstadoException;
import com.aseguradora.polizas.domain.DatosRiesgo;
import com.aseguradora.polizas.domain.EstadoPoliza;
import com.aseguradora.polizas.domain.Ipc;
import com.aseguradora.polizas.domain.Operacion;
import com.aseguradora.polizas.domain.Poliza;
import com.aseguradora.polizas.domain.PolizaModificada;
import com.aseguradora.polizas.domain.RecursoNoEncontradoException;
import com.aseguradora.polizas.domain.ReglaNegocioException;
import com.aseguradora.polizas.domain.Renovacion;
import com.aseguradora.polizas.domain.Riesgo;
import com.aseguradora.polizas.domain.TipoPoliza;
import com.aseguradora.polizas.domain.Tomador;
import com.aseguradora.polizas.repository.IpcRepository;
import com.aseguradora.polizas.repository.PolizaRepository;
import com.aseguradora.polizas.repository.RenovacionRepository;
import com.aseguradora.polizas.repository.RiesgoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PolizaServiceTest {

	static final Clock CLOCK = Clock.fixed(LocalDate.of(2026, 3, 10).atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),
			ZoneId.of("America/Bogota"));

	@Mock
	PolizaRepository polizas;

	@Mock
	RiesgoRepository riesgos;

	@Mock
	IpcRepository ipcs;

	@Mock
	RenovacionRepository renovaciones;

	@Mock
	ApplicationEventPublisher eventos;

	PolizaService service;

	@BeforeEach
	void setUp() {
		service = new PolizaService(polizas, riesgos, ipcs, renovaciones, eventos, CLOCK);
	}

	static DatosRiesgo datos(String canon) {
		return new DatosRiesgo("Calle 1", "Bogotá", "1", "Arrendatario", "2", "Arrendador", new BigDecimal(canon));
	}

	static Poliza poliza(TipoPoliza tipo) {
		return Poliza.crear("X-1", tipo, new Tomador("CC", "123456", "Tomador", null, null), LocalDate.of(2025, 6, 1),
				12, List.of(datos("1000000")));
	}

	@Test
	void listarEligeLaConsultaSegunFiltros() {
		var pageable = PageRequest.of(0, 20);
		when(polizas.findByTipoAndEstado(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA, pageable)).thenReturn(Page.empty());
		when(polizas.findByTipo(TipoPoliza.COLECTIVA, pageable)).thenReturn(Page.empty());
		when(polizas.findByEstado(EstadoPoliza.ACTIVA, pageable)).thenReturn(Page.empty());
		when(polizas.findAll(pageable)).thenReturn(Page.empty());

		service.listar(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA, pageable);
		service.listar(TipoPoliza.COLECTIVA, null, pageable);
		service.listar(null, EstadoPoliza.ACTIVA, pageable);
		service.listar(null, null, pageable);

		verify(polizas).findByTipoAndEstado(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA, pageable);
		verify(polizas).findByTipo(TipoPoliza.COLECTIVA, pageable);
		verify(polizas).findByEstado(EstadoPoliza.ACTIVA, pageable);
		verify(polizas).findAll(pageable);
	}

	@Test
	void crearAsignaNumeroSegunTipoYPublicaEvento() {
		when(polizas.siguienteNumero()).thenReturn(1234L);
		when(polizas.save(any(Poliza.class))).thenAnswer(inv -> inv.getArgument(0));
		var cmd = new CrearPolizaCommand(TipoPoliza.COLECTIVA, new Tomador("NIT", "900123456", "Inmobiliaria", null, null),
				LocalDate.of(2026, 1, 1), 12, List.of(datos("1000000"), datos("2000000")));

		var poliza = service.crear(cmd);

		assertThat(poliza.getNumero()).isEqualTo("COL-0001234");
		assertThat(poliza.getPrima()).isEqualByComparingTo("36000000");
		verify(eventos).publishEvent(new PolizaModificada(null, null, Operacion.CREACION));
	}

	@Test
	void renovarAplicaIpcDelAnioAnteriorYGuardaHistorial() {
		var poliza = poliza(TipoPoliza.INDIVIDUAL);
		when(polizas.findWithRiesgosById(7L)).thenReturn(Optional.of(poliza));
		when(ipcs.findById(2025)).thenReturn(Optional.of(new Ipc(2025, new BigDecimal("5.1"))));
		when(renovaciones.save(any(Renovacion.class))).thenAnswer(inv -> inv.getArgument(0));

		var renovacion = service.renovar(7L);

		assertThat(renovacion.getCanonNuevo()).isEqualByComparingTo("1051000");
		assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
		verify(eventos).publishEvent(new PolizaModificada(7L, null, Operacion.RENOVACION));
	}

	@Test
	void renovarSinIpcConfiguradoEsReglaDeNegocio() {
		when(polizas.findWithRiesgosById(7L)).thenReturn(Optional.of(poliza(TipoPoliza.INDIVIDUAL)));
		when(ipcs.findById(2025)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.renovar(7L)).isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("No hay IPC configurado para el año 2025");
		verifyNoInteractions(eventos);
	}

	@Test
	void renovarCanceladaEsConflictoAntesDeConsultarIpc() {
		var poliza = poliza(TipoPoliza.INDIVIDUAL);
		poliza.cancelar(LocalDate.of(2026, 1, 1));
		when(polizas.findWithRiesgosById(7L)).thenReturn(Optional.of(poliza));

		assertThatThrownBy(() -> service.renovar(7L)).isInstanceOf(ConflictoEstadoException.class);
		verify(ipcs, never()).findById(any());
		verifyNoInteractions(eventos);
	}

	@Test
	void cancelarUsaLaFechaDelRelojYPublicaEvento() {
		var poliza = poliza(TipoPoliza.INDIVIDUAL);
		when(polizas.findWithRiesgosById(7L)).thenReturn(Optional.of(poliza));

		service.cancelar(7L);

		assertThat(poliza.getFechaCancelacion()).isEqualTo(LocalDate.of(2026, 3, 10));
		verify(eventos).publishEvent(new PolizaModificada(7L, null, Operacion.CANCELACION));
	}

	@Test
	void agregarRiesgoPersisteYPublicaEvento() {
		var poliza = poliza(TipoPoliza.COLECTIVA);
		when(polizas.findWithRiesgosById(7L)).thenReturn(Optional.of(poliza));
		when(riesgos.save(any(Riesgo.class))).thenAnswer(inv -> inv.getArgument(0));

		var riesgo = service.agregarRiesgo(7L, datos("500000"));

		assertThat(riesgo.getFechaInclusion()).isEqualTo(LocalDate.of(2026, 3, 10));
		assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1500000");
		verify(eventos).publishEvent(new PolizaModificada(7L, null, Operacion.RIESGO_AGREGADO));
	}

	@Test
	void agregarRiesgoAIndividualNoPersisteNiPublica() {
		when(polizas.findWithRiesgosById(7L)).thenReturn(Optional.of(poliza(TipoPoliza.INDIVIDUAL)));

		assertThatThrownBy(() -> service.agregarRiesgo(7L, datos("500000"))).isInstanceOf(ReglaNegocioException.class);
		verifyNoInteractions(riesgos, eventos);
	}

	@Test
	void polizaInexistenteEs404() {
		when(polizas.findWithRiesgosById(99L)).thenReturn(Optional.empty());
		when(polizas.existsById(99L)).thenReturn(false);
		when(polizas.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cancelar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> service.listarRiesgos(99L)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
	}
}
