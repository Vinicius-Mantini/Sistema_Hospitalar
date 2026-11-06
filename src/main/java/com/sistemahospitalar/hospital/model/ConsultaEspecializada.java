package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class ConsultaEspecializada extends Atendimento {
    @NotBlank @Column(nullable = false, length = 100)
    private String especialidade;
    @Size(max = 1000)
    private String observacoes;

    @Override
    public boolean validarCondicoes() {
        return validarCondicoesGerais() && getProfissionalResponsavel() instanceof MedicoEspecialista medico
            && especialidade != null && !especialidade.isBlank()
            && medico.getEspecialidade() != null
            && medico.getEspecialidade().trim().equalsIgnoreCase(especialidade.trim());
    }
}
