package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.ProfissionalSaude;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalSaudeRepository extends JpaRepository<ProfissionalSaude, Long> {
    List<ProfissionalSaude> findByNomeContainingIgnoreCase(String nome);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProfissionalSaude p where p.id = :id")
    Optional<ProfissionalSaude> bloquearPorId(@Param("id") Long id);
}
