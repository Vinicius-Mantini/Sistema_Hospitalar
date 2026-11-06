package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Encaminhamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @ManyToOne(optional = false)
    private Paciente paciente;
    @NotNull @ManyToOne(optional = false)
    private MedicoClinicoGeral clinico;
    @NotNull @ManyToOne(optional = false)
    private MedicoEspecialista especialista;
    @NotBlank @Column(nullable = false, length = 500)
    private String motivo;
    @NotNull @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();
}
