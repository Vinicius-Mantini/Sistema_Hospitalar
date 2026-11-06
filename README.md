# Sistema Hospitalar — Sprint 2

Projeto acadêmico de Programação Modular / Engenharia de Software. API REST em Java 21, Spring Boot 4.1.2, Spring Data JPA, Bean Validation e banco H2 em memória.

## Entregas da sprint

- Classes de domínio com **herança `JOINED`** entre profissionais (clínico geral, especialista, cirurgião) e entre atendimentos (rotina, especializada, emergência, cirurgia).
- Enums de estados de consultas, emergência, cirurgia, internação, situação e tipos de quarto.
- Regras validadas no domínio e no serviço, sem permitir alteração arbitrária de estados por POST.
- Cinco grupos de endpoints obrigatórios: `/pacientes`, `/profissionais`, `/consultas`, `/internacoes`, `/quartos`.
- Diagrama atualizado: `diagrama/diagrama-classes.puml`, com versões PNG e SVG.
- Testes unitários (`RegrasDominioTest`) e de integração (`FluxosIntegracaoTest` e `HospitalApplicationTests`).

## Como executar

Requer **JDK 21**. Dentro da pasta que contém `pom.xml`:

```bash
# Linux/Mac
./mvnw clean test
./mvnw spring-boot:run

# Windows (PowerShell ou Prompt)
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

A primeira execução do Maven Wrapper necessita acesso à Internet para baixar o Maven e dependências, caso não estejam em cache. API local: `http://localhost:8080`. Console H2: `http://localhost:8080/h2-console` (URL JDBC `jdbc:h2:mem:hospital`, usuário `sa`, senha em branco). Os registros somem ao reiniciar a aplicação, porque o H2 é **em memória**.

> **Validação:** este pacote foi atualizado por inspeção estática. O ambiente usado para prepará-lo não conseguiu baixar o Maven, portanto não foi possível comprovar a execução da suíte neste ambiente. Execute `./mvnw clean test` antes da entrega definitiva.

## Endpoints

| Método | Caminho | Descrição |
| --- | --- | --- |
| GET/POST | `/pacientes` | Lista / cadastra pacientes |
| GET | `/pacientes/{id}` | Busca paciente |
| GET | `/pacientes/{id}/historico` | IDs dos atendimentos/internações do paciente |
| GET/POST | `/profissionais` | Lista / cadastra profissionais |
| GET | `/profissionais/{id}` | Busca profissional |
| GET | `/profissionais/especialistas?especialidade=Cardiologia` | Especialistas **ativos** da área |
| POST | `/profissionais/{id}/encaminhamentos` | Encaminhamento por clínico geral |
| GET | `/profissionais/{id}/encaminhamentos` | Encaminhamentos registrados pelo clínico |
| GET | `/profissionais/{id}/procedimentos` | Cirurgias associadas ao cirurgião |
| GET/POST | `/consultas` | Lista / cadastra modalidades de atendimento |
| GET | `/consultas/{id}` | Busca atendimento |
| PATCH | `/consultas/{id}/status` | Transição de **consulta** (JSON ou parâmetro `valor`) |
| PATCH | `/consultas/{id}/observacoes` | Registra observações **após conclusão** |
| PATCH | `/consultas/{id}/emergencia/iniciar` | Inicia avaliação de emergência |
| PATCH | `/consultas/{id}/emergencia/concluir` | Conclui emergência iniciada |
| PATCH | `/consultas/{id}/cirurgia/iniciar` | Inicia procedimento e registra horário efetivo |
| PATCH | `/consultas/{id}/cirurgia/concluir` | Conclui cirurgia iniciada |
| PATCH | `/consultas/{id}/cirurgia/cancelar` | Cancela cirurgia ainda não concluída |
| PATCH | `/consultas/{id}/cirurgia/pos-operatorio` | Adiciona observações após concluir cirurgia |
| GET/POST | `/internacoes` | Lista / registra internações |
| GET | `/internacoes/{id}` | Busca internação |
| PATCH | `/internacoes/{id}/alta` | Registra alta e libera leito |
| PATCH | `/internacoes/{id}/cancelar` | Cancela internação ativa e libera leito |
| GET/POST | `/quartos` | Lista / cadastra quartos |
| GET | `/quartos?tipo=UTI` | Filtra por tipo de quarto |
| GET | `/quartos/{id}` | Busca quarto |
| PATCH | `/quartos/{id}/manutencao` | Coloca quarto vazio em manutenção |
| PATCH | `/quartos/{id}/reativar` | Reativa quarto em manutenção |

Todas as rotas acima utilizam JSON quando recebem um corpo. Falhas cadastrais retornam HTTP 400, registros inexistentes 404, inconsistências de negócio 422 e duplicidades de banco 409.

## Exemplos: cadastros e agendamentos

### 1. Criar paciente

```http
POST /pacientes
Content-Type: application/json

{"nome":"Maria Lima","cpf":"12345678900","dataNascimento":"1985-08-20","telefone":"31999999999","email":"maria@email.com"}
```

### 2. Criar profissionais (o `tipo` discrimina a subclasse)

```http
POST /profissionais
Content-Type: application/json

{"tipo":"CLINICO_GERAL","nome":"Dra. Ana","registroConselho":"CRM-MG-101","telefone":"31999999998","email":"ana@email.com"}
```

Especialista:

```json
{"tipo":"ESPECIALISTA","nome":"Dr. Bruno","registroConselho":"CRM-MG-102","telefone":"31999999997","especialidade":"Cardiologia"}
```

Cirurgião:

```json
{"tipo":"CIRURGIAO","nome":"Dra. Carla","registroConselho":"CRM-MG-103","telefone":"31999999996","areaCirurgica":"Ortopedia"}
```

### 3. Criar consulta de rotina

Use os **IDs reais** recebidos ao criar paciente e profissional, e escolha horário futuro.

```http
POST /consultas
Content-Type: application/json

{"tipo":"ROTINA","pacienteId":1,"profissionalId":1,"dataHora":"2027-04-20T14:30:00","motivo":"Avaliação clínica"}
```

Uma consulta especializada usa `tipo: "ESPECIALIZADA"` e `especialidade: "Cardiologia"`, compatível com a área do médico. A criação de emergência usa `tipo: "EMERGENCIA"` e `prioridade: "BAIXA" | "MODERADA" | "ALTA" | "CRITICA"`. A criação de cirurgia usa `tipo: "CIRURGICO"`, `descricao`, `tipoCirurgia`, `observacoesPreOperatorias` (opcional). As observações pós-operatórias são registradas somente após a conclusão via `PATCH /consultas/{id}/cirurgia/pos-operatorio` com `{"observacoes":"..."}`.

**Atenção:** o `POST /consultas` agora utiliza um DTO com `pacienteId` e `profissionalId`, evitando problemas de desserialização e persistência de referências JPA. Ele não recebe entidades aninhadas nos campos `paciente` e `profissionalResponsavel`. O status inicial é controlado pelo servidor e não deve ser informado no cadastro.

### 4. Alterar status da consulta

```http
PATCH /consultas/1/status
Content-Type: application/json

{"status":"CONFIRMADA"}
```

Transições permitidas: `AGENDADA` → `CONFIRMADA` → `EM_ATENDIMENTO` → `CONCLUIDA`; a partir de agendada/confirmada é possível cancelar ou marcar não comparecimento; `CANCELADA`, `CONCLUIDA` e `NAO_COMPARECEU` são terminais. Para cirurgias e emergências, use as rotas específicas, que também sincronizam o estado herdado.

Para observações após conclusão: `PATCH /consultas/1/observacoes` com `{"observacoes":"Evolução favorável."}`.

### 5. Criar quarto e internação

```http
POST /quartos
Content-Type: application/json

{"numero":"A-101","tipo":"APARTAMENTO","capacidade":2}
```

```http
POST /internacoes
Content-Type: application/json

{"pacienteId":1,"quartoId":1,"observacoes":"Acompanhamento clínico"}
```

Para dar alta, execute `PATCH /internacoes/1/alta`. A operação diminui a ocupação do quarto **na mesma transação**. A reserva bloqueia o quarto no banco durante a ocupação. O cliente **não** informa manualmente `leitosOcupados`, `situacao`, `statusProcedimento`, `statusEmergencia` ou o status inicial da consulta.

**Tipos de quarto:** `ENFERMARIA`, `APARTAMENTO` e `UTI` são categorias propostas nesta implementação, pois o PDF da Sprint 2 solicita os *tipos*, mas não especifica uma lista obrigatória nem limites numéricos por categoria. O modelo registra o tipo e aplica regras gerais de capacidade, manutenção, ocupação e alta, sem impor limites arbitrários de leitos por tipo.

## Front-end

`front-end/` preserva as sete páginas HTML/CSS/JS de demonstração existentes no ZIP original. As telas **não estão integradas** à API e seus formulários não persistem dados; a Sprint 2 exige API/modelo, não integração web visual. Para cadastrar e consultar dados reais, utilize os endpoints acima (Postman, Insomnia ou `curl`).

## Estrutura

- `src/main/java/.../model`: entidades, herança, validações e transições.
- `src/main/java/.../repository`: repositórios Spring Data JPA, inclusive consultas de subclasse e bloqueio pessimista de quartos.
- `src/main/java/.../service`: operações transacionais, encaminhamento, histórico, atendimento, alta e manutenção.
- `src/main/java/.../dto`: contratos de criação e atualização, com campos de referência por ID.
- `src/main/java/.../controller`: rotas REST e tratamento de exceções.
- `src/test/java`: testes de domínio, fluxos de integração e carregamento do contexto.
- `diagrama`: diagrama de classes atualizado em PlantUML e representação visual.

As classes e rotas originais foram preservadas na medida do possível; as entradas de criação de atendimento/internação foram tornadas explícitas com DTOs para maior segurança da API.
