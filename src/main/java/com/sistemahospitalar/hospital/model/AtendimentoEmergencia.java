package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sistemahospitalar.hospital.model.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class AtendimentoEmergencia extends Atendimento {
    @NotNull
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PrioridadeEnum prioridade;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusEmergenciaEnum statusEmergencia = StatusEmergenciaEnum.AGUARDANDO;

    @Override
    public boolean validarCondicoes() {
        return validarCondicoesGerais() && prioridade != null;
    }

    public void iniciar() {
        if (statusEmergencia != StatusEmergenciaEnum.AGUARDANDO) {
            throw new IllegalStateException("Emergência não está aguardando avaliação.");
        }
        iniciarAtendimento();
        statusEmergencia = StatusEmergenciaEnum.INICIADO;
    }

    public void concluir() {
        if (statusEmergencia != StatusEmergenciaEnum.INICIADO) {
            throw new IllegalStateException("Emergência precisa ser iniciada antes da conclusão.");
        }
        concluirAtendimento();
        statusEmergencia = StatusEmergenciaEnum.CONCLUIDO;
    }
}
