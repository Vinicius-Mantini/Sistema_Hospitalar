package com.sistemahospitalar.hospital.dto;

import jakarta.validation.constraints.*;

public record CriarEncaminhamentoRequest(
    @NotNull Long pacienteId,
    @NotNull Long especialistaId,
    @NotBlank @Size(max = 500) String motivo
) {}
