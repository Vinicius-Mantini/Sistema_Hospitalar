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
public class ConsultaRotina extends Atendimento {

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(length = 500)
    private String observacoes;

    @Override
    public boolean validarCondicoes() {
        return getProfissionalResponsavel() instanceof MedicoClinicoGeral;
    }
}