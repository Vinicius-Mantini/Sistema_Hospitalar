package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class Paciente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 120)
    private String nome;
    @NotBlank @Column(nullable = false, unique = true, length = 20)
    private String cpf;
    @NotNull @Past @Column(nullable = false)
    private LocalDate dataNascimento;
    @Size(max = 20)
    private String telefone;
    @Email @Size(max = 120)
    private String email;
}
