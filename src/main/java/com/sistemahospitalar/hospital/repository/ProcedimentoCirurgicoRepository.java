package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.ProcedimentoCirurgico;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProcedimentoCirurgicoRepository extends JpaRepository<ProcedimentoCirurgico, Long> {
    @Query("select p from ProcedimentoCirurgico p where p.profissionalResponsavel.id = :profissionalId")
    List<ProcedimentoCirurgico> findDoCirurgiao(@Param("profissionalId") Long profissionalId);

    @Query("""
        select count(p) > 0 from ProcedimentoCirurgico p
        where p.salaCirurgica.id = :salaId and p.status in :estados
          and p.dataHora < :fim and p.fimPrevisto > :inicio
        """)
    boolean existeConflitoSala(@Param("salaId") Long salaId, @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim, @Param("estados") Collection<StatusAtendimentoEnum> estados);
}
