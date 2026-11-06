package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.Atendimento;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
    boolean existsByProfissionalResponsavelIdAndDataHoraAndStatusIn(
        Long profissionalId, LocalDateTime dataHora, Collection<StatusAtendimentoEnum> status);
}
