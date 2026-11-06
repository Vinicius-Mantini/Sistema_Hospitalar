package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.MedicoEspecialista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicoEspecialistaRepository extends JpaRepository<MedicoEspecialista, Long> {
    List<MedicoEspecialista> findByEspecialidadeIgnoreCaseAndAtivoTrue(String especialidade);
}
