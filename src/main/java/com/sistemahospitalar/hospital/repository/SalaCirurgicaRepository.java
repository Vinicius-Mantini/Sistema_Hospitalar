package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.SalaCirurgica;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalaCirurgicaRepository extends JpaRepository<SalaCirurgica, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SalaCirurgica s where s.id = :id")
    Optional<SalaCirurgica> bloquearPorId(@Param("id") Long id);
}
