package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class ConsultaRotina extends Atendimento {
    @NotBlank @Column(nullable = false, length = 200)
    private String motivo;
    @Size(max = 1000)
    private String observacoes;

    @Override
    public boolean validarCondicoes() {
        return validarCondicoesGerais() && getProfissionalResponsavel() instanceof MedicoClinicoGeral
            && motivo != null && !motivo.isBlank();
    }
}
