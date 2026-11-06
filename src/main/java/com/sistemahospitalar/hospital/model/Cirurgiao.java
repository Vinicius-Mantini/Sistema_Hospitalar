package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Cirurgiao extends ProfissionalSaude {
    @NotBlank @Column(nullable = false, length = 100)
    private String areaCirurgica;

    public void registrarProcedimento(ProcedimentoCirurgico procedimento) {
        if (procedimento == null) throw new IllegalArgumentException("Procedimento obrigatório.");
        procedimento.setProfissionalResponsavel(this);
    }
}
