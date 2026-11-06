package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sistemahospitalar.hospital.model.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class ProcedimentoCirurgico extends Atendimento {
    @NotBlank @Column(nullable = false, length = 200)
    private String descricao;
    @NotBlank @Column(nullable = false, length = 100)
    private String tipoCirurgia;
    @Size(max = 1000)
    private String observacoesPreOperatorias;
    @Size(max = 1000)
    private String observacoesPosOperatorias;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime dataHoraEfetiva;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusProcedimentoEnum statusProcedimento = StatusProcedimentoEnum.AGENDADO;

    @Override
    public boolean validarCondicoes() {
        return validarCondicoesGerais() && getProfissionalResponsavel() instanceof Cirurgiao
            && descricao != null && !descricao.isBlank()
            && tipoCirurgia != null && !tipoCirurgia.isBlank();
    }

    public void iniciar() {
        if (statusProcedimento != StatusProcedimentoEnum.AGENDADO) {
            throw new IllegalStateException("Apenas procedimentos agendados podem ser iniciados.");
        }
        iniciarAtendimento();
        statusProcedimento = StatusProcedimentoEnum.EM_ANDAMENTO;
        dataHoraEfetiva = LocalDateTime.now();
    }

    public void concluir() {
        if (statusProcedimento != StatusProcedimentoEnum.EM_ANDAMENTO) {
            throw new IllegalStateException("Procedimento só pode ser concluído após ser iniciado.");
        }
        concluirAtendimento();
        statusProcedimento = StatusProcedimentoEnum.CONCLUIDO;
    }

    public void cancelar() {
        if (statusProcedimento == StatusProcedimentoEnum.CONCLUIDO || statusProcedimento == StatusProcedimentoEnum.CANCELADO) {
            throw new IllegalStateException("Não é possível cancelar um procedimento finalizado.");
        }
        alterarStatus(StatusAtendimentoEnum.CANCELADA);
        statusProcedimento = StatusProcedimentoEnum.CANCELADO;
    }
}
