package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.ProcedimentoCirurgico;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProcedimentoCirurgicoRepository extends JpaRepository<ProcedimentoCirurgico, Long> {
    @Query("select p from ProcedimentoCirurgico p where p.profissionalResponsavel.id = :profissionalId")
    List<ProcedimentoCirurgico> findDoCirurgiao(@Param("profissionalId") Long profissionalId);
}
