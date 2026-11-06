package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sistemahospitalar.hospital.model.enums.StatusAtendimentoEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConsultaRotina.class, name = "ROTINA"),
    @JsonSubTypes.Type(value = ConsultaEspecializada.class, name = "ESPECIALIZADA"),
    @JsonSubTypes.Type(value = AtendimentoEmergencia.class, name = "EMERGENCIA"),
    @JsonSubTypes.Type(value = ProcedimentoCirurgico.class, name = "CIRURGICO")
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Atendimento {
    private static final Map<StatusAtendimentoEnum, Set<StatusAtendimentoEnum>> TRANSICOES = Map.of(
        StatusAtendimentoEnum.AGENDADA, EnumSet.of(StatusAtendimentoEnum.CONFIRMADA, StatusAtendimentoEnum.CANCELADA, StatusAtendimentoEnum.NAO_COMPARECEU),
        StatusAtendimentoEnum.CONFIRMADA, EnumSet.of(StatusAtendimentoEnum.EM_ATENDIMENTO, StatusAtendimentoEnum.CANCELADA, StatusAtendimentoEnum.NAO_COMPARECEU),
        StatusAtendimentoEnum.EM_ATENDIMENTO, EnumSet.of(StatusAtendimentoEnum.CONCLUIDA, StatusAtendimentoEnum.CANCELADA),
        StatusAtendimentoEnum.CONCLUIDA, EnumSet.noneOf(StatusAtendimentoEnum.class),
        StatusAtendimentoEnum.CANCELADA, EnumSet.noneOf(StatusAtendimentoEnum.class),
        StatusAtendimentoEnum.NAO_COMPARECEU, EnumSet.noneOf(StatusAtendimentoEnum.class)
    );

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @ManyToOne(optional = false)
    private Paciente paciente;

    @NotNull @Column(nullable = false)
    private LocalDateTime dataHora;

    @NotNull @ManyToOne(optional = false)
    private ProfissionalSaude profissionalResponsavel;

    @Setter(lombok.AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusAtendimentoEnum status = StatusAtendimentoEnum.AGENDADA;

    public abstract boolean validarCondicoes();

    protected boolean validarCondicoesGerais() {
        return paciente != null && dataHora != null && profissionalResponsavel != null
            && profissionalResponsavel.estaDisponivel();
    }

    public void alterarStatus(StatusAtendimentoEnum novoStatus) {
        if (novoStatus == null) throw new IllegalArgumentException("Status obrigatório.");
        if (novoStatus == status) return;
        if (!TRANSICOES.getOrDefault(status, Set.of()).contains(novoStatus)) {
            throw new IllegalStateException("Transição de " + status + " para " + novoStatus + " não permitida.");
        }
        status = novoStatus;
    }

    // Uso interno das modalidades cujo ciclo tem mais detalhes do que o ciclo de consultas.
    protected void iniciarAtendimento() {
        if (status != StatusAtendimentoEnum.AGENDADA) {
            throw new IllegalStateException("Atendimento não está aguardando início.");
        }
        alterarStatus(StatusAtendimentoEnum.CONFIRMADA);
        alterarStatus(StatusAtendimentoEnum.EM_ATENDIMENTO);
    }

    protected void concluirAtendimento() {
        alterarStatus(StatusAtendimentoEnum.CONCLUIDA);
    }
}
