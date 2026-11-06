package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sistemahospitalar.hospital.model.enums.SituacaoInternacaoEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Internacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @ManyToOne(optional = false)
    private Paciente paciente;
    @NotNull @ManyToOne(optional = false)
    private Quarto quarto;
    @NotNull @Column(nullable = false)
    private LocalDateTime dataEntrada;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime dataAlta;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SituacaoInternacaoEnum situacao = SituacaoInternacaoEnum.ATIVA;

    @Size(max = 500)
    private String observacoes;

    public void registrarAlta() {
        if (situacao != SituacaoInternacaoEnum.ATIVA) {
            throw new IllegalStateException("Somente internações ativas podem receber alta.");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (dataEntrada != null && agora.isBefore(dataEntrada)) {
            throw new IllegalStateException("Alta não pode ocorrer antes da entrada.");
        }
        dataAlta = agora;
        situacao = SituacaoInternacaoEnum.ALTA;
    }

    public void cancelar() {
        if (situacao != SituacaoInternacaoEnum.ATIVA) {
            throw new IllegalStateException("Somente internações ativas podem ser canceladas.");
        }
        situacao = SituacaoInternacaoEnum.CANCELADA;
    }
}
