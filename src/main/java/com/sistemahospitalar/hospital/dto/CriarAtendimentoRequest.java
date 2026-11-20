package com.sistemahospitalar.hospital.dto;

import com.sistemahospitalar.hospital.model.enums.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CriarAtendimentoRequest(
    @NotNull TipoAtendimentoEnum tipo,
    @NotNull Long pacienteId,
    @NotNull Long profissionalId,
    @NotNull LocalDateTime dataHora,
    String motivo,
    String especialidade,
    String observacoesPreOperatorias,
    String observacoesPosOperatorias,
    String descricao,
    String tipoCirurgia,
    PrioridadeEnum prioridade,
    Long salaCirurgicaId,
    LocalDateTime fimPrevisto
) {
    public CriarAtendimentoRequest(TipoAtendimentoEnum tipo, Long pacienteId, Long profissionalId,
            LocalDateTime dataHora, String motivo, String especialidade, String observacoesPreOperatorias,
            String observacoesPosOperatorias, String descricao, String tipoCirurgia, PrioridadeEnum prioridade) {
        this(tipo, pacienteId, profissionalId, dataHora, motivo, especialidade, observacoesPreOperatorias,
            observacoesPosOperatorias, descricao, tipoCirurgia, prioridade, null, null);
    }
}
