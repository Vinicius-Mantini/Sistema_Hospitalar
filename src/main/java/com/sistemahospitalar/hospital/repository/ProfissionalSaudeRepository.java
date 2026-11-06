package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.ProfissionalSaude;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfissionalSaudeRepository extends JpaRepository<ProfissionalSaude, Long> {
    List<ProfissionalSaude> findByNomeContainingIgnoreCase(String nome);
}
