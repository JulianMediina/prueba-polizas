package com.aseguradora.polizas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PolizaTest {

	static final LocalDate INICIO = LocalDate.of(2025, 6, 1);
	static final LocalDate HOY = LocalDate.of(2026, 1, 15);
	static final Tomador TOMADOR = new Tomador("CC", "1020304050", "Laura Gómez", "laura@example.com", "3001234567");

	static DatosRiesgo riesgo(String canon) {
		return new DatosRiesgo("Calle 1 # 2-3", "Bogotá", "1020304050", "Laura Gómez", "79865432", "Carlos Ruiz",
				new BigDecimal(canon));
	}

	static Poliza individual() {
		return Poliza.crear("IND-0000001", TipoPoliza.INDIVIDUAL, TOMADOR, INICIO, 12, List.of(riesgo("2500000")));
	}

	static Poliza colectiva(String... canones) {
		var riesgos = java.util.Arrays.stream(canones).map(PolizaTest::riesgo).toList();
		return Poliza.crear("COL-0000001", TipoPoliza.COLECTIVA, TOMADOR, INICIO, 12, riesgos);
	}

	@Nested
	@DisplayName("Creación")
	class Creacion {

		@Test
		void calculaCanonComoSumaDeRiesgosYPrimaComoCanonPorMeses() {
			var poliza = colectiva("1000000", "1500000.50");

			assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2500000.50");
			assertThat(poliza.getPrima()).isEqualByComparingTo("30000006.00");
			assertThat(poliza.getFechaFin()).isEqualTo(LocalDate.of(2026, 6, 1));
		}

		@Test
		void individualExigeExactamenteUnRiesgo() {
			assertThatThrownBy(() -> Poliza.crear("IND-1", TipoPoliza.INDIVIDUAL, TOMADOR, INICIO, 12,
					List.of(riesgo("1"), riesgo("2"))))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("exactamente un riesgo");
			assertThatThrownBy(() -> Poliza.crear("IND-1", TipoPoliza.INDIVIDUAL, TOMADOR, INICIO, 12, List.of()))
				.isInstanceOf(ReglaNegocioException.class);
		}

		@Test
		void colectivaExigeAlMenosUnRiesgo() {
			assertThatThrownBy(() -> Poliza.crear("COL-1", TipoPoliza.COLECTIVA, TOMADOR, INICIO, 12, null))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("al menos un riesgo");
		}

		@Test
		void rechazaVigenciaFueraDeRango() {
			assertThatThrownBy(() -> Poliza.crear("IND-1", TipoPoliza.INDIVIDUAL, TOMADOR, INICIO, 0,
					List.of(riesgo("1"))))
				.isInstanceOf(ReglaNegocioException.class);
		}

		@Test
		void rechazaCanonNoPositivo() {
			assertThatThrownBy(() -> colectiva("0")).isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("mayor que cero");
		}
	}

	@Nested
	@DisplayName("Regla: una póliza individual solo puede tener 1 riesgo / agregar riesgo valida el tipo")
	class AgregarRiesgo {

		@Test
		void individualNoAdmiteMasRiesgos() {
			var poliza = individual();

			assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("1000000"), HOY))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("Solo se pueden agregar riesgos a pólizas colectivas");
			assertThat(poliza.getRiesgos()).hasSize(1);
		}

		@Test
		void colectivaAgregaRiesgoYRecalculaValores() {
			var poliza = colectiva("1000000");

			var nuevo = poliza.agregarRiesgo(riesgo("500000"), HOY);

			assertThat(nuevo.getEstado()).isEqualTo(EstadoRiesgo.ACTIVO);
			assertThat(nuevo.getFechaInclusion()).isEqualTo(HOY);
			assertThat(poliza.getRiesgos()).hasSize(2);
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1500000");
			assertThat(poliza.getPrima()).isEqualByComparingTo("18000000");
		}

		@Test
		void colectivaCanceladaNoAdmiteRiesgos() {
			var poliza = colectiva("1000000");
			poliza.cancelar(HOY);

			assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("500000"), HOY))
				.isInstanceOf(ConflictoEstadoException.class);
		}
	}

	@Nested
	@DisplayName("Regla: la cancelación de una póliza cancela todos sus riesgos")
	class Cancelar {

		@Test
		void cancelaPolizaYRiesgosEnCascada() {
			var poliza = colectiva("1000000", "2000000", "3000000");

			poliza.cancelar(HOY);

			assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
			assertThat(poliza.getFechaCancelacion()).isEqualTo(HOY);
			assertThat(poliza.getRiesgos()).allSatisfy(r -> {
				assertThat(r.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
				assertThat(r.getFechaCancelacion()).isEqualTo(HOY);
			});
			assertThat(poliza.getCanonMensual()).as("canon histórico se conserva").isEqualByComparingTo("6000000");
		}

		@Test
		void noSePuedeCancelarDosVeces() {
			var poliza = individual();
			poliza.cancelar(HOY);

			assertThatThrownBy(() -> poliza.cancelar(HOY)).isInstanceOf(ConflictoEstadoException.class)
				.hasMessageContaining("ya está cancelada");
		}
	}

	@Nested
	@DisplayName("Cancelar riesgo")
	class CancelarRiesgo {

		@Test
		void cancelaRiesgoDeColectivaYRecalcula() {
			var poliza = colectiva("1000000", "2000000");
			var riesgo = poliza.getRiesgos().getFirst();

			poliza.cancelarRiesgo(riesgo, HOY);

			assertThat(riesgo.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2000000");
			assertThat(poliza.getPrima()).isEqualByComparingTo("24000000");
		}

		@Test
		void riesgoYaCanceladoEsConflicto() {
			var poliza = colectiva("1000000", "2000000", "3000000");
			var riesgo = poliza.getRiesgos().getFirst();
			poliza.cancelarRiesgo(riesgo, HOY);

			assertThatThrownBy(() -> poliza.cancelarRiesgo(riesgo, HOY)).isInstanceOf(ConflictoEstadoException.class);
		}

		@Test
		void riesgoDeIndividualNoSeCancelaPorSeparado() {
			var poliza = individual();
			var riesgo = poliza.getRiesgos().getFirst();

			assertThatThrownBy(() -> poliza.cancelarRiesgo(riesgo, HOY)).isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("cancele la póliza");
			assertThat(riesgo.isActivo()).isTrue();
		}

		@Test
		void colectivaNoPuedeQuedarSinRiesgosActivos() {
			var poliza = colectiva("1000000");
			var riesgo = poliza.getRiesgos().getFirst();

			assertThatThrownBy(() -> poliza.cancelarRiesgo(riesgo, HOY)).isInstanceOf(ReglaNegocioException.class);
			assertThat(riesgo.isActivo()).isTrue();
		}

		@Test
		void riesgoDeOtraPolizaEsRechazado() {
			var poliza = colectiva("1000000", "2000000");
			var ajeno = colectiva("3000000").getRiesgos().getFirst();

			assertThatThrownBy(() -> poliza.cancelarRiesgo(ajeno, HOY)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("Regla: renovación con IPC; no se puede renovar una póliza cancelada")
	class Renovar {

		static final Ipc IPC_2025 = new Ipc(2025, new BigDecimal("5.100"));

		@Test
		void incrementaCanonYPrimaEnIpcYExtiendeVigencia() {
			var poliza = colectiva("1000000", "2500000");

			var renovacion = poliza.renovar(IPC_2025);

			assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
			assertThat(poliza.getRiesgos()).extracting(Riesgo::getCanonMensual)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("1051000.00"), new BigDecimal("2627500.00"));
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("3678500.00");
			assertThat(poliza.getPrima()).isEqualByComparingTo("44142000.00");
			assertThat(poliza.getFechaInicio()).isEqualTo(LocalDate.of(2026, 6, 1));
			assertThat(poliza.getFechaFin()).isEqualTo(LocalDate.of(2027, 6, 1));

			assertThat(renovacion.getAnioIpc()).isEqualTo(2025);
			assertThat(renovacion.getCanonAnterior()).isEqualByComparingTo("3500000");
			assertThat(renovacion.getCanonNuevo()).isEqualByComparingTo("3678500");
			assertThat(renovacion.getPrimaAnterior()).isEqualByComparingTo("42000000");
			assertThat(renovacion.getFechaInicioAnterior()).isEqualTo(INICIO);
		}

		@Test
		void redondeaAlCentavoConHalfEven() {
			var poliza = Poliza.crear("IND-1", TipoPoliza.INDIVIDUAL, TOMADOR, INICIO, 12, List.of(riesgo("1234567.89")));

			poliza.renovar(new Ipc(2025, new BigDecimal("3.333")));

			// 1234567.89 × 1.03333 = 1275716.0377... → 1275716.04
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1275716.04");
		}

		@Test
		void noIncrementaRiesgosCancelados() {
			var poliza = colectiva("1000000", "2000000");
			var cancelado = poliza.getRiesgos().getFirst();
			poliza.cancelarRiesgo(cancelado, HOY);

			poliza.renovar(IPC_2025);

			assertThat(cancelado.getCanonMensual()).isEqualByComparingTo("1000000");
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2102000");
		}

		@Test
		void polizaCanceladaNoSeRenueva() {
			var poliza = individual();
			poliza.cancelar(HOY);

			assertThatThrownBy(() -> poliza.renovar(IPC_2025)).isInstanceOf(ConflictoEstadoException.class)
				.hasMessageContaining("No se puede renovar");
			assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
		}

		@Test
		void exigeElIpcDelAnioAnteriorALaNuevaVigencia() {
			var poliza = individual();

			assertThat(poliza.anioIpcParaRenovar()).isEqualTo(2025);
			assertThatThrownBy(() -> poliza.renovar(new Ipc(2024, BigDecimal.ONE)))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void sePuedeRenovarVariasVeces() {
			var poliza = individual();
			poliza.renovar(IPC_2025);

			assertThat(poliza.anioIpcParaRenovar()).isEqualTo(2026);
			poliza.renovar(new Ipc(2026, new BigDecimal("4")));

			assertThat(poliza.getFechaFin()).isEqualTo(LocalDate.of(2028, 6, 1));
			assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2732600.00");
		}
	}
}
