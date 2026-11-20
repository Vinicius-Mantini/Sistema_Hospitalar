package com.sistemahospitalar.hospital;

import com.jayway.jsonpath.JsonPath;
import com.sistemahospitalar.hospital.model.Cirurgiao;
import com.sistemahospitalar.hospital.model.MedicoClinicoGeral;
import com.sistemahospitalar.hospital.model.Paciente;
import com.sistemahospitalar.hospital.model.ProcedimentoCirurgico;
import com.sistemahospitalar.hospital.model.ProfissionalSaude;
import com.sistemahospitalar.hospital.repository.ProcedimentoCirurgicoRepository;
import com.sistemahospitalar.hospital.service.PacienteService;
import com.sistemahospitalar.hospital.service.ProfissionalSaudeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reserva-cirurgica-teste;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ReservaSalaCirurgicaIntegracaoTest {
    private static final AtomicInteger CADASTROS = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired PacienteService pacientes;
    @Autowired ProfissionalSaudeService profissionais;
    @Autowired ProcedimentoCirurgicoRepository procedimentos;

    private final LocalDateTime inicio = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MINUTES);

    @Test
    void cadastraEListaSalaCirurgica() throws Exception {
        Long id = cadastrarSala();
        mvc.perform(get("/salas-cirurgicas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + id + ")].nome").isNotEmpty());
    }

    @Test
    void apresentaSalaEFimPrevistoNoAtendimentoRegistrado() throws Exception {
        Long sala = cadastrarSala();
        LocalDateTime fim = inicio.plusHours(2);
        var reserva = novaCirurgia(sala, inicio, fim)
            .andExpect(status().isCreated()).andReturn();
        mvc.perform(get("/consultas/{id}", id(reserva)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.salaCirurgica.id").value(sala.intValue()))
            .andExpect(resultado -> {
                String fimPrevisto = JsonPath.read(resultado.getResponse().getContentAsString(), "$.fimPrevisto");
                assertEquals(fim, LocalDateTime.parse(fimPrevisto));
            });
    }

    @Test
    void exigeSalaCirurgicaParaNovaReserva() throws Exception {
        novaCirurgia(null, inicio, inicio.plusHours(2)).andExpect(status().is(422));
    }

    @Test
    void exigeFimPrevistoParaNovaReserva() throws Exception {
        novaCirurgia(cadastrarSala(), inicio, null).andExpect(status().is(422));
    }

    @Test
    void recusaFimIgualOuAnteriorAoInicio() throws Exception {
        Long sala = cadastrarSala();
        novaCirurgia(sala, inicio, inicio).andExpect(status().is(422));
        novaCirurgia(sala, inicio, inicio.minusMinutes(1)).andExpect(status().is(422));
    }

    @Test
    void retornaNaoEncontradoParaSalaInexistente() throws Exception {
        novaCirurgia(Long.MAX_VALUE, inicio, inicio.plusHours(2)).andExpect(status().isNotFound());
    }

    @Test
    void recusaSobreposicaoParcialEIntegralNaMesmaSala() throws Exception {
        Long sala = cadastrarSala();
        novaCirurgia(sala, inicio, inicio.plusHours(2)).andExpect(status().isCreated());

        conflito(novaCirurgia(sala, inicio.plusHours(1), inicio.plusHours(3)));
        conflito(novaCirurgia(sala, inicio.minusHours(1), inicio.plusHours(3)));
    }

    @Test
    void aceitaLimitesAdjacentesNaMesmaSala() throws Exception {
        Long sala = cadastrarSala();
        novaCirurgia(sala, inicio, inicio.plusHours(1)).andExpect(status().isCreated());

        novaCirurgia(sala, inicio.minusHours(1), inicio).andExpect(status().isCreated());
        novaCirurgia(sala, inicio.plusHours(1), inicio.plusHours(2)).andExpect(status().isCreated());
    }

    @Test
    void permiteSobreposicaoEmSalasComEquipesEPacientesDiferentes() throws Exception {
        novaCirurgia(cadastrarSala(), inicio, inicio.plusHours(2)).andExpect(status().isCreated());
        novaCirurgia(cadastrarSala(), inicio.plusHours(1), inicio.plusHours(3)).andExpect(status().isCreated());
    }

    @Test
    void liberaSalaQuandoProcedimentoECancelado() throws Exception {
        Long sala = cadastrarSala();
        MvcResult reserva = novaCirurgia(sala, inicio, inicio.plusHours(2))
            .andExpect(status().isCreated()).andReturn();

        mvc.perform(patch("/consultas/{id}/cirurgia/cancelar", id(reserva))).andExpect(status().isOk());
        novaCirurgia(sala, inicio, inicio.plusHours(2)).andExpect(status().isCreated());
    }

    @Test
    void recusaMesmoCirurgiaoEmPeriodosSobrepostosComSalasDiferentes() throws Exception {
        Long cirurgiao = cadastrarCirurgiao().getId();
        agendar(cirurgia(cadastrarPaciente().getId(), cirurgiao, cadastrarSala(), inicio, inicio.plusHours(2)))
            .andExpect(status().isCreated());
        conflito(agendar(cirurgia(cadastrarPaciente().getId(), cirurgiao, cadastrarSala(),
            inicio.plusHours(1), inicio.plusHours(3))));
    }

    @Test
    void recusaConsultaDentroDoPeriodoCirurgicoDoPaciente() throws Exception {
        Long paciente = cadastrarPaciente().getId();
        agendar(cirurgia(paciente, cadastrarCirurgiao().getId(), cadastrarSala(), inicio, inicio.plusHours(2)))
            .andExpect(status().isCreated());

        Long clinico = cadastrarProfissional(new MedicoClinicoGeral()).getId();
        conflito(agendar("""
            {"tipo":"ROTINA","pacienteId":%d,"profissionalId":%d,"dataHora":"%s","motivo":"Reavaliação"}
            """.formatted(paciente, clinico, inicio.plusHours(1))));
    }

    @Test
    void preservaCirurgiaAntigaComoReservaPontual() throws Exception {
        var antiga = new ProcedimentoCirurgico();
        antiga.setPaciente(cadastrarPaciente());
        antiga.setProfissionalResponsavel(cadastrarCirurgiao());
        antiga.setDataHora(inicio.plusHours(1));
        antiga.setDescricao("Registro anterior à reserva de salas");
        antiga.setTipoCirurgia("Ambulatorial");
        var salva = procedimentos.save(antiga);

        mvc.perform(get("/consultas/{id}", salva.getId())).andExpect(status().isOk());
        conflito(agendar(cirurgia(salva.getPaciente().getId(), cadastrarCirurgiao().getId(),
            cadastrarSala(), inicio, inicio.plusHours(2))));
    }

    @Test
    void duasRequisicoesConcorrentesGeramUmaUnicaReservaDaSala() throws Exception {
        Long sala = cadastrarSala();
        String primeira = cirurgia(cadastrarPaciente().getId(), cadastrarCirurgiao().getId(),
            sala, inicio, inicio.plusHours(2));
        String segunda = cirurgia(cadastrarPaciente().getId(), cadastrarCirurgiao().getId(),
            sala, inicio.plusHours(1), inicio.plusHours(3));
        var sincronizador = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var requisicoes = Stream.of(primeira, segunda).map(corpo -> executor.submit(() -> {
                sincronizador.await(5, TimeUnit.SECONDS);
                return agendar(corpo).andReturn();
            })).toList();

            var respostas = List.of(requisicoes.get(0).get(10, TimeUnit.SECONDS),
                requisicoes.get(1).get(10, TimeUnit.SECONDS));
            assertEquals(List.of(201, 422), respostas.stream()
                .map(resultado -> resultado.getResponse().getStatus()).sorted().toList());
            var rejeitada = respostas.stream()
                .filter(resultado -> resultado.getResponse().getStatus() == 422).findFirst().orElseThrow();
            assertEquals("ATENDIMENTO_INDISPONIVEL",
                JsonPath.read(rejeitada.getResponse().getContentAsString(), "$.codigo"));
        }
    }

    private Long cadastrarSala() throws Exception {
        String corpo = "{\"nome\":\"Sala de teste " + CADASTROS.incrementAndGet() + "\"}";
        return id(mvc.perform(post("/salas-cirurgicas").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated()).andReturn());
    }

    private ResultActions novaCirurgia(Long sala, LocalDateTime comeco, LocalDateTime fim) throws Exception {
        return agendar(cirurgia(cadastrarPaciente().getId(), cadastrarCirurgiao().getId(), sala, comeco, fim));
    }

    private ResultActions agendar(String corpo) throws Exception {
        return mvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private void conflito(ResultActions resposta) throws Exception {
        resposta.andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("ATENDIMENTO_INDISPONIVEL"));
    }

    private String cirurgia(Long paciente, Long cirurgiao, Long sala, LocalDateTime comeco, LocalDateTime fim) {
        String fimJson = fim == null ? "null" : "\"" + fim + "\"";
        return """
            {"tipo":"CIRURGICO","pacienteId":%d,"profissionalId":%d,"salaCirurgicaId":%s,
             "dataHora":"%s","fimPrevisto":%s,"tipoCirurgia":"Ambulatorial","descricao":"Procedimento fictício"}
            """.formatted(paciente, cirurgiao, sala, comeco, fimJson);
    }

    private Long id(MvcResult resposta) throws Exception {
        Number id = JsonPath.read(resposta.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private Paciente cadastrarPaciente() {
        var paciente = new Paciente();
        paciente.setNome("Paciente de teste");
        paciente.setCpf(Long.toString(91000000000L + CADASTROS.incrementAndGet()));
        paciente.setDataNascimento(LocalDate.of(1990, 10, 10));
        return pacientes.salvar(paciente);
    }

    private ProfissionalSaude cadastrarCirurgiao() {
        var cirurgiao = new Cirurgiao();
        cirurgiao.setAreaCirurgica("Ortopedia");
        return cadastrarProfissional(cirurgiao);
    }

    private ProfissionalSaude cadastrarProfissional(ProfissionalSaude profissional) {
        profissional.setNome("Profissional de teste");
        profissional.setRegistroConselho("CRM-RESERVA-" + CADASTROS.incrementAndGet());
        profissional.setTelefone("31999990000");
        return profissionais.salvar(profissional);
    }
}
