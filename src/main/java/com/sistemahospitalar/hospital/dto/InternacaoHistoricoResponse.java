package com.sistemahospitalar.hospital.dto;

import com.sistemahospitalar.hospital.model.enums.SituacaoInternacaoEnum;

import java.time.LocalDateTime;

public record InternacaoHistoricoResponse(
    Long id,
    LocalDateTime dataEntrada,
    LocalDateTime dataSaida,
    SituacaoInternacaoEnum situacao,
    Long quartoId,
    String quartoNumero
) {}
