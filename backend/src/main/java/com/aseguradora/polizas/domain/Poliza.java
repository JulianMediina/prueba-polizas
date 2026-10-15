package com.aseguradora.polizas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Agregado raíz. Todas las reglas de negocio sobre pólizas y riesgos viven aquí:
 * <ul>
 * <li>Una póliza individual tiene exactamente un riesgo; una colectiva, uno o más.</li>
 * <li>Solo se agregan riesgos a pólizas colectivas que no estén canceladas.</li>
 * <li>Una póliza cancelada no se renueva.</li>
 * <li>Cancelar la póliza cancela todos sus riesgos.</li>
 * <li>Canon de la póliza = suma de cánones de los riesgos activos; prima = canon × meses de vigencia.</li>
 * </ul>
 */
@Entity
@Table(name = "poliza")
public class Poliza {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, updatable = false)
	private String numero;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TipoPoliza tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoPoliza estado;

	@Embedded
	private Tomador tomador;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;

	@Column(name = "meses_vigencia", nullable = false)
	private int mesesVigencia;

	@Column(name = "canon_mensual", nullable = false, precision = 15, scale = 2)
	private BigDecimal canonMensual;

	@Column(nullable = false, precision = 17, scale = 2)
	private BigDecimal prima;

	@Column(name = "fecha_cancelacion")
	private LocalDate fechaCancelacion;

	@Version
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "poliza", cascade = CascadeType.PERSIST)
	@OrderBy("id")
	private List<Riesgo> riesgos = new ArrayList<>();

	protected Poliza() {
		// Requerido por JPA
	}

	private Poliza(String numero, TipoPoliza tipo, Tomador tomador, LocalDate fechaInicio, int mesesVigencia) {
		this.numero = Objects.requireNonNull(numero, "numero");
		this.tipo = Objects.requireNonNull(tipo, "tipo");
		this.tomador = Objects.requireNonNull(tomador, "tomador");
		this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
		if (mesesVigencia < 1 || mesesVigencia > 60) {
			throw new ReglaNegocioException("vigencia-invalida", "La vigencia debe estar entre 1 y 60 meses");
		}
		this.mesesVigencia = mesesVigencia;
		this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
		this.estado = EstadoPoliza.ACTIVA;
	}

	/**
	 * Crea una póliza con sus riesgos iniciales. La individual exige exactamente un riesgo
	 * y la colectiva al menos uno.
	 */
	public static Poliza crear(String numero, TipoPoliza tipo, Tomador tomador, LocalDate fechaInicio,
			int mesesVigencia, List<DatosRiesgo> riesgosIniciales) {
		var poliza = new Poliza(numero, tipo, tomador, fechaInicio, mesesVigencia);
		var datos = riesgosIniciales == null ? List.<DatosRiesgo>of() : riesgosIniciales;
		if (tipo == TipoPoliza.INDIVIDUAL && datos.size() != 1) {
			throw new ReglaNegocioException("riesgo-unico-individual",
					"Una póliza individual debe tener exactamente un riesgo");
		}
		if (tipo == TipoPoliza.COLECTIVA && datos.isEmpty()) {
			throw new ReglaNegocioException("colectiva-sin-riesgos", "Una póliza colectiva debe tener al menos un riesgo");
		}
		datos.forEach(d -> poliza.riesgos.add(new Riesgo(poliza, d, fechaInicio)));
		poliza.recalcularValores();
		return poliza;
	}

	/** Agrega un riesgo. Solo aplica a pólizas colectivas no canceladas. */
	public Riesgo agregarRiesgo(DatosRiesgo datos, LocalDate fecha) {
		validarNoCancelada("agregar riesgos a");
		if (tipo != TipoPoliza.COLECTIVA) {
			throw new ReglaNegocioException("riesgo-unico-individual",
					"Solo se pueden agregar riesgos a pólizas colectivas; una póliza individual tiene un único riesgo");
		}
		var riesgo = new Riesgo(this, datos, fecha);
		riesgos.add(riesgo);
		recalcularValores();
		return riesgo;
	}

	/**
	 * Cancela un riesgo de la póliza. El riesgo único de una individual no se cancela por separado (se cancela la
	 * póliza), y una colectiva no puede quedar sin riesgos activos.
	 */
	public void cancelarRiesgo(Riesgo riesgo, LocalDate fecha) {
		if (!riesgos.contains(riesgo)) {
			throw new IllegalArgumentException("El riesgo no pertenece a la póliza " + numero);
		}
		if (!riesgo.isActivo()) {
			throw new ConflictoEstadoException("riesgo-ya-cancelado",
					"El riesgo %s ya está cancelado".formatted(riesgo.getId()));
		}
		if (tipo == TipoPoliza.INDIVIDUAL) {
			throw new ReglaNegocioException("riesgo-individual-no-cancelable",
					"El riesgo de una póliza individual no se cancela por separado; cancele la póliza");
		}
		if (riesgosActivos().size() == 1) {
			throw new ReglaNegocioException("ultimo-riesgo-activo",
					"Una póliza colectiva debe conservar al menos un riesgo activo; para retirar el último, cancele la póliza");
		}
		riesgo.cancelar(fecha);
		recalcularValores();
	}

	/** Cancela la póliza y, en cascada, todos sus riesgos activos. Canon y prima se conservan como histórico. */
	public void cancelar(LocalDate fecha) {
		if (estado == EstadoPoliza.CANCELADA) {
			throw new ConflictoEstadoException("poliza-ya-cancelada", "La póliza %s ya está cancelada".formatted(numero));
		}
		riesgosActivos().forEach(r -> r.cancelar(fecha));
		this.estado = EstadoPoliza.CANCELADA;
		this.fechaCancelacion = fecha;
	}

	/** Una póliza cancelada no se puede renovar. */
	public void validarRenovable() {
		validarNoCancelada("renovar");
	}

	/** Año cuyo IPC se aplica a la siguiente renovación: el anterior al inicio de la nueva vigencia. */
	public int anioIpcParaRenovar() {
		return fechaFin.getYear() - 1;
	}

	/**
	 * Renueva por el mismo periodo de vigencia: la nueva vigencia empieza donde termina la actual, el canon de cada
	 * riesgo activo se incrementa en el IPC y se recalculan canon y prima. El estado pasa a RENOVADA.
	 */
	public Renovacion renovar(Ipc ipc) {
		validarRenovable();
		Objects.requireNonNull(ipc, "ipc");
		if (ipc.getAnio() != anioIpcParaRenovar()) {
			throw new IllegalArgumentException(
					"Se esperaba el IPC de %d y se recibió el de %d".formatted(anioIpcParaRenovar(), ipc.getAnio()));
		}
		var canonAnterior = canonMensual;
		var primaAnterior = prima;
		var inicioAnterior = fechaInicio;
		var finAnterior = fechaFin;

		var factor = ipc.factor();
		riesgosActivos().forEach(r -> r.ajustarCanon(factor));
		this.fechaInicio = finAnterior;
		this.fechaFin = finAnterior.plusMonths(mesesVigencia);
		recalcularValores();
		this.estado = EstadoPoliza.RENOVADA;
		return new Renovacion(this, ipc, canonAnterior, primaAnterior, inicioAnterior, finAnterior);
	}

	public List<Riesgo> riesgosActivos() {
		return riesgos.stream().filter(Riesgo::isActivo).toList();
	}

	private void recalcularValores() {
		this.canonMensual = riesgosActivos().stream()
			.map(Riesgo::getCanonMensual)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.setScale(2, RoundingMode.HALF_EVEN);
		this.prima = canonMensual.multiply(BigDecimal.valueOf(mesesVigencia)).setScale(2, RoundingMode.HALF_EVEN);
	}

	private void validarNoCancelada(String accion) {
		if (estado == EstadoPoliza.CANCELADA) {
			throw new ConflictoEstadoException("poliza-cancelada",
					"No se puede %s una póliza cancelada (%s)".formatted(accion, numero));
		}
	}

	public boolean isCancelada() {
		return estado == EstadoPoliza.CANCELADA;
	}

	public Long getId() {
		return id;
	}

	public String getNumero() {
		return numero;
	}

	public TipoPoliza getTipo() {
		return tipo;
	}

	public EstadoPoliza getEstado() {
		return estado;
	}

	public Tomador getTomador() {
		return tomador;
	}

	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	public LocalDate getFechaFin() {
		return fechaFin;
	}

	public int getMesesVigencia() {
		return mesesVigencia;
	}

	public BigDecimal getCanonMensual() {
		return canonMensual;
	}

	public BigDecimal getPrima() {
		return prima;
	}

	public LocalDate getFechaCancelacion() {
		return fechaCancelacion;
	}

	public long getVersion() {
		return version;
	}

	public List<Riesgo> getRiesgos() {
		return Collections.unmodifiableList(riesgos);
	}
}
