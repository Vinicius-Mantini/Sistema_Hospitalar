package com.sistemahospitalar.hospital.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class MedicoClinicoGeral extends ProfissionalSaude {
    public Encaminhamento encaminharPaciente(Paciente paciente, MedicoEspecialista especialista, String motivo) {
        if (paciente == null) throw new IllegalArgumentException("Paciente obrigatório para encaminhamento.");
        if (especialista == null) throw new IllegalArgumentException("Especialista obrigatório para encaminhamento.");
        if (!estaDisponivel() || !especialista.estaDisponivel()) {
            throw new IllegalStateException("Profissional inativo não pode participar do encaminhamento.");
        }
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("Motivo do encaminhamento obrigatório.");
        Encaminhamento encaminhamento = new Encaminhamento();
        encaminhamento.setPaciente(paciente);
        encaminhamento.setClinico(this);
        encaminhamento.setEspecialista(especialista);
        encaminhamento.setMotivo(motivo);
        return encaminhamento;
    }
}
