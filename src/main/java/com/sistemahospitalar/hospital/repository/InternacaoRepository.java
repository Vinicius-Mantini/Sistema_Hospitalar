package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.Internacao;
import com.sistemahospitalar.hospital.model.enums.SituacaoInternacaoEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InternacaoRepository extends JpaRepository<Internacao, Long> {
    boolean existsByPacienteIdAndSituacao(Long pacienteId, SituacaoInternacaoEnum situacao);

    @Query("select i.quarto.id from Internacao i where i.id = :id")
    Optional<Long> buscarQuartoId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Internacao i where i.id = :id")
    Optional<Internacao> bloquearPorId(@Param("id") Long id);
}
