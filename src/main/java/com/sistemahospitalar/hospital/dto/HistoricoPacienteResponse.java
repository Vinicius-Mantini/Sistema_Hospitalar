package com.sistemahospitalar.hospital.dto;

import java.util.List;

public record HistoricoPacienteResponse(
    Long pacienteId,
    List<Long> atendimentosIds,
    List<Long> internacoesIds
) {}
