package com.aseguradora.polizas.exception;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import com.aseguradora.polizas.domain.ConflictoEstadoException;
import com.aseguradora.polizas.domain.DominioException;
import com.aseguradora.polizas.domain.RecursoNoEncontradoException;
import com.aseguradora.polizas.domain.ReglaNegocioException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce excepciones a respuestas RFC 9457 (ProblemDetail). Nunca expone stack traces ni detalles internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String TIPO_BASE = "urn:polizas:error:";

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex);
	}

	@ExceptionHandler(ConflictoEstadoException.class)
	ProblemDetail conflicto(ConflictoEstadoException ex) {
		return problema(HttpStatus.CONFLICT, "Conflicto con el estado actual", ex);
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ProblemDetail reglaNegocio(ReglaNegocioException ex) {
		return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida", ex);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail edicionConcurrente(OptimisticLockingFailureException ex) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"El recurso fue modificado por otra operación; consulte de nuevo e intente otra vez");
		problem.setTitle("Edición concurrente");
		problem.setType(URI.create(TIPO_BASE + "edicion-concurrente"));
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException ex) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Valor inválido para el parámetro '%s'".formatted(ex.getName()));
		problem.setTitle("Parámetro inválido");
		problem.setType(URI.create(TIPO_BASE + "parametro-invalido"));
		var tipo = ex.getRequiredType();
		if (tipo != null && tipo.isEnum()) {
			problem.setProperty("valoresPermitidos", tipo.getEnumConstants());
		}
		return problem;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail inesperado(Exception ex) {
		log.error("Error no controlado", ex);
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocurrió un error inesperado; use el correlation id para rastrearlo");
		problem.setTitle("Error interno");
		problem.setType(URI.create(TIPO_BASE + "interno"));
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errores = new TreeMap<>();
		ex.getBindingResult()
			.getFieldErrors()
			.forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud tiene campos inválidos");
		problem.setTitle("Validación fallida");
		problem.setType(URI.create(TIPO_BASE + "validacion"));
		problem.setProperty("errores", errores);
		return ResponseEntity.badRequest().body(problem);
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, DominioException ex) {
		var problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
		problem.setTitle(titulo);
		problem.setType(URI.create(TIPO_BASE + ex.getCodigo()));
		problem.setProperty("codigo", ex.getCodigo());
		return problem;
	}
}
