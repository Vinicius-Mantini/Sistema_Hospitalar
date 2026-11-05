package com.sistemahospitalar.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
public class ProcedimentoCirurgico extends Atendimento {

    @Column(nullable = false, length = 100)
    private String tipoCirurgia;

    @Column(length = 500)
    private String observacoesPreOperatorias;

    @Column(length = 500)
    private String observacoesPosOperatorias;

    private LocalDateTime dataHoraEfetiva;

    @Override
    public boolean validarCondicoes() {
        return getProfissionalResponsavel() instanceof Cirurgiao;
    }
}