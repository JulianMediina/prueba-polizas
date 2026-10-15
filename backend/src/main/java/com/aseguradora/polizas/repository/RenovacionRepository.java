package com.aseguradora.polizas.repository;

import com.aseguradora.polizas.domain.Renovacion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RenovacionRepository extends JpaRepository<Renovacion, Long> {
}
