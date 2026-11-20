package com.sistemahospitalar.hospital;

import com.sistemahospitalar.hospital.exception.AtendimentoIndisponivelException;
import com.sistemahospitalar.hospital.exception.ProfissionalIncompativelException;
import com.sistemahospitalar.hospital.model.MedicoClinicoGeral;
import com.sistemahospitalar.hospital.model.MedicoEspecialista;
import com.sistemahospitalar.hospital.model.Paciente;
import com.sistemahospitalar.hospital.model.ProfissionalSaude;
import com.sistemahospitalar.hospital.service.AtendimentoService;
import com.sistemahospitalar.hospital.service.PacienteService;
import com.sistemahospitalar.hospital.service.ProfissionalSaudeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:agenda-teste;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AgendaIntegracaoTest {
    private static final AtomicInteger CADASTROS = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired PacienteService pacientes;
    @Autowired ProfissionalSaudeService profissionais;
    @Autowired AtendimentoService atendimentos;

    private LocalDateTime horario;
    private Long paciente;
    private Long clinico;

    @BeforeEach
    void prepararAgenda() {
        horario = LocalDateTime.now().plusDays(2).withNano(0);
        paciente = cadastrarPaciente();
        clinico = cadastrarProfissional(new MedicoClinicoGeral(), true);
    }

    @Test
    void recusaMesmoPacienteComOutroProfissionalNoMesmoHorario() throws Exception {
        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());

        var outroClinico = cadastrarProfissional(new MedicoClinicoGeral(), true);
        criar(rotina(paciente, outroClinico, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("ATENDIMENTO_INDISPONIVEL"))
            .andExpect(resultado -> assertInstanceOf(AtendimentoIndisponivelException.class, resultado.getResolvedException()));
        assertEquals(1, contarAgendamentos(paciente, horario));
    }

    @Test
    void recusaMesmoProfissionalComOutroPacienteNoMesmoHorario() throws Exception {
        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());

        criar(rotina(cadastrarPaciente(), clinico, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("ATENDIMENTO_INDISPONIVEL"));
    }

    @Test
    void recusaRepeticaoDeHorarioComFracaoDeSegundo() throws Exception {
        var horarioPreciso = horario.withNano(123_456_789);
        criar(rotina(paciente, clinico, horarioPreciso)).andExpect(status().isCreated());

        criar(rotina(cadastrarPaciente(), clinico, horarioPreciso))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("ATENDIMENTO_INDISPONIVEL"));
    }

    @Test
    void aceitaMesmoHorarioParaRecursosDistintos() throws Exception {
        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());

        var outroClinico = cadastrarProfissional(new MedicoClinicoGeral(), true);
        criar(rotina(cadastrarPaciente(), outroClinico, horario)).andExpect(status().isCreated());
    }

    @Test
    void aceitaOutroHorarioParaOsMesmosRecursos() throws Exception {
        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());

        criar(rotina(paciente, clinico, horario.plusMinutes(30))).andExpect(status().isCreated());
    }

    @Test
    void aceitaNovoAgendamentoAposCancelamento() throws Exception {
        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());
        var consulta = atendimentos.listar().stream()
            .filter(item -> item.getPaciente().getId().equals(paciente))
            .findFirst().orElseThrow();
        mvc.perform(patch("/consultas/{id}/status", consulta.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELADA\"}"))
            .andExpect(status().isOk());

        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());
    }

    @Test
    void recusaProfissionalInativo() throws Exception {
        var inativo = cadastrarProfissional(new MedicoClinicoGeral(), false);
        criar(rotina(paciente, inativo, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("PROFISSIONAL_INCOMPATIVEL"))
            .andExpect(resultado -> assertInstanceOf(ProfissionalIncompativelException.class, resultado.getResolvedException()));
    }

    @Test
    void recusaEspecialidadeDiferenteDaDoMedico() throws Exception {
        var especialista = new MedicoEspecialista();
        especialista.setEspecialidade("Cardiologia");
        var id = cadastrarProfissional(especialista, true);
        criar("""
            {"tipo":"ESPECIALIZADA","pacienteId":%d,"profissionalId":%d,"dataHora":"%s","especialidade":"Neurologia"}
            """.formatted(paciente, id, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("PROFISSIONAL_INCOMPATIVEL"));
    }

    @Test
    void recusaCirurgiaComProfissionalSemPerfilCirurgico() throws Exception {
        criar("""
            {"tipo":"CIRURGICO","pacienteId":%d,"profissionalId":%d,"dataHora":"%s",
             "descricao":"Procedimento de teste","tipoCirurgia":"Ambulatorial"}
            """.formatted(paciente, clinico, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("PROFISSIONAL_INCOMPATIVEL"));
    }

    @Test
    void preservaErroDeDadosDaConsultaQuandoProfissionalECompativel() throws Exception {
        criar("""
            {"tipo":"ROTINA","pacienteId":%d,"profissionalId":%d,"dataHora":"%s","motivo":" "}
            """.formatted(paciente, clinico, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.erro").isString())
            .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    void emergenciaTambemRespeitaAReservaDoPaciente() throws Exception {
        criar(rotina(paciente, clinico, horario)).andExpect(status().isCreated());
        var outroClinico = cadastrarProfissional(new MedicoClinicoGeral(), true);

        criar("""
            {"tipo":"EMERGENCIA","pacienteId":%d,"profissionalId":%d,"dataHora":"%s","prioridade":"ALTA"}
            """.formatted(paciente, outroClinico, horario))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("ATENDIMENTO_INDISPONIVEL"));
    }

    @Test
    void salvaSoUmAgendamentoConcorrenteDoPaciente() throws Exception {
        var outroClinico = cadastrarProfissional(new MedicoClinicoGeral(), true);
        verificarConflitoConcorrente(rotina(paciente, clinico, horario), rotina(paciente, outroClinico, horario));
        assertEquals(1, contarAgendamentos(paciente, horario));
    }

    @Test
    void salvaSoUmAgendamentoConcorrenteDoProfissional() throws Exception {
        var outroPaciente = cadastrarPaciente();
        verificarConflitoConcorrente(rotina(paciente, clinico, horario), rotina(outroPaciente, clinico, horario));
        assertEquals(1, contarAgendamentos(paciente, horario) + contarAgendamentos(outroPaciente, horario));
    }

    private void verificarConflitoConcorrente(String primeiro, String segundo) throws Exception {
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var resultados = Stream.of(primeiro, segundo).map(corpo -> executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                return criar(corpo).andReturn();
            })).toList();

            var respostas = List.of(resultados.get(0).get(10, TimeUnit.SECONDS),
                resultados.get(1).get(10, TimeUnit.SECONDS));
            assertEquals(List.of(201, 422), respostas.stream()
                .map(resposta -> resposta.getResponse().getStatus()).sorted().toList());
        }
    }

    private long contarAgendamentos(Long pacienteId, LocalDateTime dataHora) {
        return atendimentos.listar().stream()
            .filter(atendimento -> atendimento.getPaciente().getId().equals(pacienteId))
            .filter(atendimento -> atendimento.getDataHora().equals(dataHora))
            .count();
    }

    private ResultActions criar(String corpo) throws Exception {
        return mvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String rotina(Long pacienteId, Long profissionalId, LocalDateTime dataHora) {
        return """
            {"tipo":"ROTINA","pacienteId":%d,"profissionalId":%d,"dataHora":"%s","motivo":"Consulta de teste"}
            """.formatted(pacienteId, profissionalId, dataHora);
    }

    private Long cadastrarPaciente() {
        var registro = new Paciente();
        registro.setNome("Paciente de teste");
        registro.setCpf(Long.toString(90000000000L + CADASTROS.incrementAndGet()));
        registro.setDataNascimento(LocalDate.of(1990, 10, 10));
        return pacientes.salvar(registro).getId();
    }

    private Long cadastrarProfissional(ProfissionalSaude profissional, boolean ativo) {
        profissional.setNome("Profissional de teste");
        profissional.setRegistroConselho("CRM-AGENDA-" + CADASTROS.incrementAndGet());
        profissional.setTelefone("31999990000");
        profissional.setAtivo(ativo);
        return profissionais.salvar(profissional).getId();
    }
}
