package com.sistemahospitalar.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObservacoesConsultaRequest(@NotBlank @Size(max = 1000) String observacoes) {}
