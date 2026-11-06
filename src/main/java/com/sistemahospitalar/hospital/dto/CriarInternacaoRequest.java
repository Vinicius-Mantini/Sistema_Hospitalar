package com.sistemahospitalar.hospital.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CriarInternacaoRequest(
    @NotNull Long pacienteId,
    @NotNull Long quartoId,
    LocalDateTime dataEntrada,
    String observacoes
) {}
