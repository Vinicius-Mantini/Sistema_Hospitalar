package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sistemahospitalar.hospital.model.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Quarto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TipoQuartoEnum tipo;

    @Min(1) @Column(nullable = false)
    private int capacidade;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Min(0) @Column(nullable = false)
    private int leitosOcupados;

    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SituacaoQuartoEnum situacao = SituacaoQuartoEnum.DISPONIVEL;

    @Version
    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long versao;

    public boolean temVagaDisponivel() {
        return capacidade > 0 && situacao != SituacaoQuartoEnum.MANUTENCAO && leitosOcupados < capacidade;
    }

    public void ocuparLeito() {
        if (!temVagaDisponivel()) throw new IllegalStateException("Quarto sem vaga disponível.");
        leitosOcupados++;
        situacao = leitosOcupados >= capacidade ? SituacaoQuartoEnum.OCUPADO : SituacaoQuartoEnum.DISPONIVEL;
    }

    public void liberarLeito() {
        if (leitosOcupados == 0) throw new IllegalStateException("Não existe leito ocupado para liberar.");
        leitosOcupados--;
        situacao = SituacaoQuartoEnum.DISPONIVEL;
    }

    public void colocarEmManutencao() {
        if (leitosOcupados > 0) throw new IllegalStateException("Não é possível fazer manutenção com pacientes internados.");
        situacao = SituacaoQuartoEnum.MANUTENCAO;
    }

    public void reativar() {
        if (situacao != SituacaoQuartoEnum.MANUTENCAO) throw new IllegalStateException("Quarto não está em manutenção.");
        situacao = SituacaoQuartoEnum.DISPONIVEL;
    }
}
