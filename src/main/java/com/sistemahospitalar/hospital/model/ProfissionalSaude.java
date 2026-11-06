package com.sistemahospitalar.hospital.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MedicoClinicoGeral.class, name = "CLINICO_GERAL"),
    @JsonSubTypes.Type(value = MedicoEspecialista.class, name = "ESPECIALISTA"),
    @JsonSubTypes.Type(value = Cirurgiao.class, name = "CIRURGIAO")
})
@Entity
@Getter @Setter @NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ProfissionalSaude {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 100)
    private String nome;
    @NotBlank @Column(nullable = false, unique = true, length = 30)
    private String registroConselho;
    @NotBlank @Size(max = 20)
    private String telefone;
    @Email @Size(max = 120)
    private String email;
    @Column(nullable = false)
    private boolean ativo = true;

    public boolean estaDisponivel() {
        return ativo;
    }
}
