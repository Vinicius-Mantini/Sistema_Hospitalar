package com.sistemahospitalar.hospital.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
public class MedicoClinicoGeral extends ProfissionalSaude {
    // Entidade criada para associar regras de negócio específicas de consultas de rotina
}