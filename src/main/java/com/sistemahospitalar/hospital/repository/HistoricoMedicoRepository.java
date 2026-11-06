package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.HistoricoMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HistoricoMedicoRepository extends JpaRepository<HistoricoMedico, Long> {
    Optional<HistoricoMedico> findByPacienteId(Long pacienteId);
}
