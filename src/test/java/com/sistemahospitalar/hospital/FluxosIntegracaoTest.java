package com.sistemahospitalar.hospital;

import com.sistemahospitalar.hospital.dto.*;
import com.sistemahospitalar.hospital.model.*;
import com.sistemahospitalar.hospital.model.enums.*;
import com.sistemahospitalar.hospital.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FluxosIntegracaoTest {
    @Autowired PacienteService pacientes;
    @Autowired ProfissionalSaudeService profissionais;
    @Autowired AtendimentoService atendimentos;
    @Autowired QuartoService quartos;
    @Autowired InternacaoService internacoes;
    @Autowired HistoricoMedicoService historicos;

    private Paciente novoPaciente(String cpf) {
        Paciente paciente = new Paciente();
        paciente.setNome("Paciente de teste");
        paciente.setCpf(cpf);
        paciente.setDataNascimento(LocalDate.of(1995, 5, 14));
        return pacientes.salvar(paciente);
    }

    private MedicoClinicoGeral clinico(String registro) {
        MedicoClinicoGeral medico = new MedicoClinicoGeral();
        medico.setNome("Clínico de teste");
        medico.setRegistroConselho(registro);
        medico.setTelefone("31999990000");
        return (MedicoClinicoGeral) profissionais.salvar(medico);
    }

    @Test
    void buscaEspecialistaEEncaminhamentoFuncionam() {
        Paciente paciente = novoPaciente("00100100101");
        MedicoClinicoGeral geral = clinico("CRM-TESTE-01");
        MedicoEspecialista especialista = new MedicoEspecialista();
        especialista.setNome("Especialista de teste");
        especialista.setRegistroConselho("CRM-TESTE-02");
        especialista.setTelefone("31988880000");
        especialista.setEspecialidade("Cardiologia");
        especialista = (MedicoEspecialista) profissionais.salvar(especialista);
        assertEquals(1, profissionais.porEspecialidade("cardiologia").size());
        var encaminhamento = profissionais.encaminhar(geral.getId(),
            new CriarEncaminhamentoRequest(paciente.getId(), especialista.getId(), "Avaliação cardíaca"));
        assertNotNull(encaminhamento.getId());
        assertEquals(1, profissionais.listarEncaminhamentos(geral.getId()).size());
    }

    @Test
    void consultaTemEstadosEHistoricoConsistentes() {
        Paciente paciente = novoPaciente("00100100102");
        MedicoClinicoGeral medico = clinico("CRM-TESTE-03");
        LocalDateTime data = LocalDateTime.now().plusDays(2).withNano(0);
        CriarAtendimentoRequest requisicao = new CriarAtendimentoRequest(
            TipoAtendimentoEnum.ROTINA, paciente.getId(), medico.getId(), data,
            "Avaliação de rotina", null, null, null, null, null, null);
        Atendimento consulta = atendimentos.criar(requisicao);
        assertEquals(StatusAtendimentoEnum.AGENDADA, consulta.getStatus());
        assertThrows(IllegalStateException.class, () -> atendimentos.criar(requisicao));
        atendimentos.alterarStatus(consulta.getId(), StatusAtendimentoEnum.CONFIRMADA);
        atendimentos.alterarStatus(consulta.getId(), StatusAtendimentoEnum.EM_ATENDIMENTO);
        atendimentos.alterarStatus(consulta.getId(), StatusAtendimentoEnum.CONCLUIDA);
        atendimentos.registrarObservacoes(consulta.getId(), "Paciente orientado.");
        assertEquals(1, historicos.consultar(paciente.getId()).atendimentosIds().size());
        assertThrows(IllegalStateException.class,
            () -> atendimentos.alterarStatus(consulta.getId(), StatusAtendimentoEnum.AGENDADA));
    }

    @Test
    void cirurgiaRespeitaInicioEConclusao() {
        Paciente paciente = novoPaciente("00100100103");
        Cirurgiao cirurgiao = new Cirurgiao();
        cirurgiao.setNome("Cirurgião de teste");
        cirurgiao.setRegistroConselho("CRM-TESTE-04");
        cirurgiao.setTelefone("31977770000");
        cirurgiao.setAreaCirurgica("Ortopedia");
        cirurgiao = (Cirurgiao) profissionais.salvar(cirurgiao);
        Atendimento cirurgia = atendimentos.criar(new CriarAtendimentoRequest(
            TipoAtendimentoEnum.CIRURGICO, paciente.getId(), cirurgiao.getId(),
            LocalDateTime.now().plusDays(3), null, null, "Jejum", null,
            "Artroscopia", "Ortopédica", null));
        assertThrows(IllegalStateException.class, () -> atendimentos.concluirCirurgia(cirurgia.getId()));
        atendimentos.iniciarCirurgia(cirurgia.getId());
        ProcedimentoCirurgico concluida = atendimentos.concluirCirurgia(cirurgia.getId());
        assertEquals(StatusProcedimentoEnum.CONCLUIDO, concluida.getStatusProcedimento());
        assertNotNull(concluida.getDataHoraEfetiva());
        assertEquals(1, profissionais.cirurgiasDoProfissional(cirurgiao.getId()).size());
    }

    @Test
    void emergenciaExigeInicio() {
        Paciente paciente = novoPaciente("00100100104");
        MedicoClinicoGeral medico = clinico("CRM-TESTE-05");
        Atendimento emergencia = atendimentos.criar(new CriarAtendimentoRequest(
            TipoAtendimentoEnum.EMERGENCIA, paciente.getId(), medico.getId(),
            LocalDateTime.now(), null, null, null, null, null, null, PrioridadeEnum.ALTA));
        assertThrows(IllegalStateException.class, () -> atendimentos.concluirEmergencia(emergencia.getId()));
        atendimentos.iniciarEmergencia(emergencia.getId());
        assertEquals(StatusEmergenciaEnum.CONCLUIDO,
            atendimentos.concluirEmergencia(emergencia.getId()).getStatusEmergencia());
    }

    @Test
    void altaLiberaLeitoEPermiteNovaInternacao() {
        Paciente paciente = novoPaciente("00100100105");
        Quarto quarto = new Quarto();
        quarto.setNumero("UTI-T01");
        quarto.setTipo(TipoQuartoEnum.UTI);
        quarto.setCapacidade(1);
        quarto = quartos.salvar(quarto);
        Internacao primeira = internacoes.criar(new CriarInternacaoRequest(paciente.getId(), quarto.getId(), null, null));
        assertEquals(1, quartos.buscar(quarto.getId()).getLeitosOcupados());
        assertThrows(IllegalStateException.class,
            () -> internacoes.criar(new CriarInternacaoRequest(paciente.getId(), primeira.getQuarto().getId(), null, null)));
        Internacao alta = internacoes.registrarAlta(primeira.getId());
        assertEquals(SituacaoInternacaoEnum.ALTA, alta.getSituacao());
        assertEquals(0, quartos.buscar(quarto.getId()).getLeitosOcupados());
        assertThrows(IllegalStateException.class, () -> internacoes.registrarAlta(primeira.getId()));
        assertNotNull(internacoes.criar(new CriarInternacaoRequest(paciente.getId(), quarto.getId(), null, null)).getId());
        assertEquals(2, historicos.consultar(paciente.getId()).internacoesIds().size());
    }
}
