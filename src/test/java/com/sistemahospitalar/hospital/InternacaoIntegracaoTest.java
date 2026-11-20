package com.sistemahospitalar.hospital;

import com.jayway.jsonpath.JsonPath;
import com.sistemahospitalar.hospital.exception.InternacaoAtivaException;
import com.sistemahospitalar.hospital.exception.QuartoSemVagasException;
import com.sistemahospitalar.hospital.model.Paciente;
import com.sistemahospitalar.hospital.service.PacienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:internacoes-teste;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class InternacaoIntegracaoTest {
    private static final AtomicInteger CADASTROS = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired PacienteService pacientes;

    @Test
    void recusaNovaInternacaoAtivaMesmoEmOutroQuarto() throws Exception {
        Long paciente = cadastrarPaciente();
        Long quartoInicial = cadastrarQuarto(1);
        Long outroQuarto = cadastrarQuarto(1);
        internar(paciente, quartoInicial);
        mvc.perform(pedidoDeInternacao(paciente, outroQuarto))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("INTERNACAO_ATIVA"))
            .andExpect(resultado -> assertInstanceOf(InternacaoAtivaException.class, resultado.getResolvedException()));
        verificarOcupacao(quartoInicial, 1, "OCUPADO");
        verificarOcupacao(outroQuarto, 0, "DISPONIVEL");
    }

    @Test
    void recusaPacienteDiferenteQuandoOQuartoEstaLotado() throws Exception {
        Long quarto = cadastrarQuarto(1);
        internar(cadastrarPaciente(), quarto);
        verificarSemVagas(mvc.perform(pedidoDeInternacao(cadastrarPaciente(), quarto)));
        verificarOcupacao(quarto, 1, "OCUPADO");
    }

    @Test
    void recusaInternacaoEmQuartoEmManutencaoMesmoComLeitosLivres() throws Exception {
        Long quarto = cadastrarQuarto(2);
        mvc.perform(patch("/quartos/{id}/manutencao", quarto)).andExpect(status().isOk());
        verificarSemVagas(mvc.perform(pedidoDeInternacao(cadastrarPaciente(), quarto)));
        verificarOcupacao(quarto, 0, "MANUTENCAO");
    }

    @Test
    void permiteInternarNovamenteAposAltaEAtualizaDisponibilidade() throws Exception {
        Long paciente = cadastrarPaciente();
        Long quarto = cadastrarQuarto(1);
        Long primeira = internar(paciente, quarto);

        mvc.perform(patch("/internacoes/{id}/alta", primeira))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ALTA"));
        verificarOcupacao(quarto, 0, "DISPONIVEL");
        internar(paciente, quarto);
        verificarOcupacao(quarto, 1, "OCUPADO");
    }

    @Test
    void historicoExibeEntradaSaidaEQuartoDaInternacaoSemPerderOsIds() throws Exception {
        Long paciente = cadastrarPaciente();
        String numero = "HIST-" + CADASTROS.incrementAndGet();
        Long quarto = cadastrarQuarto(numero, 1);
        var entrada = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MINUTES);
        var criada = mvc.perform(post("/internacoes").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"pacienteId":%d,"quartoId":%d,"dataEntrada":"%s"}
                    """.formatted(paciente, quarto, entrada)))
            .andExpect(status().isCreated()).andReturn();
        Long internacao = id(criada);

        mvc.perform(get("/pacientes/{id}/historico", paciente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.internacoesIds[0]").value(internacao.intValue()))
            .andExpect(jsonPath("$.internacoes[0].situacao").value("ATIVA"))
            .andExpect(jsonPath("$.internacoes[0].dataSaida").isEmpty());

        mvc.perform(patch("/internacoes/{id}/alta", internacao)).andExpect(status().isOk());
        MvcResult registro = mvc.perform(get("/internacoes/{id}", internacao))
            .andExpect(status().isOk()).andReturn();
        String alta = JsonPath.read(registro.getResponse().getContentAsString(), "$.dataAlta");
        mvc.perform(get("/pacientes/{id}/historico", paciente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.internacoesIds[0]").value(internacao.intValue()))
            .andExpect(jsonPath("$.internacoes[0].id").value(internacao.intValue()))
            .andExpect(jsonPath("$.internacoes[0].quartoId").value(quarto.intValue()))
            .andExpect(jsonPath("$.internacoes[0].quartoNumero").value(numero))
            .andExpect(jsonPath("$.internacoes[0].situacao").value("ALTA"))
            .andExpect(jsonPath("$.internacoes[0].dataSaida").value(alta))
            .andExpect(resultado -> {
                String dataEntrada = JsonPath.read(resultado.getResponse().getContentAsString(), "$.internacoes[0].dataEntrada");
                assertEquals(entrada, LocalDateTime.parse(dataEntrada));
            });
    }

    @Test
    void cancelamentoLiberaLeitoERegistraSaidaNoHistorico() throws Exception {
        Long paciente = cadastrarPaciente();
        Long quarto = cadastrarQuarto(1);
        Long internacao = internar(paciente, quarto);

        mvc.perform(patch("/internacoes/{id}/cancelar", internacao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("CANCELADA"))
            .andExpect(jsonPath("$.dataAlta").isEmpty());
        verificarOcupacao(quarto, 0, "DISPONIVEL");
        mvc.perform(get("/pacientes/{id}/historico", paciente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.internacoes[0].situacao").value("CANCELADA"))
            .andExpect(jsonPath("$.internacoes[0].dataSaida").isString());
        internar(paciente, quarto);
        verificarOcupacao(quarto, 1, "OCUPADO");
    }

    @Test
    void requisicoesConcorrentesEmQuartosDiferentesCriamUmaSoInternacaoAtivaDoPaciente() throws Exception {
        Long paciente = cadastrarPaciente();
        Long primeiroQuarto = cadastrarQuarto(1);
        Long segundoQuarto = cadastrarQuarto(1);
        assertEquals(List.of(201, 422), executarEmPar(
            pedidoDeInternacao(paciente, primeiroQuarto), pedidoDeInternacao(paciente, segundoQuarto)));
        mvc.perform(get("/pacientes/{id}/historico", paciente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.internacoesIds.length()").value(1));
    }

    @Test
    void pacientesConcorrentesNaoExcedemCapacidadeDoQuarto() throws Exception {
        Long quarto = cadastrarQuarto(1);
        assertEquals(List.of(201, 422), executarEmPar(
            pedidoDeInternacao(cadastrarPaciente(), quarto), pedidoDeInternacao(cadastrarPaciente(), quarto)));
        verificarOcupacao(quarto, 1, "OCUPADO");
    }

    @Test
    void altaECancelamentoConcorrentesDaMesmaInternacaoLiberamApenasUmLeito() throws Exception {
        Long quarto = cadastrarQuarto(2);
        Long internacao = internar(cadastrarPaciente(), quarto);
        Long outraInternacao = internar(cadastrarPaciente(), quarto);

        assertEquals(List.of(200, 422), executarEmPar(
            patch("/internacoes/{id}/alta", internacao), patch("/internacoes/{id}/cancelar", internacao)));
        verificarOcupacao(quarto, 1, "DISPONIVEL");
        mvc.perform(get("/internacoes/{id}", outraInternacao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ATIVA"));
    }

    @Test
    void duasAltasConcorrentesDeInternacoesDiferentesLiberamOsDoisLeitos() throws Exception {
        Long quarto = cadastrarQuarto(2);
        Long primeira = internar(cadastrarPaciente(), quarto);
        Long segunda = internar(cadastrarPaciente(), quarto);

        assertEquals(List.of(200, 200), executarEmPar(
            patch("/internacoes/{id}/alta", primeira), patch("/internacoes/{id}/alta", segunda)));
        verificarOcupacao(quarto, 0, "DISPONIVEL");
    }

    private List<Integer> executarEmPar(MockHttpServletRequestBuilder primeira,
            MockHttpServletRequestBuilder segunda) throws Exception {
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var requisicoes = Stream.of(primeira, segunda).map(pedido -> executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                return mvc.perform(pedido).andReturn().getResponse().getStatus();
            })).toList();

            return Stream.of(requisicoes.get(0).get(10, TimeUnit.SECONDS),
                requisicoes.get(1).get(10, TimeUnit.SECONDS)).sorted().toList();
        }
    }

    private Long internar(Long paciente, Long quarto) throws Exception {
        return id(mvc.perform(pedidoDeInternacao(paciente, quarto)).andExpect(status().isCreated()).andReturn());
    }

    private MockHttpServletRequestBuilder pedidoDeInternacao(Long paciente, Long quarto) {
        return post("/internacoes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"pacienteId\":%d,\"quartoId\":%d}".formatted(paciente, quarto));
    }

    private void verificarSemVagas(ResultActions resposta) throws Exception {
        resposta.andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("QUARTO_SEM_VAGAS"))
            .andExpect(resultado -> assertInstanceOf(QuartoSemVagasException.class, resultado.getResolvedException()));
    }

    private void verificarOcupacao(Long quarto, int ocupacao, String situacao) throws Exception {
        mvc.perform(get("/quartos/{id}", quarto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.leitosOcupados").value(ocupacao))
            .andExpect(jsonPath("$.situacao").value(situacao));
    }

    private Long cadastrarQuarto(int capacidade) throws Exception {
        return cadastrarQuarto("Q-TESTE-" + CADASTROS.incrementAndGet(), capacidade);
    }

    private Long cadastrarQuarto(String numero, int capacidade) throws Exception {
        return id(mvc.perform(post("/quartos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"%s\",\"capacidade\":%d,\"tipo\":\"ENFERMARIA\"}".formatted(numero, capacidade)))
            .andExpect(status().isCreated()).andReturn());
    }

    private Long cadastrarPaciente() {
        var paciente = new Paciente();
        paciente.setNome("Paciente de teste");
        paciente.setCpf(Long.toString(93000000000L + CADASTROS.incrementAndGet()));
        paciente.setDataNascimento(LocalDate.of(1990, 10, 10));
        return pacientes.salvar(paciente).getId();
    }

    private Long id(MvcResult resposta) throws Exception {
        Number id = JsonPath.read(resposta.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
