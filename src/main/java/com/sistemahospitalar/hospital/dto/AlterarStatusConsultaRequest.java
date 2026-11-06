package com.sistemahospitalar.hospital.dto;

import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusConsultaRequest(@NotNull StatusAtendimentoEnum status) {
}
