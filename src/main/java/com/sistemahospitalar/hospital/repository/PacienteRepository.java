package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.Paciente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.id = :id")
    Optional<Paciente> bloquearPorId(@Param("id") Long id);
}
