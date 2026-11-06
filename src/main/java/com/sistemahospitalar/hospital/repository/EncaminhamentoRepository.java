package com.sistemahospitalar.hospital.repository;

import com.sistemahospitalar.hospital.model.Encaminhamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, Long> {
    List<Encaminhamento> findByClinicoId(Long clinicoId);
}
