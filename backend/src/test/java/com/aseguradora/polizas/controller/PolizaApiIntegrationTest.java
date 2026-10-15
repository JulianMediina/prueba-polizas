package com.aseguradora.polizas.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aseguradora.polizas.IntegracionBase;
import com.aseguradora.polizas.integration.core.CorePolizasPort;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class PolizaApiIntegrationTest extends IntegracionBase {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	CorePolizasPort core;

	// ---------- Utilidades ----------

	static MockHttpServletRequestBuilder conKey(MockHttpServletRequestBuilder req) {
		return req.header("x-api-key", API_KEY);
	}

	static String riesgoJson(String canon) {
		return """
				{"direccion":"Calle 100 # 10-20","ciudad":"Bogotá",
				 "arrendatario":{"documento":"1020304050","nombre":"Ana Arrendataria"},
				 "arrendador":{"documento":"79865432","nombre":"Pedro Arrendador"},
				 "canonMensual":%s}""".formatted(canon);
	}

	static String polizaJson(String tipo, String fechaInicio, String... canones) {
		var riesgos = String.join(",", java.util.Arrays.stream(canones).map(PolizaApiIntegrationTest::riesgoJson).toList());
		return """
				{"tipo":"%s","fechaInicio":"%s","mesesVigencia":12,
				 "tomador":{"tipoDocumento":"CC","documento":"1020304050","nombre":"Tomador Prueba","email":"t@example.com"},
				 "riesgos":[%s]}""".formatted(tipo, fechaInicio, riesgos);
	}

	long crear(String json) throws Exception {
		var body = mvc.perform(conKey(post("/polizas")).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	long primerRiesgo(long polizaId) throws Exception {
		var body = mvc.perform(conKey(get("/polizas/{id}/riesgos", polizaId))).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$[0].id")).longValue();
	}

	// ---------- Tests ----------

	@Nested
	class Seguridad {

		@Test
		void sinApiKeyResponde401ConProblemDetail() throws Exception {
			mvc.perform(get("/polizas"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("No autorizado"));
		}

		@Test
		void apiKeyIncorrectaResponde401() throws Exception {
			mvc.perform(get("/polizas").header("x-api-key", "otra")).andExpect(status().isUnauthorized());
			mvc.perform(post("/core-mock/evento").header("x-api-key", "otra")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}")).andExpect(status().isUnauthorized());
		}

		@Test
		void healthEsPublico() throws Exception {
			mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
		}

		@Test
		void propagaElCorrelationIdOGeneraUno() throws Exception {
			mvc.perform(conKey(get("/polizas")).header("X-Correlation-Id", "abc-123"))
				.andExpect(header().string("X-Correlation-Id", "abc-123"));
			mvc.perform(conKey(get("/polizas")).header("X-Correlation-Id", "inyección\nmaliciosa"))
				.andExpect(header().string("X-Correlation-Id", not("inyección\nmaliciosa")));
		}
	}

	@Nested
	class Listar {

		@Test
		void filtraPorTipoYEstado() throws Exception {
			mvc.perform(conKey(get("/polizas")).param("tipo", "COLECTIVA").param("estado", "ACTIVA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[*].tipo", everyItem(is("COLECTIVA"))))
				.andExpect(jsonPath("$.contenido[*].estado", everyItem(is("ACTIVA"))))
				.andExpect(jsonPath("$.contenido[*].numero", hasItems("COL-0000005", "COL-0000006")));
		}

		@Test
		void filtraSoloPorEstado() throws Exception {
			mvc.perform(conKey(get("/polizas")).param("estado", "CANCELADA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[*].estado", everyItem(is("CANCELADA"))));
		}

		@Test
		void paginaLosResultados() throws Exception {
			mvc.perform(conKey(get("/polizas")).param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(2))
				.andExpect(jsonPath("$.tamano").value(2));
		}

		@Test
		void enumInvalidoEs400ConValoresPermitidos() throws Exception {
			mvc.perform(conKey(get("/polizas")).param("tipo", "OTRA"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.valoresPermitidos", hasItems("INDIVIDUAL", "COLECTIVA")));
		}

		@Test
		void tamanoFueraDeRangoEs400() throws Exception {
			mvc.perform(conKey(get("/polizas")).param("size", "500")).andExpect(status().isBadRequest());
		}
	}

	@Nested
	class Crear {

		@Test
		void creaIndividualCalculandoPrima() throws Exception {
			mvc.perform(conKey(post("/polizas")).contentType(MediaType.APPLICATION_JSON)
				.content(polizaJson("INDIVIDUAL", "2026-01-01", "2000000")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("http://localhost/polizas/")))
				.andExpect(jsonPath("$.numero", startsWith("IND-")))
				.andExpect(jsonPath("$.estado").value("ACTIVA"))
				.andExpect(jsonPath("$.fechaFin").value("2027-01-01"))
				.andExpect(jsonPath("$.canonMensual").value(2000000.00))
				.andExpect(jsonPath("$.prima").value(24000000.00));
			verify(core, timeout(3000)).notificar(argThat(e -> "CREACION".equals(e.operacion())));
		}

		@Test
		void individualConDosRiesgosEs422() throws Exception {
			mvc.perform(conKey(post("/polizas")).contentType(MediaType.APPLICATION_JSON)
				.content(polizaJson("INDIVIDUAL", "2026-01-01", "1000000", "2000000")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("riesgo-unico-individual"));
		}

		@Test
		void camposInvalidosSon400ConDetallePorCampo() throws Exception {
			mvc.perform(conKey(post("/polizas")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tipo\":\"COLECTIVA\",\"mesesVigencia\":0,\"riesgos\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.tomador").exists())
				.andExpect(jsonPath("$.errores.fechaInicio").exists())
				.andExpect(jsonPath("$.errores.mesesVigencia").exists())
				.andExpect(jsonPath("$.errores.riesgos").exists());
		}
	}

	@Nested
	class Riesgos {

		@Test
		void listaRiesgosDeLaPoliza() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2026-01-01", "1000000", "2000000"));

			mvc.perform(conKey(get("/polizas/{id}/riesgos", id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].polizaId").value(id))
				.andExpect(jsonPath("$[0].arrendador.nombre").value("Pedro Arrendador"));
		}

		@Test
		void polizaInexistenteEs404() throws Exception {
			mvc.perform(conKey(get("/polizas/{id}/riesgos", 999999)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("recurso-no-encontrado"));
		}

		@Test
		void agregaRiesgoAColectivaYRecalculaPrima() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2026-01-01", "1000000"));

			mvc.perform(conKey(post("/polizas/{id}/riesgos", id)).contentType(MediaType.APPLICATION_JSON)
				.content(riesgoJson("500000")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
			mvc.perform(conKey(get("/polizas/{id}", id))).andExpect(jsonPath("$.prima").value(18000000.00));
			verify(core, timeout(3000))
				.notificar(argThat(e -> e.polizaId() == id && "RIESGO_AGREGADO".equals(e.operacion()) && e.riesgoId() != null));
		}

		@Test
		void agregarRiesgoAIndividualEs422() throws Exception {
			var id = crear(polizaJson("INDIVIDUAL", "2026-01-01", "1000000"));

			mvc.perform(conKey(post("/polizas/{id}/riesgos", id)).contentType(MediaType.APPLICATION_JSON)
				.content(riesgoJson("500000")))
				.andExpect(status().isUnprocessableContent());
		}

		@Test
		void agregarRiesgoAColectivaCanceladaEs409() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2026-01-01", "1000000"));
			mvc.perform(conKey(post("/polizas/{id}/cancelar", id))).andExpect(status().isOk());

			mvc.perform(conKey(post("/polizas/{id}/riesgos", id)).contentType(MediaType.APPLICATION_JSON)
				.content(riesgoJson("500000")))
				.andExpect(status().isConflict());
		}

		@Test
		void riesgoConCanonInvalidoEs400() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2026-01-01", "1000000"));

			mvc.perform(conKey(post("/polizas/{id}/riesgos", id)).contentType(MediaType.APPLICATION_JSON)
				.content(riesgoJson("-5")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.canonMensual").exists());
		}

		@Test
		void cancelaRiesgoDeColectiva() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2026-01-01", "1000000", "2000000"));
			var riesgoId = primerRiesgo(id);

			mvc.perform(conKey(post("/riesgos/{id}/cancelar", riesgoId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADO"));
			mvc.perform(conKey(post("/riesgos/{id}/cancelar", riesgoId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("riesgo-ya-cancelado"));
			mvc.perform(conKey(get("/polizas/{id}", id))).andExpect(jsonPath("$.canonMensual").value(2000000.00));
		}

		@Test
		void riesgoDeIndividualNoSeCancelaPorSeparado() throws Exception {
			var id = crear(polizaJson("INDIVIDUAL", "2026-01-01", "1000000"));

			mvc.perform(conKey(post("/riesgos/{id}/cancelar", primerRiesgo(id))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("riesgo-individual-no-cancelable"));
		}

		@Test
		void riesgoInexistenteEs404() throws Exception {
			mvc.perform(conKey(post("/riesgos/{id}/cancelar", 999999))).andExpect(status().isNotFound());
		}
	}

	@Nested
	class Renovar {

		@Test
		void renuevaConIpcDelAnioAnterior() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2025-06-01", "1000000", "2500000"));

			mvc.perform(conKey(post("/polizas/{id}/renovar", id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.poliza.estado").value("RENOVADA"))
				.andExpect(jsonPath("$.anioIpc").value(2025))
				.andExpect(jsonPath("$.ipcAplicado").value(5.1))
				.andExpect(jsonPath("$.canonAnterior").value(3500000.00))
				.andExpect(jsonPath("$.canonNuevo").value(3678500.00))
				.andExpect(jsonPath("$.primaNueva").value(44142000.00))
				.andExpect(jsonPath("$.poliza.fechaInicio").value("2026-06-01"))
				.andExpect(jsonPath("$.poliza.fechaFin").value("2027-06-01"));
			verify(core, timeout(3000)).notificar(argThat(e -> e.polizaId() == id && "RENOVACION".equals(e.operacion())
					&& "ACTUALIZACION".equals(e.evento())));
		}

		@Test
		void polizaCanceladaNoSeRenueva409() throws Exception {
			var id = crear(polizaJson("INDIVIDUAL", "2025-06-01", "1000000"));
			mvc.perform(conKey(post("/polizas/{id}/cancelar", id))).andExpect(status().isOk());

			mvc.perform(conKey(post("/polizas/{id}/renovar", id)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("poliza-cancelada"));
		}

		@Test
		void sinIpcConfiguradoEs422() throws Exception {
			var id = crear(polizaJson("INDIVIDUAL", "2030-01-01", "1000000"));

			mvc.perform(conKey(post("/polizas/{id}/renovar", id)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("ipc-no-configurado"));
		}
	}

	@Nested
	class Cancelar {

		@Test
		void cancelaPolizaYTodosSusRiesgos() throws Exception {
			var id = crear(polizaJson("COLECTIVA", "2026-01-01", "1000000", "2000000", "3000000"));

			mvc.perform(conKey(post("/polizas/{id}/cancelar", id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"))
				.andExpect(jsonPath("$.fechaCancelacion").exists());
			mvc.perform(conKey(get("/polizas/{id}/riesgos", id)))
				.andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADO"))));
			verify(core, timeout(3000)).notificar(argThat(e -> e.polizaId() == id && "CANCELACION".equals(e.operacion())));

			mvc.perform(conKey(post("/polizas/{id}/cancelar", id)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("poliza-ya-cancelada"));
		}

		@Test
		void polizaInexistenteEs404() throws Exception {
			mvc.perform(conKey(post("/polizas/{id}/cancelar", 999999))).andExpect(status().isNotFound());
		}
	}

	@Nested
	class CoreMock {

		@Test
		void registraElEventoYResponde202() throws Exception {
			mvc.perform(conKey(post("/core-mock/evento")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.estado").value("REGISTRADO"))
				.andExpect(jsonPath("$.polizaId").value(555));
		}

		@Test
		void eventoInvalidoEs400() throws Exception {
			mvc.perform(conKey(post("/core-mock/evento")).contentType(MediaType.APPLICATION_JSON).content("{\"evento\":\"\"}"))
				.andExpect(status().isBadRequest());
		}
	}
}
