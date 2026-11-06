package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class MedicoEspecialista extends ProfissionalSaude {
    @NotBlank @Column(nullable = false, length = 100)
    private String especialidade;
}
