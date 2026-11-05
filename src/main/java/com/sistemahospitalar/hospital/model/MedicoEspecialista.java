package com.sistemahospitalar.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
public class MedicoEspecialista extends ProfissionalSaude {

    @Column(nullable = false, length = 100)
    private String especialidade;

}