package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.Quarto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import com.sistemahospitalar.hospital.model.enums.TipoQuartoEnum;

public interface QuartoRepository extends JpaRepository<Quarto, Long> {
    List<Quarto> findByTipo(TipoQuartoEnum tipo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quarto q where q.id = :id")
    Optional<Quarto> bloquearPorId(@Param("id") Long id);
}
