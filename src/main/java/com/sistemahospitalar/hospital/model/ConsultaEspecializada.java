package com.sistemahospitalar.hospital.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
public class ConsultaEspecializada extends Atendimento {

    @Override
    public boolean validarCondicoes() {
        return getProfissionalResponsavel() instanceof MedicoEspecialista;
    }
}