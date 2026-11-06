package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class HistoricoMedico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @OneToOne(optional = false)
    @JoinColumn(unique = true, nullable = false)
    private Paciente paciente;

    @JsonIgnore @OneToMany
    private List<Atendimento> atendimentos = new ArrayList<>();

    @JsonIgnore @OneToMany
    private List<Internacao> internacoes = new ArrayList<>();

    public void adicionarAtendimento(Atendimento atendimento) {
        if (atendimento != null && !atendimentos.contains(atendimento)) atendimentos.add(atendimento);
    }

    public void adicionarInternacao(Internacao internacao) {
        if (internacao != null && !internacoes.contains(internacao)) internacoes.add(internacao);
    }
}
