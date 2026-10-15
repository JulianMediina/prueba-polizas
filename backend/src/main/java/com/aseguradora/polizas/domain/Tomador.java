package com.aseguradora.polizas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Tomador de la póliza: el arrendatario en la individual; la inmobiliaria o copropiedad en la colectiva. */
@Embeddable
public record Tomador(
		@Column(name = "tomador_tipo_documento", nullable = false) String tipoDocumento,
		@Column(name = "tomador_documento", nullable = false) String documento,
		@Column(name = "tomador_nombre", nullable = false) String nombre,
		@Column(name = "tomador_email") String email,
		@Column(name = "tomador_telefono") String telefono) {
}
