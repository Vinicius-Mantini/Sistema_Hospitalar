# Refatoração UI/UX — Hospital SIH · Sprint 2

## O que foi entregue

- `front-end/consultas.html`: interface corporativa e responsiva de consultas/atendimentos.
- `front-end/css/consultas.css`: identidade visual exclusiva da página de consultas (Inter, azul, branco, cinza, cartões, estados e mobile).
- `front-end/js-consultas.js`: **novo arquivo** de integração com a API Spring Boot. O `js-app.js` original foi mantido integralmente.
- `front-end/css/style.css`: camada de melhorias visuais conservadoras nas demais telas (nenhum identificador HTML ou contrato JS é alterado).
- `src/main/resources/static/`: cópia das páginas e assets do front-end para **servir tudo pela mesma origem do Spring Boot**, evitando bloqueios de CORS.

## Como iniciar a página

1. Execute o back-end Spring Boot normalmente (por exemplo, na IDE ou com `./mvnw spring-boot:run`, caso o Maven Wrapper esteja disponível na sua máquina).
2. Abra **http://localhost:8080/consultas.html** no navegador.
3. Cadastre pacientes e profissionais na API, se ainda não existirem registros. O banco H2 em memória reinicia quando a aplicação é reiniciada.
4. Selecione uma modalidade: os profissionais ativos compatíveis e os campos exigidos serão exibidos automaticamente.
5. Cadastre a consulta. Na tabela, clique em **Gerenciar** para acessar apenas as transições válidas.

> **Importante:** abrir `front-end/consultas.html` diretamente com `file://` ou iniciar um servidor front-end em porta diferente de 8080 não garante a comunicação com a API; o back-end fornecido não declara CORS. Por isso a pasta `src/main/resources/static/` faz parte da entrega. Acesse via Spring Boot.

## Endpoints utilizados (já implementados no Java)

| Método | Rota | Uso |
|---|---|---|
| `GET` | `/pacientes` | Popular lista de pacientes |
| `GET` | `/profissionais` | Filtrar profissionais ativos conforme modalidade |
| `GET` | `/consultas` | Popular tabela, indicadores e filtros |
| `POST` | `/consultas` | Cadastrar as quatro modalidades |
| `PATCH` | `/consultas/{id}/status` | Transições de rotina e especializada |
| `PATCH` | `/consultas/{id}/observacoes` | Observações após consulta concluída |
| `PATCH` | `/consultas/{id}/emergencia/iniciar` | Iniciar emergência |
| `PATCH` | `/consultas/{id}/emergencia/concluir` | Concluir emergência iniciada |
| `PATCH` | `/consultas/{id}/cirurgia/iniciar` | Iniciar cirurgia |
| `PATCH` | `/consultas/{id}/cirurgia/concluir` | Concluir cirurgia iniciada |
| `PATCH` | `/consultas/{id}/cirurgia/cancelar` | Cancelar cirurgia quando permitido |
| `PATCH` | `/consultas/{id}/cirurgia/pos-operatorio` | Observações pós-operatórias após conclusão |

**Payload de exemplo para agendamento de rotina:**

```json
{"tipo":"ROTINA","pacienteId":1,"profissionalId":1,"dataHora":"2026-12-10T09:30","motivo":"Avaliação clínica"}
```

Os valores enviados para o campo `tipo` são os enums da API: `ROTINA`, `ESPECIALIZADA`, `EMERGENCIA`, `CIRURGICO` (o usuário vê as quatro descrições completas em português). As seis situações mapeadas são `AGENDADA`, `CONFIRMADA`, `EM_ATENDIMENTO`, `CONCLUIDA`, `CANCELADA`, `NAO_COMPARECEU`.

## Cuidados de compatibilidade

- A página `consultas.html` original **não possuía `id` ou `name` em seus controles**. Todos os identificadores agora existentes são novos; não há identificadores pré-existentes removidos ou renomeados.
- `js-app.js` do ZIP original **não fazia nenhuma chamada HTTP**: o formulário antigo só disparava um toast e reset. Foi mantido byte a byte, enquanto a tela nova usa `js-consultas.js`, que contém as chamadas `fetch` reais.
- Em `consultas.html` o atributo `data-demo-form` foi retirado propositalmente para que o **handler de demonstração** do `js-app.js` não intercepte o envio real do formulário. Nenhum handler funcional prévio foi alterado.
- Não foram modificados os arquivos Java, DTOs, enums, controllers, serviços, repositórios, configuração Spring ou `pom.xml`.
- Demais páginas `index.html`, `pacientes.html`, `profissionais.html`, `internacoes.html`, `quartos.html` e `historico.html` continuam com seu HTML e comportamentos demonstrativos originais; apenas recebem melhorias estéticas através do CSS global.
- Sem registros cadastrados, os seletores ficam desabilitados e a tabela exibe um estado vazio real — **nenhum paciente, profissional ou consulta fictícios são inseridos na API**.
- O estilo utiliza **CSS customizado**, não Tailwind via CDN, evitando dependência do Tailwind em produção. A fonte Inter é carregada do Google Fonts quando disponível, com fallback de sistema.

## Verificações realizadas

- `node --check` no JavaScript novo: aprovado.
- Testes no navegador Chromium via Playwright com **API simulada**: aprovado, sem erros de execução de scripts.
- Casos verificados: quatro modalidades, preenchimento automático de especialidade, mapeamentos `POST`, atualização `PATCH /status`, conclusão de emergência, observações pós-operatórias, filtros de situação, seis badges, e impedimento visual de ações em consultas canceladas.
- Screenshots de desktop (1440 px) e mobile (390 px) gerados e inspecionados; não há overflow horizontal no documento mobile (a tabela possui rolagem horizontal interna proposital).
- Os contratos foram conferidos contra os DTOs/controladores do projeto e o PDF Sprint 2.

**Limite dos testes:** não foi realizado um teste de integração contra uma instância Java/H2 em execução no ambiente de entrega. O Maven Wrapper enviado não inclui o arquivo `.mvn/wrapper/maven-wrapper.jar` e a ferramenta Maven não estava instalada; os testes HTTP feitos no navegador usaram respostas controladas. Portanto a integração foi implementada conforme os contratos existentes, mas não é correto afirmar que foi comprovada contra o banco/servidor real.
