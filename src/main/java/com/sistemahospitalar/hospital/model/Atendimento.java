package com.sistemahospitalar.hospital.model;

import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomePaciente;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimentoEnum status = StatusAtendimentoEnum.AGENDADA;

    @ManyToOne
    @JoinColumn(name = "profissional_id", nullable = false)
    private ProfissionalSaude profissionalResponsavel;

    // Método que obriga cada tipo de atendimento a ter sua própria regra de validação
    public abstract boolean validarCondicoes();
}