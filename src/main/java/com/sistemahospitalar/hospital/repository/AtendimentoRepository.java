package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.Atendimento;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    // Atendimentos sem fim previsto reservam apenas o instante de início.
    String RESERVA_SOBREPOSTA = """
        select count(a) > 0 from Atendimento a
        where a.status in :estados
          and (a.dataHora = :inicio
            or (a.dataHora > :inicio and a.dataHora < :fim)
            or (a.dataHora < :inicio and a.fimPrevisto > :inicio))
        """;

    @Query(RESERVA_SOBREPOSTA + " and a.profissionalResponsavel.id = :id")
    boolean existeConflitoProfissional(@Param("id") Long profissionalId,
        @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
        @Param("estados") Collection<StatusAtendimentoEnum> estados);

    @Query(RESERVA_SOBREPOSTA + " and a.paciente.id = :id")
    boolean existeConflitoPaciente(@Param("id") Long pacienteId,
        @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
        @Param("estados") Collection<StatusAtendimentoEnum> estados);
}
