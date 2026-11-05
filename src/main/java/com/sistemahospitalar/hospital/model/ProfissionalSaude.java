package com.sistemahospitalar.hospital.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ProfissionalSaude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 20)
    private String registroConselho; // Ex: CRM

    @Column(length = 20)
    private String telefone;

    @Column(length = 100)
    private String email;
}