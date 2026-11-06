package com.sistemahospitalar.hospital.model;

import com.sistemahospitalar.hospital.model.enums.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class RegrasDominioTest {
    @Test
    void quartoRespeitaCapacidadeEManutencao() {
        Quarto quarto = new Quarto();
        quarto.setCapacidade(1);
        quarto.setTipo(TipoQuartoEnum.UTI);
        quarto.ocuparLeito();
        assertFalse(quarto.temVagaDisponivel());
        assertThrows(IllegalStateException.class, quarto::ocuparLeito);
        assertThrows(IllegalStateException.class, quarto::colocarEmManutencao);
        quarto.liberarLeito();
        quarto.colocarEmManutencao();
        assertFalse(quarto.temVagaDisponivel());
        quarto.reativar();
        assertTrue(quarto.temVagaDisponivel());
        assertThrows(IllegalStateException.class, quarto::liberarLeito);
    }

    @Test
    void consultaImpossibilitaRetrocessoDeEstadosTerminais() {
        ConsultaRotina consulta = new ConsultaRotina();
        consulta.alterarStatus(StatusAtendimentoEnum.CONFIRMADA);
        consulta.alterarStatus(StatusAtendimentoEnum.EM_ATENDIMENTO);
        consulta.alterarStatus(StatusAtendimentoEnum.CONCLUIDA);
        assertThrows(IllegalStateException.class,
            () -> consulta.alterarStatus(StatusAtendimentoEnum.AGENDADA));

        ConsultaRotina cancelada = new ConsultaRotina();
        cancelada.alterarStatus(StatusAtendimentoEnum.CANCELADA);
        assertThrows(IllegalStateException.class,
            () -> cancelada.alterarStatus(StatusAtendimentoEnum.CONCLUIDA));
    }

    @Test
    void cirurgiaSoPodeSerConcluidaAposInicio() {
        ProcedimentoCirurgico cirurgia = new ProcedimentoCirurgico();
        assertThrows(IllegalStateException.class, cirurgia::concluir);
        cirurgia.iniciar();
        assertEquals(StatusProcedimentoEnum.EM_ANDAMENTO, cirurgia.getStatusProcedimento());
        assertNotNull(cirurgia.getDataHoraEfetiva());
        cirurgia.concluir();
        assertEquals(StatusProcedimentoEnum.CONCLUIDO, cirurgia.getStatusProcedimento());
        assertThrows(IllegalStateException.class, cirurgia::iniciar);
        assertThrows(IllegalStateException.class, cirurgia::cancelar);
    }

    @Test
    void emergenciaExigeIniciarAntesDeConcluir() {
        AtendimentoEmergencia emergencia = new AtendimentoEmergencia();
        assertThrows(IllegalStateException.class, emergencia::concluir);
        emergencia.iniciar();
        assertEquals(StatusEmergenciaEnum.INICIADO, emergencia.getStatusEmergencia());
        emergencia.concluir();
        assertEquals(StatusEmergenciaEnum.CONCLUIDO, emergencia.getStatusEmergencia());
        assertThrows(IllegalStateException.class, emergencia::iniciar);
    }

    @Test
    void consultaEspecializadaExigeEspecialidadeCompativelSemNullPointer() {
        ConsultaEspecializada consulta = new ConsultaEspecializada();
        MedicoEspecialista especialista = new MedicoEspecialista();
        consulta.setProfissionalResponsavel(especialista);
        consulta.setPaciente(new Paciente());
        consulta.setDataHora(LocalDateTime.now().plusDays(1));
        consulta.setEspecialidade("Neurologia");
        assertFalse(consulta.validarCondicoes());
        especialista.setEspecialidade("Cardiologia");
        assertFalse(consulta.validarCondicoes());
        especialista.setEspecialidade("Neurologia");
        assertTrue(consulta.validarCondicoes());
    }

    @Test
    void internacaoNaoAdmiteAltaDuplicada() {
        Internacao internacao = new Internacao();
        internacao.setDataEntrada(LocalDateTime.now().minusDays(1));
        internacao.registrarAlta();
        assertEquals(SituacaoInternacaoEnum.ALTA, internacao.getSituacao());
        assertNotNull(internacao.getDataAlta());
        assertThrows(IllegalStateException.class, internacao::registrarAlta);
        assertThrows(IllegalStateException.class, internacao::cancelar);
    }
}
