package com.sistemahospitalar.hospital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
public class Cirurgiao extends ProfissionalSaude {

    @Column(nullable = false, length = 50)
    private String registroCirurgico; 

}