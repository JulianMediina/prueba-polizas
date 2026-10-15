package com.aseguradora.polizas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Riesgo asegurado: un inmueble arrendado con su arrendatario (asegurado) y su arrendador (beneficiario).
 * Su ciclo de vida lo controla la {@link Poliza}; por eso los métodos que cambian estado son de paquete.
 */
@Entity
@Table(name = "riesgo")
public class Riesgo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "poliza_id", nullable = false, updatable = false)
	private Poliza poliza;

	@Column(nullable = false)
	private String direccion;

	@Column(nullable = false)
	private String ciudad;

	@Column(name = "arrendatario_documento", nullable = false)
	private String arrendatarioDocumento;

	@Column(name = "arrendatario_nombre", nullable = false)
	private String arrendatarioNombre;

	@Column(name = "arrendador_documento", nullable = false)
	private String arrendadorDocumento;

	@Column(name = "arrendador_nombre", nullable = false)
	private String arrendadorNombre;

	@Column(name = "canon_mensual", nullable = false, precision = 15, scale = 2)
	private BigDecimal canonMensual;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoRiesgo estado;

	@Column(name = "fecha_inclusion", nullable = false)
	private LocalDate fechaInclusion;

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

	protected Riesgo() {
		// Requerido por JPA
	}

	Riesgo(Poliza poliza, DatosRiesgo datos, LocalDate fechaInclusion) {
		Objects.requireNonNull(datos, "datos del riesgo");
		if (datos.canonMensual() == null || datos.canonMensual().signum() <= 0) {
			throw new ReglaNegocioException("canon-invalido", "El canon mensual del riesgo debe ser mayor que cero");
		}
		this.poliza = poliza;
		this.direccion = datos.direccion();
		this.ciudad = datos.ciudad();
		this.arrendatarioDocumento = datos.arrendatarioDocumento();
		this.arrendatarioNombre = datos.arrendatarioNombre();
		this.arrendadorDocumento = datos.arrendadorDocumento();
		this.arrendadorNombre = datos.arrendadorNombre();
		this.canonMensual = datos.canonMensual().setScale(2, RoundingMode.HALF_EVEN);
		this.estado = EstadoRiesgo.ACTIVO;
		this.fechaInclusion = fechaInclusion;
	}

	public boolean isActivo() {
		return estado == EstadoRiesgo.ACTIVO;
	}

	void cancelar(LocalDate fecha) {
		if (!isActivo()) {
			throw new ConflictoEstadoException("riesgo-ya-cancelado", "El riesgo %s ya está cancelado".formatted(id));
		}
		this.estado = EstadoRiesgo.CANCELADO;
		this.fechaCancelacion = fecha;
	}

	void ajustarCanon(BigDecimal factor) {
		this.canonMensual = canonMensual.multiply(factor).setScale(2, RoundingMode.HALF_EVEN);
	}

	public Long getId() {
		return id;
	}

	public Poliza getPoliza() {
		return poliza;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getCiudad() {
		return ciudad;
	}

	public String getArrendatarioDocumento() {
		return arrendatarioDocumento;
	}

	public String getArrendatarioNombre() {
		return arrendatarioNombre;
	}

	public String getArrendadorDocumento() {
		return arrendadorDocumento;
	}

	public String getArrendadorNombre() {
		return arrendadorNombre;
	}

	public BigDecimal getCanonMensual() {
		return canonMensual;
	}

	public EstadoRiesgo getEstado() {
		return estado;
	}

	public LocalDate getFechaInclusion() {
		return fechaInclusion;
	}

	public LocalDate getFechaCancelacion() {
		return fechaCancelacion;
	}
}
