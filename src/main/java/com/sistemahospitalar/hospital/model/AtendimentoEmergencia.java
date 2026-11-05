package com.sistemahospitalar.hospital.model;

import com.sistemahospitalar.hospital.model.enums.PrioridadeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
public class AtendimentoEmergencia extends Atendimento {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeEnum prioridade;

    @Override
    public boolean validarCondicoes() {
        return prioridade != null;
    }
}